package io.github.guest01.aldotwidget.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 시안(디자인 캔버스)의 색 이름을 그대로 옮긴 색 묶음. 위젯(WidgetColors)과 같은 값을 쓴다.
 * Material 색 체계(primary, surface…)만으로는 시안의 회색 단계가 다 표현되지 않아 따로 둔다.
 */
@Immutable
data class AppColors(
    /** 화면 바탕 */
    val bg: Color,
    val text: Color,
    /** 보조 글자 (라벨, 설명) */
    val sub: Color,
    /** 더 흐린 글자 (안내 문구) */
    val muted: Color,
    /** 바탕보다 살짝 어두운 카드 */
    val panel: Color,
    /** 막대 바탕, 테두리 */
    val line: Color,
    /** 카드 안 구분선 */
    val divider: Color,
    /** 아이콘 받침 */
    val stage: Color,
    /** 분할 버튼 바탕 */
    val segment: Color,
    /** 선택된 칸 바탕 (분할 버튼) */
    val raised: Color,
    /** 버튼 바탕. 흰 글자가 올라간다 */
    val button: Color,
    /** 강조 글자·막대·아이콘. 다크 모드에서는 어두운 바탕에서 읽히는 밝은 파랑 */
    val accent: Color,
    val warn: Color,
    val warnBg: Color,
)

val LightAppColors = AppColors(
    bg = Color(0xFFFFFFFF),
    text = Color(0xFF191F28),
    sub = Color(0xFF4E5968),
    muted = Color(0xFF6B7684),
    panel = Color(0xFFF9FAFB),
    line = Color(0xFFE5E8EB),
    divider = Color(0xFFEEF0F3),
    stage = Color(0xFFE8EEF9),
    segment = Color(0xFFF2F4F6),
    raised = Color(0xFFFFFFFF),
    button = Color(0xFF1B64DA),
    accent = Color(0xFF1B64DA),
    warn = Color(0xFFB42318),
    warnBg = Color(0xFFFEF3F2),
)

val DarkAppColors = AppColors(
    bg = Color(0xFF101318),
    text = Color(0xFFF2F4F6),
    sub = Color(0xFFB0B8C1),
    muted = Color(0xFF8B95A1),
    panel = Color(0xFF1B1F26),
    line = Color(0xFF3A414B),
    divider = Color(0xFF2E343D),
    stage = Color(0xFF1A2333),
    segment = Color(0xFF1B1F26),
    raised = Color(0xFF262B34),
    button = Color(0xFF1B64DA),
    accent = Color(0xFF7FB0FF),
    warn = Color(0xFFFF8A80),
    warnBg = Color(0xFF2C1B1B),
)
