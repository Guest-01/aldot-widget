package io.github.guest01.aldotwidget.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.guest01.aldotwidget.ui.settings.AppIcon
import io.github.guest01.aldotwidget.ui.settings.WideButton
import org.junit.Test

/** 도구가 제대로 도는지(한글 글꼴, 테마) 확인하는 가장 작은 스냅숏 */
class SmokeScreenshotTest : ScreenshotTest() {
    @Test
    fun smoke() {
        show {
            Column(Modifier.width(390.dp).padding(24.dp)) {
                AppIcon(52.dp)
                Text("남았닷 스냅숏 시험 · 1.0.1", fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
                WideButton("업데이트 확인", primary = false, onClick = {}, modifier = Modifier.padding(top = 12.dp))
                WideButton("바로 설치", primary = true, onClick = {}, modifier = Modifier.padding(top = 8.dp))
            }
        }
        shot("smoke")
    }
}
