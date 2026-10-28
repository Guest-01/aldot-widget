package io.github.guest01.aldotwidget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.guest01.aldotwidget.data.api.AldotJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.io.IOException

enum class SyncStatus {
    /** 아직 한 번도 조회하지 않음 */
    NEVER,
    OK,
    NEED_LOGIN,
    ERROR,
}

/** 앱 화면과 위젯이 함께 보는 상태. 마지막 성공 스냅샷과 마지막 시도 결과를 같이 들고 있다. */
@Serializable
data class UsageState(
    val snapshot: UsageSnapshot? = null,
    val status: SyncStatus = SyncStatus.NEVER,
    val errorMessage: String? = null,
)

// 파일 하나당 DataStore 인스턴스는 프로세스에 하나만 있어야 한다. 최상위 위임 프로퍼티로 만드는 게 표준 방식이다.
private val Context.usageDataStore: DataStore<Preferences> by preferencesDataStore(name = "usage")

/** 사용량 상태를 DataStore에 JSON 한 덩어리로 저장한다. */
class UsageStore(context: Context) {

    private val dataStore = context.applicationContext.usageDataStore

    /** 값이 바뀔 때마다 새 상태를 내보내는 스트림. 구독하는 화면·위젯이 자동으로 다시 그려진다. */
    val state: Flow<UsageState> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> decode(prefs[KEY]) }

    suspend fun update(transform: (UsageState) -> UsageState) {
        dataStore.edit { prefs ->
            prefs[KEY] = UsageStateJson.encode(transform(decode(prefs[KEY])))
        }
    }

    // 저장된 JSON이 깨졌거나 형식이 바뀌었으면 처음 상태로 시작한다
    private fun decode(json: String?): UsageState =
        json?.let { runCatching { UsageStateJson.decode(it) }.getOrNull() } ?: UsageState()

    private companion object {
        val KEY = stringPreferencesKey("usage_state")
    }
}

/** 저장 형식 변환. 안드로이드에 의존하지 않게 분리해서 단위 테스트한다. */
object UsageStateJson {
    fun encode(state: UsageState): String = AldotJson.encodeToString(UsageState.serializer(), state)
    fun decode(json: String): UsageState = AldotJson.decodeFromString(UsageState.serializer(), json)
}
