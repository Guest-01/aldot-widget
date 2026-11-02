package io.github.guest01.aldotwidget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.guest01.aldotwidget.AldotApp
import io.github.guest01.aldotwidget.data.AppSettings
import io.github.guest01.aldotwidget.data.RefreshInterval
import io.github.guest01.aldotwidget.data.RefreshPolicy
import io.github.guest01.aldotwidget.data.SessionManager
import io.github.guest01.aldotwidget.data.SettingsStore
import io.github.guest01.aldotwidget.data.SyncStatus
import io.github.guest01.aldotwidget.data.UsageRepository
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.data.UsageState
import io.github.guest01.aldotwidget.data.UsageStore
import io.github.guest01.aldotwidget.data.UsageSync
import io.github.guest01.aldotwidget.work.RefreshScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 앱 화면이 보여 줄 수 있는 상태. 이 값 하나로 어떤 화면을 그릴지 정한다. */
sealed interface MainUiState {
    data object Loading : MainUiState
    data object NeedLogin : MainUiState
    data class Ready(
        val snapshot: UsageSnapshot,
        val refreshing: Boolean = false,
        /** 새로고침이 실패해도 이전 값은 계속 보여 주고, 오류만 따로 표시한다 */
        val error: String? = null,
        /** 마지막으로 새로고침을 직접 누른 시각. 1분 동안 버튼을 잠근다 */
        val lastRequestedAt: Long? = null,
    ) : MainUiState

    data class Failed(val message: String) : MainUiState
}

/**
 * 저장된 상태 + 지금 새로고침 중인지 → 화면 상태. 안드로이드에 의존하지 않아 단위 테스트할 수 있다.
 * 위젯 버튼으로 시작된 갱신(refreshingSince)도 앱 화면에 "갱신 중"으로 보인다.
 */
internal fun toUiState(stored: UsageState, refreshing: Boolean, now: Long = System.currentTimeMillis()): MainUiState {
    val snapshot = stored.snapshot
    val anyRefreshing = refreshing || RefreshPolicy.isRefreshing(stored.refreshingSince, now)
    return when {
        // 로그인 직후 첫 조회 중에는 로그인 화면 대신 로딩을 보여 준다
        stored.status == SyncStatus.NEED_LOGIN -> if (refreshing) MainUiState.Loading else MainUiState.NeedLogin
        snapshot != null -> MainUiState.Ready(
            snapshot = snapshot,
            refreshing = anyRefreshing,
            error = if (stored.status == SyncStatus.ERROR) stored.errorMessage else null,
            lastRequestedAt = stored.lastRequestedAt,
        )
        stored.status == SyncStatus.ERROR && !refreshing ->
            MainUiState.Failed(stored.errorMessage ?: "알 수 없는 오류")
        else -> MainUiState.Loading
    }
}

/**
 * 화면 상태를 들고 있는 객체. 화면 회전처럼 Activity가 다시 만들어져도 살아남아서,
 * 회전할 때마다 API를 다시 부르지 않는다.
 *
 * 사용량 값 자체는 UsageStore(DataStore)가 유일한 출처이고, 이 클래스는 그걸 화면 상태로 바꿔 보여 줄 뿐이다.
 * 그래서 위젯 버튼으로 갱신해도 앱 화면이 같이 바뀐다.
 */
class MainViewModel(
    private val sync: UsageSync,
    store: UsageStore,
    private val repository: UsageRepository,
    private val session: SessionManager,
    private val settingsStore: SettingsStore,
    private val scheduler: RefreshScheduler,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)

    val state: StateFlow<MainUiState> = combine(store.state, refreshing, ::toUiState)
        // 화면이 구독하는 동안만 흐름을 유지 (화면 회전 같은 5초 미만의 끊김은 유지)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState.Loading)

    /** 저장소에서 읽기 전에는 null. 읽기 전 기본값(안내 미완료)으로 안내 화면이 잠깐 깜빡이는 것을 막는다. */
    val settings: StateFlow<AppSettings?> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRefreshInterval(interval: RefreshInterval) {
        viewModelScope.launch {
            settingsStore.setRefreshInterval(interval)
            // 로그인된 상태에서만 주기 작업이 있어야 한다
            if (state.value is MainUiState.Ready) scheduler.ensureScheduled()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsStore.setOnboardingCompleted() }
    }

    init {
        refresh()
    }

    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try {
                sync.refresh()
            } finally {
                refreshing.value = false
            }
        }
    }

    /** 새로고침 버튼. 1분 제한은 UsageSync가 지킨다 (위젯 버튼과 같은 제한을 공유). */
    fun refreshFromButton() {
        if (refreshing.value) return
        viewModelScope.launch { sync.requestRefresh(viaWorker = false) }
    }

    /** 로그인 화면이 "로그인이 끝났는지" 물어볼 때 쓴다. */
    suspend fun isLoggedIn(): Boolean = repository.isLoggedIn()

    fun onLoginCompleted() = refresh()

    fun logout() {
        viewModelScope.launch {
            session.logout()
            sync.markLoggedOut()
        }
    }

    companion object {
        /** ViewModel은 직접 생성하지 않고 이 팩토리를 통해 만든다 (수동 DI). */
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as AldotApp).container
                MainViewModel(
                    sync = container.usageSync,
                    store = container.usageStore,
                    repository = container.usageRepository,
                    session = container.sessionManager,
                    settingsStore = container.settingsStore,
                    scheduler = container.refreshScheduler,
                )
            }
        }
    }
}
