package io.github.guest01.aldotwidget.screenshot

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 화면 스냅숏 테스트의 공통 틀. PC(JVM)에서 Robolectric으로 Compose 화면을 그려 그림으로 남긴다.
 *
 * - 그림은 app/build/outputs/roborazzi/ 에 쌓인다 (저장소에는 넣지 않는다).
 * - ./gradlew recordRoborazziDebug 로 찍는다. 애니메이션은 mainClock을 멈추고 시각을 옮겨 가며 찍는다.
 * - 앱의 Application(AldotApp)은 WorkManager 등을 띄우므로 쓰지 않고 빈 Application으로 돌린다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [36], qualifiers = "w390dp-h844dp-xhdpi")
abstract class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** 테마를 입혀 그린다. dark면 어두운 테마 */
    protected fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            AldotWidgetTheme(darkTheme = dark) {
                Box(Modifier.background(AldotTheme.colors.bg).padding(0.dp)) { content() }
            }
        }
    }

    /** 지금 화면을 이름으로 저장한다 */
    protected fun shot(name: String) {
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    /** 화면 밖에서 상태를 바꾸고 바로 화면에 알린다 (시계를 멈춰 둔 채로 상태 변화를 찍을 때) */
    protected fun change(block: () -> Unit) {
        compose.runOnUiThread(block)
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeByFrame()
    }

    /** 애니메이션 시계를 멈추고 ms만큼 옮긴 뒤 찍는다 (처음 호출 전에 mainClock.autoAdvance = false) */
    protected fun shotAt(name: String, ms: Long) {
        compose.mainClock.advanceTimeBy(ms)
        shot(name)
    }
}
