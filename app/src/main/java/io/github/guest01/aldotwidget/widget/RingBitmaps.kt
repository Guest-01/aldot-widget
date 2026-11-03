package io.github.guest01.aldotwidget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap

/**
 * 원형 게이지 그림. Glance(위젯)에는 원을 그리는 부품이 없어서 비트맵으로 그려 이미지로 넣는다.
 * 흰색으로만 그리고 색은 위젯에서 입힌다(tint). 그래야 다크 모드 색을 런처가 바꿔 끼울 수 있다.
 */
internal object RingBitmaps {

    /** 바탕 고리 (한 바퀴 전체) */
    fun track(sizePx: Int, strokePx: Float): Bitmap = draw(sizePx, strokePx, sweep = 360f, roundCap = false)

    /** 남은 비율만큼의 고리. 12시 방향에서 시계 방향으로 */
    fun arc(sizePx: Int, strokePx: Float, ratio: Float): Bitmap =
        draw(sizePx, strokePx, sweep = 360f * ratio.coerceIn(0f, 1f), roundCap = true)

    private fun draw(sizePx: Int, strokePx: Float, sweep: Float, roundCap: Boolean): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        if (sweep <= 0f) return bitmap
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            color = Color.WHITE
            strokeCap = if (roundCap) Paint.Cap.ROUND else Paint.Cap.BUTT
        }
        // 선 두께의 절반만큼 안쪽에 그려야 바깥이 잘리지 않는다
        val inset = strokePx / 2
        val bounds = RectF(inset, inset, sizePx - inset, sizePx - inset)
        Canvas(bitmap).drawArc(bounds, -90f, sweep, false, paint)
        return bitmap
    }
}
