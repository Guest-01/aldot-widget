package io.github.guest01.aldotwidget.data

import java.util.Locale

/** 사용량을 화면에 쓸 글자로 바꾼다. 예: 3044253 KB → "2.9 GB", 851749 KB → "831 MB" */
object UsageFormat {

    /**
     * GB는 소수점 한 자리까지 쓴다 (0.1 GB = 약 100 MB라 버리면 잃는 양이 크다).
     * MB는 정수로만 쓴다. 소수점 아래는 1 MB도 안 돼 의미가 적고, 계기판·위젯에서 자리만 차지한다.
     * 남은 양을 실제보다 많아 보이게 하지 않도록 반올림하지 않고 버린다 (1023.9 MB → "1023 MB", "1024 MB"가 되지 않게).
     */
    fun dataKb(kb: Long): String = when {
        kb >= KB_PER_GB -> String.format(Locale.ROOT, "%.1f GB", kb.toDouble() / KB_PER_GB)
        kb >= KB_PER_MB -> "${kb / KB_PER_MB} MB"
        else -> "$kb KB"
    }

    /** 분 단위만 (남은 통화량에서 초는 의미가 적고 위젯은 자리가 좁다). 예: 3484 → "58분", 7 → "0분" */
    fun voiceMinutes(seconds: Long): String = "${seconds / 60}분"

    fun smsCount(count: Long): String = "${count}건"

    private const val KB_PER_MB = 1024L
    private const val KB_PER_GB = 1024L * 1024L
}
