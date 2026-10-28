package io.github.guest01.aldotwidget.ui

import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.SyncStatus
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.data.UsageState
import io.github.guest01.aldotwidget.data.UsageStateJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class UiStateTest {

    private val snapshot = UsageSnapshot(
        phoneMasked = "010-12**-34**", partnerName = "티플러스", planName = "요금제",
        periodStart = "2026.10.01", periodEnd = "2026.10.28", remainDays = 3,
        data = Quota(3044253, 2192504), voice = Quota(3484, 297), sms = Quota(59, 0),
        fetchedAt = Instant.ofEpochMilli(1_793_000_000_000),
    )

    @Test
    fun `처음 실행하면 로딩`() {
        assertEquals(MainUiState.Loading, toUiState(UsageState(), refreshing = true))
    }

    @Test
    fun `저장된 값이 있으면 새로고침 중에도 바로 보여 준다`() {
        val ui = toUiState(UsageState(snapshot, SyncStatus.OK), refreshing = true)
        assertEquals(MainUiState.Ready(snapshot, refreshing = true), ui)
    }

    @Test
    fun `갱신이 실패해도 이전 값과 오류를 함께 보여 준다`() {
        val ui = toUiState(UsageState(snapshot, SyncStatus.ERROR, "timeout"), refreshing = false)
        assertEquals(MainUiState.Ready(snapshot, error = "timeout"), ui)
    }

    @Test
    fun `저장된 값 없이 실패하면 오류 화면`() {
        val ui = toUiState(UsageState(status = SyncStatus.ERROR, errorMessage = "timeout"), refreshing = false)
        assertEquals(MainUiState.Failed("timeout"), ui)
    }

    @Test
    fun `로그인이 필요하면 로그인 화면, 로그인 직후 조회 중이면 로딩`() {
        val needLogin = UsageState(snapshot, SyncStatus.NEED_LOGIN)
        assertEquals(MainUiState.NeedLogin, toUiState(needLogin, refreshing = false))
        assertEquals(MainUiState.Loading, toUiState(needLogin, refreshing = true))
    }

    @Test
    fun `저장 형식(JSON)으로 바꿨다가 되돌려도 같은 값`() {
        val state = UsageState(snapshot, SyncStatus.ERROR, "timeout")
        val json = UsageStateJson.encode(state)
        assertTrue(json.contains("\"fetchedAt\":1793000000000"))
        assertEquals(state, UsageStateJson.decode(json))
    }
}
