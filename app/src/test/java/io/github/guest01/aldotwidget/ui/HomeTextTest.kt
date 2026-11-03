package io.github.guest01.aldotwidget.ui

import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.ui.main.HomeStatus
import io.github.guest01.aldotwidget.ui.main.HomeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeTextTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val now = ZonedDateTime.of(2026, 11, 2, 21, 0, 0, 0, zone).toInstant()
    private val snapshot = UsageSnapshot(
        phoneMasked = "010-12**-34**", partnerName = "티플러스", planName = "[티플러스] 5G 15GB+",
        periodStart = "2026.11.01", periodEnd = "2026.11.02", remainDays = 28,
        data = Quota(15L * 1024 * 1024, 9L * 1024 * 1024 + 300 * 1024), voice = Quota(300L * 60, 87L * 60 + 25), sms = Quota(100, 12),
        fetchedAt = ZonedDateTime.of(2026, 11, 2, 20, 8, 0, 0, zone).toInstant(),
    )
    private val ready = MainUiState.Ready(snapshot)

    @Test
    fun `평소에는 갱신 시각, 새로고침 가능`() {
        assertEquals(HomeStatus("20:08 기준", "20:08", false, false, true), HomeText.status(ready, now, zone))
    }

    @Test
    fun `갱신 중이면 새로고침을 잠근다`() {
        assertEquals(HomeStatus("갱신 중…", "20:08", false, true, false), HomeText.status(ready.copy(refreshing = true), now, zone))
    }

    @Test
    fun `방금 눌렀으면 1분 동안 새로고침을 잠근다`() {
        val justNow = ready.copy(lastRequestedAt = now.toEpochMilli() - 30_000)
        assertEquals(HomeStatus("방금 갱신했어요", "20:08", false, false, false), HomeText.status(justNow, now, zone))
        val minuteAgo = ready.copy(lastRequestedAt = now.toEpochMilli() - 60_000)
        assertEquals(true, HomeText.status(minuteAgo, now, zone).canRefresh)
    }

    @Test
    fun `실패하면 시각만 경고로, 1분 제한은 그대로`() {
        val failed = ready.copy(error = "timeout")
        assertEquals(HomeStatus("20:08 기준", "20:08", true, false, true), HomeText.status(failed, now, zone))
        val failedJustNow = failed.copy(lastRequestedAt = now.toEpochMilli() - 10_000)
        assertEquals(false, HomeText.status(failedJustNow, now, zone).canRefresh)
    }

    @Test
    fun `이용 기간은 시작일부터 조회일 + 남은 날까지`() {
        val period = HomeText.period(snapshot)!!
        assertEquals(30, period.totalDays)
        assertEquals(2, period.dayNumber)
        assertEquals(28, period.daysLeft)
        assertEquals("11월 1일 ~ 11월 30일", HomeText.periodRange(period))
        assertEquals("28일 남음", HomeText.daysLeft(period))
        assertEquals("오늘까지", HomeText.daysLeft(HomeText.period(snapshot.copy(periodEnd = "2026.11.30", remainDays = 0))!!))
    }

    @Test
    fun `기간을 알 수 없으면 null`() {
        assertNull(HomeText.period(snapshot.copy(periodStart = null)))
        assertNull(HomeText.period(snapshot.copy(periodEnd = "말일")))
        assertNull(HomeText.period(snapshot.copy(remainDays = null)))
    }

    @Test
    fun `하루 사용량 안내`() {
        val period = HomeText.period(snapshot)
        assertEquals("남은 28일, 하루 0.2 GB씩 쓸 수 있어요", HomeText.dataPill(snapshot.data, period, 28))
        val empty = Quota(15L * 1024 * 1024, 15L * 1024 * 1024)
        assertEquals("데이터를 다 썼어요 · 12월 1일에 다시 채워져요", HomeText.dataPill(empty, period, 28))
        val lastDay = HomeText.period(snapshot.copy(periodEnd = "2026.11.30", remainDays = 0))
        assertEquals("오늘이 마지막 날이에요 · 내일 다시 채워져요", HomeText.dataPill(snapshot.data, lastDay, 0))
        assertNull(HomeText.dataPill(null, period, 28))
    }

    @Test
    fun `하루 양은 짧게`() {
        assertEquals("0.2 GB", HomeText.dailyAmount(5L * 1024 * 1024 / 28 + 30_000))
        assertEquals("85 MB", HomeText.dailyAmount(85L * 1024 + 300))
        assertEquals("900 KB", HomeText.dailyAmount(900))
    }

    @Test
    fun `통신사와 음성 분`() {
        assertEquals("티플러스 · LG U+망", HomeText.carrier(snapshot))
        assertEquals("LG U+망", HomeText.carrier(snapshot.copy(partnerName = null)))
        assertEquals("212", HomeText.voiceMinutes(snapshot.voice!!.remaining))
    }
}
