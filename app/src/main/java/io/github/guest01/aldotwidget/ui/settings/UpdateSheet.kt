package io.github.guest01.aldotwidget.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.guest01.aldotwidget.BuildConfig
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.ui.DrawnCheck
import io.github.guest01.aldotwidget.ui.LocalReduceMotion
import io.github.guest01.aldotwidget.ui.rememberGuideClock
import io.github.guest01.aldotwidget.ui.rippleAlpha
import io.github.guest01.aldotwidget.ui.seg
import io.github.guest01.aldotwidget.ui.smooth
import kotlin.math.PI
import kotlin.math.sin
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.update.InstallState
import io.github.guest01.aldotwidget.update.UpdateInfo
import io.github.guest01.aldotwidget.update.UpdateManager
import io.github.guest01.aldotwidget.update.UpdateText

/**
 * 업데이트 창 (시안 "업데이트 화면" ③~⑨). 아래에서 올라오고, 설치 단계에 따라 아래쪽이 바뀐다.
 * 새 버전 안내 → 내려받는 중 → 설치 중. 막히면 안내형(설치 허용, 취소, 자동 차단, 실패)으로 바뀐다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(
    info: UpdateInfo,
    install: InstallState,
    updates: UpdateManager,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AldotTheme.colors
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bg,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
        ) {
            when (install) {
                InstallState.None, is InstallState.Downloading, InstallState.Installing ->
                    NotesPart(info, install, onInstall = updates::startInstall, onOpenBrowser = onOpenBrowser, onCancel = updates::cancelDownload)
                InstallState.NeedPermission -> GuidePart(
                    icon = R.drawable.ic_download, warn = false,
                    title = "설치를 한 번만 허용해 주세요",
                    body = "앱에서 바로 업데이트하려면 Android 설정에서 남았닷을 허용해야 해요.",
                    primary = "설정 열기" to {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
                        )
                    },
                    secondary = "브라우저로 받기" to onOpenBrowser,
                ) { PermissionPicture() }
                InstallState.Cancelled -> GuidePart(
                    icon = R.drawable.ic_error_outline, warn = false,
                    title = "설치를 취소했어요",
                    body = "Google Play 프로텍트나 설치 확인 창에서 취소하면 설치가 멈춰요. 다시 설치하려면 '다시 시도'를 눌러 주세요.",
                    primary = "다시 시도" to updates::startInstall,
                    secondary = "나중에" to onDismiss,
                )
                InstallState.Blocked -> GuidePart(
                    icon = R.drawable.ic_shield_off, warn = true,
                    title = "자동 차단이 설치를 막았어요",
                    body = "갤럭시의 '보안 위험 자동 차단'이 켜져 있으면 스토어 밖에서 받은 앱은 설치되지 않아요.",
                    primary = "다시 시도" to updates::startInstall,
                    secondary = "나중에" to onDismiss,
                ) { BlockSteps() }
                InstallState.InstallFailed -> GuidePart(
                    icon = R.drawable.ic_shield_off, warn = true,
                    title = "설치하지 못했어요",
                    body = "갤럭시는 '보안 위험 자동 차단'이 켜져 있으면 설치가 막혀요. 켜져 있다면 잠시 끄고 다시 시도하거나, 브라우저로 받아 주세요.",
                    primary = "다시 시도" to updates::startInstall,
                    secondary = "브라우저로 받기" to onOpenBrowser,
                ) { BlockSteps() }
                InstallState.DownloadFailed -> GuidePart(
                    icon = R.drawable.ic_wifi_off, warn = true,
                    title = "내려받지 못했어요",
                    body = "인터넷 연결을 확인하고 다시 시도해 주세요. 계속 안 되면 브라우저로 받을 수 있어요.",
                    primary = "다시 시도" to updates::startInstall,
                    secondary = "브라우저로 받기" to onOpenBrowser,
                )
            }
        }
    }
}

/** 새 버전 · 내려받는 중 · 설치 중: 위쪽(버전, 바뀐 점)은 같고 아래 버튼 자리만 바뀐다 */
@Composable
private fun NotesPart(
    info: UpdateInfo,
    install: InstallState,
    onInstall: () -> Unit,
    onOpenBrowser: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = AldotTheme.colors
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(48.dp)
        Column(Modifier.padding(start = 14.dp)) {
            Text(
                "새 버전 ${info.version}", fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, color = colors.text,
                modifier = Modifier.semantics { heading() },
            )
            Text(UpdateText.summary(info, BuildConfig.VERSION_NAME), fontSize = 13.sp, lineHeight = 18.sp, color = colors.sub)
        }
    }
    if (info.notes.isNotEmpty()) {
        Text("바뀐 점", fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = colors.muted, modifier = Modifier.padding(top = 22.dp))
        Column(
            Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.segment)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            info.notes.forEach { note ->
                Row {
                    Text("•", fontSize = 14.sp, lineHeight = 21.sp, color = colors.muted)
                    Text(note, fontSize = 14.sp, lineHeight = 21.sp, color = colors.text, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    when (install) {
        is InstallState.Downloading -> {
            ProgressPart("내려받는 중", "${UpdateText.size(install.read)} / ${UpdateText.size(install.total)}",
                progress = if (install.total > 0) install.read.toFloat() / install.total else 0f)
            SubButton("취소", onCancel)
        }
        InstallState.Installing -> {
            InstallingPart(UpdateText.size(info.size))
            Text(
                "앱이 잠깐 닫혀요. 끝나면 알림으로 알려 드려요.\n" +
                    "처음에는 Google Play 프로텍트 창이 뜰 수 있어요. '앱 설치 안함'을 누르면 설치가 멈춰요.",
                fontSize = 14.sp, lineHeight = 21.sp, color = colors.sub, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(top = 12.dp),
            )
        }
        else -> {
            WideButton("바로 설치", primary = true, onClick = onInstall, height = 56.dp, modifier = Modifier.padding(top = 24.dp))
            SubButton("브라우저로 받기", onOpenBrowser)
        }
    }
}

@Composable
internal fun ProgressPart(label: String, amount: String, progress: Float) {
    val colors = AldotTheme.colors
    // 받은 양이 띄엄띄엄 와도 막대는 부드럽게 따라간다
    val shown by animateFloatAsState(progress, tween(if (LocalReduceMotion.current) 0 else 300), label = "download")
    Column(
        Modifier.padding(top = 24.dp).heightIn(min = 56.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.weight(1f))
            Text(amount, fontSize = 15.sp, lineHeight = 20.sp, color = colors.sub)
        }
        LinearProgressIndicator(
            progress = { shown },
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = colors.button,
            trackColor = colors.line,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/** 다 받은 뒤: 동그라미가 톡 커지며 체크가 그려지고 "설치하는 중"으로 넘어간다 (시안 애니메이션 ⑦) */
@Composable
internal fun InstallingPart(amount: String) {
    val colors = AldotTheme.colors
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduce) 1f else 0f) }
    val draw = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(Unit) {
        draw.animateTo(1f, tween(durationMillis = 450, delayMillis = 150))
    }
    Row(
        Modifier.padding(top = 24.dp).fillMaxWidth().heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.clip(CircleShape).background(colors.button),
            contentAlignment = Alignment.Center,
        ) {
            DrawnCheck(draw.value, Color.White, Modifier.size(22.dp), strokeWidth = 2.8.dp)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text("다 받았어요. 설치하는 중", fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Text(amount, fontSize = 13.sp, lineHeight = 18.sp, color = colors.sub)
        }
    }
}

@Composable
private fun SubButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).heightIn(min = 44.dp)) {
        Text(label, fontSize = 15.sp, lineHeight = 20.sp, color = AldotTheme.colors.sub)
    }
}

/** 안내형: 둥근 아이콘 + 제목 + 설명 + (그림이나 단계) + 버튼 두 개 */
@Composable
private fun GuidePart(
    icon: Int,
    warn: Boolean,
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>,
    extra: @Composable () -> Unit = {},
) {
    val colors = AldotTheme.colors
    Box(
        Modifier.padding(top = 8.dp).size(48.dp).clip(CircleShape).background(if (warn) colors.warnBg else colors.stage),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = if (warn) colors.warn else colors.accent, modifier = Modifier.size(24.dp))
    }
    Text(
        title, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, color = colors.text,
        modifier = Modifier.padding(top = 16.dp).semantics { heading() },
    )
    Text(body, fontSize = 15.sp, lineHeight = 24.sp, color = colors.sub, modifier = Modifier.padding(top = 8.dp))
    extra()
    WideButton(primary.first, primary = true, onClick = primary.second, height = 56.dp, modifier = Modifier.padding(top = 24.dp))
    SubButton(secondary.first, secondary.second)
}

/** 설치 허용: Android 설정에서 켤 스위치 그림 (눌러지는 스위치가 아니라 그림이다). 스위치가 켜지는 모습을 몇 번 보여 준다 */
@Composable
internal fun PermissionPicture() {
    val colors = AldotTheme.colors
    Column(
        Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.segment)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
            .clearAndSetSemantics { },
    ) {
        Text("Android 설정 · 출처를 알 수 없는 앱 설치", fontSize = 12.sp, lineHeight = 16.sp, color = colors.muted)
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(32.dp)
            Text(
                "남았닷", fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = colors.text,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            GuideSwitch()
        }
    }
    Text(
        "켜고 뒤로 돌아오면 이어서 설치해요. 처음 한 번만 하면 돼요.",
        fontSize = 13.sp, lineHeight = 19.sp, color = colors.muted, modifier = Modifier.padding(top = 10.dp),
    )
}

/**
 * 스위치 그림: 꺼진 스위치에 탭 물결이 퍼지고 켜진 뒤 한 번 반짝인다 (시안 '안내 애니메이션' ①).
 * 3.2초 한 바퀴를 세 번 돌고 켜진 모습에서 멈춘다.
 */
@Composable
private fun GuideSwitch() {
    val colors = AldotTheme.colors
    val t by rememberGuideClock(cycleMs = 3200)
    val on = smooth(seg(t, 0.38f, 0.48f))
    val ripple = seg(t, 0.28f, 0.50f)
    val glow = seg(t, 0.52f, 0.82f).let { if (it <= 0f || it >= 1f) 0f else sin(it * PI.toFloat()) }
    val offTrack = lerp(colors.line, colors.muted, 0.45f)
    Canvas(Modifier.size(width = 44.dp, height = 26.dp)) {
        val radius = CornerRadius(size.height / 2)
        if (glow > 0f) {
            val grow = 5.dp.toPx() * glow
            drawRoundRect(
                colors.button.copy(alpha = 0.25f * glow), topLeft = Offset(-grow, -grow),
                size = Size(size.width + grow * 2, size.height + grow * 2), cornerRadius = CornerRadius(size.height / 2 + grow),
            )
        }
        drawRoundRect(lerp(offTrack, colors.button, on), cornerRadius = radius)
        val center = Offset(13.dp.toPx() + 18.dp.toPx() * on, size.height / 2)
        val alpha = rippleAlpha(ripple)
        if (alpha > 0f) drawCircle(colors.button.copy(alpha = alpha), radius = 22.dp.toPx() * (0.3f + 0.7f * ripple), center = center)
        drawCircle(Color.White, radius = 10.dp.toPx(), center = center)
    }
}

/** 보안 위험 자동 차단 끄는 길 (Fold8 One UI 9 메뉴 이름). 1 → 2 → 3이 차례로 강조된다 (시안 '안내 애니메이션' ④) */
@Composable
internal fun BlockSteps() {
    val colors = AldotTheme.colors
    val steps = listOf(
        "설정 → 보안 및 개인정보 보호 → 보안 위험 자동 차단",
        "잠시 끄고 여기로 돌아와 '다시 시도'",
        "설치가 끝나면 다시 켜도 돼요",
    )
    val t by rememberGuideClock(cycleMs = 3600)
    // 한 단계에 1/3씩, 끝 무렵 잠깐은 강조 없이. 멈춘 뒤(t = 1)에는 모두 보통 모습
    val active = if (t >= 1f) -1 else (t * 3).toInt().takeIf { (t * 3) % 1f < 0.9f } ?: -1
    Column(
        Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.segment)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        steps.forEachIndexed { index, step ->
            val on = index == active
            // 꺼질 때도 같은 색에서 투명도만 낮춘다. Color.Transparent(투명한 검정)로 가면 중간에 검게 깜박인다
            val rowBg by animateColorAsState(if (on) colors.stage else colors.stage.copy(alpha = 0f), tween(180), label = "stepBg")
            val numBg by animateColorAsState(if (on) colors.button else colors.line, tween(180), label = "stepNum")
            val numInk by animateColorAsState(if (on) Color.White else colors.sub, tween(180), label = "stepInk")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(rowBg).padding(8.dp)) {
                Box(
                    Modifier.padding(top = 1.dp).size(20.dp).clip(CircleShape).background(numBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = numInk)
                }
                Text(step, fontSize = 14.sp, lineHeight = 21.sp, color = colors.text, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}
