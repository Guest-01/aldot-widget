package io.github.guest01.aldotwidget.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class WidgetTextTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val now = ZonedDateTime.of(2026, 11, 2, 21, 0, 0, 0, zone).toInstant()
    private val today = ZonedDateTime.of(2026, 11, 2, 20, 8, 0, 0, zone).toInstant()
    private val earlier = ZonedDateTime.of(2026, 10, 27, 20, 8, 0, 0, zone).toInstant()

    @Test
    fun `오늘 조회한 값은 시각만, 다른 날이면 날짜도 쓴다`() {
        assertEquals("20:08 기준", WidgetText.long(today, failed = false, refreshing = false, now, zone))
        assertEquals("10/27 20:08 기준", WidgetText.long(earlier, failed = false, refreshing = false, now, zone))
        assertEquals("20:08", WidgetText.short(today, failed = false, refreshing = false, now, zone))
        assertEquals("10/27", WidgetText.short(earlier, failed = false, refreshing = false, now, zone))
    }

    @Test
    fun `갱신 중이 실패보다 먼저 보인다`() {
        assertEquals("갱신 중…", WidgetText.long(today, failed = true, refreshing = true, now, zone))
        assertEquals("갱신 중", WidgetText.short(today, failed = true, refreshing = true, now, zone))
        assertEquals("갱신 실패 · 20:08", WidgetText.long(today, failed = true, refreshing = false, now, zone))
        assertEquals("갱신 실패", WidgetText.short(today, failed = true, refreshing = false, now, zone))
    }

    @Test
    fun `용량은 숫자와 단위로 나눈다`() {
        assertEquals("5.7" to "GB", WidgetText.splitUnit("5.7 GB"))
        assertEquals("-" to "", WidgetText.splitUnit("-"))
    }

    @Test
    fun `실측 칸 크기에 맞는 모양을 고른다`() {
        assertEquals(WidgetLayout.TINY, WidgetLayout.of(DpSize(113.dp, 101.dp))) // Fold8 커버 1×1
        assertEquals(WidgetLayout.COMPACT, WidgetLayout.of(DpSize(226.dp, 101.dp))) // Fold8 커버 2×1
        assertEquals(WidgetLayout.SQUARE, WidgetLayout.of(DpSize(179.dp, 210.dp))) // Pixel 2×2
        assertEquals(WidgetLayout.WIDE, WidgetLayout.of(DpSize(360.dp, 210.dp))) // Pixel 4×2
        assertEquals(WidgetLayout.TINY, WidgetLayout.of(DpSize(90.dp, 210.dp))) // 1×2도 1×1 모양
    }
}
