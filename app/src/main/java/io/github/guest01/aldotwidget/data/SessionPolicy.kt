package io.github.guest01.aldotwidget.data

import kotlin.random.Random

/**
 * 로그인 유지 규칙. 알닷 로그인 쿠키는 재발급 후 2시간이면 만료되므로(실측) 그 안에 다시 재발급해야 한다.
 *
 * - 조회할 때: 마지막 연장 후 45분이 지났으면 먼저 연장한다. 토큰이 만료될 때까지 기다리면
 *   제시간에 실행돼도 연장이 안 되는 경우가 생겨 여유가 거의 사라진다.
 * - 안전망 알람: 연장할 때마다 60~75분 뒤로 다시 맞춘다. 폰이 쉬는 동안 알람은 30~40분 늦게 온다(실측:
 *   80~95분으로 맞췄을 때 113~116분 뒤 도착). 늦게 오고 한 번 실패해도 2시간 안에 재시도할 여유를 둔 값이다.
 *   범위를 둔 것은 사용자들의 요청이 한 시각에 몰리지 않게 하기 위해서다.
 */
object SessionPolicy {
    private const val MINUTE = 60_000L
    private const val EXTEND_AFTER = 45 * MINUTE
    private const val ALARM_MIN = 60 * MINUTE
    private const val ALARM_MAX = 75 * MINUTE
    private const val SESSION_LIFETIME = 120 * MINUTE

    /** 알람 때 네트워크가 안 되면 잠시 뒤 다시 시도한다 */
    const val ALARM_RETRY_MILLIS = 10 * MINUTE

    fun shouldExtend(lastExtendedAt: Long, now: Long): Boolean =
        // 시계가 뒤로 돌아간 경우(lastExtendedAt > now)도 안전하게 연장한다
        now - lastExtendedAt >= EXTEND_AFTER || lastExtendedAt > now

    fun nextAlarmDelayMillis(random: Random = Random.Default): Long =
        random.nextLong(ALARM_MIN, ALARM_MAX + 1)

    /** 쿠키가 이미 만료됐을 시간이 지났으면 오프라인 재시도를 멈춘다 (다음 조회 때 결과를 알게 된다) */
    fun shouldRetryAlarm(lastExtendedAt: Long, now: Long): Boolean =
        now - lastExtendedAt < SESSION_LIFETIME
}
