package io.github.guest01.aldotwidget.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.RefreshInterval
import io.github.guest01.aldotwidget.data.ThemeColor
import io.github.guest01.aldotwidget.ui.LocalReduceMotion
import io.github.guest01.aldotwidget.ui.main.Gauge
import io.github.guest01.aldotwidget.ui.onboarding.BatteryDialogPreview
import io.github.guest01.aldotwidget.ui.onboarding.DoneHeader
import io.github.guest01.aldotwidget.ui.onboarding.NotificationPreview
import io.github.guest01.aldotwidget.ui.onboarding.OnboardingScreen
import io.github.guest01.aldotwidget.ui.onboarding.OnboardingStep
import io.github.guest01.aldotwidget.ui.onboarding.WelcomeScreen
import io.github.guest01.aldotwidget.ui.onboarding.WidgetIllustration
import io.github.guest01.aldotwidget.ui.settings.AppCard
import io.github.guest01.aldotwidget.ui.settings.BlockSteps
import io.github.guest01.aldotwidget.ui.settings.InstallingPart
import io.github.guest01.aldotwidget.ui.settings.PermissionPicture
import io.github.guest01.aldotwidget.ui.settings.ProgressPart
import io.github.guest01.aldotwidget.ui.settings.SectionTitle
import io.github.guest01.aldotwidget.ui.settings.StatusRow
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme
import io.github.guest01.aldotwidget.update.CheckState
import io.github.guest01.aldotwidget.update.UpdateInfo
import io.github.guest01.aldotwidget.update.UpdateUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 애니메이션 미리보기. 개발용 앱(debug)에만 들어간다 (src/debug). 앱 안 애니메이션 열 개를 한 화면에 모아
 * 폰 설정이나 앱 상태를 바꾸지 않고 몇 번이고 다시 본다.
 *
 *   adb shell am start -n io.github.guest01.aldotwidget/.debug.MotionGalleryActivity
 *
 * 매니페스트에서 DUMP 권한으로 막아서 adb shell만 열 수 있다.
 */
class MotionGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MotionGallery() }
    }
}

@Composable
internal fun MotionGallery(startDark: Boolean = isSystemInDarkTheme()) {
    var dark by remember { mutableStateOf(startDark) }
    var reduce by remember { mutableStateOf(false) }
    // 위의 스위치를 바꾸거나 '모두 다시 보기'를 누르면 전부 처음부터
    var all by remember { mutableIntStateOf(0) }
    // 실제 시작 화면 흐름 (환영 → 안내 단계 전부 → 준비 끝). null이면 미리보기 목록
    var flow by remember { mutableStateOf<Flow?>(null) }
    AldotWidgetTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalReduceMotion provides reduce) {
            val current = flow
            if (current != null) {
                StartFlow(current, dark, reduce, onFlow = { flow = it })
            } else {
                val colors = AldotTheme.colors
                Column(
                    Modifier.fillMaxSize().background(colors.bg).windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("애니메이션 미리보기", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text("시험용 앱에만 있는 화면이에요", fontSize = 13.sp, color = colors.sub)
                    Toggle("어둡게", dark) { dark = it; all++ }
                    Toggle("애니메이션 제거 (폰 설정 흉내)", reduce) { reduce = it; all++ }
                    Row {
                        TextButton(onClick = { all++ }) { Text("모두 다시 보기") }
                        TextButton(onClick = { flow = Flow.WELCOME }) { Text("시작 화면 흐름 보기") }
                    }
                    key(all) { Items() }
                }
            }
        }
    }
}

private enum class Flow { WELCOME, STEPS }

/**
 * 실제 첫 실행 화면을 그대로 띄운다. 로그인은 건너뛰고, 이미 허용된 알림·배터리 단계도 모두 보여 준다.
 * 버튼은 진짜로 동작한다 (알림 권한 창, 배터리 창, 위젯 추가 창). 이미 허용된 폰에서는 창이 안 뜨거나 바로 돌아온다.
 * 뒤로 가기나 마지막 '시작하기'로 미리보기 목록에 돌아온다.
 */
@Composable
private fun StartFlow(flow: Flow, dark: Boolean, reduce: Boolean, onFlow: (Flow?) -> Unit) {
    var themeColor by remember { mutableStateOf(ThemeColor.DEFAULT) }
    val modifier = Modifier.fillMaxSize().background(AldotTheme.colors.bg).windowInsetsPadding(WindowInsets.safeDrawing)
    when (flow) {
        Flow.WELCOME -> {
            BackHandler { onFlow(null) }
            WelcomeScreen(onStart = { onFlow(Flow.STEPS) }, modifier = modifier)
        }
        Flow.STEPS -> {
            // 첫 단계에서 뒤로 가면 환영 화면으로 (안의 단계에서는 OnboardingScreen이 이전 단계로 돌린다)
            BackHandler { onFlow(Flow.WELCOME) }
            // 위젯 단계에서 고른 색이 바로 입혀지게 테마를 한 번 더 감싼다 (움직임 줄이기는 미리보기 스위치 그대로)
            AldotWidgetTheme(themeColor = themeColor, darkTheme = dark) {
                CompositionLocalProvider(LocalReduceMotion provides reduce) {
                    OnboardingScreen(
                        themeColor = themeColor,
                        onThemeColorChange = { themeColor = it },
                        refreshInterval = RefreshInterval.DEFAULT,
                        onFinished = { onFlow(null) },
                        modifier = Modifier.fillMaxSize().background(AldotTheme.colors.bg).windowInsetsPadding(WindowInsets.safeDrawing),
                        stepsOverride = OnboardingStep.entries,
                    )
                }
            }
        }
    }
}

@Composable
private fun Items() {
    Card("① 설치 허용 스위치") { PermissionPicture() }
    Card("② 배터리 창 '허용' 누르기") { BatteryDialogPreview(samsung = true) }
    Card("③ 알림 내려오기") { NotificationPreview() }
    Card("④ 자동 차단 단계") { BlockSteps() }
    StatusCard()
    UpdateCardDemo()
    DownloadDemo()
    Card("⑧ 홈 계기판") {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AldotTheme.colors.button).padding(vertical = 16.dp)) {
            Gauge(Quota(total = 15L * 1024 * 1024, used = (1.8 * 1024 * 1024).toLong()))
        }
    }
    Card("⑨ 준비 끝") { DoneHeader(RefreshInterval.DEFAULT) }
    Card("⑩ 환영 화면 위젯 그림") { WidgetIllustration() }
}

/** ⑤ 꺼짐 ↔ 켜짐: 버튼으로 바꾼다 (배터리 창에서 '허용'하고 돌아온 순간) */
@Composable
private fun StatusCard() {
    var on by remember { mutableStateOf(false) }
    Card("⑤ 확인 필요 → 켜짐", action = if (on) "끄기" else "켜기", onAction = { on = !on }) {
        SectionTitle("위젯 동작", badge = if (on) null else "1가지 확인 필요")
        StatusRow(R.drawable.ic_moon, "밤에도 로그인 유지", on, onFix = { on = true })
    }
}

/** ⑥ 업데이트 확인: 카드 안 버튼을 누르면 1초 뒤 새 버전을 찾은 것처럼, '최신일 때'는 새 버전 없이 */
@Composable
private fun UpdateCardDemo() {
    val scope = rememberCoroutineScope()
    val morning = remember {
        java.time.LocalDate.now().atTime(9, 14).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val idle = UpdateUiState(lastCheckedAt = morning)
    var state by remember { mutableStateOf(idle) }
    var findNew by remember { mutableStateOf(true) }
    fun check() {
        scope.launch {
            state = state.copy(check = CheckState.CHECKING)
            delay(1000)
            state = state.copy(
                check = CheckState.IDLE,
                lastCheckedAt = System.currentTimeMillis(),
                checkedByUser = true,
                checkedThisSession = true,
                available = if (findNew) UpdateInfo("1.0.2", 3, "aldot-widget-1.0.2.apk", 11_500_000, "x", "2026-11-20") else null,
            )
        }
    }
    Card(
        "⑥ 업데이트 확인 결과",
        action = if (findNew) "다음엔 최신으로" else "다음엔 새 버전으로",
        onAction = { findNew = !findNew; state = idle },
    ) {
        Text("카드의 '업데이트 확인'을 눌러 보세요", fontSize = 13.sp, color = AldotTheme.colors.sub)
        AppCard(state, onCheck = ::check, onOpenUpdate = {})
    }
}

/** ⑦ 받는 중 막대 → 다 받으면 체크 */
@Composable
private fun DownloadDemo() {
    var run by remember { mutableIntStateOf(0) }
    Card("⑦ 내려받기 → 설치 중", action = "다시 보기", onAction = { run++ }) {
        key(run) {
            var progress by remember { mutableStateOf(0f) }
            LaunchedEffect(Unit) {
                delay(400)
                // 실제처럼 띄엄띄엄 받은 양이 온다
                for (p in listOf(0.08f, 0.21f, 0.33f, 0.47f, 0.62f, 0.70f, 0.86f, 1f)) {
                    progress = p
                    delay(350)
                }
            }
            if (progress < 1f) {
                ProgressPart("받는 중", "${"%.1f".format(11.5 * progress)}MB / 11.5MB", progress)
            } else {
                InstallingPart("11.5MB")
            }
        }
    }
}

/** 제목 + '다시 보기' 버튼 + 내용. 다시 보기는 내용을 새로 그려서 애니메이션을 처음부터 돌린다 */
@Composable
private fun Card(
    title: String,
    action: String = "다시 보기",
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AldotTheme.colors
    var replay by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.panel).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.weight(1f))
            TextButton(onClick = { if (onAction != null) onAction() else replay++ }) { Text(action) }
        }
        key(replay) { Column(Modifier.padding(top = 8.dp)) { content() } }
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, color = AldotTheme.colors.text, modifier = Modifier.weight(1f))
        Switch(value, onChange)
    }
}
