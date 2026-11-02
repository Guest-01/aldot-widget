package io.github.guest01.aldotwidget.data

/**
 * 사용자가 직접 누르는 새로고침 규칙.
 *
 * - 1분에 한 번만 받는다. 위젯을 연타해도 알닷 서버에 요청이 몰리지 않게 하려는 것이다.
 * - "갱신 중" 표시는 2분이 지나면 끝난 것으로 본다. 작업이 중간에 죽어도 표시가 계속 남지 않게 하려는 것이다.
 */
object RefreshPolicy {
    const val COOLDOWN_MILLIS = 60_000L
    private const val REFRESHING_TIMEOUT_MILLIS = 2 * 60_000L

    fun canRequest(lastRequestedAt: Long?, now: Long): Boolean =
        // 시계가 뒤로 돌아간 경우(lastRequestedAt > now)도 막지 않는다
        lastRequestedAt == null || now - lastRequestedAt >= COOLDOWN_MILLIS || lastRequestedAt > now

    fun isRefreshing(refreshingSince: Long?, now: Long): Boolean =
        refreshingSince != null && now - refreshingSince in 0 until REFRESHING_TIMEOUT_MILLIS
}
