package io.github.guest01.aldotwidget.data

import io.github.guest01.aldotwidget.data.api.AldotApi
import io.github.guest01.aldotwidget.data.api.AldotException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 가짜 서버로 "회선 정보 → 실시간 사용량" 조회 순서와 실패 갈래를 확인한다. */
class UsageRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: UsageRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = UsageRepository(AldotApi(OkHttpClient(), server.url("/")))
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private val realTime = """{"resultCode":"0000","firstDay":"2026.11.01","lastDay":"2026.11.03","remainDay":27,
        "useDataList":[{"alloValue":"15728640","useValue":"9744384"}],
        "useVoicList":[{"alloValue":"18000","useValue":"5245"}],
        "useSmsList":[{"alloValue":"100","useValue":"12"}]}"""

    @Test
    fun `아이디가 있는 첫 회선으로 실시간 사용량을 조회한다`() = runBlocking {
        server.enqueue(json(200, """{"resultCode":"0000","entrSvcList":[
            {"entrId":"","xuserId":"X0"},
            {"entrId":"E1","xuserId":"X1","encnTlno":"010-12**-34**","mvnopPtnNm":"알뜰폰","ppNm":"5G 데이터 15GB+"},
            {"entrId":"E2","xuserId":"X2"}]}"""))
        server.enqueue(json(200, realTime))

        val snapshot = repository.fetchUsage()

        assertEquals("/umah/fo/care/main/v1/lineInfo", server.takeRequest().requestUrl!!.encodedPath)
        val usageRequest = server.takeRequest().requestUrl!!
        assertEquals("E1", usageRequest.queryParameter("entrId"))
        assertEquals("X1", usageRequest.queryParameter("xUserId"))
        assertEquals("5G 데이터 15GB+", snapshot.planName)
        assertEquals("알뜰폰", snapshot.partnerName)
        assertEquals(27, snapshot.remainDays)
        assertEquals(Quota(18000, 5245), snapshot.voice)
    }

    @Test
    fun `조회할 회선이 없으면 NoLine`() {
        server.enqueue(json(200, """{"resultCode":"0000","entrSvcList":[]}"""))
        assertThrows(AldotException.NoLine::class.java) { runBlocking { repository.fetchUsage() } }
        server.enqueue(json(200, """{"resultCode":"0000"}"""))
        assertThrows(AldotException.NoLine::class.java) { runBlocking { repository.fetchUsage() } }
    }

    @Test
    fun `서버가 정상이 아닌 결과 코드를 주면 그 코드를 담은 서버 오류`() {
        server.enqueue(json(200, """{"resultCode":"E0001","resultMsg":"점검 중"}"""))
        val error = assertThrows(AldotException.Server::class.java) { runBlocking { repository.fetchUsage() } }
        assertEquals("E0001", error.resultCode)
    }

    @Test
    fun `로그인 확인은 회선 정보가 성공하는지로 본다`() = runBlocking {
        server.enqueue(json(200, """{"resultCode":"0000","entrSvcList":[]}"""))
        assertTrue(repository.isLoggedIn())
        server.enqueue(json(401, """{"resultCode":"N0009","resultMsg":"로그아웃"}"""))
        assertFalse(repository.isLoggedIn())
    }
}
