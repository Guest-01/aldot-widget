package io.github.guest01.aldotwidget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import io.github.guest01.aldotwidget.work.RefreshWorker

/** 위젯의 새로고침 버튼. 직접 조회하지 않고 백그라운드 작업을 예약만 한다 (위젯 콜백은 실행 시간이 짧다). */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        RefreshWorker.enqueueNow(context)
    }
}
