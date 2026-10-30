package io.github.guest01.aldotwidget

import android.app.Application
import android.content.Context
import android.webkit.WebSettings
import io.github.guest01.aldotwidget.data.Notifier
import io.github.guest01.aldotwidget.data.RefreshLog
import io.github.guest01.aldotwidget.data.SessionManager
import io.github.guest01.aldotwidget.data.SettingsStore
import io.github.guest01.aldotwidget.data.UsageRepository
import io.github.guest01.aldotwidget.data.UsageStore
import io.github.guest01.aldotwidget.data.UsageSync
import io.github.guest01.aldotwidget.data.api.AldotApi
import io.github.guest01.aldotwidget.data.api.TokenRefreshInterceptor
import io.github.guest01.aldotwidget.data.api.WebViewCookieJar
import io.github.guest01.aldotwidget.widget.WidgetPreviews
import io.github.guest01.aldotwidget.work.RefreshScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 앱 프로세스가 뜰 때 가장 먼저 만들어지는 객체. 앱 전체에서 하나만 있는 의존성을 여기서 만든다.
 * (Hilt 같은 DI 라이브러리 대신 쓰는 수동 DI. 매니페스트의 android:name으로 등록한다.)
 */
class AldotApp : Application() {
    val container: AppContainer by lazy { AppContainer(applicationContext) }

    /** 앱 프로세스가 살아 있는 동안 쓰는 코루틴 범위 (화면과 무관한 일회성 작업용) */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { WidgetPreviews.publishIfNeeded(this@AldotApp) }
    }
}

class AppContainer(private val context: Context) {

    // WebView와 같은 User-Agent를 써서, 서버 입장에서 사이트를 쓰는 브라우저와 같은 클라이언트로 보이게 한다.
    private val userAgent: String = WebSettings.getDefaultUserAgent(context)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(WebViewCookieJar())
        .addInterceptor { chain ->
            // API는 www.uplusmvno.com 페이지에서 부르도록 만들어졌으므로, 브라우저가 붙이는 헤더를 같이 보낸다.
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Origin", SITE_ORIGIN)
                    .header("Referer", "$SITE_ORIGIN/")
                    .build()
            )
        }
        .addInterceptor(
            TokenRefreshInterceptor(AldotApi.DEFAULT_BASE_URL.resolve(AldotApi.REFRESH_PATH)!!)
        )
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api = AldotApi(httpClient)
    val usageRepository = UsageRepository(api)
    val sessionManager = SessionManager(api)
    val usageStore = UsageStore(context)
    val settingsStore = SettingsStore(context)
    val refreshScheduler = RefreshScheduler(context, settingsStore)
    val notifier = Notifier(context)
    val usageSync = UsageSync(context, usageRepository, usageStore, refreshScheduler, notifier, RefreshLog(context))

    companion object {
        const val SITE_ORIGIN = "https://www.uplusmvno.com"
    }
}
