package io.github.guest01.aldotwidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** 화면에서 `AldotTheme.colors.sub`처럼 시안 색을 꺼내 쓴다. */
object AldotTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

/**
 * 앱 테마. 위젯과 같은 고정 색을 쓰고(배경화면 색을 따르는 dynamic color는 쓰지 않는다),
 * 다크 모드는 폰 설정을 따른다.
 * Material 부품(버튼, 로딩 표시 등)도 같은 색이 나오도록 Material 색 체계에도 옮겨 둔다.
 */
@Composable
fun AldotWidgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
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
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
