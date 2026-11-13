package io.github.guest01.aldotwidget.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.guest01.aldotwidget.AldotApp
import io.github.guest01.aldotwidget.AppContainer
import io.github.guest01.aldotwidget.data.ErrorText
import io.github.guest01.aldotwidget.data.SyncStatus
import io.github.guest01.aldotwidget.data.UsageState
import io.github.guest01.aldotwidget.data.sampleSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 샘플 데이터 모드 스위치. 개발용 앱(debug)에만 들어간다 (src/debug). 로그인 없이 화면을 확인할 때 쓴다.
 *
 *   adb shell am broadcast -n io.github.guest01.aldotwidget/.debug.DemoReceiver --es state ok
 *
 * state:
 * - ok: 샘플 모드를 켜고 정상 상태 (홈 화면과 위젯에 가짜 데이터). 새로고침은 서버 대신 1초쯤 뒤 시각만 바뀐다.
 * - refreshing: "갱신 중" 표시 (2분 뒤 저절로 풀린다)
 * - error: 갱신 실패 (이전 값 + 경고)
 * - exhausted: 데이터를 다 쓴 상태 (남은 데이터 0)
 * - lastday: 이용 기간 마지막 날 (남은 날 0)
 * - long: 아주 긴 요금제·파트너사 이름 (줄바꿈 확인용)
 * - nodata: 음성·문자 정보가 없고 기간을 알 수 없는 응답
 * - login: 로그인이 풀린 상태 (다시 로그인 화면)
 * - onboarding: 첫 실행 안내만 처음부터 다시 (지금 상태는 그대로)
 * - fresh: 처음 설치한 상태 (환영 화면부터, 로그인 화면은 실제 알닷 사이트)
 * - off: 샘플 모드를 끄고 저장된 사용량을 지운다 (다시 실제 로그인이 필요하다)
 * - notify: 실제 알림 세 가지(로그인 만료, 새 버전, 업데이트 완료)를 띄워 본다. 로그인·데이터는 그대로
 *
 * 다른 앱이 보내지 못하도록 매니페스트에서 수신 권한을 DUMP로 막았다 (adb shell은 이 권한이 있다).
 */
class DemoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra("state") ?: "ok"
        val container = (context.applicationContext as AldotApp).container
        // 저장소에 쓰는 동안 시스템이 수신기를 끝내지 않게 한다
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                apply(container, state)
                Log.i(TAG, "demo state=$state")
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun apply(container: AppContainer, state: String) {
        val settings = container.settingsStore
        val store = container.usageStore
        val now = System.currentTimeMillis()
        when (state) {
            "off" -> {
                settings.setDemoMode(false)
                store.update { UsageState() }
            }
            "fresh" -> {
                settings.setDemoMode(true)
                settings.setOnboardingCompleted(false)
                store.update { UsageState() }
            }
            "onboarding" -> settings.setOnboardingCompleted(false)
            "notify" -> {
                // 같은 종류는 한 자리에 덮어쓰이므로(업데이트 두 가지는 같은 번호) 새 버전 → 업데이트 완료 순서로 잠깐 띄운다
                container.notifier.notifyNeedLogin()
                container.notifier.notifyUpdateAvailable(
                    io.github.guest01.aldotwidget.update.UpdateInfo("1.0.2", 3, "aldot-widget-1.0.2.apk", 11_500_000, "x"),
                )
                kotlinx.coroutines.delay(4_000)
                container.notifier.notifyUpdated("1.0.2")
                return
            }
            else -> {
                settings.setDemoMode(true)
                store.update { current ->
                    // ok는 지금 시각의 새 데이터로, 나머지는 이전 값을 그대로 두고 상태만 바꾼다
                    val sample = sampleSnapshot()
                    val snapshot = when (state) {
                        "ok" -> sample
                        "exhausted" -> sample.copy(data = sample.data?.let { it.copy(used = it.total) })
                        "lastday" -> sample.copy(
                            periodEnd = sample.periodEnd?.let { lastDayOf(it) } ?: sample.periodEnd,
                            remainDays = 0,
                            data = sample.data?.let { it.copy(used = it.total - 1_258_291) },
                        )
                        "long" -> sample.copy(
                            partnerName = "아주긴이름의알뜰폰파트너사",
                            planName = "[아주긴이름의알뜰폰파트너사] 5G 데이터 무제한 플러스 프리미엄 요금제 (음성 무제한·문자 기본 제공)",
                        )
                        "nodata" -> sample.copy(voice = null, sms = null, periodStart = null, remainDays = null)
                        else -> current.snapshot ?: sample
                    }
                    val base = UsageState(snapshot = snapshot, status = SyncStatus.OK)
                    when (state) {
                        "refreshing" -> base.copy(refreshingSince = now, lastRequestedAt = now)
                        "error" -> base.copy(status = SyncStatus.ERROR, errorMessage = ErrorText.UNREACHABLE)
                        "login" -> base.copy(status = SyncStatus.NEED_LOGIN)
                        else -> base
                    }
                }
            }
        }
        container.usageSync.redrawWidgets()
    }

    /** "2026.11.03" → "2026.11.30" (같은 달의 말일) */
    private fun lastDayOf(apiDate: String): String {
        val format = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        return format.format(LocalDate.parse(apiDate, format).with(TemporalAdjusters.lastDayOfMonth()))
    }

    private companion object {
        const val TAG = "AldotDemo"
    }
}
