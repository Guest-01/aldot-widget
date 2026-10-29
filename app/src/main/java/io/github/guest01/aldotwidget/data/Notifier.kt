package io.github.guest01.aldotwidget.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.guest01.aldotwidget.MainActivity
import io.github.guest01.aldotwidget.R

/**
 * 시스템 알림. Android 8+는 알림마다 "채널"이 있어야 하고, 사용자는 채널별로 알림을 끄거나 켤 수 있다.
 * Android 13+는 알림 권한(POST_NOTIFICATIONS)을 사용자에게 받아야 보낼 수 있다.
 */
class Notifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // canNotify()로 확인했다
    fun notifyNeedLogin() {
        if (!canNotify()) return
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_LOGIN, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("로그인 알림")
                .setDescription("자동 갱신 중 다시 로그인이 필요해지면 알려 줘요")
                .build()
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_LOGIN)
            .setSmallIcon(R.drawable.ic_stat_usage)
            .setContentTitle("알닷 로그인이 만료됐어요")
            .setContentText("위젯을 계속 갱신하려면 다시 로그인해 주세요")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(ID_NEED_LOGIN, notification)
    }

    fun cancelNeedLogin() = manager.cancel(ID_NEED_LOGIN)

    private companion object {
        const val CHANNEL_LOGIN = "login"
        const val ID_NEED_LOGIN = 1
    }
}
