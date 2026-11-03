package io.github.guest01.aldotwidget.data.api

import java.io.IOException

/**
 * 알닷 API 오류. IOException을 상속해서 OkHttp의 네트워크 오류와 같은 catch로 받을 수 있게 한다.
 */
sealed class AldotException(message: String) : IOException(message) {
    /** 토큰 재발급까지 실패해서 다시 로그인해야 하는 상태 */
    class NeedLogin(message: String = "다시 로그인해야 해요") : AldotException(message)

    /** 로그인은 됐지만 조회할 회선이 없음 */
    class NoLine : AldotException("조회할 회선이 없어요")

    /** 그 밖의 서버 오류 (HTTP 오류 또는 resultCode가 정상이 아님) */
    class Server(val httpCode: Int, val resultCode: String?, message: String?) :
        AldotException(message ?: "서버 오류 (HTTP $httpCode, $resultCode)")
}
