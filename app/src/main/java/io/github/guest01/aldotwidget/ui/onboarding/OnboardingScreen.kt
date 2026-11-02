package io.github.guest01.aldotwidget.ui.onboarding

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.data.RefreshInterval
import io.github.guest01.aldotwidget.data.ThemeColor
import io.github.guest01.aldotwidget.ui.batteryExemptionIntent
import io.github.guest01.aldotwidget.ui.canPostNotifications
import io.github.guest01.aldotwidget.ui.isIgnoringBatteryOptimizations
import io.github.guest01.aldotwidget.ui.main.MaxContentWidth
import io.github.guest01.aldotwidget.ui.notificationSettingsIntent
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.ui.theme.ThemeColorPicker
import io.github.guest01.aldotwidget.widget.CompactWidgetReceiver
import io.github.guest01.aldotwidget.widget.SquareWidgetReceiver
import io.github.guest01.aldotwidget.widget.TinyWidgetReceiver
import io.github.guest01.aldotwidget.widget.UsageWidget
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

enum class OnboardingStep { NOTIFICATIONS, BATTERY, WIDGET }

/**
 * 로그인 다음에 보여 줄 단계. 이미 허용된 알림·배터리 단계는 건너뛴다.
 * 위젯 단계는 테마 색도 고르는 곳이라 위젯이 이미 있어도 보여 준다.
 */
internal fun onboardingSteps(needsNotifications: Boolean, needsBattery: Boolean): List<OnboardingStep> = buildList {
    if (needsNotifications) add(OnboardingStep.NOTIFICATIONS)
    if (needsBattery) add(OnboardingStep.BATTERY)
    add(OnboardingStep.WIDGET)
}

internal fun onboardingSteps(context: Context) = onboardingSteps(
    needsNotifications = !canPostNotifications(context),
    needsBattery = !isIgnoringBatteryOptimizations(context),
)

/** 위젯 고르기 단계의 세 항목 (시안 ⑤). 위젯 목록의 이름·설명과 같다 */
internal enum class WidgetChoice(
    val label: String,
    val size: String,
    val description: String,
    val receiver: KClass<out GlanceAppWidgetReceiver>,
) {
    TINY("데이터 고리", "1×1", "남은 비율만 작게", TinyWidgetReceiver::class),
    COMPACT("데이터 남은 양", "2×1", "고리와 남은 용량", CompactWidgetReceiver::class),
    SQUARE("사용량 요약", "2×2", "데이터·음성·문자, 넓히면 막대로", SquareWidgetReceiver::class),
}

/**
 * 첫 로그인 직후 한 번 보여 주는 단계별 안내 (시안 ③~⑥). 위젯 앱은 앱보다 홈 화면에서 쓰는 게 목적이라,
 * 위젯이 제대로 돌아가는 데 필요한 설정을 처음에 한 번에 끝내게 한다. 모든 단계는 건너뛸 수 있다.
 * 단계 번호는 로그인(1)에서 이어서 센다.
 */
@Composable
fun OnboardingScreen(
    themeColor: ThemeColor,
    onThemeColorChange: (ThemeColor) -> Unit,
    refreshInterval: RefreshInterval,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // 시작할 때 한 번만 정한다. 진행 중에 알림이 허용되면서 목록이 바뀌면 번호가 흔들린다.
    val steps = rememberSaveable { onboardingSteps(context).map { it.name } }.map(OnboardingStep::valueOf)
    var index by rememberSaveable { mutableIntStateOf(0) }
    var requestedWidget by rememberSaveable { mutableStateOf<String?>(null) }
    val next: () -> Unit = { index++ }
    fun position() = StepPosition(number = index + 2, total = steps.size + 1)

    when (steps.getOrNull(index)) {
        OnboardingStep.NOTIFICATIONS -> NotificationStep(position(), onDone = next, modifier = modifier)
        OnboardingStep.BATTERY -> BatteryStep(position(), onDone = next, modifier = modifier)
        OnboardingStep.WIDGET -> WidgetStep(
            position = position(),
            themeColor = themeColor,
            onThemeColorChange = onThemeColorChange,
            onRequested = { requestedWidget = it.name; next() },
            onSkip = next,
            modifier = modifier,
        )
        null -> DoneStep(
            requestedWidget = requestedWidget?.let(WidgetChoice::valueOf),
            refreshInterval = refreshInterval,
            onFinished = onFinished,
            modifier = modifier,
        )
    }
}

@Composable
private fun NotificationStep(position: StepPosition, onDone: () -> Unit, modifier: Modifier) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    OnboardingPage(
        position = position,
        title = accentTitle("로그인이 풀리면\n", "알려 드릴까요?"),
        body = "밤사이 로그인이 풀려 위젯이 멈추면 이런 알림을 한 번 보내 드려요.",
        skipLabel = "건너뛰기",
        onSkip = onDone,
        primaryLabel = "알림 받기",
        onPrimary = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onDone()
            }
        },
        modifier = modifier,
    ) {
        val colors = AldotTheme.colors
        // 실제로 보내는 알림(Notifier)과 같은 문구의 미리보기
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.segment).padding(horizontal = 16.dp, vertical = 28.dp)) {
            Row(
                Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp))
                    .background(colors.raised).padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colors.button),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_stat_usage), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("알닷 위젯 · 지금", fontSize = 12.sp, color = colors.muted)
                    Text("알닷 로그인이 만료됐어요", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text("위젯을 계속 갱신하려면 다시 로그인해 주세요", fontSize = 14.sp, color = colors.sub)
                }
            }
        }
        Text("이 알림 말고는 보내지 않아요", fontSize = 13.sp, color = colors.muted, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun BatteryStep(position: StepPosition, onDone: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    // 요청 창이든 앱 정보 화면이든 돌아오면 다음 단계로 간다. 설정 여부는 마지막 화면과 홈 화면이 다시 확인한다.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onDone() }
    OnboardingPage(
        position = position,
        title = accentTitle("", "밤에도", " 로그인을\n유지할까요?"),
        body = "알닷 로그인은 2시간 동안 아무 요청이 없으면 풀려요. 폰이 쉬는 밤에도 위젯이 로그인을 이어 가려면 배터리 사용을 허용해 주세요.",
        skipLabel = "나중에",
        onSkip = onDone,
        primaryLabel = "허용하러 가기",
        onPrimary = { launcher.launch(batteryExemptionIntent(context)) },
        modifier = modifier,
    ) {
        val colors = AldotTheme.colors
        // 다음에 뜰 시스템 창을 미리 보여 주고 어느 버튼을 누르면 되는지 알려 준다
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.segment)
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("다음 화면에서 이런 창이 떠요", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.sub)
            Column(
                Modifier.padding(top = 10.dp).fillMaxWidth().shadow(6.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
                    .background(colors.raised).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            ) {
                Text("앱이 백그라운드에서 항상 실행되도록 허용할까요?", fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Text("배터리 사용량이 늘어날 수 있어요.", fontSize = 13.sp, color = colors.sub, modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("거부", fontSize = 14.sp, color = colors.sub, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                    Surface(shape = CircleShape, color = colors.raised, border = BorderStroke(2.dp, colors.accent)) {
                        Text("허용", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            }
            Row(Modifier.align(Alignment.End).padding(top = 10.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_arrow_upward), contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Text("'허용'을 눌러 주세요", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent, modifier = Modifier.padding(start = 6.dp))
            }
        }
        Text(
            "시험한 폰에서는 하루 배터리 사용량의 0.2% 미만이었어요\n실제 창의 문구는 폰마다 조금 달라요",
            fontSize = 13.sp, lineHeight = 20.sp, color = colors.muted, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun WidgetStep(
    position: StepPosition,
    themeColor: ThemeColor,
    onThemeColorChange: (ThemeColor) -> Unit,
    onRequested: (WidgetChoice) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { GlanceAppWidgetManager(context) }
    var picked by rememberSaveable { mutableStateOf(WidgetChoice.SQUARE) }
    val colors = AldotTheme.colors
    OnboardingPage(
        position = position,
        title = accentTitle("홈 화면에 둘\n", "위젯", "을 골라 주세요"),
        body = "색은 앱과 위젯에 함께 적용돼요. 나중에 설정에서 바꿀 수 있어요.",
        skipLabel = "건너뛰기",
        onSkip = onSkip,
        primaryLabel = "홈 화면에 추가",
        onPrimary = {
            // 런처에 "홈 화면에 추가할까요?" 창을 요청하고 바로 다음 화면으로 간다 (창은 그 위에 뜬다)
            scope.launch { manager.requestPinGlanceAppWidget(picked.receiver.java) }
            onRequested(picked)
        },
        footnote = "나중에 홈 화면을 길게 눌러 추가할 수도 있어요",
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WidgetChoice.entries.forEach { choice ->
                WidgetOption(choice, selected = choice == picked, onClick = { picked = choice })
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("색", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.padding(end = 10.dp))
            ThemeColorPicker(themeColor, onThemeColorChange, Modifier.weight(1f))
            Text(themeColor.label, fontSize = 13.sp, color = colors.sub)
        }
    }
}

@Composable
private fun WidgetOption(choice: WidgetChoice, selected: Boolean, onClick: () -> Unit) {
    val colors = AldotTheme.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.stage else colors.bg,
        border = BorderStroke(2.dp, if (selected) colors.accent else colors.line),
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(width = 112.dp, height = 72.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (selected) colors.bg.copy(alpha = 0.6f) else colors.stage),
                contentAlignment = Alignment.Center,
            ) {
                when (choice) {
                    WidgetChoice.TINY -> MiniCard(Modifier.size(48.dp, 46.dp)) { MiniRing(30.dp, 4.dp) }
                    WidgetChoice.COMPACT -> MiniCard(Modifier.size(92.dp, 44.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MiniRing(26.dp, 3.5.dp)
                            Column(Modifier.padding(start = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("5.7 GB", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                                Box(Modifier.size(32.dp, 3.dp).clip(CircleShape).background(colors.line))
                            }
                        }
                    }
                    WidgetChoice.SQUARE -> MiniCard(Modifier.size(58.dp)) {
                        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("5.7 GB", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(colors.line)) {
                                Box(Modifier.fillMaxWidth(0.38f).fillMaxHeight().clip(CircleShape).background(colors.accent))
                            }
                            Box(Modifier.fillMaxWidth(0.7f).padding(top = 3.dp).height(3.dp).clip(CircleShape).background(colors.line))
                            Box(Modifier.fillMaxWidth(0.55f).height(3.dp).clip(CircleShape).background(colors.line))
                        }
                    }
                }
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(choice.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Text("${choice.size} · ${choice.description}", fontSize = 13.sp, color = colors.sub)
            }
        }
    }
}

@Composable
private fun MiniCard(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(AldotTheme.colors.raised), contentAlignment = Alignment.Center) {
        content()
    }
}

/** 위젯 그림 속 작은 고리 (남은 38%) */
@Composable
private fun MiniRing(size: Dp, stroke: Dp) {
    val colors = AldotTheme.colors
    Canvas(Modifier.size(size)) {
        val width = stroke.toPx()
        val topLeft = Offset(width / 2, width / 2)
        val arcSize = Size(this.size.width - width, this.size.height - width)
        drawArc(colors.line, 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
        drawArc(colors.accent, -90f, 360f * 0.38f, false, topLeft, arcSize, style = Stroke(width, cap = StrokeCap.Round))
    }
}

/** 마지막 화면 (시안 ⑥): 무엇이 켜졌는지 정리해 보여 주고, 꺼진 것은 여기서 바로 켤 수 있게 한다. */
@Composable
private fun DoneStep(
    requestedWidget: WidgetChoice?,
    refreshInterval: RefreshInterval,
    onFinished: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val colors = AldotTheme.colors

    // 시스템 창(위젯 추가, 배터리)에서 돌아올 때마다 다시 확인한다
    var notificationsOn by remember { mutableStateOf(canPostNotifications(context)) }
    var batteryOn by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var hasWidget by remember { mutableStateOf(false) }
    var checkCount by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        notificationsOn = canPostNotifications(context)
        batteryOn = isIgnoringBatteryOptimizations(context)
        checkCount++
        onPauseOrDispose { }
    }
    LaunchedEffect(checkCount) {
        hasWidget = GlanceAppWidgetManager(context).getGlanceIds(UsageWidget::class.java).isNotEmpty()
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        batteryOn = isIgnoringBatteryOptimizations(context)
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = MaxContentWidth).fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 72.dp, bottom = 24.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(colors.button), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(
                    accentTitle("준비 끝!\n이제 ", "홈 화면", "에서 확인하세요"),
                    fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp,
                    color = colors.text, modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    "위젯이 ${refreshInterval.label}마다 알아서 갱신돼요.",
                    fontSize = 15.sp, lineHeight = 24.sp, color = colors.sub, modifier = Modifier.padding(top = 12.dp),
                )
                Column(Modifier.padding(top = 32.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.panel).padding(horizontal = 18.dp)) {
                    SummaryLine("알닷 로그인", ok = true, value = "연결됨")
                    SummaryDivider()
                    SummaryLine(
                        "로그인 만료 알림", ok = notificationsOn, value = if (notificationsOn) "켜짐" else "꺼짐",
                        onFix = { context.startActivity(notificationSettingsIntent(context)) },
                    )
                    SummaryDivider()
                    SummaryLine(
                        "밤에도 로그인 유지", ok = batteryOn, value = if (batteryOn) "켜짐" else "꺼짐",
                        detail = "밤사이 로그인이 풀릴 수 있어요",
                        onFix = { batteryLauncher.launch(batteryExemptionIntent(context)) },
                    )
                    SummaryDivider()
                    SummaryLine(
                        "홈 화면 위젯", ok = hasWidget,
                        value = when {
                            hasWidget && requestedWidget != null -> requestedWidget.label
                            hasWidget -> "추가됨"
                            else -> "나중에"
                        },
                    )
                }
            }
            PrimaryButton("확인", onFinished, Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun SummaryLine(label: String, ok: Boolean, value: String, detail: String? = null, onFix: (() -> Unit)? = null) {
    val colors = AldotTheme.colors
    // 꺼진 항목 중 켤 수 있는 것만 경고로 보여 준다 (위젯은 나중에 추가해도 되는 선택 사항)
    val fix = onFix?.takeIf { !ok }
    val warn = fix != null
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(if (warn) R.drawable.ic_error_outline else R.drawable.ic_check),
            contentDescription = null,
            tint = when {
                warn -> colors.warn
                ok -> colors.accent
                else -> colors.line
            },
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(label, fontSize = 15.sp, color = colors.text)
            if (warn && detail != null) Text(detail, fontSize = 12.sp, color = colors.muted)
        }
        if (fix != null) {
            // 글자 버튼은 최소 폭(58dp)이 있어 짧은 "켜기"는 양옆 여백이 넓다. 글자 끝이 다른 줄의 값과 맞도록 민다
            TextButton(onClick = fix, modifier = Modifier.offset(x = 16.dp)) {
                Text("켜기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        } else {
            Spacer(Modifier.width(8.dp))
            Text(value, fontSize = 14.sp, color = colors.sub)
        }
    }
}

@Composable
private fun SummaryDivider() {
    HorizontalDivider(thickness = 1.dp, color = AldotTheme.colors.divider)
}
