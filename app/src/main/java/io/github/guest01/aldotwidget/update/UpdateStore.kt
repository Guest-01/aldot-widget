package io.github.guest01.aldotwidget.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "update")

/** 업데이트 확인 기록. 로그아웃해도 지우지 않는다 */
class UpdateStore(context: Context) {

    private val dataStore = context.applicationContext.updateDataStore

    private suspend fun prefs(): Preferences =
        dataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }.first()

    /** 마지막으로 확인에 성공한 시각 (epoch 밀리초). 한 번도 없으면 0 */
    suspend fun lastCheckedAt(): Long = prefs()[LAST_CHECKED_AT] ?: 0L

    /** 마지막 확인에서 찾은 새 버전. 없으면 null (최신이거나 아직 확인 전) */
    suspend fun available(): UpdateInfo? =
        prefs()[AVAILABLE]?.let { runCatching { UpdateInfo.parse(it) }.getOrNull() }

    /** 마지막 확인을 '업데이트 확인' 버튼으로 했는지 ("자동으로 확인했어요"를 맞게 보여 주려고) */
    suspend fun lastCheckedByUser(): Boolean = prefs()[LAST_CHECKED_BY_USER] ?: false

    /** byUser가 null이면 확인 방법 기록은 그대로 둔다 (확인 시각만 고칠 때) */
    suspend fun saveCheck(at: Long, available: UpdateInfo?, byUser: Boolean? = null) {
        dataStore.edit {
            it[LAST_CHECKED_AT] = at
            if (byUser != null) it[LAST_CHECKED_BY_USER] = byUser
            if (available != null) it[AVAILABLE] = UpdateInfo.toJson(available) else it.remove(AVAILABLE)
        }
    }

    /** 새 버전 알림을 보낸 가장 높은 versionCode. 버전마다 한 번만 알린다 */
    suspend fun notifiedVersionCode(): Int = prefs()[NOTIFIED_CODE] ?: 0

    suspend fun setNotifiedVersionCode(code: Int) {
        dataStore.edit { it[NOTIFIED_CODE] = code }
    }

    /** 앱 안에서 설치를 시작한 버전. 새 버전이 처음 깨어날 때 완료 알림을 보낼지 정한다 */
    suspend fun installingVersionCode(): Int = prefs()[INSTALLING_CODE] ?: 0

    suspend fun setInstallingVersionCode(code: Int) {
        dataStore.edit { if (code > 0) it[INSTALLING_CODE] = code else it.remove(INSTALLING_CODE) }
    }

    private companion object {
        val LAST_CHECKED_AT = longPreferencesKey("last_checked_at")
        val LAST_CHECKED_BY_USER = booleanPreferencesKey("last_checked_by_user")
        val AVAILABLE = stringPreferencesKey("available")
        val NOTIFIED_CODE = intPreferencesKey("notified_version_code")
        val INSTALLING_CODE = intPreferencesKey("installing_version_code")
    }
}
