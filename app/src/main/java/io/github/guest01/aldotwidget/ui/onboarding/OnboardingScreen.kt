package io.github.guest01.aldotwidget.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import io.github.guest01.aldotwidget.ui.batteryExemptionIntent
import io.github.guest01.aldotwidget.ui.canPostNotifications
import io.github.guest01.aldotwidget.ui.isIgnoringBatteryOptimizations
import io.github.guest01.aldotwidget.widget.CompactWidgetReceiver
import io.github.guest01.aldotwidget.widget.DetailedWidgetReceiver
import io.github.guest01.aldotwidget.widget.SquareWidgetReceiver
import io.github.guest01.aldotwidget.widget.UsageWidget
import kotlinx.coroutines.launch

enum class OnboardingStep { NOTIFICATIONS, BATTERY, WIDGET }

/** 이미 끝난 단계는 건너뛴다 (알림이 이미 허용됐거나, 배터리 예외가 됐거나, 위젯이 이미 홈 화면에 있으면). */
internal fun onboardingSteps(
    needsNotifications: Boolean,
    needsBattery: Boolean,
    needsWidget: Boolean,
): List<OnboardingStep> = buildList {
    if (needsNotifications) add(OnboardingStep.NOTIFICATIONS)
    if (needsBattery) add(OnboardingStep.BATTERY)
    if (needsWidget) add(OnboardingStep.WIDGET)
}

/**
 * 첫 로그인 직후 한 번 보여 주는 단계별 안내. 위젯 앱은 앱보다 홈 화면에서 쓰는 게 목적이라,
 * 위젯이 제대로 돌아가는 데 필요한 설정을 처음에 한 번에 끝내게 한다. 모든 단계는 건너뛸 수 있다.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 시작할 때 한 번만 계산한다. 단계를 진행하는 중에 목록이 바뀌면 번호가 흔들린다.
    val steps by produceState<List<OnboardingStep>?>(initialValue = null) {
        val hasWidget = GlanceAppWidgetManager(context).getGlanceIds(UsageWidget::class.java).isNotEmpty()
        value = onboardingSteps(
            needsNotifications = !canPostNotifications(context),
            needsBattery = !isIgnoringBatteryOptimizations(context),
            needsWidget = !hasWidget,
        )
    }
    var index by rememberSaveable { mutableIntStateOf(0) }

    val current = steps ?: return
    LaunchedEffect(current, index) {
        if (index >= current.size) onFinished()
    }
    val step = current.getOrNull(index) ?: return
    val next: () -> Unit = { index++ }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "${index + 1} / ${current.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (step) {
            OnboardingStep.NOTIFICATIONS -> NotificationStep(onDone = next)
            OnboardingStep.BATTERY -> BatteryStep(onDone = next)
            OnboardingStep.WIDGET -> WidgetStep(onDone = next)
        }
    }
}

@Composable
private fun StepText(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(body, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun NotificationStep(onDone: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    StepText(
        title = "로그인이 풀리면 알려 드릴까요?",
        body = "밤사이 로그인이 풀려 위젯이 멈추면 알림으로 한 번 알려 드려요. 다른 알림은 보내지 않아요.",
    )
    Spacer(Modifier.padding(4.dp))
    Button(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onDone()
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("알림 허용") }
    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("건너뛰기") }
}

@Composable
private fun BatteryStep(onDone: () -> Unit) {
    val context = LocalContext.current
    // 요청 창이든 앱 정보 화면이든 돌아오면 다음 단계로 간다. 설정 여부는 사용량 화면의 배너가 계속 확인한다.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onDone() }
    StepText(
        title = "밤에도 로그인을 유지할까요?",
        body = "알닷 로그인은 2시간 동안 아무 요청이 없으면 풀려요. 폰이 쉬는 밤에도 위젯이 로그인을 이어 가려면 " +
            "이 앱의 배터리 사용을 '제한 없음'으로 허용해야 해요.\n\n" +
            "시험한 폰에서는 하루 배터리 사용량의 0.2% 미만이었어요.",
    )
    Spacer(Modifier.padding(4.dp))
    Button(
        onClick = { launcher.launch(batteryExemptionIntent(context)) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("허용하기") }
    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("나중에") }
}

@Composable
private fun WidgetStep(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { GlanceAppWidgetManager(context) }
    StepText(
        title = "홈 화면에 위젯을 추가하세요",
        body = "크기를 고르면 홈 화면에 바로 추가돼요. 나중에 홈 화면을 길게 눌러 위젯 목록에서 추가할 수도 있어요.",
    )
    listOf(
        "데이터 남은 양 (2×1)" to CompactWidgetReceiver::class.java,
        "사용량 요약 (2×2)" to SquareWidgetReceiver::class.java,
        "사용량 상세 (4×2)" to DetailedWidgetReceiver::class.java,
    ).forEach { (label, receiver) ->
        OutlinedButton(
            onClick = { scope.launch { manager.requestPinGlanceAppWidget(receiver) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(label) }
    }
    Spacer(Modifier.padding(4.dp))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("완료") }
}
