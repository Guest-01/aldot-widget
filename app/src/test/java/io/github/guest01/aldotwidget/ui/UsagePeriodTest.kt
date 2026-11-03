package io.github.guest01.aldotwidget.ui

import io.github.guest01.aldotwidget.data.sampleSnapshot
import io.github.guest01.aldotwidget.ui.main.HomeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** 이용 기간 날짜 칸은 그달 날짜 수만큼 그린다: 2월(28·29일), 31일 달, 달의 첫날·마지막 날 */
class UsagePeriodTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private fun sampleOn(date: LocalDate) = sampleSnapshot(date.atStartOfDay(zone).plusHours(12).toInstant(), zone)

    @Test
    fun `달마다 날짜 수가 맞다`() {
        assertEquals(28, HomeText.period(sampleOn(LocalDate.of(2027, 2, 10)))!!.totalDays)
        assertEquals(29, HomeText.period(sampleOn(LocalDate.of(2028, 2, 10)))!!.totalDays)
        assertEquals(31, HomeText.period(sampleOn(LocalDate.of(2026, 12, 10)))!!.totalDays)
    }

    @Test
    fun `달의 첫날과 마지막 날`() {
        val first = HomeText.period(sampleOn(LocalDate.of(2026, 11, 1)))!!
        assertEquals(1, first.dayNumber)
        assertEquals(29, first.daysLeft)
        val last = HomeText.period(sampleOn(LocalDate.of(2026, 11, 30)))!!
        assertEquals(30, last.dayNumber)
        assertEquals(0, last.daysLeft)
        assertEquals("오늘까지", HomeText.daysLeft(last))
    }

    @Test
    fun `조회한 날이 기간 밖이면 이상한 값이라 보여 주지 않는다`() {
        // 시작일보다 앞선 조회일 (서버 값이 꼬였을 때) → 칸을 그리지 않고 "이용 기간 정보 없음"
        val broken = sampleOn(LocalDate.of(2026, 11, 10)).copy(periodStart = "2026.11.20")
        assertNull(HomeText.period(broken))
    }
}
