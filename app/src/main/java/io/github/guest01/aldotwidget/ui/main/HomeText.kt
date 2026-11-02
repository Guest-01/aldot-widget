package io.github.guest01.aldotwidget.ui.main

import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.RefreshPolicy
import io.github.guest01.aldotwidget.data.UsageFormat
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.ui.MainUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 홈 화면 새로고침 줄의 내용 */
internal data class HomeStatus(
    val text: String,
    val failed: Boolean,
    val refreshing: Boolean,
    val buttonLabel: String,
    val buttonEnabled: Boolean,
)

/**
 * 홈 화면의 글자들. 안드로이드에 의존하지 않게 분리해서 단위 테스트한다.
 * 시안 "앱 화면" ①~④의 새로고침 줄과 같다.
 */
internal object HomeText {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("M월 d일 HH:mm")
    private val periodEndFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val monthDayFormat = DateTimeFormatter.ofPattern("M월 d일")

    fun status(ready: MainUiState.Ready, now: Instant, zone: ZoneId): HomeStatus {
        val coolingDown = !RefreshPolicy.canRequest(ready.lastRequestedAt, now.toEpochMilli())
        val at = ready.snapshot.fetchedAt.atZone(zone)
        val time = (if (at.toLocalDate() == now.atZone(zone).toLocalDate()) timeFormat else dateTimeFormat).format(at)
        val failed = ready.error != null && !ready.refreshing
        val text = when {
            ready.refreshing -> "갱신 중…"
            failed -> "갱신 실패 · $time 기준 값"
            coolingDown -> "방금 갱신했어요"
            else -> "$time 기준"
        }
        val (label, enabled) = when {
            ready.refreshing -> "새로고침" to false
            coolingDown -> "1분 뒤 가능" to false
            failed -> "다시 시도" to true
            else -> "새로고침" to true
        }
        return HomeStatus(text, failed, ready.refreshing, label, enabled)
    }

    /** 요금제 아래 한 줄. 예: "알뜰폰 · 010-****-**** · 11월 30일까지 28일 남음" */
    fun planSubtitle(snapshot: UsageSnapshot): String {
        // 알닷의 lastDay(periodEnd)는 기간이 끝나는 날이 아니라 조회한 날이다 (10/26 조회, 5일 남음 → 10/31 끝).
        // 그래서 끝나는 날 = 조회한 날 + 남은 날.
        val days = snapshot.remainDays
        val queriedOn = snapshot.periodEnd?.let { runCatching { LocalDate.parse(it.trim(), periodEndFormat) }.getOrNull() }
        val until = if (queriedOn != null && days != null) "${monthDayFormat.format(queriedOn.plusDays(days.toLong()))}까지" else null
        val remain = days?.let { "${it}일 남음" }
        val period = listOfNotNull(until, remain).joinToString(" ").ifEmpty { null }
        return listOfNotNull(snapshot.partnerName, snapshot.phoneMasked, period).joinToString(" · ")
    }

    /** 데이터 카드 아래 줄. 예: "15.0 GB 중 9.3 GB 사용" */
    fun dataCaption(quota: Quota) = "${UsageFormat.dataKb(quota.total)} 중 ${UsageFormat.dataKb(quota.used)} 사용"

    /** 음성 카드 아래 줄. 좁은 카드에 맞게 분 단위만 쓴다. 예: "300분 중 87분 사용" */
    fun voiceCaption(quota: Quota) = "${quota.total / 60}분 중 ${quota.used / 60}분 사용"

    fun smsCaption(quota: Quota) = "${quota.total}건 중 ${quota.used}건 사용"
}
