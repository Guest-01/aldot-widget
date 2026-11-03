package io.github.guest01.aldotwidget.ui.main

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.RefreshPolicy
import io.github.guest01.aldotwidget.data.UsageFormat
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.data.sampleSnapshot
import io.github.guest01.aldotwidget.ui.MainUiState
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/** 폴드 안쪽 화면처럼 넓은 화면에서도 카드가 너무 넓어지지 않게 가운데에 이 폭까지만 쓴다 */
internal val MaxContentWidth = 520.dp

/** 위쪽 색 영역 위의 흰 글자 */
private val OnAccent = Color.White
private val OnAccentSub = Color.White.copy(alpha = 0.85f)

/**
 * 앱을 열면 보이는 화면 (시안 "앱 화면"의 홈).
 * 위쪽은 테마 색 영역(남은 데이터 계기판), 아래는 음성·문자 도넛과 회선 정보, 맨 아래는 갱신 시각.
 * 테마 색 영역은 상태 표시줄 뒤까지 칠한다. 그래서 modifier에는 아래쪽 여백만 준다.
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
    val period = HomeText.period(snapshot)
    LightStatusBarIcons()

    // 내용이 화면에 다 들어가면 갱신 시각 줄을 바닥에 붙이고, 넘치면(큰 글자 등) 전체를 한 번에 스크롤한다
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Hero(snapshot.data, HomeText.dataPill(snapshot.data, period, snapshot.remainDays), status, onRefresh, onOpenSettings)
                Column(Modifier.widthIn(max = MaxContentWidth).fillMaxWidth().padding(horizontal = 20.dp)) {
                    if (status.failed) {
                        Banner(
                            title = "갱신하지 못했어요",
                            // 이유(인터넷 없음, 서버 문제…). 맨 아래 줄에 마지막 값의 시각이 빨간색으로 보인다
                            body = ready.error ?: "${status.time} 값을 보여 주고 있어요",
                            action = "다시 시도",
                            onAction = onRefresh,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                    if (showBatteryWarning) {
                        Banner(
                            title = "밤에 로그인이 풀릴 수 있어요",
                            body = "배터리 '제한 없음'이 필요해요",
                            action = "허용하기",
                            onAction = onFixBattery,
                            modifier = Modifier.padding(top = if (status.failed) 8.dp else 16.dp),
                        )
                    }
                    Row(
                        Modifier.padding(top = 16.dp).fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QuotaCard(
                            "음성", R.drawable.ic_phone, snapshot.voice, HomeText::voiceMinutes, "분",
                            Modifier.weight(1f),
                        )
                        QuotaCard("문자", R.drawable.ic_message, snapshot.sms, { "$it" }, "건", Modifier.weight(1f))
                    }
                    LineCard(snapshot, period, Modifier.padding(top = 12.dp))
                }
            }
            Footer(status, onAddWidget, Modifier.widthIn(max = MaxContentWidth).fillMaxWidth().padding(top = 16.dp))
        }
    }
}

/** 1분 제한이 끝나는 순간 아이콘이 다시 켜지도록, 그때 한 번 시각을 새로 읽는다. */
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

/** 홈 화면에 있는 동안 상태 표시줄 아이콘을 흰색으로 (테마 색 영역 위라서). 나가면 원래대로 */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        val before = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = before }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 큰 숫자는 폰 글자 크기를 115%까지만 따른다 (더 키우면 계기판을 넘친다). 위젯과 같은 상한 */
@Composable
private fun cappedSp(size: Int): TextUnit {
    val scale = LocalDensity.current.fontScale
    return if (scale <= MAX_HERO_FONT_SCALE) size.sp else (size * MAX_HERO_FONT_SCALE / scale).sp
}

private const val MAX_HERO_FONT_SCALE = 1.15f

/** 위쪽 테마 색 영역: 제목줄(새로고침·설정), 남은 데이터 반원 계기판, 하루 사용량 안내 */
@Composable
private fun Hero(
    data: Quota?,
    pill: String?,
    status: HomeStatus,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = AldotTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(colors.button)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = MaxContentWidth).fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("알닷 위젯", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = OnAccent, modifier = Modifier.weight(1f))
                // 갱신 중이면 도는 표시, 1분 제한 중이면 흐린 아이콘(누를 수 없음)
                IconButton(onClick = onRefresh, enabled = status.canRefresh) {
                    if (status.refreshing) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp), color = OnAccent, trackColor = OnAccent.copy(alpha = 0.3f), strokeWidth = 2.2.dp,
                        )
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_refresh), contentDescription = "새로고침", tint = OnAccent,
                            modifier = Modifier.size(22.dp).alpha(if (status.canRefresh) 1f else 0.4f),
                        )
                    }
                }
                // 아이콘 버튼은 누르는 영역(48dp)이 아이콘보다 커서, 아이콘이 오른쪽 여백과 줄이 맞도록 민다
                IconButton(onClick = onOpenSettings, modifier = Modifier.offset(x = 12.dp)) {
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = "설정", tint = OnAccent, modifier = Modifier.size(22.dp))
                }
            }
            Gauge(data, Modifier.padding(top = 6.dp))
            if (pill != null) {
                Text(
                    pill, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnAccent, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** 남은 데이터 반원 계기판. 가운데 아래에 "남은 데이터"와 큰 숫자 */
@Composable
private fun Gauge(data: Quota?, modifier: Modifier = Modifier) {
    val ratio = data?.remainingRatio ?: 0f
    Box(modifier.size(width = 264.dp, height = 150.dp), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            // 반지름 112dp 원의 위쪽 절반. 원의 가운데는 아래에서 14dp 위
            val radius = 112.dp.toPx()
            val center = Offset(size.width / 2, size.height - 14.dp.toPx())
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            drawArc(Color.White.copy(alpha = 0.22f), 180f, 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (ratio > 0f) {
                drawArc(Color.White, 180f, 180f * ratio.coerceAtMost(1f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_data), contentDescription = null, tint = OnAccentSub, modifier = Modifier.size(15.dp))
                Text("남은 데이터", fontSize = 14.sp, color = OnAccentSub, modifier = Modifier.padding(start = 4.dp))
            }
            val totalText = data?.let { UsageFormat.dataKb(it.total).replace(".0 ", " ") }
            val (number, unit) = when {
                data == null -> "-" to ""
                // 다 쓴 경우 "0 KB" 대신 전체량과 같은 단위로 "0 GB"
                data.remaining <= 0 -> "0" to splitUnit(totalText.orEmpty()).second
                else -> splitUnit(UsageFormat.dataKb(data.remaining))
            }
            val total = totalText?.let { "/ $it" }
            Text(
                buildAnnotatedString {
                    append(number)
                    withStyle(SpanStyle(fontSize = cappedSp(20), letterSpacing = 0.sp)) { append(" $unit") }
                    if (total != null) {
                        withStyle(SpanStyle(fontSize = cappedSp(15), fontWeight = FontWeight.Medium, letterSpacing = 0.sp, color = Color.White.copy(alpha = 0.7f))) {
                            append(" $total")
                        }
                    }
                },
                fontSize = cappedSp(50), lineHeight = cappedSp(56), fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp,
                color = OnAccent, maxLines = 1,
            )
        }
    }
}

/** "5.7 GB" → ("5.7", "GB") */
private fun splitUnit(text: String): Pair<String, String> {
    val space = text.lastIndexOf(' ')
    return if (space < 0) text to "" else text.substring(0, space) to text.substring(space + 1)
}

/** 갱신 실패·배터리 안내. 위쪽 색 영역 바로 아래에 붙는다 */
@Composable
private fun Banner(title: String, body: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.warnBg)
            .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_error_outline), contentDescription = null, tint = colors.warn, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Text(body, fontSize = 12.sp, color = colors.sub)
        }
        TextButton(onClick = onAction) {
            Text(action, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent)
        }
    }
}

/** 음성·문자: 도넛(남은 비율) 가운데에 아이콘, 오른쪽에 남은 양과 작은 전체량 */
@Composable
private fun QuotaCard(
    label: String,
    icon: Int,
    quota: Quota?,
    format: (Long) -> String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val colors = AldotTheme.colors
    Row(
        modifier.fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(colors.segment).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Donut(quota?.remainingRatio ?: 0f, size = 56.dp, stroke = 6.dp)
            Icon(painterResource(icon), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text("남은 $label", fontSize = 13.sp, color = colors.sub, maxLines = 1)
            if (quota != null) {
                Text(
                    buildAnnotatedString {
                        append(format(quota.remaining))
                        withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)) { append(unit) }
                    },
                    fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, color = colors.text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text("/ ${format(quota.total)}$unit", fontSize = 12.sp, color = colors.muted, maxLines = 1)
            } else {
                Text("정보 없음", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
            }
        }
    }
}

/** 회선 정보: 요금제(서버가 준 이름 그대로), 통신사, 번호, 이용 기간과 날짜 칸 */
@Composable
private fun LineCard(snapshot: UsageSnapshot, period: UsagePeriod?, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, colors.line), RoundedCornerShape(20.dp))
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
    ) {
        InfoRow("요금제", snapshot.planName ?: "-")
        InfoDivider()
        InfoRow("통신사", HomeText.carrier(snapshot))
        InfoDivider()
        InfoRow("번호", snapshot.phoneMasked ?: "-")
        InfoDivider()
        if (period != null) {
            Row(Modifier.padding(top = 12.dp)) {
                Text("이용 기간", fontSize = 13.sp, color = colors.sub, modifier = Modifier.weight(1f))
                Text(HomeText.periodRange(period), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
            }
            // 기간의 날마다 한 칸: 지난 날은 진하게, 오늘은 테마 색, 남은 날은 옅게
            Row(Modifier.padding(top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(period.totalDays) { i ->
                    val day = i + 1
                    val color = when {
                        day < period.dayNumber -> colors.sub
                        day == period.dayNumber -> colors.accent
                        else -> colors.line
                    }
                    Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                }
            }
            Row(Modifier.padding(top = 6.dp)) {
                Text("오늘 ${period.dayNumber}일째", fontSize = 12.sp, color = colors.muted, modifier = Modifier.weight(1f))
                Text(HomeText.daysLeft(period), fontSize = 12.sp, color = colors.muted)
            }
        } else {
            Text("이용 기간 정보 없음", fontSize = 13.sp, color = colors.muted, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = AldotTheme.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = colors.sub, modifier = Modifier.width(64.dp))
        Text(
            value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.text, textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(thickness = 1.dp, color = AldotTheme.colors.divider)
}

/** 맨 아래: 갱신 시각(갱신 중·방금 갱신·실패면 그 표시)과 위젯 추가 */
@Composable
private fun Footer(status: HomeStatus, onAddWidget: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    val statusColor = if (status.failed) colors.warn else colors.muted
    Row(modifier.padding(start = 20.dp, end = 8.dp, bottom = 8.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
        Text(
            status.text, fontSize = 13.sp, color = statusColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 5.dp),
        )
        TextButton(onClick = onAddWidget) {
            Text("홈 화면에 위젯 추가 ›", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.sub)
        }
    }
}

/** 도넛 게이지. 12시 방향에서 시계 방향으로 남은 비율만큼 */
@Composable
private fun Donut(ratio: Float, size: Dp, stroke: Dp) {
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
