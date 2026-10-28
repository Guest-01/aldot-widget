package io.github.guest01.aldotwidget.ui.login

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import java.net.URISyntaxException

private const val LOGIN_URL = "https://www.uplusmvno.com/login"
private const val SITE_HOST = "www.uplusmvno.com"

/**
 * 실제 알닷 로그인 페이지를 WebView로 띄운다. 아이디·비밀번호는 알닷 페이지에 직접 입력되고, 앱은 보지 않는다.
 *
 * 로그인이 끝났는지는 페이지 주소가 로그인 화면을 벗어날 때마다 API로 확인한다.
 * (로그인 성공 후 사이트가 다른 페이지로 이동하므로 그 시점에 쿠키가 생겨 있다.)
 */
@SuppressLint("SetJavaScriptEnabled") // 알닷 사이트가 Vue(Nuxt) 앱이라 JS 없이는 아무것도 안 뜬다
@Composable
fun LoginScreen(
    checkLoggedIn: suspend () -> Boolean,
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val currentCheck by rememberUpdatedState(checkLoggedIn)
    val currentOnLoggedIn by rememberUpdatedState(onLoggedIn)

    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var checking by remember { mutableStateOf(false) }

    // 시스템 뒤로 가기를 누르면 앱을 닫는 대신 WebView 안에서 뒤로 간다
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    fun checkIfLoggedIn(url: String?) {
        if (checking || !isPastLoginPage(url)) return
        checking = true
        scope.launch {
            val loggedIn = runCatching { currentCheck() }.getOrDefault(false)
            checking = false
            if (loggedIn) currentOnLoggedIn()
        }
    }

    Column(modifier.fillMaxSize()) {
        Text(
            text = "알닷 계정으로 로그인해 주세요",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        if (loading || checking) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true // 사이트가 localStorage에 로그인 표시를 저장한다
                    CookieManager.getInstance().setAcceptCookie(true)
                    // 페이지(www)와 API(api)의 호스트가 달라서, API가 내려주는 쿠키도 받도록 허용한다
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                            view.openNonWebLink(request.url)

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            loading = true
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            loading = false
                            checkIfLoggedIn(url)
                        }

                        // SPA라 페이지를 새로 불러오지 않고 주소만 바꾸는 경우(history.pushState)도 여기로 온다
                        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                            canGoBack = view.canGoBack()
                            checkIfLoggedIn(url)
                        }
                    }
                    loadUrl(LOGIN_URL)
                    webView = this
                }
            },
            onRelease = { it.destroy() },
        )
    }
}

/** 알닷 사이트 안이면서 로그인 관련 화면(/login...)이 아닌 곳 */
private fun isPastLoginPage(url: String?): Boolean {
    val uri = url?.let(Uri::parse) ?: return false
    return uri.host == SITE_HOST && !(uri.path ?: "").startsWith("/login")
}

/**
 * http(s)가 아닌 링크(예: 카카오톡 앱을 여는 intent://)는 WebView가 열 수 없으니 다른 앱으로 넘긴다.
 * 웹 페이지가 임의의 앱 화면을 열지 못하도록 브라우저용 링크로만 제한한다 (BROWSABLE, component 제거).
 */
private fun WebView.openNonWebLink(uri: Uri): Boolean {
    if (uri.scheme == "http" || uri.scheme == "https") return false
    val intent = try {
        if (uri.scheme == "intent") Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
        else Intent(Intent.ACTION_VIEW, uri)
    } catch (e: URISyntaxException) {
        return true
    }
    intent.addCategory(Intent.CATEGORY_BROWSABLE)
    intent.component = null
    intent.selector = null
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // 해당 앱이 없으면 페이지가 지정한 웹 대체 주소로 이동
        intent.getStringExtra("browser_fallback_url")?.let(::loadUrl)
    }
    return true
}
