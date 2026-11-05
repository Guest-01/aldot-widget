package io.github.guest01.aldotwidget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.guest01.aldotwidget.data.ThemeColor
import io.github.guest01.aldotwidget.ui.MainUiState
import io.github.guest01.aldotwidget.ui.MainViewModel
import io.github.guest01.aldotwidget.ui.batteryExemptionIntent
import io.github.guest01.aldotwidget.ui.canPostNotifications
import io.github.guest01.aldotwidget.ui.isIgnoringBatteryOptimizations
import io.github.guest01.aldotwidget.ui.notificationSettingsIntent
import io.github.guest01.aldotwidget.ui.login.LoginScreen
import io.github.guest01.aldotwidget.ui.main.ErrorScreen
import io.github.guest01.aldotwidget.ui.main.LoadingScreen
import io.github.guest01.aldotwidget.ui.main.HomeScreen
import io.github.guest01.aldotwidget.ui.onboarding.OnboardingScreen
import io.github.guest01.aldotwidget.ui.onboarding.StepPosition
import io.github.guest01.aldotwidget.ui.onboarding.WelcomeScreen
import io.github.guest01.aldotwidget.ui.onboarding.onboardingSteps
import io.github.guest01.aldotwidget.ui.settings.AboutScreen
import io.github.guest01.aldotwidget.ui.settings.SettingsScreen
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme
import io.github.guest01.aldotwidget.widget.SquareWidgetReceiver
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** 새 버전 알림을 눌러 열렸는지. 홈 → 설정 → 앱 정보로 가서 업데이트 창을 띄운다 */
    private val openUpdate = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) readOpenUpdate(intent)
        setContent {
            AldotRoot(openUpdate = openUpdate.value, onOpenUpdateHandled = { openUpdate.value = false })
        }
    }

    // 앱이 이미 떠 있을 때 알림을 누르면 새 화면을 만들지 않고 여기로 온다 (SINGLE_TOP)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readOpenUpdate(intent)
    }

    private fun readOpenUpdate(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATE, false) == true) openUpdate.value = true
    }

    companion object {
        const val EXTRA_OPEN_UPDATE = "open_update"
    }
}

/** 상태에 따라 로딩 / 로그인 / 첫 실행 안내 / 홈·설정 / 오류 화면 중 하나를 그린다. */
@Composable
private fun AldotRoot(
    openUpdate: Boolean,
    onOpenUpdateHandled: () -> Unit,
    viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val update by viewModel.updates.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 배터리 예외와 알림 허용 여부. 설정 화면이나 요청 창에서 돌아온 뒤 바로 반영되도록 화면으로 돌아올 때마다 다시 확인한다.
    var ignoringBatteryOptimizations by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var notificationsOn by remember { mutableStateOf(canPostNotifications(context)) }
    LifecycleResumeEffect(Unit) {
        ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
        notificationsOn = canPostNotifications(context)
        onPauseOrDispose { }
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
    }

    // 홈 ↔ 설정 ↔ 앱 정보. 화면이 몇 개뿐이라 내비게이션 라이브러리 없이 상태로 바꾼다 (회전해도 유지).
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    // 알림을 눌러 들어왔으면 앱 정보에서 업데이트 창을 바로 띄운다 (한 번만)
    var openSheetOnStart by remember { mutableStateOf(false) }
    BackHandler(enabled = showSettings && !showAbout) { showSettings = false }
    BackHandler(enabled = showAbout) { showAbout = false }
    // 로그아웃 뒤 다시 로그인하면 설정이 아니라 홈으로 돌아오게 한다
    val needLogin = state is MainUiState.NeedLogin
    LaunchedEffect(needLogin) { if (needLogin) { showSettings = false; showAbout = false } }
    LaunchedEffect(openUpdate) {
        if (openUpdate) {
            showSettings = true
            showAbout = true
            openSheetOnStart = true
            onOpenUpdateHandled()
        }
    }

    val addWidget: () -> Unit = {
        // 런처에 "이 위젯을 홈 화면에 추가할까요?" 창을 띄워 달라고 요청한다 (Android 8+ 표준 기능)
        scope.launch {
            val supported = GlanceAppWidgetManager(context)
                .requestPinGlanceAppWidget(SquareWidgetReceiver::class.java)
            if (!supported) {
                Toast.makeText(context, "이 홈 화면 앱은 위젯 추가 요청을 지원하지 않아요. 홈 화면을 길게 눌러 추가해 주세요", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 첫 실행에서 환영 화면을 지나 로그인으로 갔는지 (로그인 화면의 뒤로 가기로 돌아올 수 있다)
    var welcomeDone by rememberSaveable { mutableStateOf(false) }
    // 첫 실행 로그인 화면에서 시스템 뒤로 가기: 앱을 닫지 않고 환영 화면으로 (사이트 안에서 뒤로 갈 곳이 있으면 그쪽이 먼저)
    BackHandler(enabled = needLogin && welcomeDone && settings?.onboardingCompleted == false) { welcomeDone = false }

    // 테마 색은 설정에 저장돼 있어서, 고르는 즉시 앱 전체가 다시 그려진다
    AldotWidgetTheme(themeColor = settings?.themeColor ?: ThemeColor.DEFAULT) {
        // Scaffold가 상태 표시줄·내비게이션 바 높이를 innerPadding으로 알려 준다 (edge-to-edge 대응)
        Scaffold(Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            val current = settings
            when (val s = state) {
                MainUiState.Loading -> LoadingScreen(modifier)
                MainUiState.NeedLogin -> when {
                    current == null -> LoadingScreen(modifier)
                    // 첫 실행: ① 환영 → ② 로그인. 로그인이 풀려 다시 로그인할 때는 로그인 화면만
                    !current.onboardingCompleted && !welcomeDone -> WelcomeScreen(onStart = { welcomeDone = true }, modifier = modifier)
                    else -> LoginScreen(
                        checkLoggedIn = viewModel::isLoggedIn,
                        onLoggedIn = viewModel::onLoginCompleted,
                        modifier = modifier,
                        firstRun = if (current.onboardingCompleted) {
                            null
                        } else {
                            StepPosition(1, onboardingSteps(context).size + 1) to { welcomeDone = false }
                        },
                    )
                }
                is MainUiState.Ready -> when {
                    current == null -> LoadingScreen(modifier)
                    !current.onboardingCompleted -> OnboardingScreen(
                        themeColor = current.themeColor,
                        onThemeColorChange = viewModel::setThemeColor,
                        refreshInterval = current.refreshInterval,
                        onFinished = viewModel::completeOnboarding,
                        modifier = modifier,
                    )
                    showSettings && showAbout -> AboutScreen(
                        update = update,
                        updates = viewModel.updates,
                        onBack = { showAbout = false },
                        openSheetOnStart = openSheetOnStart,
                        modifier = modifier,
                    )
                    showSettings -> SettingsScreen(
                        refreshInterval = current.refreshInterval,
                        onRefreshIntervalChange = viewModel::setRefreshInterval,
                        themeColor = current.themeColor,
                        onThemeColorChange = viewModel::setThemeColor,
                        batteryOn = ignoringBatteryOptimizations,
                        onFixBattery = { batteryLauncher.launch(batteryExemptionIntent(context)) },
                        notificationsOn = notificationsOn,
                        onFixNotifications = { context.startActivity(notificationSettingsIntent(context)) },
                        hasUpdate = update.available != null,
                        onOpenAbout = { showAbout = true; openSheetOnStart = false },
                        onLogout = viewModel::logout,
                        onBack = { showSettings = false },
                        modifier = modifier,
                    )
                    else -> HomeScreen(
                        ready = s,
                        showBatteryWarning = !ignoringBatteryOptimizations,
                        onFixBattery = { batteryLauncher.launch(batteryExemptionIntent(context)) },
                        onRefresh = viewModel::refreshFromButton,
                        onAddWidget = addWidget,
                        onOpenSettings = { showSettings = true },
                        // 홈의 테마 색 영역은 상태 표시줄 뒤까지 칠하므로 위쪽 여백은 홈 화면이 직접 넣는다
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                    )
                }
                is MainUiState.Failed -> ErrorScreen(s.message, onRetry = viewModel::refresh, modifier = modifier)
            }
        }
    }
}
