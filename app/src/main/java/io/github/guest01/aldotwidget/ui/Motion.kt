package io.github.guest01.aldotwidget.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 움직임 줄이기. 폰의 '애니메이션 제거'(Android 설정 → 접근성, 또는 개발자 옵션의 애니메이션 배율 0)를 켰으면 true.
 * 켜져 있으면 안내 애니메이션을 돌리지 않고 마지막 모습만 보여 준다. 테마(AldotWidgetTheme)가 채운다.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

fun reduceMotionEnabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * 안내 애니메이션의 시계. 0에서 1까지 cycleMs 동안 일정하게 흐르기를 repeats번 하고 1에서 멈춘다.
 * 각 부품은 이 값의 구간(seg)을 보고 움직인다. 1일 때의 모습이 멈춘 뒤 남는 모습이다.
 */
@Composable
fun rememberGuideClock(cycleMs: Int, repeats: Int = 3, pauseMs: Long = 400, startDelayMs: Long = 300): State<Float> {
    val reduce = LocalReduceMotion.current
    val clock = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(reduce) {
        if (reduce) {
            clock.snapTo(1f)
            return@LaunchedEffect
        }
        delay(startDelayMs)
        repeat(repeats) { round ->
            clock.snapTo(0f)
            clock.animateTo(1f, tween(cycleMs, easing = LinearEasing))
            if (round < repeats - 1) delay(pauseMs)
        }
    }
    return clock.asState()
}

/** 시계 값 t가 [start, end] 구간에서 0→1로 가는 비율 (구간 밖이면 0 또는 1) */
fun seg(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

/** 부드럽게 시작하고 끝나는 곡선 */
fun smooth(x: Float): Float = x * x * (3 - 2 * x)

/** 화면 전환·결과 표시용 짧은 움직임. '애니메이션 제거'를 켰으면 바로 바뀐다 */
@Composable
fun <T> motionTween(durationMillis: Int = 250, delay: Int = 0): androidx.compose.animation.core.FiniteAnimationSpec<T> =
    motionSpec(LocalReduceMotion.current, durationMillis, delay)

/** motionTween과 같지만 Composable 밖(AnimatedContent의 transitionSpec 등)에서 쓴다 */
fun <T> motionSpec(reduce: Boolean, durationMillis: Int = 250, delay: Int = 0): androidx.compose.animation.core.FiniteAnimationSpec<T> =
    if (reduce) androidx.compose.animation.core.snap() else tween(durationMillis, delayMillis = delay)

/** 빠르게 시작해 천천히 멈추는 곡선 (차오르는 막대·고리) */
fun easeOut(x: Float): Float = 1 - (1 - x) * (1 - x) * (1 - x)

/** 살짝 지나쳤다 돌아오는 곡선 (톡 나타나는 동그라미, 내려와 튕기는 알림) */
fun overshoot(x: Float): Float = androidx.compose.animation.core.EaseOutBack.transform(x)

/** 그려지는 체크 표시. progress 0이면 안 보이고 1이면 다 그려진다 */
@Composable
fun DrawnCheck(progress: Float, color: Color, modifier: Modifier = Modifier, strokeWidth: Dp = 2.6.dp) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.21f, h * 0.52f)
            lineTo(w * 0.40f, h * 0.71f)
            lineTo(w * 0.79f, h * 0.31f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), part, true)
        if (progress > 0f) {
            drawPath(part, color, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** 탭 물결: 가운데에서 퍼지며 옅어지는 동그라미. p는 0→1 */
fun rippleAlpha(p: Float, peak: Float = 0.35f): Float = if (p <= 0f || p >= 1f) 0f else peak * (1f - p)

