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
import io.github.guest01.aldotwidget.update.UpdateInfo
import io.github.guest01.aldotwidget.update.UpdateText

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
                .setName("로그인 만료 알림")
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
            .setColor(ICON_COLOR)
            .setContentTitle("알닷 로그인이 만료됐어요")
            .setContentText("위젯을 계속 갱신하려면 다시 로그인해 주세요")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(ID_NEED_LOGIN, notification)
    }

    fun cancelNeedLogin() = manager.cancel(ID_NEED_LOGIN)

    /**
     * 업데이트 알림 채널. 확인 시각이 정해져 있지 않아 밤에도 올 수 있어서 소리·진동 없이 알림창에만 쌓인다.
     * 사용자는 이 종류만 따로 끌 수 있다.
     */
    private fun createUpdateChannel() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_UPDATE, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("업데이트 알림")
                .setDescription("새 버전이 나왔을 때와 업데이트가 끝났을 때 알려 줘요")
                .build()
        )
    }

    /** 누르면 앱이 열리고 업데이트 창이 바로 뜬다 */
    @SuppressLint("MissingPermission") // canNotify()로 확인했다
    fun notifyUpdateAvailable(info: UpdateInfo) {
        if (!canNotify()) return
        createUpdateChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE)
            .setSmallIcon(R.drawable.ic_stat_usage)
            .setColor(ICON_COLOR)
            .setContentTitle("새 버전 ${UpdateText.subject(info.version)} 나왔어요")
            .setContentText("눌러서 바뀐 점을 보고 설치해요")
            .setContentIntent(openApp(MainActivity.EXTRA_OPEN_UPDATE))
            .setAutoCancel(true)
            .build()
        manager.notify(ID_UPDATE, notification)
    }

    @SuppressLint("MissingPermission") // canNotify()로 확인했다
    fun notifyUpdated(versionName: String) {
        if (!canNotify()) return
        createUpdateChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE)
            .setSmallIcon(R.drawable.ic_stat_usage)
            .setColor(ICON_COLOR)
            .setContentTitle("${UpdateText.toward(versionName)} 업데이트했어요")
            .setContentText("로그인과 설정은 그대로예요")
            .setContentIntent(openApp(null))
            .setAutoCancel(true)
            .build()
        manager.notify(ID_UPDATE, notification)
    }

    private fun openApp(extra: String?): PendingIntent = PendingIntent.getActivity(
        context,
        if (extra == null) 0 else 1,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { if (extra != null) putExtra(extra, true) },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val CHANNEL_LOGIN = "login"
        const val CHANNEL_UPDATE = "update"
        const val ID_NEED_LOGIN = 1
        const val ID_UPDATE = 2
        /** 알림창에서 아이콘 뒤 동그라미 색. 앱 아이콘 바탕과 같은 파랑 (고른 테마 색과 상관없이). 폰에 따라 무시하기도 한다 */
        const val ICON_COLOR = 0xFF1B64DA.toInt()
    }
}
