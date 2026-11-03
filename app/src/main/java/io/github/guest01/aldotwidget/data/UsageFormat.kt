package io.github.guest01.aldotwidget.data

import java.util.Locale

/** 알닷 사이트와 같은 표기로 바꾼다. 예: 3044253 KB → "2.9 GB", 851749 KB → "831.8 MB" */
object UsageFormat {

    fun dataKb(kb: Long): String = when {
        kb >= KB_PER_GB -> String.format(Locale.ROOT, "%.1f GB", kb.toDouble() / KB_PER_GB)
        kb >= KB_PER_MB -> String.format(Locale.ROOT, "%.1f MB", kb.toDouble() / KB_PER_MB)
        else -> "$kb KB"
    }

    /** 분 단위만 (남은 통화량에서 초는 의미가 적고 위젯은 자리가 좁다). 예: 3484 → "58분", 7 → "0분" */
    fun voiceMinutes(seconds: Long): String = "${seconds / 60}분"

    fun smsCount(count: Long): String = "${count}건"

    private const val KB_PER_MB = 1024L
    private const val KB_PER_GB = 1024L * 1024L
}
