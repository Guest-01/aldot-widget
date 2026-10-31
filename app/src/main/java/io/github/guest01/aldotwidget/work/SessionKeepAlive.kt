package io.github.guest01.aldotwidget.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import io.github.guest01.aldotwidget.AldotApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 로그인 유지용 안전망 알람.
 *
 * WorkManager 작업은 폰이 절전 모드(Doze)에 들어가면 몇 시간씩 밀린다(실측 2시간 37분). 로그인은 2시간이면 풀리므로
 * 절전 중에도 울리는 setAndAllowWhileIdle 알람으로 연장한다. 정확한 시각을 보장하지 않는 대신 특별한 권한이 필요 없고,
 * 시스템이 앱당 약 9분에 1번으로 제한한다. 폰 전체의 절전을 깨지 않고 이 앱만 잠깐 실행된다.
 *
 * 같은 PendingIntent로 다시 맞추면 이전 알람이 대체되므로, 연장할 때마다 뒤로 밀려서 평소에는 울리지 않는다.
 * 재부팅하면 알람이 사라지지만 앱 프로세스가 뜰 때 AldotApp이 다시 맞춘다.
 */
class SessionKeepAlive(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private fun pendingIntent() = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, KeepAliveReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun arm(delayMillis: Long) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delayMillis,
            pendingIntent(),
        )
        Log.i(TAG, "keep-alive alarm armed: in ${delayMillis / 60_000} min")
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
        Log.i(TAG, "keep-alive alarm cancelled")
    }

    private companion object {
        const val TAG = "AldotSync"
    }
}

/** 알람이 울리면 시스템이 부르는 수신기. 앱 안에서만 쓰므로 매니페스트에서 exported=false로 등록한다. */
class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 수신기는 원래 곧바로 끝나야 한다. goAsync는 네트워크 요청이 끝날 때까지 프로세스를 잠시 살려 둔다.
        val pending = goAsync()
        val sync = (context.applicationContext as AldotApp).container.usageSync
        CoroutineScope(Dispatchers.Default).launch {
            try {
                withTimeoutOrNull(TIMEOUT_MS) { sync.keepSessionAlive() }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
