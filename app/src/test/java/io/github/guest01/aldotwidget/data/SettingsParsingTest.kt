package io.github.guest01.aldotwidget.data

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsParsingTest {

    @Test
    fun `처음 설치하면 기본값`() {
        assertEquals(AppSettings(RefreshInterval.HOUR_1, onboardingCompleted = false, ThemeColor.BLUE), appSettingsFrom(emptyPreferences()))
    }

    @Test
    fun `저장한 값을 그대로 읽는다`() {
        val prefs = mutablePreferencesOf(
            SettingsKeys.INTERVAL to "MINUTES_30",
            SettingsKeys.ONBOARDING_COMPLETED to true,
            SettingsKeys.THEME_COLOR to "ORANGE",
        )
        assertEquals(AppSettings(RefreshInterval.MINUTES_30, onboardingCompleted = true, ThemeColor.ORANGE), appSettingsFrom(prefs))
    }

    @Test
    fun `예전 버전이 저장한, 지금은 없는 값은 기본값으로`() {
        // 3시간 주기는 로그인이 2시간이면 풀려서 없앴다. 그 전에 고른 사람의 설정이 남아 있을 수 있다
        val prefs = mutablePreferencesOf(SettingsKeys.INTERVAL to "HOURS_3", SettingsKeys.THEME_COLOR to "CORAL")
        val settings = appSettingsFrom(prefs)
        assertEquals(RefreshInterval.DEFAULT, settings.refreshInterval)
        assertEquals(ThemeColor.DEFAULT, settings.themeColor)
    }
}
