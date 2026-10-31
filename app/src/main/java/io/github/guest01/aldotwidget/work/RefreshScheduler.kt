package io.github.guest01.aldotwidget.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.guest01.aldotwidget.data.SettingsStore
import java.util.concurrent.TimeUnit

/**
 * 자동 갱신(주기 작업) 등록·취소.
 *
 * WorkManager는 등록된 작업을 자체 DB에 저장해서 재부팅·앱 업데이트 뒤에도 다시 예약한다.
 * 이름을 붙인 고유 작업(enqueueUniquePeriodicWork)이라 몇 번을 불러도 하나만 유지된다.
 */
class RefreshScheduler(
    private val context: Context,
    private val settings: SettingsStore,
) {
    /** 설정된 주기로 등록한다. 이미 있으면 주기·조건만 갱신하고 예약 시점은 유지한다 (UPDATE). */
    suspend fun ensureScheduled() {
        val interval = settings.current().refreshInterval
        // flex: 주기의 마지막 1/4 안에서 시스템이 편한 때 실행한다. 다른 앱 작업과 묶여 배터리에 유리하고,
        // 사용자들의 요청이 같은 간격으로 몰리지 않는다.
        val flexMinutes = interval.minutes / 4
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(
            interval.minutes, TimeUnit.MINUTES,
            flexMinutes, TimeUnit.MINUTES,
        )
            // 주기 작업은 기본적으로 등록 즉시 첫 실행을 한다. 등록은 항상 방금 조회에 성공한 직후라서 한 주기 미룬다.
            // (UPDATE로 기존 작업을 갱신할 때는 기존 예약 시점이 유지된다)
            .setInitialDelay(interval.minutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setInputData(workDataOf(RefreshWorker.KEY_FROM_BACKGROUND to true))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
        Log.i(TAG, "periodic refresh scheduled: every ${interval.minutes} min")
    }

    /** 로그인이 만료됐거나 로그아웃했을 때. 실패할 요청을 계속 보내지 않게 멈춘다. */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC)
        Log.i(TAG, "periodic refresh cancelled")
    }

    private companion object {
        const val TAG = "AldotSync"
        const val UNIQUE_PERIODIC = "usage-refresh-periodic"
    }
}
