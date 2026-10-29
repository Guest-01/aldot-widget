package io.github.guest01.aldotwidget.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// 위젯 목록의 항목 하나 = 수신기 하나 + 메타데이터 XML 하나 (매니페스트에 각각 등록).
// 세 수신기 모두 같은 반응형 위젯(UsageWidget)을 쓴다. 차이는 처음 놓이는 크기와 목록의 이름·미리보기뿐이고,
// 놓은 뒤 크기를 바꾸면 모양이 크기에 맞게 바뀐다.

/** 2×1: 데이터 남은 양 */
class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()
}

/** 2×2: 데이터 크게 + 음성·문자 */
class SquareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()
}

/** 4×2: 데이터·음성·문자 막대 + 새로고침 */
class DetailedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()
}

val allWidgetReceivers = listOf(
    CompactWidgetReceiver::class,
    SquareWidgetReceiver::class,
    DetailedWidgetReceiver::class,
)
