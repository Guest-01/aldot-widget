package io.github.guest01.aldotwidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import io.github.guest01.aldotwidget.data.ThemeColor
import io.github.guest01.aldotwidget.ui.LocalReduceMotion
import io.github.guest01.aldotwidget.ui.reduceMotionEnabled

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** 화면에서 `AldotTheme.colors.sub`처럼 시안 색을 꺼내 쓴다. */
object AldotTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

/** 바탕색과 테마 색을 섞은 옅은 색 (아이콘 받침, 선택된 칸). 테마 색이 바뀌면 같이 바뀐다. */
private fun tint(base: Color, accent: Color, amount: Float) = lerp(base, accent, amount)

/** 고른 테마 색을 시안의 색 묶음에 끼워 넣는다. 버튼은 어두운 화면에서도 진한 색(흰 글자)을 쓴다. */
internal fun appColors(darkTheme: Boolean, themeColor: ThemeColor): AppColors {
    val base = if (darkTheme) DarkAppColors else LightAppColors
    val day = Color(themeColor.day)
    val accent = if (darkTheme) Color(themeColor.night) else day
    return base.copy(
        button = day,
        accent = accent,
        stage = tint(base.bg, accent, if (darkTheme) 0.14f else 0.10f),
    )
}

/**
 * 앱 테마. 위젯과 같은 고정 색에 사용자가 고른 테마 색을 쓰고(배경화면 색을 따르는 dynamic color는 쓰지 않는다),
 * 다크 모드는 폰 설정을 따른다.
 * Material 부품(버튼, 로딩 표시 등)도 같은 색이 나오도록 Material 색 체계에도 옮겨 둔다.
 */
@Composable
fun AldotWidgetTheme(
    themeColor: ThemeColor = ThemeColor.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = appColors(darkTheme, themeColor)
    val scheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    val colorScheme = scheme.copy(
        primary = colors.button,
        onPrimary = Color.White,
        background = colors.bg,
        onBackground = colors.text,
        surface = colors.bg,
        onSurface = colors.text,
        surfaceVariant = colors.panel,
        onSurfaceVariant = colors.sub,
        outline = colors.line,
        outlineVariant = colors.divider,
        error = colors.warn,
        errorContainer = colors.warnBg,
        onErrorContainer = colors.text,
    )
    // '애니메이션 제거'를 켠 폰에서는 안내 애니메이션을 마지막 모습으로만 보여 준다
    val context = LocalContext.current
    val reduceMotion = remember(context) { reduceMotionEnabled(context) }
    CompositionLocalProvider(LocalAppColors provides colors, LocalReduceMotion provides reduceMotion) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
