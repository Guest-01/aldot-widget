package io.github.guest01.aldotwidget.data

import java.time.Instant

/** 미리보기(위젯 목록, Compose Preview)에 쓰는 가짜 사용량. 실제 사용자 정보는 넣지 않는다. */
fun sampleSnapshot(fetchedAt: Instant = Instant.now()) = UsageSnapshot(
    phoneMasked = "010-****-****",
    partnerName = "알뜰폰",
    planName = "5G 요금제 (300분/15GB)",
    periodStart = "2026.10.01",
    periodEnd = "2026.10.20",
    remainDays = 11,
    data = Quota(total = 15L * 1024 * 1024, used = 9L * 1024 * 1024 + 300 * 1024),
    voice = Quota(total = 300L * 60, used = 87L * 60 + 25),
    sms = Quota(total = 100, used = 12),
    fetchedAt = fetchedAt,
)
