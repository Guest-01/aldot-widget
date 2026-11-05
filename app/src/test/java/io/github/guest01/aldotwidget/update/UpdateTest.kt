package io.github.guest01.aldotwidget.update

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class UpdateInfoTest {

    private val sample = """
        {
          "version": "1.1.0",
          "versionCode": 2,
          "apk": "aldot-widget-1.1.0.apk",
          "size": 4301824,
          "sha256": "9f2c",
          "released": "2026-11-20",
          "notes": ["무제한 요금제", "글씨 크기"],
          "extra": "앞으로 늘어날 필드는 무시한다"
        }
    """.trimIndent()

    @Test
    fun `update json을 읽는다`() {
        val info = UpdateInfo.parse(sample)
        assertEquals("1.1.0", info.version)
        assertEquals(2, info.versionCode)
        assertEquals(listOf("무제한 요금제", "글씨 크기"), info.notes)
    }

    @Test
    fun `released와 notes는 없어도 된다`() {
        val info = UpdateInfo.parse("""{"version":"1.0.1","versionCode":2,"apk":"a.apk","size":1,"sha256":"x"}""")
        assertNull(info.released)
        assertTrue(info.notes.isEmpty())
    }

    @Test
    fun `versionCode가 클 때만 새 버전이다`() {
        val info = UpdateInfo.parse(sample)
        assertTrue(info.isNewerThan(1))
        assertFalse(info.isNewerThan(2))
        assertFalse(info.isNewerThan(3))
    }

    @Test
    fun `저장했다 읽어도 같다`() {
        val info = UpdateInfo.parse(sample)
        assertEquals(info, UpdateInfo.parse(UpdateInfo.toJson(info)))
    }

    @Test
    fun `자동 확인은 하루가 지나야 다시 한다`() {
        val day = UpdatePolicy.CHECK_INTERVAL_MILLIS
        assertTrue(UpdatePolicy.isDue(lastCheckedAt = 0, now = 1_000))
        assertFalse(UpdatePolicy.isDue(lastCheckedAt = 1_000, now = 1_000 + day - 1))
        assertTrue(UpdatePolicy.isDue(lastCheckedAt = 1_000, now = 1_000 + day))
    }

    @Test
    fun `크기와 날짜를 화면 글자로`() {
        assertEquals("4.1MB", UpdateText.size(4_301_824))
        assertEquals("0.0MB", UpdateText.size(0))
        assertEquals("11월 20일", UpdateText.date("2026-11-20"))
        assertNull(UpdateText.date("어제"))
        assertNull(UpdateText.date(null))
        assertEquals("지금 1.0.0 · 4.1MB · 11월 20일", UpdateText.summary(UpdateInfo.parse(sample), "1.0.0"))
    }

    @Test
    fun `버전 번호 뒤 조사는 마지막 숫자를 읽는 소리를 따른다`() {
        // 영(받침) 일(ㄹ) 이 삼(받침) 사 오 육(받침) 칠(ㄹ) 팔(ㄹ) 구
        val subject = (0..9).map { UpdateText.subject("1.1.$it").removePrefix("1.1.$it") }
        assertEquals(listOf("이", "이", "가", "이", "가", "가", "이", "이", "이", "가"), subject)
        val toward = (0..9).map { UpdateText.toward("1.1.$it").removePrefix("1.1.$it") }
        assertEquals(listOf("으로", "로", "로", "으로", "로", "로", "으로", "로", "로", "로"), toward)
        assertEquals("1.10.0이", UpdateText.subject("1.10.0"))
    }
}

/** 가짜 서버로 update.json 받기와 실패를 확인한다 */
class UpdateCheckerTest {

    private lateinit var server: MockWebServer
    private lateinit var checker: UpdateChecker

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        checker = UpdateChecker(OkHttpClient(), server.url("/releases/latest/download/update.json"))
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `받아서 읽는다`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"version":"1.0.1","versionCode":2,"apk":"a.apk","size":1,"sha256":"x"}"""))
        assertEquals(2, checker.fetch().versionCode)
    }

    @Test
    fun `공개 전 저장소처럼 404면 실패다`() {
        server.enqueue(MockResponse().setResponseCode(404))
        assertThrows(IOException::class.java) { runBlocking { checker.fetch() } }
    }

    @Test
    fun `읽을 수 없는 내용이면 실패다`() {
        server.enqueue(MockResponse().setBody("<html>not json</html>"))
        assertThrows(IOException::class.java) { runBlocking { checker.fetch() } }
    }

    @Test
    fun `APK는 update json과 같은 곳에서 받는다`() {
        val info = UpdateInfo("1.0.1", 2, "aldot-widget-1.0.1.apk", 1, "x")
        assertEquals(server.url("/releases/latest/download/aldot-widget-1.0.1.apk"), checker.apkUrl(info))
    }
}
