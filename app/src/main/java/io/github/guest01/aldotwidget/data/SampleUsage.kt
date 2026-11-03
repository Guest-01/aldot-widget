package io.github.guest01.aldotwidget.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 미리보기(위젯 목록, Compose Preview)와 샘플 데이터 모드에 쓰는 가짜 사용량. 실제 사용자 정보는 넣지 않는다.
 * 번호는 알닷 응답처럼 일부만 가리고, 기간은 이번 달 1일~말일.
 */
fun sampleSnapshot(fetchedAt: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): UsageSnapshot {
    val today = fetchedAt.atZone(zone).toLocalDate()
    val end = today.with(TemporalAdjusters.lastDayOfMonth())
    val apiDate = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    return UsageSnapshot(
        phoneMasked = "010-12**-34**",
        // 위젯 목록 미리보기와 환영 그림에도 쓰여서, 특정 파트너사가 드러나지 않는 이름으로
        partnerName = "알뜰폰",
        planName = "5G 데이터 15GB+",
        periodStart = apiDate.format(today.withDayOfMonth(1)),
        // 알닷의 lastDay는 조회한 날이다 (끝나는 날 = 조회한 날 + 남은 날)
        periodEnd = apiDate.format(today),
        remainDays = ChronoUnit.DAYS.between(today, end).toInt(),
        data = Quota(total = 15L * 1024 * 1024, used = 9L * 1024 * 1024 + 300 * 1024),
        voice = Quota(total = 300L * 60, used = 87L * 60 + 25),
        sms = Quota(total = 100, used = 12),
        fetchedAt = fetchedAt,
    )
}
