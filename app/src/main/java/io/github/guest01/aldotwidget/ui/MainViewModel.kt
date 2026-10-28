package io.github.guest01.aldotwidget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.guest01.aldotwidget.AldotApp
import io.github.guest01.aldotwidget.data.SessionManager
import io.github.guest01.aldotwidget.data.UsageRepository
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.data.api.AldotException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** 앱 화면이 보여 줄 수 있는 상태. 이 값 하나로 어떤 화면을 그릴지 정한다. */
sealed interface MainUiState {
    data object Loading : MainUiState
    data object NeedLogin : MainUiState
    data class Ready(
        val snapshot: UsageSnapshot,
        val refreshing: Boolean = false,
        /** 새로고침이 실패해도 이전 값은 계속 보여 주고, 오류만 따로 표시한다 */
        val error: String? = null,
    ) : MainUiState

    data class Failed(val message: String) : MainUiState
}

/**
 * 화면 상태를 들고 있는 객체. 화면 회전처럼 Activity가 다시 만들어져도 살아남아서,
 * 회전할 때마다 API를 다시 부르지 않는다.
 */
class MainViewModel(
    private val repository: UsageRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val current = _state.value
        if (current is MainUiState.Ready && current.refreshing) return
        _state.value = if (current is MainUiState.Ready) {
            current.copy(refreshing = true, error = null)
        } else {
            MainUiState.Loading
        }

        viewModelScope.launch {
            _state.value = try {
                MainUiState.Ready(repository.fetchUsage())
            } catch (e: AldotException.NeedLogin) {
                MainUiState.NeedLogin
            } catch (e: IOException) {
                val message = e.message ?: "네트워크 오류"
                if (current is MainUiState.Ready) {
                    current.copy(refreshing = false, error = message)
                } else {
                    MainUiState.Failed(message)
                }
            }
        }
    }

    /** 로그인 화면이 "로그인이 끝났는지" 물어볼 때 쓴다. */
    suspend fun isLoggedIn(): Boolean = repository.isLoggedIn()

    fun onLoginCompleted() = refresh()

    fun logout() {
        viewModelScope.launch {
            session.clear()
            _state.value = MainUiState.NeedLogin
        }
    }

    companion object {
        /** ViewModel은 직접 생성하지 않고 이 팩토리를 통해 만든다 (수동 DI). */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AldotApp
                MainViewModel(app.container.usageRepository, app.container.sessionManager)
            }
        }
    }
}
