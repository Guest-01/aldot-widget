package io.github.guest01.aldotwidget.screenshot

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.guest01.aldotwidget.debug.MotionGallery
import org.junit.Test
import org.robolectric.annotation.Config

/** 애니메이션 미리보기 화면(개발용 앱에만 있음)이 제대로 그려지는지. 길어서 세로로 크게 찍는다 */
@Config(qualifiers = "w390dp-h4000dp-xhdpi")  // 흐름 화면도 이 크기로 찍힌다 (아래가 비어 보일 뿐)
class MotionGalleryScreenshotTest : ScreenshotTest() {

    @Test
    fun gallery() {
        compose.mainClock.autoAdvance = false
        compose.setContent { MotionGallery(startDark = false) }
        shotAt("gallery_light", 6_000)
    }

    @Test
    fun galleryDark() {
        compose.mainClock.autoAdvance = false
        compose.setContent { MotionGallery(startDark = true) }
        shotAt("gallery_dark", 6_000)
    }

    /** '시작 화면 흐름 보기' → 환영 화면 → '시작하기' → 알림 단계 (이미 허용된 폰에서도 모든 단계) */
    @Test
    fun startFlow() {
        compose.setContent { MotionGallery(startDark = false) }
        compose.onNodeWithText("시작 화면 흐름 보기").performClick()
        compose.waitForIdle()
        shot("gallery_flow_welcome")
        compose.onNodeWithText("알닷 계정으로 시작하기").performClick()
        compose.waitForIdle()
        shot("gallery_flow_step")
    }
}
