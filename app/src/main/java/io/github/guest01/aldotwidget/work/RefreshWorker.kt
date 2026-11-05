package io.github.guest01.aldotwidget.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.guest01.aldotwidget.AldotApp
import io.github.guest01.aldotwidget.data.SyncStatus

/**
 * WorkManager가 실행하는 백그라운드 조회 작업. 앱 화면이 없어도, 앱 프로세스가 죽어 있어도 시스템이 띄워서 실행한다.
 * 위젯 버튼(일회성)과 자동 갱신(주기, RefreshScheduler)이 같은 작업을 쓴다.
 */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as AldotApp).container
        val sync = container.usageSync
        val fromBackground = inputData.getBoolean(KEY_FROM_BACKGROUND, false)
        val status = sync.refresh(fromBackground = fromBackground)
        // 자동 갱신에 얹어 하루 한 번 새 버전을 확인한다 (실패해도 갱신 결과와 상관없다)
        if (fromBackground) container.updates.checkIfDue()
        return when (status) {
            // 네트워크 오류는 잠시 뒤 두 번까지 다시 시도 (WorkManager가 간격을 늘려 가며 재실행)
            SyncStatus.ERROR -> if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    companion object {
        /** 자동 갱신에서 실행됐는지. 사용자가 보고 있지 않을 때만 재로그인 알림을 보낸다. */
        const val KEY_FROM_BACKGROUND = "from_background"

        private const val UNIQUE_REFRESH_NOW = "usage-refresh-now"
        private const val MAX_RETRIES = 2

        /** 지금 한 번 조회. 이미 예약돼 있으면 새로 만들지 않는다 (버튼 연타 방지). */
        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_REFRESH_NOW, ExistingWorkPolicy.KEEP, request)
        }
    }
}
