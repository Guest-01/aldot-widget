package io.github.guest01.aldotwidget.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

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

    private suspend inline fun <reified T> get(url: HttpUrl): T = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            val header = runCatching { AldotJson.decodeFromString<ResultHeader>(body) }.getOrNull()
            when {
                response.code == 401 || response.code == 403 || header?.resultCode in LOGOUT_CODES ->
                    throw AldotException.NeedLogin()
                !response.isSuccessful ->
                    throw AldotException.Server(response.code, header?.resultCode, header?.resultMsg)
                header?.resultCode != null && header.resultCode != "0000" ->
                    throw AldotException.Server(response.code, header.resultCode, header.resultMsg)
            }
            AldotJson.decodeFromString<T>(body)
        }
    }

    companion object {
        val DEFAULT_BASE_URL = "https://api.uplusmvno.com/".toHttpUrl()
        const val LINE_INFO_PATH = "/umah/fo/care/main/v1/lineInfo"
        const val REAL_TIME_PATH = "/umah/fo/care/use/v1/real-time"
        const val REFRESH_PATH = "/umah/fo/user/v1/refreshTokenCreate"

        /** 사이트가 즉시 로그아웃 처리하는 결과 코드 */
        private val LOGOUT_CODES = setOf("N0009", "N9999")
    }
}
