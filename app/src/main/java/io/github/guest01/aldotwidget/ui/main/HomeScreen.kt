package io.github.guest01.aldotwidget.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.RefreshPolicy
import io.github.guest01.aldotwidget.data.UsageFormat
import io.github.guest01.aldotwidget.data.sampleSnapshot
import io.github.guest01.aldotwidget.ui.MainUiState
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** 폴드 안쪽 화면처럼 넓은 화면에서도 카드가 너무 넓어지지 않게 가운데에 이 폭까지만 쓴다 */
internal val MaxContentWidth = 520.dp

/**
 * 앱을 열면 보이는 화면 (시안 "앱 화면" ①~⑤).
 * 위는 스크롤되고, 아래 "위젯 추가"와 안내 문구는 화면 바닥에 붙어 있다.
 */
@Composable
fun HomeScreen(
    ready: MainUiState.Ready,
    showBatteryWarning: Boolean,
    onFixBattery: () -> Unit,
    onRefresh: () -> Unit,
    onAddWidget: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AldotTheme.colors
    val snapshot = ready.snapshot
    val status = HomeText.status(ready, cooldownClock(ready.lastRequestedAt), ZoneId.systemDefault())

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = MaxContentWidth).fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "알닷 위젯", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                // 아이콘 버튼은 누르는 영역(48dp)이 아이콘보다 커서, 아이콘이 카드 오른쪽 끝과 줄이 맞도록 민다
                IconButton(onClick = onOpenSettings, modifier = Modifier.offset(x = 12.dp)) {
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = "설정", tint = colors.sub)
                }
            }

            // 내용이 화면에 다 들어가면 "위젯 추가"를 바닥에 붙이고, 넘치면(큰 글자 등) 전체를 한 번에 스크롤한다.
            // 바닥을 따로 고정하면 넘칠 때 새로고침 줄이 그 뒤로 숨는다.
            BoxWithConstraints(Modifier.weight(1f)) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            snapshot.planName ?: "요금제", fontSize = 22.sp, lineHeight = 30.sp,
                            fontWeight = FontWeight.ExtraBold, color = colors.text,
                        )
                        Text(HomeText.planSubtitle(snapshot), fontSize = 13.sp, color = colors.sub, modifier = Modifier.padding(top = 4.dp))

                        if (showBatteryWarning) BatteryBanner(onFixBattery, Modifier.padding(top = 16.dp))

                        DataCard(snapshot.data, Modifier.padding(top = 16.dp))
                        Row(
                            Modifier.padding(top = 12.dp).fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SmallQuotaCard("음성", snapshot.voice, UsageFormat::voiceSeconds, HomeText::voiceCaption, Modifier.weight(1f))
                            SmallQuotaCard("문자", snapshot.sms, UsageFormat::smsCount, HomeText::smsCaption, Modifier.weight(1f))
                        }
                        StatusRow(status, onRefresh, Modifier.padding(top = 12.dp))
                    }
                    Column(Modifier.padding(top = 16.dp)) {
                        AddWidgetRow(onAddWidget)
                        Text(
                            "알닷에 공개된 실시간 사용량이에요.\n통신사 전산 반영은 조금 늦을 수 있어요.",
                            fontSize = 12.sp, lineHeight = 18.sp, color = colors.muted, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 1분 제한이 끝나는 순간 버튼이 다시 켜지도록, 그때 한 번 시각을 새로 읽는다. */
@Composable
private fun cooldownClock(lastRequestedAt: Long?): Instant {
    val now by produceState(Instant.now(), lastRequestedAt) {
        value = Instant.now()
        val endsIn = (lastRequestedAt ?: return@produceState) + RefreshPolicy.COOLDOWN_MILLIS - System.currentTimeMillis()
        if (endsIn > 0) {
            delay(endsIn + 100)
            value = Instant.now()
        }
    }
    return now
}

@Composable
private fun BatteryBanner(onFix: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.warnBg)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_error_outline), contentDescription = null, tint = colors.warn, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("밤에 로그인이 풀릴 수 있어요", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Text("배터리 사용을 '제한 없음'으로 허용해 주세요", fontSize = 12.sp, color = colors.sub)
        }
        TextButton(onClick = onFix) {
            Text("허용하기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent)
        }
    }
}

/** 데이터: 왼쪽 고리(남은 %), 오른쪽 남은 용량 */
@Composable
private fun DataCard(quota: Quota?, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.panel).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ratio = quota?.remainingRatio ?: 0f
        Box(contentAlignment = Alignment.Center) {
            Ring(ratio, size = 104.dp, stroke = 11.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(ratio * 100).roundToInt()}%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
                Text("남음", fontSize = 11.sp, color = colors.sub)
            }
        }
        Column(Modifier.padding(start = 20.dp)) {
            Text("데이터", fontSize = 13.sp, color = colors.sub)
            Text(
                quota?.let { UsageFormat.dataKb(it.remaining) } ?: "정보 없음",
                fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp,
                color = colors.text, maxLines = 1,
            )
            if (quota != null) Text(HomeText.dataCaption(quota), fontSize = 13.sp, color = colors.muted)
        }
    }
}

/** 음성·문자: 남은 양, 막대, 제공량 중 사용량 */
@Composable
private fun SmallQuotaCard(
    label: String,
    quota: Quota?,
    format: (Long) -> String,
    caption: (Quota) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = AldotTheme.colors
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(colors.panel).padding(16.dp)) {
        Text(label, fontSize = 13.sp, color = colors.sub)
        Text(
            quota?.let { format(it.remaining) } ?: "정보 없음",
            fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, color = colors.text,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
        )
        if (quota != null) {
            QuotaBar(quota.remainingRatio, Modifier.padding(top = 10.dp))
            Text(caption(quota), fontSize = 12.sp, color = colors.muted, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** 마지막 갱신 시각(또는 갱신 중·실패)과 새로고침 버튼 */
@Composable
private fun StatusRow(status: HomeStatus, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        if (status.failed) {
            Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = colors.warn, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            status.text, fontSize = 13.sp, color = if (status.failed) colors.warn else colors.sub,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        val content = if (status.buttonEnabled) colors.accent else colors.muted
        Surface(
            onClick = onRefresh,
            enabled = status.buttonEnabled,
            shape = CircleShape,
            color = colors.bg,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier.padding(start = 12.dp).height(40.dp),
        ) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (status.refreshing) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = colors.accent, strokeWidth = 2.dp)
                } else {
                    Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(status.buttonLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = content)
            }
        }
    }
}

@Composable
private fun AddWidgetRow(onClick: () -> Unit) {
    val colors = AldotTheme.colors
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = colors.panel, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.stage),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_widgets), contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("홈 화면에 위젯 추가", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Text("1×1 · 2×1 · 2×2", fontSize = 12.sp, color = colors.muted)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
        }
    }
}

/** 원형 게이지. 위젯의 고리와 같은 모양이다. */
@Composable
private fun Ring(ratio: Float, size: Dp, stroke: Dp) {
    val colors = AldotTheme.colors
    Canvas(Modifier.size(size)) {
        val width = stroke.toPx()
        // 선 두께의 절반만큼 안쪽에 그려야 바깥이 잘리지 않는다
        val topLeft = Offset(width / 2, width / 2)
        val arcSize = Size(this.size.width - width, this.size.height - width)
        drawArc(colors.line, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width))
        if (ratio > 0f) {
            drawArc(
                colors.accent, -90f, 360f * ratio.coerceAtMost(1f), useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun QuotaBar(ratio: Float, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.line)) {
        Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(colors.accent))
    }
}

// Android Studio의 Compose Preview에서 기기 없이 화면을 볼 수 있게 하는 예시 데이터
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    AldotWidgetTheme {
        HomeScreen(
            ready = MainUiState.Ready(sampleSnapshot()),
            showBatteryWarning = true,
            onFixBattery = {},
            onRefresh = {},
            onAddWidget = {},
            onOpenSettings = {},
        )
    }
}
