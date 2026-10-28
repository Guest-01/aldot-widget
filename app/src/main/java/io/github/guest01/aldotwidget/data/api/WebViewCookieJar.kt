package io.github.guest01.aldotwidget.data.api

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * OkHttp가 WebView의 쿠키 저장소(CookieManager)를 그대로 쓰게 하는 연결 고리.
 *
 * - 로그인은 WebView에서 하므로 로그인 쿠키는 CookieManager에 들어 있다.
 * - OkHttp 요청에는 거기서 꺼낸 쿠키를 싣고, 응답의 Set-Cookie(토큰 재발급)는 다시 거기에 저장한다.
 * 저장소가 하나뿐이라 앱 화면, 위젯, 백그라운드 작업이 항상 같은 로그인 상태를 본다.
 */
class WebViewCookieJar : CookieJar {

    private val cookieManager: CookieManager
        get() = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val manager = cookieManager
        cookies.forEach { manager.setCookie(url.toString(), it.toString()) }
        // 메모리에만 있던 쿠키를 디스크에 기록 (앱이 갑자기 종료돼도 유지되도록)
        manager.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // getCookie는 "a=1; b=2" 형식으로 이름=값만 돌려준다
        val header = cookieManager.getCookie(url.toString()) ?: return emptyList()
        return header.split(';').mapNotNull { Cookie.parse(url, it.trim()) }
    }
}
