package io.github.guest01.aldotwidget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.runtime.LaunchedEffect
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
import io.github.guest01.aldotwidget.ui.MainUiState
import io.github.guest01.aldotwidget.ui.MainViewModel
import io.github.guest01.aldotwidget.ui.login.LoginScreen
import io.github.guest01.aldotwidget.ui.main.ErrorScreen
import io.github.guest01.aldotwidget.ui.main.LoadingScreen
import io.github.guest01.aldotwidget.ui.main.UsageScreen
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

/** 상태에 따라 로딩 / 로그인 / 사용량 / 오류 화면 중 하나를 그린다. */
@Composable
private fun AldotRoot(viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 알림 권한(Android 13+): 처음으로 사용량이 보인 순간, 즉 로그인에 성공한 직후 한 번만 묻는다.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 거절해도 위젯에는 "로그인 필요"가 보이므로 따로 처리하지 않는다 */ }
    LaunchedEffect(state is MainUiState.Ready, settings.notificationPermissionAsked) {
        if (state is MainUiState.Ready && !settings.notificationPermissionAsked &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            viewModel.onNotificationPermissionAsked()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 배터리 최적화 예외 여부. 설정 화면에 다녀온 뒤 바로 반영되도록 화면으로 돌아올 때마다 다시 확인한다.
    var ignoringBatteryOptimizations by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    LifecycleResumeEffect(Unit) {
        ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
        onPauseOrDispose { }
    }

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
                refreshInterval = settings.refreshInterval,
                onRefreshIntervalChange = viewModel::setRefreshInterval,
                showBatteryTip = !ignoringBatteryOptimizations && !settings.batteryTipDismissed,
                onOpenBatterySettings = { openAppDetailsSettings(context) },
                onDismissBatteryTip = viewModel::dismissBatteryTip,
                modifier = modifier,
            )
            is MainUiState.Failed -> ErrorScreen(s.message, onRetry = viewModel::refresh, modifier = modifier)
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

/** 이 앱의 "앱 정보" 화면. 삼성은 여기서 배터리 → 제한 없음을 고를 수 있다. */
private fun openAppDetailsSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    )
}
