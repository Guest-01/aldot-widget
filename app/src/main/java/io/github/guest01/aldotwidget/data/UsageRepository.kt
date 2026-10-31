package io.github.guest01.aldotwidget.data

import io.github.guest01.aldotwidget.data.api.AldotApi
import io.github.guest01.aldotwidget.data.api.AldotException
import java.time.Instant

/**
 * 화면(ViewModel)과 나중에 붙일 위젯·백그라운드 작업이 공통으로 쓰는 데이터 진입점.
 * 지금은 API만 감싸지만, 2단계에서 마지막 스냅샷 저장이 여기에 추가된다.
 */
class UsageRepository(private val api: AldotApi) {

    /** 회선 정보 → 실시간 사용량 순서로 조회한다. 회선이 여러 개면 일단 첫 번째 회선만 본다. */
    suspend fun fetchUsage(): UsageSnapshot {
        val line = api.lineInfo().entrSvcList
            ?.firstOrNull { !it.entrId.isNullOrBlank() && !it.xuserId.isNullOrBlank() }
            ?: throw AldotException.NoLine()
        val realTime = api.realTime(line.entrId!!, line.xuserId!!)
        return realTime.toSnapshot(line, Instant.now())
    }

    /** 로그인을 2시간 연장한다. */
    suspend fun extendSession() = api.refreshSession()

    /** 로그인된 상태인지 가볍게 확인한다 (회선 정보 API가 성공하는지). */
    suspend fun isLoggedIn(): Boolean = try {
        api.lineInfo()
        true
    } catch (e: AldotException.NeedLogin) {
        false
    }
}
