package io.github.guest01.aldotwidget.update

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.IOException

/**
 * update.json을 받아 읽는다.
 * 알닷용 클라이언트(로그인 쿠키, 브라우저 헤더)와 섞이지 않게 따로 만든 클라이언트를 쓴다.
 */
class UpdateChecker(private val client: OkHttpClient, private val updateUrl: HttpUrl) {

    /** @throws IOException 연결 실패, 200이 아닌 응답(공개 전 저장소의 404 포함), 읽을 수 없는 내용 */
    suspend fun fetch(): UpdateInfo {
        val request = Request.Builder().url(updateUrl).header("Accept", "application/json").build()
        client.newCall(request).executeAsync().use { response ->
            if (!response.isSuccessful) throw IOException("update.json HTTP ${response.code}")
            val text = response.body.string()
            return try {
                UpdateInfo.parse(text)
            } catch (e: IllegalArgumentException) {
                throw IOException("update.json을 읽을 수 없어요", e)
            }
        }
    }

    /** APK 주소: update.json과 같은 곳에 있는 파일 */
    fun apkUrl(info: UpdateInfo): HttpUrl =
        updateUrl.resolve(info.apk) ?: throw IOException("APK 주소를 만들 수 없어요: ${info.apk}")
}
