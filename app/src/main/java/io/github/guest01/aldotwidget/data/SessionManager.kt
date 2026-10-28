package io.github.guest01.aldotwidget.data

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import io.github.guest01.aldotwidget.data.api.AldotApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** 로그인 상태를 끝내는 일을 맡는다. */
class SessionManager(private val api: AldotApi) {

    /**
     * 알닷 사이트와 같은 순서로 로그아웃한다.
     * 1. 서버에 로그아웃을 알린다. 실패해도(오프라인, 이미 만료) 무시한다.
     * 2. 기기의 쿠키와 사이트 저장소는 무조건 지운다. 그래야 버튼이 항상 동작한다.
     */
    suspend fun logout() {
        val serverResult = withTimeoutOrNull(SERVER_LOGOUT_TIMEOUT_MS) {
            runCatching { api.logout() }
        }
        when {
            serverResult == null -> Log.w(TAG, "server logout timed out")
            serverResult.isFailure -> Log.w(TAG, "server logout failed: ${serverResult.exceptionOrNull()?.message}")
            else -> Log.i(TAG, "server logout ok")
        }
        clearLocal()
    }

    /** 쿠키(토큰)와 사이트의 localStorage를 함께 지워야 다음 로그인 화면이 깨끗하게 뜬다. */
    private suspend fun clearLocal() {
        WebStorage.getInstance().deleteAllData()
        val cookies = CookieManager.getInstance()
        suspendCancellableCoroutine { cont ->
            cookies.removeAllCookies { cont.resume(Unit) }
        }
        cookies.flush()
    }

    private companion object {
        const val TAG = "AldotSession"
        const val SERVER_LOGOUT_TIMEOUT_MS = 5_000L
    }
}
