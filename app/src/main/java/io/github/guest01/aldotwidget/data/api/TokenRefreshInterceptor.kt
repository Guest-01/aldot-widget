package io.github.guest01.aldotwidget.data.api

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * 액세스 토큰이 만료되면(401 NS009 / 403 NS008) 토큰을 재발급하고 원래 요청을 한 번만 다시 보낸다.
 * 알닷 사이트의 axios 응답 인터셉터와 같은 동작이다.
 *
 * 토큰은 쿠키로 오가므로 이 클래스는 쿠키를 직접 만지지 않는다.
 * 재발급 응답의 Set-Cookie는 OkHttp의 CookieJar가 저장하고, 재시도 요청에 자동으로 실린다.
 */
class TokenRefreshInterceptor(private val refreshUrl: HttpUrl) : Interceptor {

    private val lock = Any()

    /** 마지막으로 재발급에 성공한 시각 (System.nanoTime) */
    @Volatile
    private var lastRefreshAt = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedAt = System.nanoTime()
        val response = chain.proceed(request)
        if (request.url == refreshUrl || !isTokenExpired(response)) return response

        // 같은 호출 안에서 다시 proceed()하려면 이전 응답을 먼저 닫아야 한다.
        // 재발급이 실패하면 원래 응답을 그대로 돌려줘야 하니 본문을 복사해 둔다.
        val failedBody = response.peekBody(MAX_PEEK).bytes()
        val contentType = response.body.contentType()
        response.close()

        synchronized(lock) {
            // 기다리는 사이 다른 요청이 이미 재발급했다면 재발급을 건너뛴다.
            if (lastRefreshAt < startedAt) {
                if (!refresh(chain)) {
                    return response.newBuilder()
                        .body(failedBody.toResponseBody(contentType))
                        .build()
                }
                lastRefreshAt = System.nanoTime()
            }
        }
        return chain.proceed(request)
    }

    private fun refresh(chain: Interceptor.Chain): Boolean {
        val refreshRequest = Request.Builder()
            .url(refreshUrl)
            .post("{}".toRequestBody(JSON))
            .build()
        return chain.proceed(refreshRequest).use { resp ->
            resp.isSuccessful && resultCodeOf(resp.peekBody(MAX_PEEK).string()) == "0000"
        }
    }

    private fun isTokenExpired(response: Response): Boolean {
        if (response.code != 401 && response.code != 403) return false
        val code = resultCodeOf(response.peekBody(MAX_PEEK).string())
        return (response.code == 401 && code == "NS009") || (response.code == 403 && code == "NS008")
    }

    private fun resultCodeOf(body: String): String? =
        runCatching { AldotJson.decodeFromString<ResultHeader>(body).resultCode }.getOrNull()

    private companion object {
        const val MAX_PEEK = 64L * 1024
        val JSON = "application/json".toMediaType()
    }
}
