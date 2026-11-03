package io.github.guest01.aldotwidget.ui.main

import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.RefreshPolicy
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.ui.MainUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** 홈 화면 아래 줄(갱신 시각)과 새로고침 아이콘의 상태 */
internal data class HomeStatus(
    val text: String,
    /** 마지막 갱신 시각. 실패 안내에 "20:08 값을 보여 주고 있어요"처럼 쓴다 */
    val time: String,
    val failed: Boolean,
    val refreshing: Boolean,
    /** 새로고침 아이콘을 누를 수 있는지 (갱신 중이거나 1분 제한 중이면 흐리게) */
    val canRefresh: Boolean,
)

/** 이용 기간. 알닷은 시작일, 조회한 날, 남은 날 수만 알려 준다 */
internal data class UsagePeriod(val start: LocalDate, val today: LocalDate, val end: LocalDate) {
    val totalDays: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
    /** 오늘이 기간의 며칠째인지 (1부터) */
    val dayNumber: Int get() = ChronoUnit.DAYS.between(start, today).toInt() + 1
    /** 오늘을 빼고 남은 날 */
    val daysLeft: Int get() = ChronoUnit.DAYS.between(today, end).toInt()
}

/**
 * 홈 화면의 글자들. 안드로이드에 의존하지 않게 분리해서 단위 테스트한다.
 * 시안 "앱 화면"의 홈(①~⑨)과 같다.
 */
internal object HomeText {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("M월 d일 HH:mm")
    private val apiDateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val monthDayFormat = DateTimeFormatter.ofPattern("M월 d일")

    fun status(ready: MainUiState.Ready, now: Instant, zone: ZoneId): HomeStatus {
        val coolingDown = !RefreshPolicy.canRequest(ready.lastRequestedAt, now.toEpochMilli())
        val at = ready.snapshot.fetchedAt.atZone(zone)
        val time = (if (at.toLocalDate() == now.atZone(zone).toLocalDate()) timeFormat else dateTimeFormat).format(at)
        val failed = ready.error != null && !ready.refreshing
        val text = when {
            ready.refreshing -> "갱신 중…"
            // 실패는 위쪽 안내 카드가 알리고, 아래 줄은 경고 색으로 시각만
            failed -> "$time 기준"
            coolingDown -> "방금 갱신했어요"
            else -> "$time 기준"
        }
        return HomeStatus(text, time, failed, ready.refreshing, canRefresh = !ready.refreshing && !coolingDown)
    }

    fun period(snapshot: UsageSnapshot): UsagePeriod? {
        val start = parse(snapshot.periodStart) ?: return null
        // 알닷의 lastDay(periodEnd)는 기간이 끝나는 날이 아니라 조회한 날이다 (10/26 조회, 5일 남음 → 10/31 끝).
        // 그래서 끝나는 날 = 조회한 날 + 남은 날.
        val today = parse(snapshot.periodEnd) ?: return null
        val days = snapshot.remainDays ?: return null
        val period = UsagePeriod(start, today, today.plusDays(days.toLong()))
        return period.takeIf { it.dayNumber in 1..it.totalDays }
    }

    /** 예: "11월 1일 ~ 11월 30일" */
    fun periodRange(period: UsagePeriod) = "${monthDayFormat.format(period.start)} ~ ${monthDayFormat.format(period.end)}"

    /** 오른쪽 아래. 예: "28일 남음", 마지막 날은 "오늘까지" */
    fun daysLeft(period: UsagePeriod) = if (period.daysLeft == 0) "오늘까지" else "${period.daysLeft}일 남음"

    /** 예: "티플러스 · LG U+망". 알닷 회선은 모두 LG U+망이다 */
    fun carrier(snapshot: UsageSnapshot) = listOfNotNull(snapshot.partnerName, "LG U+망").joinToString(" · ")

    /** 위쪽 계기판 아래 한 줄. 남은 날에 맞춰 하루에 쓸 수 있는 양, 다 썼거나 마지막 날이면 그 안내 */
    fun dataPill(data: Quota?, period: UsagePeriod?, remainDays: Int?): String? {
        if (data == null) return null
        val days = period?.daysLeft ?: remainDays
        return when {
            data.remaining <= 0 -> period?.let { "데이터를 다 썼어요 · ${monthDayFormat.format(it.end.plusDays(1))}에 다시 채워져요" }
                ?: "데이터를 다 썼어요"
            days == null -> null
            days == 0 -> "오늘이 마지막 날이에요 · 내일 다시 채워져요"
            else -> "남은 ${days}일, 하루 ${dailyAmount(data.remaining / days)}씩 쓸 수 있어요"
        }
    }

    /** 하루 양은 대략이면 돼서 짧게. 100 MB 이상은 "0.2 GB", 그보다 적으면 "85 MB" */
    fun dailyAmount(kb: Long): String = when {
        kb >= 100 * KB_PER_MB -> String.format(Locale.ROOT, "%.1f GB", kb.toDouble() / KB_PER_GB)
        kb >= KB_PER_MB -> "${kb / KB_PER_MB} MB"
        else -> "$kb KB"
    }

    /** 음성은 분 단위만 (초는 버린다). 예: 12755초 → "212" */
    fun voiceMinutes(seconds: Long) = "${seconds / 60}"

    private fun parse(value: String?) = value?.let { runCatching { LocalDate.parse(it.trim(), apiDateFormat) }.getOrNull() }

    private const val KB_PER_MB = 1024L
    private const val KB_PER_GB = 1024L * 1024L
}
