package io.github.guest01.aldotwidget.data

import java.util.Locale

/** 알닷 사이트와 같은 표기로 바꾼다. 예: 3044253 KB → "2.9 GB", 851749 KB → "831.8 MB" */
object UsageFormat {

    fun dataKb(kb: Long): String = when {
        kb >= KB_PER_GB -> String.format(Locale.ROOT, "%.1f GB", kb.toDouble() / KB_PER_GB)
        kb >= KB_PER_MB -> String.format(Locale.ROOT, "%.1f MB", kb.toDouble() / KB_PER_MB)
        else -> "$kb KB"
    }

    /** 예: 3484 → "58분 4초", 300 → "5분", 7 → "7초" */
    fun voiceSeconds(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return when {
            m == 0L -> "${s}초"
            s == 0L -> "${m}분"
            else -> "${m}분 ${s}초"
        }
    }

    fun smsCount(count: Long): String = "${count}건"

    private const val KB_PER_MB = 1024L
    private const val KB_PER_GB = 1024L * 1024L
}
