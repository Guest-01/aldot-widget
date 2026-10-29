package io.github.guest01.aldotwidget.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeedLoginNotifyTest {

    @Test
    fun `자동 갱신에서 처음 만료되면 알린다`() {
        assertTrue(shouldNotifyNeedLogin(SyncStatus.OK, SyncStatus.NEED_LOGIN, fromBackground = true))
        assertTrue(shouldNotifyNeedLogin(SyncStatus.ERROR, SyncStatus.NEED_LOGIN, fromBackground = true))
    }

    @Test
    fun `이미 만료 상태면 다시 알리지 않는다`() {
        assertFalse(shouldNotifyNeedLogin(SyncStatus.NEED_LOGIN, SyncStatus.NEED_LOGIN, fromBackground = true))
    }

    @Test
    fun `앱이나 위젯에서 직접 조회했으면 알리지 않는다`() {
        assertFalse(shouldNotifyNeedLogin(SyncStatus.OK, SyncStatus.NEED_LOGIN, fromBackground = false))
    }

    @Test
    fun `만료가 아니면 알리지 않는다`() {
        assertFalse(shouldNotifyNeedLogin(SyncStatus.OK, SyncStatus.ERROR, fromBackground = true))
        assertFalse(shouldNotifyNeedLogin(SyncStatus.NEED_LOGIN, SyncStatus.OK, fromBackground = true))
    }
}
