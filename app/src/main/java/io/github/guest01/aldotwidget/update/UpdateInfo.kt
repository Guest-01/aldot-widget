package io.github.guest01.aldotwidget.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.Locale

/**
 * 릴리스에 함께 올리는 update.json 한 개. Gradle의 makeReleaseUpdate 작업이 만든다 (app/build.gradle.kts).
 * 앱은 versionCode로 새 버전인지 비교하고, size와 sha256으로 받은 APK가 온전한지 확인한다.
 */
@Serializable
data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    /** APK 파일 이름. update.json과 같은 곳(릴리스 첨부 파일)에 있다 */
    val apk: String,
    val size: Long,
    val sha256: String,
    /** 만든 날 (yyyy-MM-dd). 없을 수도 있다 */
    val released: String? = null,
    val notes: List<String> = emptyList(),
) {
    fun isNewerThan(currentVersionCode: Int) = versionCode > currentVersionCode

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): UpdateInfo = json.decodeFromString(serializer(), text)

        fun toJson(info: UpdateInfo): String = json.encodeToString(serializer(), info)
    }
}

/** 자동 확인 주기: 마지막으로 확인에 성공한 뒤 하루가 지나야 다시 묻는다 */
object UpdatePolicy {
    const val CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L

    fun isDue(lastCheckedAt: Long, now: Long) = lastCheckedAt <= 0L || now - lastCheckedAt >= CHECK_INTERVAL_MILLIS
}

/** 업데이트 화면과 알림에 쓰는 글자 */
object UpdateText {
    /** "4.1MB" (1MB = 1024×1024) */
    fun size(bytes: Long): String = String.format(Locale.US, "%.1fMB", bytes / (1024.0 * 1024.0))

    /** "2026-11-20" → "11월 20일". 읽을 수 없으면 null */
    fun date(released: String?): String? = released
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.let { "${it.monthValue}월 ${it.dayOfMonth}일" }

    /** "지금 1.0.0 · 4.1MB · 11월 20일" */
    fun summary(info: UpdateInfo, currentVersion: String): String =
        listOfNotNull("지금 $currentVersion", size(info.size), date(info.released)).joinToString(" · ")

    /**
     * 버전 번호 뒤 조사. 마지막 숫자를 읽는 소리로 받침을 정한다.
     * 영·삼·육은 받침(이/으로), 일·칠·팔은 ㄹ 받침(이/로), 이·사·오·구는 받침 없음(가/로).
     */
    fun subject(version: String): String = version + if (lastDigit(version) in "013678") "이" else "가"

    fun toward(version: String): String = version + if (lastDigit(version) in "036") "으로" else "로"

    private fun lastDigit(version: String): Char = version.lastOrNull { it.isDigit() } ?: '0'
}
