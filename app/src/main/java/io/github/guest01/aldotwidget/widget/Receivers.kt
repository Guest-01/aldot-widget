package io.github.guest01.aldotwidget.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// 위젯 목록의 항목 하나 = 수신기 하나 + 메타데이터 XML 하나 (매니페스트에 각각 등록).
// 세 수신기 모두 같은 위젯(UsageWidget)을 쓴다. 차이는 처음 놓이는 크기와 목록의 이름·미리보기뿐이고,
// 놓은 뒤 크기를 바꾸면 모양이 크기에 맞게 바뀐다 (2×2를 넓히면 막대 세 줄 모양).
// 수신기 클래스 이름을 바꾸거나 지우면 이미 홈 화면에 놓인 그 위젯이 사라지므로 주의한다.

/** 1×1: 데이터 고리 */
class TinyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()
}

/** 2×1: 고리와 데이터 남은 양 */
class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()
}

/** 2×2: 데이터 크게 + 음성·문자 */
class SquareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()
}

val allWidgetReceivers = listOf(
    TinyWidgetReceiver::class,
    CompactWidgetReceiver::class,
    SquareWidgetReceiver::class,
)
