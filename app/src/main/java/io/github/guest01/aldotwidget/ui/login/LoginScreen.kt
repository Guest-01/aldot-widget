package io.github.guest01.aldotwidget.ui.login

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.ui.onboarding.LoginStepHeader
import io.github.guest01.aldotwidget.ui.onboarding.StepPosition
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
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
    /** 첫 실행이면 단계 표시("1 / 4")와 뒤로 가기(환영 화면으로). 로그인이 풀려 다시 로그인할 때는 없다 */
    firstRun: Pair<StepPosition, () -> Unit>? = null,
) {
    val colors = AldotTheme.colors
    val scope = rememberCoroutineScope()
    val currentCheck by rememberUpdatedState(checkLoggedIn)
    val currentOnLoggedIn by rememberUpdatedState(onLoggedIn)

    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var checking by remember { mutableStateOf(false) }
    // 알닷 로그인 화면이 실제로 그려졌는지. 그 전까지는 빈 흰 화면 대신 로딩 안내를 덮어 보여 준다
    var siteVisible by remember { mutableStateOf(false) }
    // 사이트가 페이지 로드 완료를 여러 번 알려 와서, 그려졌는지 확인하는 작업은 한 번만 돌린다
    var waitingForSite by remember { mutableStateOf(false) }
    val openedAt = remember { SystemClock.elapsedRealtime() }

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
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)) {
            if (firstRun != null) {
                LoginStepHeader(firstRun.first, onBack = firstRun.second, modifier = Modifier.padding(bottom = 16.dp))
            }
            Text(
                "알닷 계정으로 로그인해 주세요", fontSize = 24.sp, lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp, color = colors.text,
                modifier = Modifier.padding(top = if (firstRun != null) 8.dp else 16.dp),
            )
            // 비밀번호를 앱이 보는지 걱정하는 사람이 많아서, 입력 칸 바로 위에서 먼저 답한다
            Row(
                Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(colors.segment).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_lock), contentDescription = null, tint = colors.accent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    // 문장마다 한 줄. 이어 쓰면 마지막 줄에 "않아요."만 남는다
                    "아래는 알닷 사이트예요.\n비밀번호는 이 앱에 저장되지 않아요.",
                    fontSize = 13.sp, lineHeight = 20.sp, color = colors.sub, modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
        // 화면이 뜬 뒤 다른 페이지로 넘어가거나(카카오 등) 로그인 여부를 확인하는 동안 얇은 진행 표시
        Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(2.dp)) {
            if (siteVisible && (loading || checking)) {
                LinearProgressIndicator(Modifier.fillMaxSize(), color = colors.accent, trackColor = colors.line)
            } else {
                HorizontalDivider(thickness = 1.dp, color = colors.line)
            }
        }
        // 불러오는 동안 WebView가 자기 칸 밖(위쪽 제목)까지 흰색으로 칠해서, 칸 안으로 자르고
        // 사이트가 그려지기 전에는 숨겨 둔다 (숨겨도 페이지는 계속 불러온다)
        Box(Modifier.fillMaxSize().clipToBounds()) {
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
                                Log.d(TAG, "page finished +${SystemClock.elapsedRealtime() - openedAt}ms")
                                if (!siteVisible && !waitingForSite) {
                                    waitingForSite = true
                                    view.whenContentDrawn {
                                        Log.d(TAG, "login form visible +${SystemClock.elapsedRealtime() - openedAt}ms")
                                        siteVisible = true
                                    }
                                }
                                checkIfLoggedIn(url)
                            }

                            // SPA라 페이지를 새로 불러오지 않고 주소만 바꾸는 경우(history.pushState)도 여기로 온다
                            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                canGoBack = view.canGoBack()
                                checkIfLoggedIn(url)
                            }
                        }
                        Log.d(TAG, "load start +${SystemClock.elapsedRealtime() - openedAt}ms")
                        loadUrl(LOGIN_URL)
                        webView = this
                    }
                },
                update = { it.visibility = if (siteVisible) View.VISIBLE else View.INVISIBLE },
                onRelease = { it.destroy() },
            )
            if (!siteVisible) {
                Column(
                    Modifier.fillMaxSize().background(colors.bg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(32.dp), color = colors.accent, strokeWidth = 3.dp)
                    Text(
                        "알닷 로그인 화면을 불러오고 있어요", fontSize = 14.sp, color = colors.sub,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

private const val TAG = "AldotLogin"

/**
 * 알닷 사이트는 JS로 화면을 그려서, 페이지 로드가 끝난(onPageFinished) 뒤에도 잠시 흰 화면이다.
 * 입력 칸이나 버튼이 생길 때까지 0.15초마다 확인하고, 3초가 지나면 그냥 보여 준다.
 */
private fun WebView.whenContentDrawn(attempt: Int = 0, onDrawn: () -> Unit) {
    evaluateJavascript("document.querySelector('input, button') !== null") { result ->
        if (result == "true" || attempt >= 20) {
            onDrawn()
        } else {
            postDelayed({ whenContentDrawn(attempt + 1, onDrawn) }, 150)
        }
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
