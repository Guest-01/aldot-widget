package io.github.guest01.aldotwidget.widget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import io.github.guest01.aldotwidget.AldotApp
import io.github.guest01.aldotwidget.MainActivity
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.RefreshPolicy
import io.github.guest01.aldotwidget.data.SyncStatus
import io.github.guest01.aldotwidget.data.ThemeColor
import io.github.guest01.aldotwidget.data.UsageFormat
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.data.UsageState
import io.github.guest01.aldotwidget.data.sampleSnapshot
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * 홈 화면 위젯. Compose와 비슷하게 쓰지만, 실제로는 RemoteViews로 바뀌어 런처가 그린다.
 * 그래서 androidx.compose.material3 컴포넌트가 아니라 androidx.glance.* 컴포넌트만 쓸 수 있다.
 */
class UsageWidget : GlanceAppWidget() {

    // 런처가 알려 주는 실제 크기로 그린다. 고리 크기와 막대 길이를 위젯 크기에 맞춰 계산하기 때문이다.
    override val sizeMode = SizeMode.Exact

    // 위젯 목록 미리보기는 실제 크기를 모르므로 여러 크기로 미리 그려 둔다.
    // 런처는 미리보기 칸에 들어가는 것 중 가장 큰 것을 고른다 (칸보다 크면 못 고르고 더 작은 모양이 나온다).
    override val previewSizeMode = SizeMode.Responsive(PREVIEW_SIZES)

    /** Android 15+ 위젯 목록에 보일 미리보기. 실제 위젯과 같은 코드로 가짜 데이터를 그린다. */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            UsageWidgetContent(UsageState(snapshot = sampleSnapshot(), status = SyncStatus.OK))
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 런처가 알려 준 실제 크기 (레이아웃 기준값을 정할 때 참고). adb logcat -s AldotWidget
        Log.d(TAG, "sizes=${GlanceAppWidgetManager(context).getAppWidgetSizes(id)}")
        val container = (context.applicationContext as AldotApp).container
        // 처음 그릴 때부터 고른 색이 나오도록 설정을 먼저 읽어 둔다
        val initialSettings = container.settingsStore.current()
        provideContent {
            val state by container.usageStore.state.collectAsState(initial = null)
            val settings by container.settingsStore.settings.collectAsState(initial = initialSettings)
            CompositionLocalProvider(LocalAccent provides accentOf(settings.themeColor)) {
                UsageWidgetContent(state)
            }
        }
    }

    companion object {
        private const val TAG = "AldotWidget"

        // 1칸은 고리만 있어서 한 크기면 되고, 2칸은 막대 길이가 폭을 따르므로 폭을 몇 단계로 나눈다
        private val PREVIEW_SIZES = buildSet {
            add(DpSize(80.dp, 80.dp))
            for (width in listOf(140, 170, 200, 230)) {
                add(DpSize(width.dp, 80.dp))
                add(DpSize(width.dp, 180.dp))
            }
        }
    }
}

/**
 * 시안의 고정 색. 폰이 다크 모드면 night 색을 쓴다 (런처가 알아서 바꿔 끼운다).
 * 강조색은 사용자가 고른 테마 색(LocalAccent)이고, 다크 모드에서는 같은 계열의 밝은 색이다.
 */
private object WidgetColors {
    val card = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1F242C))
    val text = ColorProvider(day = Color(0xFF191F28), night = Color(0xFFF2F4F6))
    val sub = ColorProvider(day = Color(0xFF4E5968), night = Color(0xFFB0B8C1))
    val track = ColorProvider(day = Color(0xFFE5E8EB), night = Color(0xFF3A414B))
    val error = ColorProvider(day = Color(0xFFB42318), night = Color(0xFFFF8A80))
}

/** 사용자가 고른 테마 색(설정). 위젯 전체에 내려보낸다. */
private val LocalAccent = staticCompositionLocalOf { accentOf(ThemeColor.DEFAULT) }

private fun accentOf(color: ThemeColor) = ColorProvider(day = Color(color.day), night = Color(color.night))

/** 아래쪽 상태 줄에 필요한 값 */
private class WidgetStatus(val failed: Boolean, val refreshing: Boolean, val long: String, val short: String)

@Composable
private fun UsageWidgetContent(state: UsageState?) {
    val size = LocalSize.current
    val layout = WidgetLayout.of(size)
    val tiny = layout == WidgetLayout.TINY

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.card)
            .launcherCorner()
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        val snapshot = state?.snapshot
        when {
            // 위젯 글자는 줄바꿈 규칙을 정할 수 없어서 1×1에서는 한 줄에 들어가는 짧은 문구를 쓴다
            state == null -> Message(tiny, if (tiny) "불러오는 중" else "불러오는 중…")
            state.status == SyncStatus.NEED_LOGIN ->
                if (tiny) Message(true, "로그인 필요", "눌러서 열기") else Message(false, "로그인이 필요해요", "눌러서 앱 열기")
            snapshot == null && state.status == SyncStatus.ERROR ->
                if (tiny) Message(true, "갱신 실패", "눌러서 열기") else Message(false, "불러오지 못했어요", "눌러서 앱 열기")
            snapshot == null ->
                if (tiny) Message(true, "알닷 위젯", "눌러서 시작") else Message(false, "알닷 위젯", "눌러서 로그인")
            else -> {
                val status = statusOf(snapshot, state)
                when (layout) {
                    WidgetLayout.TINY -> Tiny(snapshot, status, size)
                    WidgetLayout.COMPACT -> Compact(snapshot, status, size)
                    WidgetLayout.SQUARE -> Square(snapshot, status, size)
                    WidgetLayout.WIDE -> Wide(snapshot, status, size)
                }
                // 1×1은 아래 시각 줄이 새로고침 버튼이다
                if (!tiny) CornerRefresh(status.refreshing)
            }
        }
    }
}

private fun statusOf(snapshot: UsageSnapshot, state: UsageState): WidgetStatus {
    val now = Instant.now()
    val zone = ZoneId.systemDefault()
    val failed = state.status == SyncStatus.ERROR
    val refreshing = RefreshPolicy.isRefreshing(state.refreshingSince, now.toEpochMilli())
    return WidgetStatus(
        failed = failed,
        refreshing = refreshing,
        long = WidgetText.long(snapshot.fetchedAt, failed, refreshing, now, zone),
        short = WidgetText.short(snapshot.fetchedAt, failed, refreshing, now, zone),
    )
}

/** 1×1: 가운데 고리 안에 남은 용량, 아래 시각 줄을 누르면 새로고침 */
@Composable
private fun Tiny(snapshot: UsageSnapshot, status: WidgetStatus, size: DpSize) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val data = snapshot.data
        // 1칸 크기는 기기마다 크게 다르다 (Pixel ≈82dp, Fold8 커버 ≈113dp). 남는 높이에 맞춰 고리를 키우고,
        // 작은 고리에서는 글자와 선을 줄여 숫자가 고리에 겹치지 않게 한다.
        val ringSize = minOf(60.dp, size.height - 12.dp - TINY_STATUS_HEIGHT, size.width - 16.dp)
        val small = ringSize < 56.dp
        Ring(ratio = data?.remainingRatio ?: 0f, size = ringSize, stroke = if (small) 5.dp else 6.dp) {
            val (number, unit) = WidgetText.splitUnit(data?.let { UsageFormat.dataKb(it.remaining) } ?: "-")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(number, style = valueStyle(if (small) 12 else 14), maxLines = 1)
                if (unit.isNotEmpty()) Text(unit, style = subStyle(if (small) 8 else 9), maxLines = 1)
            }
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(TINY_STATUS_HEIGHT)
                .clickable(actionRunCallback<RefreshAction>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIcon(status, 11.dp)
            Spacer(GlanceModifier.width(3.dp))
            Text(status.short, style = statusStyle(status), maxLines = 1)
        }
    }
}

private val TINY_STATUS_HEIGHT = 20.dp

/** 2×1: 왼쪽 고리(남은 %), 오른쪽 남은 용량과 상태 */
@Composable
private fun Compact(snapshot: UsageSnapshot, status: WidgetStatus, size: DpSize) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(CONTENT_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val data = snapshot.data
        val ratio = data?.remainingRatio ?: 0f
        Ring(ratio = ratio, size = minOf(64.dp, size.height - CONTENT_PADDING * 2), stroke = 7.dp) {
            Text("${(ratio * 100).roundToInt()}%", style = valueStyle(13))
        }
        Spacer(GlanceModifier.width(12.dp))
        Column {
            Text("데이터 남음", style = subStyle(11))
            Text(data?.let { UsageFormat.dataKb(it.remaining) } ?: "정보 없음", style = valueStyle(20), maxLines = 1)
            StatusLine(status)
        }
    }
}

/** 2×2: 데이터 크게 + 막대, 음성·문자 한 줄씩, 맨 아래 상태 */
@Composable
private fun Square(snapshot: UsageSnapshot, status: WidgetStatus, size: DpSize) {
    Column(GlanceModifier.fillMaxSize().padding(CONTENT_PADDING)) {
        // 첫 줄은 오른쪽 위 새로고침 아이콘과 높이를 맞춘다
        Box(GlanceModifier.height(20.dp), contentAlignment = Alignment.CenterStart) {
            Text(snapshot.partnerName ?: "알닷", style = subStyle(11), maxLines = 1)
        }
        // 가장 낮은 2×2(180dp)에서도 맨 아래 상태 줄이 잘리지 않는 간격이다
        Spacer(GlanceModifier.height(6.dp))
        Text("데이터 남음", style = subStyle(11))
        val data = snapshot.data
        Text(data?.let { UsageFormat.dataKb(it.remaining) } ?: "정보 없음", style = valueStyle(24), maxLines = 1)
        if (data != null) {
            Spacer(GlanceModifier.height(6.dp))
            Bar(data.remainingRatio, width = size.width - CONTENT_PADDING * 2)
        }
        Spacer(GlanceModifier.height(10.dp))
        SmallQuotaLine("음성", snapshot.voice, UsageFormat::voiceSeconds)
        Spacer(GlanceModifier.height(4.dp))
        SmallQuotaLine("문자", snapshot.sms, UsageFormat::smsCount)
        Spacer(GlanceModifier.defaultWeight())
        StatusLine(status)
    }
}

/** 2×2를 넓혔을 때: 위에 이름과 상태, 아래에 막대 세 줄 */
@Composable
private fun Wide(snapshot: UsageSnapshot, status: WidgetStatus, size: DpSize) {
    Column(GlanceModifier.fillMaxSize().padding(CONTENT_PADDING)) {
        Row(GlanceModifier.height(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(snapshot.partnerName ?: "알닷", style = subStyle(12), maxLines = 1)
            Spacer(GlanceModifier.width(8.dp))
            StatusLine(status)
        }
        // 남는 높이는 가운데에 두어 막대들이 아래 여백(16dp)에 붙게 한다
        Spacer(GlanceModifier.defaultWeight())
        val barWidth = size.width - CONTENT_PADDING * 2
        QuotaRow("데이터", snapshot.data, UsageFormat::dataKb, barWidth)
        Spacer(GlanceModifier.height(14.dp))
        QuotaRow("음성", snapshot.voice, UsageFormat::voiceSeconds, barWidth)
        Spacer(GlanceModifier.height(14.dp))
        QuotaRow("문자", snapshot.sms, UsageFormat::smsCount, barWidth)
    }
}

private val CONTENT_PADDING = 16.dp

@Composable
private fun SmallQuotaLine(label: String, quota: Quota?, format: (Long) -> String) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = subStyle(12), modifier = GlanceModifier.defaultWeight())
        Text(quota?.let { format(it.remaining) } ?: "-", style = valueStyle(12), maxLines = 1)
    }
}

@Composable
private fun QuotaRow(label: String, quota: Quota?, format: (Long) -> String, barWidth: Dp) {
    Column(GlanceModifier.fillMaxWidth()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = subStyle(13), modifier = GlanceModifier.defaultWeight())
            Text(quota?.let { "${format(it.remaining)} 남음" } ?: "정보 없음", style = valueStyle(13), maxLines = 1)
        }
        if (quota != null) {
            Spacer(GlanceModifier.height(5.dp))
            Bar(quota.remainingRatio, barWidth)
        }
    }
}

/** 상태 줄: 실패면 경고 아이콘 + 빨간 글자, 아니면 회색 글자 */
@Composable
private fun StatusLine(status: WidgetStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (status.failed && !status.refreshing) {
            WarningIcon(11.dp)
            Spacer(GlanceModifier.width(3.dp))
        }
        Text(status.long, style = statusStyle(status), maxLines = 1)
    }
}

/** 1×1 시각 줄 앞의 아이콘: 갱신 중이면 도는 표시, 실패면 경고, 아니면 새로고침 */
@Composable
private fun StatusIcon(status: WidgetStatus, size: Dp) {
    when {
        status.refreshing -> CircularProgressIndicator(GlanceModifier.size(size), color = LocalAccent.current)
        status.failed -> WarningIcon(size)
        else -> Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = "새로고침",
            modifier = GlanceModifier.size(size),
            colorFilter = ColorFilter.tint(WidgetColors.sub),
        )
    }
}

@Composable
private fun WarningIcon(size: Dp) {
    Image(
        provider = ImageProvider(R.drawable.ic_warning),
        contentDescription = "갱신 실패",
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(WidgetColors.error),
    )
}

/** 2×1 이상: 오른쪽 위 새로고침. 아이콘 가장자리가 위·오른쪽 16dp에 오도록 40dp 버튼을 5dp 안쪽에 둔다. */
@Composable
private fun CornerRefresh(refreshing: Boolean) {
    Box(GlanceModifier.fillMaxSize().padding(5.dp), contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = GlanceModifier.size(40.dp).cornerRadius(20.dp).clickable(actionRunCallback<RefreshAction>()),
            contentAlignment = Alignment.Center,
        ) {
            if (refreshing) {
                CircularProgressIndicator(GlanceModifier.size(18.dp), color = LocalAccent.current)
            } else {
                Image(
                    provider = ImageProvider(R.drawable.ic_refresh),
                    contentDescription = "새로고침",
                    modifier = GlanceModifier.size(18.dp),
                    colorFilter = ColorFilter.tint(WidgetColors.sub),
                )
            }
        }
    }
}

/** 원형 게이지. 바탕 고리와 남은 비율 고리를 겹치고 가운데에 글자를 둔다. */
@Composable
private fun Ring(ratio: Float, size: Dp, stroke: Dp, content: @Composable () -> Unit) {
    val density = LocalContext.current.resources.displayMetrics.density
    val sizePx = (size.value * density).roundToInt().coerceAtLeast(1)
    val strokePx = stroke.value * density
    Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(RingBitmaps.track(sizePx, strokePx)),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(WidgetColors.track),
        )
        Image(
            provider = ImageProvider(RingBitmaps.arc(sizePx, strokePx, ratio)),
            contentDescription = "남은 비율 ${(ratio * 100).roundToInt()}%",
            modifier = GlanceModifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(LocalAccent.current),
        )
        content()
    }
}

/** 둥근 막대. 위젯 크기를 알고 있어서(SizeMode.Exact) 채운 길이를 dp로 계산한다. */
@Composable
private fun Bar(ratio: Float, width: Dp, height: Dp = 6.dp) {
    Box(GlanceModifier.width(width).height(height).background(WidgetColors.track).cornerRadius(height / 2)) {
        val filled = width * ratio.coerceIn(0f, 1f)
        if (filled > 0.dp) {
            // 아주 짧으면 둥근 끝이 찌그러지지 않게 최소 길이를 막대 높이로 둔다
            Box(
                GlanceModifier.width(maxOf(filled, height)).height(height)
                    .background(LocalAccent.current).cornerRadius(height / 2),
            ) {}
        }
    }
}

@Composable
private fun Message(tiny: Boolean, title: String, subtitle: String? = null) {
    val align = if (tiny) TextAlign.Center else TextAlign.Start
    // 1칸은 좁아서(Pixel ≈82dp) 여백을 줄이고 글자를 한 줄로 고정한다. 넘치면 글자 중간에서 줄이 바뀐다.
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = if (tiny) 8.dp else CONTENT_PADDING, vertical = CONTENT_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (tiny) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(title, style = valueStyle(if (tiny) 12 else 15).copy(textAlign = align), maxLines = if (tiny) 1 else 2)
        if (subtitle != null) {
            Spacer(GlanceModifier.height(4.dp))
            Text(subtitle, style = subStyle(if (tiny) 10 else 11).copy(textAlign = align), maxLines = if (tiny) 1 else 2)
        }
    }
}

/** 런처의 위젯 모서리와 같은 둥글기 (Android 12+). 그 아래 버전은 모서리를 깎을 수 없다. */
private fun GlanceModifier.launcherCorner(): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        this
    }

private fun valueStyle(sizeSp: Int) =
    TextStyle(color = WidgetColors.text, fontSize = sizeSp.sp, fontWeight = FontWeight.Bold)

private fun subStyle(sizeSp: Int) = TextStyle(color = WidgetColors.sub, fontSize = sizeSp.sp)

private fun statusStyle(status: WidgetStatus) =
    TextStyle(color = if (status.failed && !status.refreshing) WidgetColors.error else WidgetColors.sub, fontSize = 10.sp)
