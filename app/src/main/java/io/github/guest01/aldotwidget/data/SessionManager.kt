package io.github.guest01.aldotwidget.data

import android.webkit.CookieManager
import android.webkit.WebStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** 로그인 상태(쿠키와 사이트 저장소)를 지우는 일을 맡는다. */
class SessionManager {

    /**
     * 기기에 있는 알닷 로그인 흔적을 지운다. 서버 쪽 로그아웃 API는 부르지 않는다.
     * 쿠키(토큰)와 사이트의 localStorage를 함께 지워야 다음 로그인 화면이 깨끗하게 뜬다.
     */
    suspend fun clear() {
        WebStorage.getInstance().deleteAllData()
        val cookies = CookieManager.getInstance()
        suspendCancellableCoroutine { cont ->
            cookies.removeAllCookies { cont.resume(Unit) }
        }
        cookies.flush()
    }
}
