package io.github.guest01.aldotwidget.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.ThemeColor
import io.github.guest01.aldotwidget.ui.LocalReduceMotion
import io.github.guest01.aldotwidget.ui.main.Gauge
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme
import org.junit.Test

/**
 * 홈 계기판의 큰 숫자 줄이 고리와 겹치지 않는지 본다.
 * 클리앙 제보(2026-11-20): 남은 데이터가 100 GB를 넘으면 "100.7 GB / 186.7 GB"가 고리 왼쪽 끝과 겹쳤다.
 * 움직임 줄이기를 켜서 다 오른 최종 화면을 찍는다.
 */
class GaugeScreenshotTest : ScreenshotTest() {

    private fun gb(value: Double) = (value * 1024 * 1024).toLong()

    private fun gauges(name: String, fontScale: Float = 1f, themeColor: ThemeColor = ThemeColor.ORANGE) {
        val cases = listOf(
            Quota(total = gb(15.0), used = gb(15.0) - gb(5.7)), // 평소 (기존 디자인 그대로여야 함)
            Quota(total = gb(15.0), used = gb(15.0) - gb(15.0)), // 다 남음, 네 글자
            Quota(total = gb(186.7), used = gb(186.7) - gb(100.7) + 1), // 제보된 값
            Quota(total = gb(15.0), used = gb(15.0) - 851749), // MB로 내려감: "831 MB"
            Quota(total = 1024L * 1024 * 1000, used = 1024L * 1024 * 1000 - gb(999.9)), // 극단: 999.9 GB / 1000 GB
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalReduceMotion provides true,
                LocalDensity provides Density(density.density, fontScale),
            ) {
                AldotWidgetTheme(themeColor = themeColor, darkTheme = false) {
                    Column(
                        Modifier.background(AldotTheme.colors.button).width(350.dp).padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        cases.forEach { Box { Gauge(it) } }
                    }
                }
            }
        }
        shot(name)
    }

    @Test
    fun gauge() = gauges("gauge_numbers")

    /** 폰 글자 크기를 키운 경우. 계기판 숫자는 115%까지만 커진다 */
    @Test
    fun gaugeLargeFont() = gauges("gauge_numbers_font130", fontScale = 1.3f)

    @Test
    fun gaugeBlue() = gauges("gauge_numbers_blue", themeColor = ThemeColor.BLUE)
}
