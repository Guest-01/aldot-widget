package io.github.guest01.aldotwidget.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.glance.appwidget.updateAll
import io.github.guest01.aldotwidget.data.api.AldotException
import io.github.guest01.aldotwidget.widget.UsageWidget
import io.github.guest01.aldotwidget.work.RefreshScheduler
import io.github.guest01.aldotwidget.work.RefreshWorker
import io.github.guest01.aldotwidget.work.SessionKeepAlive
import kotlinx.coroutines.delay
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
 * "조회 → 저장 → 위젯 다시 그리기"와 로그인 유지를 맡는 곳.
 * 앱 화면의 새로고침, 위젯 버튼, 자동 갱신(주기 작업), 안전망 알람이 모두 여기를 거친다.
 * 결과에 따라 자동 갱신과 알람을 등록·취소하고, 필요하면 재로그인 알림을 보낸다.
 */
class UsageSync(
    private val context: Context,
    private val repository: UsageRepository,
    private val store: UsageStore,
    private val settings: SettingsStore,
    private val scheduler: RefreshScheduler,
    private val keepAlive: SessionKeepAlive,
    private val notifier: Notifier,
    private val refreshLog: RefreshLog,
) {
    // 화면과 위젯 버튼이 동시에 눌려도 조회는 한 번에 하나씩
    private val mutex = Mutex()

    /** @param fromBackground 자동 갱신(주기 작업)에서 불렸는지 */
    suspend fun refresh(fromBackground: Boolean = false): SyncStatus = mutex.withLock {
        if (settings.demoMode()) return@withLock demoRefresh()
        val previous = store.state.first().status
        val status = try {
            extendSessionIfDue()
            val snapshot = repository.fetchUsage()
            store.update {
                it.copy(snapshot = snapshot, status = SyncStatus.OK, errorMessage = null, refreshingSince = null)
            }
            SyncStatus.OK
        } catch (e: AldotException.NeedLogin) {
            // 토큰 만료: 마지막 값은 남겨 두고 상태만 바꾼다
            store.update { it.copy(status = SyncStatus.NEED_LOGIN, errorMessage = null, refreshingSince = null) }
            SyncStatus.NEED_LOGIN
        } catch (e: IOException) {
            store.update {
                it.copy(status = SyncStatus.ERROR, errorMessage = e.message ?: "네트워크 오류", refreshingSince = null)
            }
            SyncStatus.ERROR
        }
        Log.i(TAG, "refresh: $status (background=$fromBackground, previous=$previous)")
        refreshLog.append("${if (fromBackground) "background" else "foreground"} $status")

        updateWidgets()
        when (status) {
            SyncStatus.OK -> {
                notifier.cancelNeedLogin()
                // 앱·위젯에서 조회에 성공하면 자동 갱신이 등록돼 있는지 확인한다.
                // 주기 작업 자신이 실행될 때는 건드리지 않는다.
                if (!fromBackground) scheduler.ensureScheduled()
            }
            SyncStatus.NEED_LOGIN -> onSessionLost(notify = shouldNotifyNeedLogin(previous, status, fromBackground))
            else -> Unit
        }
        status
    }

    /**
     * 위젯·앱의 새로고침 버튼. 1분에 한 번만 받는다(RefreshPolicy).
     * 받으면 "갱신 중"으로 표시해서 위젯과 앱 화면에 같이 보이게 한다.
     *
     * @param viaWorker true(위젯): 조회를 백그라운드 작업에 맡긴다 (위젯 콜백은 오래 실행할 수 없다).
     *                  false(앱 화면): 여기서 바로 조회한다.
     * @return 요청을 받았는지 (1분 제한에 걸리면 false)
     */
    suspend fun requestRefresh(viaWorker: Boolean): Boolean {
        val now = System.currentTimeMillis()
        if (!RefreshPolicy.canRequest(store.state.first().lastRequestedAt, now)) return false
        if (!isOnline()) {
            // 작업이 네트워크를 기다리는 동안 "갱신 중"이 계속 보이지 않도록 바로 실패로 표시한다
            store.update { it.copy(status = SyncStatus.ERROR, errorMessage = "인터넷에 연결되어 있지 않아요") }
            updateWidgets()
            return true
        }
        store.update { it.copy(refreshingSince = now, lastRequestedAt = now) }
        updateWidgets()
        if (viaWorker) RefreshWorker.enqueueNow(context) else refresh()
        return true
    }

    private fun isOnline(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** 알람 처리가 제한 시간을 넘겼을 때. 취소되면 아래 기록이 남지 않으므로 따로 남긴다. */
    suspend fun recordAlarmTimeout() = refreshLog.append("alarm TIMEOUT")

    /** 안전망 알람이 울렸을 때: 사용량은 조회하지 않고 로그인만 연장한다 (요청 1개). */
    suspend fun keepSessionAlive(): Unit = mutex.withLock {
        if (settings.demoMode()) return@withLock
        // 절전 중에 통신이 막혀 있는지 기록해 둔다 (BLOCKED면 알람은 울렸지만 네트워크를 못 쓰는 상태)
        val network = context.getSystemService(ConnectivityManager::class.java).activeNetworkInfo?.detailedState
        refreshLog.append("alarm fired net=$network")
        val previous = store.state.first().status
        if (previous == SyncStatus.NEED_LOGIN) {
            keepAlive.cancel()
            return@withLock
        }
        try {
            repository.extendSession()
            onSessionExtended()
            refreshLog.append("alarm extend OK")
        } catch (e: AldotException.NeedLogin) {
            refreshLog.append("alarm NEED_LOGIN")
            store.update { it.copy(status = SyncStatus.NEED_LOGIN, errorMessage = null) }
            updateWidgets()
            onSessionLost(notify = true)
        } catch (e: IOException) {
            refreshLog.append("alarm ERROR ${e.javaClass.simpleName}")
            val now = System.currentTimeMillis()
            if (SessionPolicy.shouldRetryAlarm(settings.sessionExtendedAt(), now)) {
                keepAlive.arm(SessionPolicy.ALARM_RETRY_MILLIS)
            }
        }
    }

    /** 앱 프로세스가 뜰 때 알람을 다시 맞춘다. 재부팅하면 알람이 사라지기 때문이다. */
    suspend fun ensureKeepAliveArmed() {
        val extendedAt = settings.sessionExtendedAt()
        if (extendedAt == 0L || store.state.first().status == SyncStatus.NEED_LOGIN) return
        val dueIn = extendedAt + SessionPolicy.nextAlarmDelayMillis() - System.currentTimeMillis()
        keepAlive.arm(dueIn.coerceAtLeast(MIN_ALARM_DELAY_MILLIS))
    }

    /** 로그아웃: 개인정보인 마지막 사용량도 지우고 "로그인 필요" 상태로 둔다. */
    suspend fun markLoggedOut() {
        notifier.cancelNeedLogin()
        store.update { UsageState(status = SyncStatus.NEED_LOGIN) }
        updateWidgets()
        onSessionLost(notify = false)
    }

    /** 마지막 연장 후 시간이 지났으면 조회 전에 먼저 연장한다. 네트워크 오류는 뒤이은 조회 결과에 맡긴다. */
    private suspend fun extendSessionIfDue() {
        if (!SessionPolicy.shouldExtend(settings.sessionExtendedAt(), System.currentTimeMillis())) return
        try {
            repository.extendSession()
            onSessionExtended()
            refreshLog.append("extend OK")
        } catch (e: AldotException.NeedLogin) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "extend failed: ${e.message}")
            refreshLog.append("extend ERROR ${e.javaClass.simpleName}")
        }
    }

    private suspend fun onSessionExtended() {
        settings.setSessionExtendedAt(System.currentTimeMillis())
        keepAlive.arm(SessionPolicy.nextAlarmDelayMillis())
    }

    private suspend fun onSessionLost(notify: Boolean) {
        settings.setSessionExtendedAt(0)
        keepAlive.cancel()
        if (notify) notifier.notifyNeedLogin()
        // 반드시 마지막에: 주기 작업 안에서 불렸다면 자기 자신이 취소되어 이후 코드가 중단된다
        scheduler.cancel()
    }

    /** 샘플 데이터 모드의 새로고침: 서버에 묻지 않고 잠깐 기다린 뒤 가짜 데이터의 시각만 새로 바꾼다. */
    private suspend fun demoRefresh(): SyncStatus {
        delay(DEMO_REFRESH_MILLIS)
        store.update {
            it.copy(snapshot = sampleSnapshot(), status = SyncStatus.OK, errorMessage = null, refreshingSince = null)
        }
        refreshLog.append("demo OK")
        updateWidgets()
        return SyncStatus.OK
    }

    /** 테마 색처럼 사용량이 아닌 것이 바뀌었을 때도 홈 화면의 위젯을 다시 그린다. */
    suspend fun redrawWidgets() = updateWidgets()

    private suspend fun updateWidgets() {
        runCatching { UsageWidget().updateAll(context) }
            .onFailure { Log.w(TAG, "widget update failed", it) }
    }

    private companion object {
        const val TAG = "AldotSync"
        const val MIN_ALARM_DELAY_MILLIS = 60_000L
        const val DEMO_REFRESH_MILLIS = 1_200L
    }
}
