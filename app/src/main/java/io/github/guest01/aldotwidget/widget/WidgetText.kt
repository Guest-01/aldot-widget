package io.github.guest01.aldotwidget.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 위젯 크기별 모양. 시안의 "위젯 크기 × 상태" 표와 같다. */
internal enum class WidgetLayout {
    /** 1×1: 가운데 고리, 아래 시각 줄이 새로고침 */
    TINY,

    /** 2×1: 왼쪽 고리, 오른쪽 남은 용량 */
    COMPACT,

    /** 2×2: 데이터 크게 + 음성·문자 */
    SQUARE,

    /** 2×2를 넓혔을 때: 막대 세 줄 */
    WIDE;

    companion object {
        // 실측 칸 크기: Fold8 커버 1칸 ≈113dp·2칸 226dp·1줄 101dp, Pixel 2칸 179dp·2줄 210dp.
        // 기준값은 칸 경계 사이에 두었다 (1칸 < 140dp ≤ 2칸, 1줄 < 180dp ≤ 2줄, 2칸 < 250dp ≤ 3칸).
        private val TWO_COLUMNS = 140.dp
        private val TWO_ROWS = 180.dp
        private val THREE_COLUMNS = 250.dp

        fun of(size: DpSize): WidgetLayout = when {
            size.width < TWO_COLUMNS -> TINY
            size.height < TWO_ROWS -> COMPACT
            size.width < THREE_COLUMNS -> SQUARE
            else -> WIDE
        }
    }
}

/** 위젯 아래쪽 상태 글자. 안드로이드에 의존하지 않게 분리해서 단위 테스트한다. */
internal object WidgetText {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormat = DateTimeFormatter.ofPattern("M/d")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("M/d HH:mm")

    /** 넓은 위젯용. 예: "20:08 기준", "10/27 20:08 기준", "갱신 중…", "갱신 실패 · 20:08" */
    fun long(fetchedAt: Instant, failed: Boolean, refreshing: Boolean, now: Instant, zone: ZoneId): String {
        val at = fetchedAt.atZone(zone)
        val time = (if (at.toLocalDate() == now.atZone(zone).toLocalDate()) timeFormat else dateTimeFormat).format(at)
        return when {
            refreshing -> "갱신 중…"
            failed -> "갱신 실패 · $time"
            else -> "$time 기준"
        }
    }

    /** 1×1용 짧은 글자. 예: "20:08", "10/27", "갱신 중", "갱신 실패" */
    fun short(fetchedAt: Instant, failed: Boolean, refreshing: Boolean, now: Instant, zone: ZoneId): String {
        val at = fetchedAt.atZone(zone)
        return when {
            refreshing -> "갱신 중"
            failed -> "갱신 실패"
            at.toLocalDate() == now.atZone(zone).toLocalDate() -> timeFormat.format(at)
            else -> dateFormat.format(at)
        }
    }

    /** "5.7 GB" → ("5.7", "GB"). 1×1 고리 안에 숫자와 단위를 두 줄로 쓰기 위해 나눈다. */
    fun splitUnit(text: String): Pair<String, String> {
        val space = text.lastIndexOf(' ')
        return if (space < 0) text to "" else text.substring(0, space) to text.substring(space + 1)
    }
}
