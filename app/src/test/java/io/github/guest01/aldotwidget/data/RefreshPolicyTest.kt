package io.github.guest01.aldotwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {

    private val second = 1_000L
    private val now = 1_800_000_000_000L

    @Test
    fun `새로고침은 1분에 한 번만 받는다`() {
        assertTrue(RefreshPolicy.canRequest(null, now))
        assertFalse(RefreshPolicy.canRequest(now - 59 * second, now))
        assertTrue(RefreshPolicy.canRequest(now - 60 * second, now))
    }

    @Test
    fun `시계가 뒤로 돌아가도 새로고침을 막지 않는다`() {
        assertTrue(RefreshPolicy.canRequest(now + 30 * second, now))
    }

    @Test
    fun `갱신 중 표시는 2분이 지나면 끝난 것으로 본다`() {
        assertFalse(RefreshPolicy.isRefreshing(null, now))
        assertTrue(RefreshPolicy.isRefreshing(now - 119 * second, now))
        assertFalse(RefreshPolicy.isRefreshing(now - 120 * second, now))
        assertFalse(RefreshPolicy.isRefreshing(now + 10 * second, now))
    }

    @Test
    fun `새 항목이 없던 예전 저장 형식도 읽는다`() {
        val state = UsageStateJson.decode("""{"status":"OK","errorMessage":null}""")
        assertEquals(SyncStatus.OK, state.status)
        assertNull(state.refreshingSince)
        assertNull(state.lastRequestedAt)
    }
}
