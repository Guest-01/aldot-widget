package io.github.guest01.aldotwidget.data

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import io.github.guest01.aldotwidget.data.api.AldotException
import io.github.guest01.aldotwidget.widget.UsageWidget
import io.github.guest01.aldotwidget.work.RefreshScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * 백그라운드 갱신에서 로그인이 처음 만료됐을 때만 알린다.
 * 사용자가 앱·위젯을 보고 있을 때(fromBackground=false)는 화면에 이미 "로그인 필요"가 보이므로 보내지 않고,
 * 이미 만료 상태였다면 같은 알림을 반복하지 않는다.
 */
internal fun shouldNotifyNeedLogin(previous: SyncStatus, current: SyncStatus, fromBackground: Boolean) =
    fromBackground && current == SyncStatus.NEED_LOGIN && previous != SyncStatus.NEED_LOGIN

/**
 * "조회 → 저장 → 위젯 다시 그리기"를 한 번에 하는 곳.
 * 앱 화면의 새로고침, 위젯 버튼, 자동 갱신(주기 작업)이 모두 이 함수를 부른다.
 * 결과에 따라 자동 갱신을 등록·취소하고, 필요하면 재로그인 알림을 보낸다.
 */
class UsageSync(
    private val context: Context,
    private val repository: UsageRepository,
    private val store: UsageStore,
    private val scheduler: RefreshScheduler,
    private val notifier: Notifier,
    private val refreshLog: RefreshLog,
) {
    // 화면과 위젯 버튼이 동시에 눌려도 조회는 한 번에 하나씩
    private val mutex = Mutex()

    /** @param fromBackground 자동 갱신(주기 작업)에서 불렸는지 */
    suspend fun refresh(fromBackground: Boolean = false): SyncStatus = mutex.withLock {
        val previous = store.state.first().status
        val status = try {
            val snapshot = repository.fetchUsage()
            store.update { UsageState(snapshot = snapshot, status = SyncStatus.OK) }
            SyncStatus.OK
        } catch (e: AldotException.NeedLogin) {
            // 토큰 만료: 마지막 값은 남겨 두고 상태만 바꾼다
            store.update { it.copy(status = SyncStatus.NEED_LOGIN, errorMessage = null) }
            SyncStatus.NEED_LOGIN
        } catch (e: IOException) {
            store.update { it.copy(status = SyncStatus.ERROR, errorMessage = e.message ?: "네트워크 오류") }
            SyncStatus.ERROR
        }
        Log.i(TAG, "refresh: $status (background=$fromBackground, previous=$previous)")
        refreshLog.append(status, fromBackground)

        updateWidgets()
        when (status) {
            SyncStatus.OK -> {
                notifier.cancelNeedLogin()
                // 앱·위젯에서 조회에 성공하면 자동 갱신이 등록돼 있는지 확인한다.
                // 주기 작업 자신이 실행될 때는 건드리지 않는다.
                if (!fromBackground) scheduler.ensureScheduled()
            }
            SyncStatus.NEED_LOGIN -> {
                if (shouldNotifyNeedLogin(previous, status, fromBackground)) notifier.notifyNeedLogin()
                // 반드시 마지막에: 주기 작업 안에서 불렸다면 자기 자신이 취소되어 이후 코드가 중단된다
                scheduler.cancel()
            }
            else -> Unit
        }
        status
    }

    /** 로그아웃: 개인정보인 마지막 사용량도 지우고 "로그인 필요" 상태로 둔다. */
    suspend fun markLoggedOut() {
        notifier.cancelNeedLogin()
        store.update { UsageState(status = SyncStatus.NEED_LOGIN) }
        updateWidgets()
        scheduler.cancel()
    }

    private suspend fun updateWidgets() {
        runCatching { UsageWidget().updateAll(context) }
            .onFailure { Log.w(TAG, "widget update failed", it) }
    }

    private companion object {
        const val TAG = "AldotSync"
    }
}
