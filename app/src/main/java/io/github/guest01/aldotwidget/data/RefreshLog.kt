package io.github.guest01.aldotwidget.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 갱신 기록을 앱 전용 파일에 남긴다. 절전 모드 때문에 자동 갱신이 얼마나 밀렸는지 나중에 확인하기 위한 것으로,
 * 시스템 로그(logcat)는 금방 지워져서 밤새 기록을 볼 수 없다.
 * 시각과 결과만 기록하고 사용량·전화번호 같은 개인정보는 넣지 않는다.
 *
 * 보는 방법: adb shell run-as io.github.guest01.aldotwidget cat files/refresh_log.txt
 */
class RefreshLog(context: Context) {

    private val file = File(context.filesDir, "refresh_log.txt")
    private val timeFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")

    /** @param event 예: "background OK", "alarm extend OK" */
    suspend fun append(event: String) = withContext(Dispatchers.IO) {
        runCatching {
            file.appendText("${timeFormat.format(LocalDateTime.now())} $event\n")
            trimIfNeeded()
        }.onFailure { Log.w(TAG, "refresh log failed", it) }
        Unit
    }

    private fun trimIfNeeded() {
        if (file.length() < MAX_BYTES) return
        file.writeText(file.readLines().takeLast(KEEP_LINES).joinToString(separator = "\n", postfix = "\n"))
    }

    private companion object {
        const val TAG = "AldotSync"
        const val MAX_BYTES = 64L * 1024
        const val KEEP_LINES = 500
    }
}
