package io.github.guest01.aldotwidget.data.api

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/** 가짜 서버(MockWebServer)로 토큰 만료 → 재발급 → 재시도 흐름을 확인한다. */
class AldotApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AldotApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/")
        val client = OkHttpClient.Builder()
            .addInterceptor(TokenRefreshInterceptor(baseUrl.resolve(AldotApi.REFRESH_PATH)!!))
            .build()
        api = AldotApi(client, baseUrl)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private val expired = """{"resultCode":"NS009","resultMsg":"AccessToken is Expired."}"""
    private val lineOk = """{"resultCode":"0000","entrSvcList":[{"entrId":"E1","xuserId":"X1"}]}"""

    @Test
    fun `토큰이 만료되면 재발급 후 원래 요청을 다시 보낸다`() = runBlocking {
        server.enqueue(json(401, expired))
        server.enqueue(json(200, """{"resultCode":"0000","resultMsg":"토큰재발행완료."}"""))
        server.enqueue(json(200, lineOk))

        val result = api.lineInfo()

        assertEquals("E1", result.entrSvcList!!.single().entrId)
        assertEquals(AldotApi.LINE_INFO_PATH, server.takeRequest().path)
        val refresh = server.takeRequest()
        assertEquals("POST", refresh.method)
        assertEquals(AldotApi.REFRESH_PATH, refresh.path)
        assertEquals(AldotApi.LINE_INFO_PATH, server.takeRequest().path)
    }

    @Test
    fun `재발급도 실패하면 다시 로그인해야 한다`() {
        server.enqueue(json(401, expired))
        server.enqueue(json(401, expired))

        assertThrows(AldotException.NeedLogin::class.java) {
            runBlocking { api.lineInfo() }
        }
        assertEquals(2, server.requestCount) // 원래 요청 + 재발급 한 번. 무한 재시도하지 않는다.
    }

    @Test
    fun `로그아웃은 빈 JSON을 POST하고 빈 응답도 성공으로 본다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200)) // 실제 서버도 본문 없이 응답한다

        api.logout()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals(AldotApi.LOGOUT_PATH, request.path)
        assertEquals("{}", request.body.readUtf8())
    }

    @Test
    fun `실시간 사용량 요청에 회선 파라미터를 싣는다`() = runBlocking {
        server.enqueue(json(200, """{"resultCode":"0000","remainDay":3}"""))

        val result = api.realTime(entrId = "E1", xUserId = "X1")

        assertEquals(3, result.remainDay)
        val url = server.takeRequest().requestUrl!!
        assertEquals("E1", url.queryParameter("entrId"))
        assertEquals("X1", url.queryParameter("xUserId"))
    }
}
