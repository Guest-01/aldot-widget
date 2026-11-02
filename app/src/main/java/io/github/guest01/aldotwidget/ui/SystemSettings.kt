package io.github.guest01.aldotwidget.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * 배터리 최적화 예외("제한 없음")를 시스템 요청 창으로 받을지.
 * - true: "백그라운드에서 계속 실행되도록 허용할까요?" 창을 바로 띄운다. APK 직접 배포용.
 * - false: 앱 정보 화면을 열어 사용자가 배터리 → 제한 없음을 고르게 한다.
 *   Play 스토어는 요청 창을 메신저·알람 같은 일부 앱에만 허용하므로 Play에 올릴 때는 false로 바꾼다.
 */
const val USE_SYSTEM_BATTERY_REQUEST = true

/**
 * 밤에도 로그인을 유지하려면 필요하다. 절전 중에는 앱의 통신이 막히는데, 예외 앱은 막히지 않는다(실기기 확인).
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** 배터리 예외를 요청하는 화면의 Intent. 요청 창을 열 수 없는 기기면 앱 정보 화면으로 대신한다. */
@SuppressLint("BatteryLife") // 의도적으로 요청 창을 쓴다. USE_SYSTEM_BATTERY_REQUEST 설명 참고
fun batteryExemptionIntent(context: Context): Intent {
    val packageUri = Uri.fromParts("package", context.packageName, null)
    if (USE_SYSTEM_BATTERY_REQUEST) {
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
        if (request.resolveActivity(context.packageManager) != null) return request
    }
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
}

/** 이 앱의 알림 설정 화면. 알림 권한을 거절한 뒤에는 앱이 다시 물어볼 수 없어서 여기로 보낸다. */
fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

/** 이 앱의 "앱 정보" 화면. 삼성은 여기서 배터리 → 제한 없음을 고를 수 있다. */
fun openAppDetailsSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        )
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
