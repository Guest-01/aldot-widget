package io.github.guest01.aldotwidget.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import io.github.guest01.aldotwidget.AldotApp
import io.github.guest01.aldotwidget.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 시스템이 보내는 설치 결과. 성공하면 앱이 먼저 끝나서 보통 오지 않고,
 * 확인 창이 필요하거나(드묾) 실패했을 때 온다.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val updates = (context.applicationContext as AldotApp).container.updates
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Log.i("InstallResult", "status=$status message=$message")
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // 조건이 안 맞으면 Android 설치 확인 창을 띄워 달라고 한다
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm != null) {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    updates.onUserActionRequested()
                } else {
                    updates.onInstallFailed(status, message)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> updates.onInstallFailed(status, message)
        }
    }
}

/** 이 앱이 새 버전으로 바뀐 직후 시스템이 한 번 보낸다 (MY_PACKAGE_REPLACED) */
class UpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val updates = (context.applicationContext as AldotApp).container.updates
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updates.onPackageReplaced(BuildConfig.VERSION_NAME)
            } finally {
                pending.finish()
            }
        }
    }
}
