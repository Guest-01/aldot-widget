package io.github.guest01.aldotwidget.ui

import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.ui.main.HomeStatus
import io.github.guest01.aldotwidget.ui.main.HomeText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeTextTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val now = ZonedDateTime.of(2026, 11, 2, 21, 0, 0, 0, zone).toInstant()
    private val snapshot = UsageSnapshot(
        phoneMasked = "010-****-****", partnerName = "알뜰폰", planName = "요금제",
        periodStart = "2026.11.01", periodEnd = "2026.11.02", remainDays = 28,
        data = Quota(15L * 1024 * 1024, 9L * 1024 * 1024), voice = Quota(300L * 60, 87L * 60 + 25), sms = Quota(100, 12),
        fetchedAt = ZonedDateTime.of(2026, 11, 2, 20, 8, 0, 0, zone).toInstant(),
    )
    private val ready = MainUiState.Ready(snapshot)

    @Test
    fun `평소에는 갱신 시각과 새로고침 버튼`() {
        assertEquals(HomeStatus("20:08 기준", false, false, "새로고침", true), HomeText.status(ready, now, zone))
    }

    @Test
    fun `갱신 중이면 버튼을 잠근다`() {
        val status = HomeText.status(ready.copy(refreshing = true), now, zone)
        assertEquals(HomeStatus("갱신 중…", false, true, "새로고침", false), status)
    }

    @Test
    fun `방금 눌렀으면 1분 동안 버튼을 잠근다`() {
        val justNow = ready.copy(lastRequestedAt = now.toEpochMilli() - 30_000)
        assertEquals(HomeStatus("방금 갱신했어요", false, false, "1분 뒤 가능", false), HomeText.status(justNow, now, zone))
        val minuteAgo = ready.copy(lastRequestedAt = now.toEpochMilli() - 60_000)
        assertEquals("새로고침", HomeText.status(minuteAgo, now, zone).buttonLabel)
    }

    @Test
    fun `실패하면 이전 값의 시각과 다시 시도, 1분 제한은 그대로`() {
        val failed = ready.copy(error = "timeout")
        assertEquals(HomeStatus("갱신 실패 · 20:08 기준 값", true, false, "다시 시도", true), HomeText.status(failed, now, zone))
        val failedJustNow = failed.copy(lastRequestedAt = now.toEpochMilli() - 10_000)
        assertEquals(HomeStatus("갱신 실패 · 20:08 기준 값", true, false, "1분 뒤 가능", false), HomeText.status(failedJustNow, now, zone))
    }

    @Test
    fun `요금제 아래 줄에 기간 끝(조회일 + 남은 날)과 남은 날`() {
        assertEquals("알뜰폰 · 010-****-**** · 11월 30일까지 28일 남음", HomeText.planSubtitle(snapshot))
        assertEquals("알뜰폰 · 010-****-****", HomeText.planSubtitle(snapshot.copy(periodEnd = null, remainDays = null)))
        assertEquals("알뜰폰 · 010-****-**** · 3일 남음", HomeText.planSubtitle(snapshot.copy(periodEnd = "말일", remainDays = 3)))
    }

    @Test
    fun `카드 아래 줄`() {
        assertEquals("15.0 GB 중 9.0 GB 사용", HomeText.dataCaption(snapshot.data!!))
        assertEquals("300분 중 87분 사용", HomeText.voiceCaption(snapshot.voice!!))
        assertEquals("100건 중 12건 사용", HomeText.smsCaption(snapshot.sms!!))
    }
}
