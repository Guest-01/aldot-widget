package io.github.guest01.aldotwidget.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SessionPolicyTest {

    private val minute = 60_000L
    private val now = 1_800_000_000_000L

    @Test
    fun `45분이 지나야 연장한다`() {
        assertFalse(SessionPolicy.shouldExtend(now - 44 * minute, now))
        assertTrue(SessionPolicy.shouldExtend(now - 45 * minute, now))
    }

    @Test
    fun `연장 기록이 없으면 바로 연장한다`() {
        assertTrue(SessionPolicy.shouldExtend(0, now))
    }

    @Test
    fun `시계가 뒤로 돌아가도 연장한다`() {
        assertTrue(SessionPolicy.shouldExtend(now + 10 * minute, now))
    }

    @Test
    fun `알람은 항상 2시간보다 충분히 앞서 울린다`() {
        repeat(1000) { seed ->
            val delay = SessionPolicy.nextAlarmDelayMillis(Random(seed))
            assertTrue("too early: $delay", delay >= 80 * minute)
            assertTrue("too late: $delay", delay <= 95 * minute)
        }
    }

    @Test
    fun `세션이 끝났을 시간이 지나면 재시도를 멈춘다`() {
        assertTrue(SessionPolicy.shouldRetryAlarm(now - 119 * minute, now))
        assertFalse(SessionPolicy.shouldRetryAlarm(now - 120 * minute, now))
    }
}
