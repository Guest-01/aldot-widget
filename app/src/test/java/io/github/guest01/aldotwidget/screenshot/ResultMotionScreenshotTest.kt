package io.github.guest01.aldotwidget.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.ui.main.Gauge
import io.github.guest01.aldotwidget.ui.settings.AppCard
import io.github.guest01.aldotwidget.ui.settings.SectionTitle
import io.github.guest01.aldotwidget.ui.settings.StatusRow
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.update.CheckState
import io.github.guest01.aldotwidget.update.UpdateInfo
import io.github.guest01.aldotwidget.update.UpdateUiState
import org.junit.Test

/** 누른 결과가 바뀌는 순간과 홈 계기판 (시안 '2·3순위' ⑤⑥⑧) */
class ResultMotionScreenshotTest : ScreenshotTest() {

    private fun frame(content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        show { Column(Modifier.width(390.dp).padding(24.dp)) { content() } }
    }

    /** ⑤ 꺼짐 → (배터리 창에서 돌아옴) → 켜짐: 칩이 접히고 받침 색이 돌아오며 체크가 그려진다 */
    @Test
    fun statusTurnsOn() {
        var on by mutableStateOf(false)
        frame {
            SectionTitle("위젯 동작", badge = if (on) null else "1가지 확인 필요")
            StatusRow(R.drawable.ic_moon, "밤에도 로그인 유지", on, onFix = {})
        }
        shotAt("status_0_off", 50)
        change { on = true }
        shotAt("status_1_swap", 150)
        shotAt("status_2_draw", 300)
        shotAt("status_3_end", 1000)
    }

    /** ⑥ 업데이트 확인 → 확인 중 → 새 버전: 문구가 바뀌고 카드가 물들며 '업데이트' 버튼이 톡 나타난다 */
    @Test
    fun updateCardResult() {
        val info = UpdateInfo("1.0.2", 3, "aldot-widget-1.0.2.apk", 11_500_000, "x", "2026-11-20")
        // 오늘 오전에 자동으로 확인한 상태에서 시작
        val morning = java.time.LocalDate.now().atTime(9, 14).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        var state by mutableStateOf(UpdateUiState(lastCheckedAt = morning))
        frame { AppCard(state, onCheck = {}, onOpenUpdate = {}) }
        shotAt("card_0_idle", 50)
        change { state = state.copy(check = CheckState.CHECKING) }
        shotAt("card_1_checking", 400)
        change { state = state.copy(check = CheckState.IDLE, available = info) }
        shotAt("card_2_found", 150)
        shotAt("card_3_end", 1000)
    }

    /** ⑧ 계기판이 0에서 남은 양(13.2 / 15 GB)까지 차오르고 숫자가 올라간다 */
    @Test
    fun gaugeFills() {
        val gb = 1024L * 1024
        frame {
            Box(Modifier.background(AldotTheme.colors.button).padding(vertical = 16.dp)) {
                Gauge(Quota(total = 15 * gb, used = (1.8 * gb).toLong()))
            }
        }
        shotAt("gauge_0", 50)
        shotAt("gauge_1", 300)
        shotAt("gauge_2", 400)
        shotAt("gauge_2b", 500)
        shotAt("gauge_3_end", 1500)
    }

    /** 계기판 숫자를 세는 단위: 스무 번 안팎으로 바뀌게 */
    @Test
    fun countSteps() {
        org.junit.Assert.assertEquals(1, io.github.guest01.aldotwidget.ui.main.countStep(13.2f))
        org.junit.Assert.assertEquals(1, io.github.guest01.aldotwidget.ui.main.countStep(20f))
        org.junit.Assert.assertEquals(5, io.github.guest01.aldotwidget.ui.main.countStep(85f))
        org.junit.Assert.assertEquals(50, io.github.guest01.aldotwidget.ui.main.countStep(850f))
    }
}
