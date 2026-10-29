package io.github.guest01.aldotwidget.widget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Android 15+ 위젯 목록에 "실제 화면으로 만든 미리보기"를 등록한다 (Generated Previews).
 * 그 아래 버전은 메타데이터 XML의 android:previewImage 그림을 쓴다.
 *
 * 시스템이 호출 횟수를 제한하므로(rate limit) 앱이 새로 설치·업데이트됐을 때 한 번만 보낸다.
 */
object WidgetPreviews {

    private const val TAG = "AldotWidget"
    private const val PREFS = "widget_previews"
    private const val KEY_PUBLISHED_FOR = "published_for_install"

    suspend fun publishIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

        // 설치·업데이트할 때마다 바뀌는 값. 개발 중에는 versionCode가 그대로라 이걸 쓴다.
        val installStamp = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_PUBLISHED_FOR, -1) == installStamp) return

        val manager = GlanceAppWidgetManager(context)
        val results = allWidgetReceivers.map { receiver ->
            runCatching { manager.setWidgetPreviews(receiver) }
                .onFailure { Log.w(TAG, "preview failed: ${receiver.simpleName}", it) }
                .getOrNull()
        }
        Log.i(TAG, "widget previews published: $results")
        if (results.all { it == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS }) {
            prefs.edit { putLong(KEY_PUBLISHED_FOR, installStamp) }
        }
    }
}
