package io.github.guest01.aldotwidget.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.guest01.aldotwidget.ui.settings.BlockSteps
import io.github.guest01.aldotwidget.ui.settings.InstallingPart
import io.github.guest01.aldotwidget.ui.settings.PermissionPicture
import org.junit.Test

/**
 * 업데이트 창 안내 애니메이션의 주요 순간 (시안 '안내 애니메이션' ①④, '2·3순위' ⑦).
 * 시계를 멈추고 옮겨 가며 찍어서, 순간마다 모습이 맞는지 본다.
 */
class GuideMotionScreenshotTest : ScreenshotTest() {

    private fun frame(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        show { Column(Modifier.width(390.dp).padding(24.dp)) { content() } }
    }

    /** ① 시작 지연 300ms 뒤 3.2초 한 바퀴: 꺼짐 → 물결 → 켜짐 → 반짝 → 세 바퀴 뒤 켜진 채 멈춤 */
    @Test
    fun permissionSwitch() {
        frame { PermissionPicture() }
        shotAt("guide_switch_0_off", 300 + 600)        // t≈0.19 꺼짐
        shotAt("guide_switch_1_ripple", 500)          // t≈0.34 물결
        shotAt("guide_switch_2_on", 400)              // t≈0.47 켜지는 중
        shotAt("guide_switch_3_glow", 700)            // t≈0.69 반짝
        shotAt("guide_switch_4_end", 12_000)          // 세 바퀴 뒤 멈춤
    }

    /** ④ 3.6초 한 바퀴에 1 → 2 → 3 */
    @Test
    fun blockSteps() {
        frame { BlockSteps() }
        shotAt("guide_steps_1", 300 + 600)
        shotAt("guide_steps_2", 1200)
        shotAt("guide_steps_3", 1200)
        shotAt("guide_steps_end", 12_000)
    }

    /** ④ 단계 강조가 빠지는 중간 (어두운 테마): 검게 깜박이지 않고 옅어지기만 해야 한다 */
    @Test
    fun blockStepsFadeDark() {
        compose.mainClock.autoAdvance = false
        show(dark = true) { Column(Modifier.width(390.dp).padding(24.dp)) { BlockSteps() } }
        shotAt("guide_steps_fade_dark", 300 + 1080 + 90)
    }

    /** '애니메이션 제거'를 켠 폰: 움직이지 않고 처음부터 마지막 모습(켜진 스위치, 다 그린 체크)만 보인다 */
    @Test
    fun reduceMotionShowsFinalState() {
        frame {
            androidx.compose.runtime.CompositionLocalProvider(io.github.guest01.aldotwidget.ui.LocalReduceMotion provides true) {
                PermissionPicture()
                InstallingPart("11.5MB")
            }
        }
        shotAt("reduce_motion_start", 16)
    }

    /** ⑦ 다 받은 뒤 동그라미가 커지며 체크가 그려진다 */
    @Test
    fun installing() {
        frame { InstallingPart("11.5MB") }
        shotAt("install_check_0", 16)
        shotAt("install_check_1", 250)
        shotAt("install_check_2", 1000)
    }
}
