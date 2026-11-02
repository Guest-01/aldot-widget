package io.github.guest01.aldotwidget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.guest01.aldotwidget.data.RefreshInterval
import io.github.guest01.aldotwidget.ui.MainUiState
import io.github.guest01.aldotwidget.ui.MainViewModel
import io.github.guest01.aldotwidget.ui.batteryExemptionIntent
import io.github.guest01.aldotwidget.ui.isIgnoringBatteryOptimizations
import io.github.guest01.aldotwidget.ui.login.LoginScreen
import io.github.guest01.aldotwidget.ui.main.ErrorScreen
import io.github.guest01.aldotwidget.ui.main.LoadingScreen
import io.github.guest01.aldotwidget.ui.main.UsageScreen
import io.github.guest01.aldotwidget.ui.onboarding.OnboardingScreen
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme
import io.github.guest01.aldotwidget.widget.DetailedWidgetReceiver
import kotlinx.coroutines.launch

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

/** 상태에 따라 로딩 / 로그인 / 첫 실행 안내 / 사용량 / 오류 화면 중 하나를 그린다. */
@Composable
private fun AldotRoot(viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 배터리 예외 여부. 설정 화면이나 요청 창에서 돌아온 뒤 바로 반영되도록 화면으로 돌아올 때마다 다시 확인한다.
    var ignoringBatteryOptimizations by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    LifecycleResumeEffect(Unit) {
        ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
        onPauseOrDispose { }
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
    }

    // Scaffold가 상태 표시줄·내비게이션 바 높이를 innerPadding으로 알려 준다 (edge-to-edge 대응)
    Scaffold(Modifier.fillMaxSize()) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        val current = settings
        when (val s = state) {
            MainUiState.Loading -> LoadingScreen(modifier)
            MainUiState.NeedLogin -> LoginScreen(
                checkLoggedIn = viewModel::isLoggedIn,
                onLoggedIn = viewModel::onLoginCompleted,
                modifier = modifier,
            )
            is MainUiState.Ready -> when {
                current == null -> LoadingScreen(modifier)
                !current.onboardingCompleted -> OnboardingScreen(
                    onFinished = viewModel::completeOnboarding,
                    modifier = modifier,
                )
                else -> UsageScreen(
                    snapshot = s.snapshot,
                    refreshing = s.refreshing,
                    error = s.error,
                    onRefresh = viewModel::refresh,
                    onLogout = viewModel::logout,
                    onAddWidget = {
                        // 런처에 "이 위젯을 홈 화면에 추가할까요?" 창을 띄워 달라고 요청한다 (Android 8+ 표준 기능)
                        scope.launch {
                            val supported = GlanceAppWidgetManager(context)
                                .requestPinGlanceAppWidget(DetailedWidgetReceiver::class.java)
                            if (!supported) {
                                Toast.makeText(context, "이 홈 화면 앱은 위젯 추가 요청을 지원하지 않아요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    refreshInterval = current.refreshInterval,
                    onRefreshIntervalChange = viewModel::setRefreshInterval,
                    showBatteryWarning = !ignoringBatteryOptimizations,
                    onFixBattery = { batteryLauncher.launch(batteryExemptionIntent(context)) },
                    modifier = modifier,
                )
            }
            is MainUiState.Failed -> ErrorScreen(s.message, onRetry = viewModel::refresh, modifier = modifier)
        }
    }
}
