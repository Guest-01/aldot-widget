package io.github.guest01.aldotwidget.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync

val AldotJson = Json {
    ignoreUnknownKeys = true
    // null이 와도 기본값이 있는 필드면 기본값으로 채운다
    coerceInputValues = true
}

/**
 * 알닷케어 비공식 API 클라이언트. 엔드포인트는 docs/aldot-api.md 참고.
 * 인증은 OkHttpClient에 연결된 CookieJar가, 토큰 재발급은 TokenRefreshInterceptor가 맡는다.
 */
class AldotApi(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL,
) {
    suspend fun lineInfo(): LineInfoResponse =
        get(baseUrl.resolve(LINE_INFO_PATH)!!)

    suspend fun realTime(entrId: String, xUserId: String): RealTimeResponse =
        get(
            baseUrl.resolve(REAL_TIME_PATH)!!.newBuilder()
                .addQueryParameter("entrId", entrId)
                .addQueryParameter("xUserId", xUserId)
                .build()
        )

    /**
     * 서버 세션을 끊는다. 사이트와 같이 빈 JSON을 POST한다.
     * 서버는 빈 본문으로 응답하므로 본문은 해석하지 않고 상태 코드만 확인한다.
     */
    suspend fun logout() {
        val request = Request.Builder()
            .url(baseUrl.resolve(LOGOUT_PATH)!!)
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .build()
        send(request)
    }

    private suspend inline fun <reified T> get(url: HttpUrl): T {
        val body = send(Request.Builder().url(url).get().build())
        return AldotJson.decodeFromString<T>(body)
    }

    /** 요청을 보내고 오류를 AldotException으로 바꾼 뒤, 정상이면 응답 본문을 돌려준다. */
    private suspend fun send(request: Request): String {
        // executeAsync는 코루틴이 취소되면 HTTP 요청도 취소한다 (블로킹 execute()는 취소되지 않음)
        val response = client.newCall(request).executeAsync()
        // 본문 읽기는 여전히 블로킹이라 IO 스레드에서 한다
        val body = withContext(Dispatchers.IO) { response.use { it.body.string() } }
        val header = runCatching { AldotJson.decodeFromString<ResultHeader>(body) }.getOrNull()
        when {
            response.code == 401 || response.code == 403 || header?.resultCode in LOGOUT_CODES ->
                throw AldotException.NeedLogin()
            !response.isSuccessful ->
                throw AldotException.Server(response.code, header?.resultCode, header?.resultMsg)
            header?.resultCode != null && header.resultCode != "0000" ->
                throw AldotException.Server(response.code, header.resultCode, header.resultMsg)
        }
        return body
    }

    companion object {
        val DEFAULT_BASE_URL = "https://api.uplusmvno.com/".toHttpUrl()
        const val LINE_INFO_PATH = "/umah/fo/care/main/v1/lineInfo"
        const val REAL_TIME_PATH = "/umah/fo/care/use/v1/real-time"
        const val REFRESH_PATH = "/umah/fo/user/v1/refreshTokenCreate"
        const val LOGOUT_PATH = "/umah/fo/user/v1/logout"

        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        /** 사이트가 즉시 로그아웃 처리하는 결과 코드 */
        private val LOGOUT_CODES = setOf("N0009", "N9999")
    }
}
