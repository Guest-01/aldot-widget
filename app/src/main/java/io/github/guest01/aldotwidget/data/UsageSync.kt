package io.github.guest01.aldotwidget.data

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import io.github.guest01.aldotwidget.data.api.AldotException
import io.github.guest01.aldotwidget.widget.UsageWidget
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * "조회 → 저장 → 위젯 다시 그리기"를 한 번에 하는 곳.
 * 앱 화면의 새로고침, 위젯 버튼, (3단계) 주기 작업이 모두 이 함수를 부른다.
 */
class UsageSync(
    private val context: Context,
    private val repository: UsageRepository,
    private val store: UsageStore,
) {
    // 화면과 위젯 버튼이 동시에 눌려도 조회는 한 번에 하나씩
    private val mutex = Mutex()

    suspend fun refresh(): SyncStatus = mutex.withLock {
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
        Log.i(TAG, "refresh: $status")
        updateWidgets()
        status
    }

    /** 로그아웃: 개인정보인 마지막 사용량도 지우고 "로그인 필요" 상태로 둔다. */
    suspend fun markLoggedOut() {
        store.update { UsageState(status = SyncStatus.NEED_LOGIN) }
        updateWidgets()
    }

    private suspend fun updateWidgets() {
        runCatching { UsageWidget().updateAll(context) }
            .onFailure { Log.w(TAG, "widget update failed", it) }
    }

    private companion object {
        const val TAG = "AldotSync"
    }
}
