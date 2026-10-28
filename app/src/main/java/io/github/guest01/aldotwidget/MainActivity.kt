package io.github.guest01.aldotwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.guest01.aldotwidget.ui.MainUiState
import io.github.guest01.aldotwidget.ui.MainViewModel
import io.github.guest01.aldotwidget.ui.login.LoginScreen
import io.github.guest01.aldotwidget.ui.main.ErrorScreen
import io.github.guest01.aldotwidget.ui.main.LoadingScreen
import io.github.guest01.aldotwidget.ui.main.UsageScreen
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AldotWidgetTheme {
                AldotRoot()
            }
        }
    }
}

/** 상태에 따라 로딩 / 로그인 / 사용량 / 오류 화면 중 하나를 그린다. */
@Composable
private fun AldotRoot(viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Scaffold가 상태 표시줄·내비게이션 바 높이를 innerPadding으로 알려 준다 (edge-to-edge 대응)
    Scaffold(Modifier.fillMaxSize()) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (val s = state) {
            MainUiState.Loading -> LoadingScreen(modifier)
            MainUiState.NeedLogin -> LoginScreen(
                checkLoggedIn = viewModel::isLoggedIn,
                onLoggedIn = viewModel::onLoginCompleted,
                modifier = modifier,
            )
            is MainUiState.Ready -> UsageScreen(
                snapshot = s.snapshot,
                refreshing = s.refreshing,
                error = s.error,
                onRefresh = viewModel::refresh,
                onLogout = viewModel::logout,
                modifier = modifier,
            )
            is MainUiState.Failed -> ErrorScreen(s.message, onRetry = viewModel::refresh, modifier = modifier)
        }
    }
}
