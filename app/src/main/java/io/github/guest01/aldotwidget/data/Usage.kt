package io.github.guest01.aldotwidget.data

import io.github.guest01.aldotwidget.data.api.LineDto
import io.github.guest01.aldotwidget.data.api.RealTimeResponse
import io.github.guest01.aldotwidget.data.api.UsageItemDto
import java.time.Instant

/** 제공량과 사용량. 단위는 종류마다 다르다 (데이터 KB, 음성 초, 문자 건). */
data class Quota(val total: Long, val used: Long) {
    val remaining: Long get() = (total - used).coerceAtLeast(0)

    /** 남은 비율 0.0~1.0 */
    val remainingRatio: Float get() = if (total <= 0) 0f else remaining.toFloat() / total
}

/** 화면과 위젯이 그리는 데 필요한 값만 모은 사용량 스냅샷 */
data class UsageSnapshot(
    val phoneMasked: String?,
    val partnerName: String?,
    val planName: String?,
    val periodStart: String?,
    val periodEnd: String?,
    val remainDays: Int?,
    val data: Quota?,
    val voice: Quota?,
    val sms: Quota?,
    val fetchedAt: Instant,
)

fun RealTimeResponse.toSnapshot(line: LineDto, fetchedAt: Instant) = UsageSnapshot(
    phoneMasked = line.encnTlno,
    partnerName = line.mvnopPtnNm,
    planName = line.ppNm,
    periodStart = firstDay,
    periodEnd = lastDay,
    remainDays = remainDay,
    data = useDataList.sumQuota(),
    voice = useVoicList.sumQuota(),
    sms = useSmsList.sumQuota(),
    fetchedAt = fetchedAt,
)

/**
 * 한 종류 안에 항목이 여러 개일 수 있다 (기본 제공 + 이월/추가 데이터 등). 숫자로 읽히는 항목만 합산한다.
 * 읽을 수 있는 항목이 하나도 없으면 null (화면에서 "정보 없음"으로 표시).
 */
private fun List<UsageItemDto>?.sumQuota(): Quota? {
    val parsed = this.orEmpty().mapNotNull { item ->
        val total = item.alloValue?.trim()?.toLongOrNull() ?: return@mapNotNull null
        val used = item.useValue?.trim()?.toLongOrNull() ?: 0L
        total to used
    }
    if (parsed.isEmpty()) return null
    return Quota(total = parsed.sumOf { it.first }, used = parsed.sumOf { it.second })
}
