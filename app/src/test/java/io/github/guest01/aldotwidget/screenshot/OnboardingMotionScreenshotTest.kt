package io.github.guest01.aldotwidget.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.guest01.aldotwidget.data.RefreshInterval
import io.github.guest01.aldotwidget.ui.onboarding.BatteryDialogPreview
import io.github.guest01.aldotwidget.ui.onboarding.DoneHeader
import io.github.guest01.aldotwidget.ui.onboarding.NotificationPreview
import io.github.guest01.aldotwidget.ui.onboarding.WidgetIllustration
import org.junit.Test

/** 첫 실행 안내 애니메이션의 주요 순간 (시안 '안내 애니메이션' ②③, '2·3순위' ⑨⑩) */
class OnboardingMotionScreenshotTest : ScreenshotTest() {

    private fun frame(content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        show { Column(Modifier.width(390.dp).padding(24.dp)) { content() } }
    }

    /** ② 2.8초 한 바퀴: 30~46%에 '확인'이 눌리고 물결이 퍼진다. 화살표는 1.4초마다 까딱 */
    @Test
    fun batteryPress() {
        frame { BatteryDialogPreview(samsung = true) }
        shotAt("battery_0_rest", 300 + 300)       // t≈0.11
        shotAt("battery_1_press", 700)            // t≈0.36 눌림 + 물결
        shotAt("battery_2_after", 600)            // t≈0.57
        shotAt("battery_3_end", 12_000)
    }

    /** ③ 알림이 위에서 내려와 튕기고 안내가 나타난다 (1.6초, 한 번) */
    @Test
    fun notificationDrop() {
        frame { NotificationPreview() }
        shotAt("notif_0_start", 300 + 100)
        shotAt("notif_1_drop", 250)
        shotAt("notif_2_landed", 500)
        shotAt("notif_3_end", 2_000)
    }

    /** ⑨ 준비 끝 */
    @Test
    fun done() {
        frame { DoneHeader(RefreshInterval.DEFAULT) }
        shotAt("done_0", 150 + 150)
        shotAt("done_1", 350)
        shotAt("done_2", 400)
        shotAt("done_3_end", 2_000)
    }

    /** ⑩ 환영 화면 위젯 그림 */
    @Test
    fun welcome() {
        frame { WidgetIllustration() }
        shotAt("welcome_0", 250 + 150)
        shotAt("welcome_1", 500)
        shotAt("welcome_2", 500)
        shotAt("welcome_3_end", 2_000)
    }
}
