package io.github.guest01.aldotwidget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * 자동 갱신 주기. 많은 사람이 쓰면 알닷 서버 부담이 커서 기본값은 보수적으로 1시간.
 * (알닷 자체도 "전산 반영이 늦을 수 있다"고 안내해서 30분보다 자주 조회해도 얻는 게 적다)
 * WorkManager 주기 작업의 최소 주기는 15분이다.
 *
 * 상한: 알닷의 refreshToken 쿠키는 발급 후 2시간이면 만료된다(실측). 2시간 안에 한 번은 갱신해야
 * 로그인이 이어지므로 2시간 이상의 주기는 두지 않는다. (저장된 값이 목록에 없으면 기본값으로 돌아간다)
 */
enum class RefreshInterval(val minutes: Long, val label: String) {
    MINUTES_30(30, "30분"),
    HOUR_1(60, "1시간");

    companion object {
        val DEFAULT = HOUR_1
    }
}

data class AppSettings(
    val refreshInterval: RefreshInterval = RefreshInterval.DEFAULT,
    /** 첫 로그인 뒤 단계별 안내(알림·배터리·위젯)를 끝냈는지. 한 번만 보여 준다. */
    val onboardingCompleted: Boolean = false,
)

// 사용량(usage)과 다른 파일에 저장한다. 로그아웃할 때 사용량은 지우지만 설정은 남긴다.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                refreshInterval = prefs[KEY_INTERVAL]
                    ?.let { name -> RefreshInterval.entries.firstOrNull { it.name == name } }
                    ?: RefreshInterval.DEFAULT,
                onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setRefreshInterval(interval: RefreshInterval) {
        dataStore.edit { it[KEY_INTERVAL] = interval.name }
    }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = true }
    }

    /** 로그인을 마지막으로 연장한 시각 (epoch 밀리초). 로그인 전이거나 만료됐으면 0 */
    suspend fun sessionExtendedAt(): Long = dataStore.data.first()[KEY_SESSION_EXTENDED_AT] ?: 0L

    suspend fun setSessionExtendedAt(epochMillis: Long) {
        dataStore.edit { it[KEY_SESSION_EXTENDED_AT] = epochMillis }
    }

    private companion object {
        val KEY_INTERVAL = stringPreferencesKey("refresh_interval")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_SESSION_EXTENDED_AT = longPreferencesKey("session_extended_at")
    }
}
