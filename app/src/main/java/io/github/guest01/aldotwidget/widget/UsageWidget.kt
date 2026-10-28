package io.github.guest01.aldotwidget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.guest01.aldotwidget.AldotApp
import io.github.guest01.aldotwidget.MainActivity
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.SyncStatus
import io.github.guest01.aldotwidget.data.UsageFormat
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.data.UsageState
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 홈 화면 위젯. Compose와 비슷하게 쓰지만, 실제로는 RemoteViews로 바뀌어 런처가 그린다.
 * 그래서 androidx.compose.material3 컴포넌트가 아니라 androidx.glance.* 컴포넌트만 쓸 수 있다.
 */
class UsageWidget : GlanceAppWidget() {

    // 위젯 크기가 바뀌면 아래 크기들 중 들어맞는 가장 큰 크기로 미리 그려 둔 화면을 쓴다
    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, DETAILED))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = (context.applicationContext as AldotApp).container.usageStore
        provideContent {
            val state by store.state.collectAsState(initial = null)
            GlanceTheme {
                UsageWidgetContent(state)
            }
        }
    }

    companion object {
        /** 2×1 정도: 데이터 남은 양만 */
        val COMPACT = DpSize(110.dp, 40.dp)

        /** 3×2 이상: 데이터·음성·문자 */
        val DETAILED = DpSize(180.dp, 110.dp)
    }
}

/** 시스템이 위젯 이벤트(추가·삭제·갱신 요청)를 보내는 수신기. 매니페스트에 등록한다. */
class UsageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()
}

@Composable
private fun UsageWidgetContent(state: UsageState?) {
    val size = LocalSize.current
    val detailed = size.width >= UsageWidget.DETAILED.width && size.height >= UsageWidget.DETAILED.height

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.CenterStart,
    ) {
        val snapshot = state?.snapshot
        when {
            state == null -> Message("불러오는 중…")
            state.status == SyncStatus.NEED_LOGIN -> Message("로그인이 필요해요", "눌러서 앱 열기")
            snapshot == null && state.status == SyncStatus.ERROR -> Message("불러오지 못했어요", state.errorMessage)
            snapshot == null -> Message("알닷 위젯", "눌러서 로그인")
            detailed -> Detailed(snapshot, failed = state.status == SyncStatus.ERROR)
            else -> Compact(snapshot)
        }
    }
}

@Composable
private fun Compact(snapshot: UsageSnapshot) {
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("데이터", style = labelStyle(), modifier = GlanceModifier.defaultWeight())
            Text(fetchedAtText(snapshot), style = captionStyle())
        }
        val data = snapshot.data
        Text(
            text = data?.let { "${UsageFormat.dataKb(it.remaining)} 남음" } ?: "정보 없음",
            style = valueStyle(16),
        )
        if (data != null) {
            Spacer(GlanceModifier.height(4.dp))
            QuotaBar(data)
        }
    }
}

@Composable
private fun Detailed(snapshot: UsageSnapshot, failed: Boolean) {
    // 위젯이 내용보다 크면 남는 공간이 아래에 몰리지 않도록 세로 가운데 정렬
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text(snapshot.partnerName ?: "알닷", style = labelStyle())
                Text(
                    text = fetchedAtText(snapshot) + if (failed) " · 갱신 실패" else "",
                    style = captionStyle(),
                )
            }
            CircleIconButton(
                imageProvider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "새로고침",
                onClick = actionRunCallback<RefreshAction>(),
                backgroundColor = null,
                contentColor = GlanceTheme.colors.onSurfaceVariant,
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        QuotaRow("데이터", snapshot.data, UsageFormat::dataKb)
        Spacer(GlanceModifier.height(6.dp))
        QuotaRow("음성", snapshot.voice, UsageFormat::voiceSeconds)
        Spacer(GlanceModifier.height(6.dp))
        QuotaRow("문자", snapshot.sms, UsageFormat::smsCount)
    }
}

@Composable
private fun QuotaRow(label: String, quota: Quota?, format: (Long) -> String) {
    Column(GlanceModifier.fillMaxWidth()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = labelStyle(), modifier = GlanceModifier.defaultWeight())
            Text(quota?.let { "${format(it.remaining)} 남음" } ?: "정보 없음", style = valueStyle(13))
        }
        if (quota != null) {
            Spacer(GlanceModifier.height(3.dp))
            QuotaBar(quota)
        }
    }
}

@Composable
private fun QuotaBar(quota: Quota) {
    LinearProgressIndicator(
        progress = quota.remainingRatio,
        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
        color = GlanceTheme.colors.primary,
        backgroundColor = GlanceTheme.colors.secondaryContainer,
    )
}

@Composable
private fun Message(title: String, subtitle: String? = null) {
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = valueStyle(14))
        if (subtitle != null) Text(subtitle, style = captionStyle(), maxLines = 2)
    }
}

@Composable
private fun labelStyle() = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)

@Composable
private fun captionStyle() = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)

@Composable
private fun valueStyle(sizeSp: Int) =
    TextStyle(color = GlanceTheme.colors.onSurface, fontSize = sizeSp.sp, fontWeight = FontWeight.Bold)

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateTimeFormat = DateTimeFormatter.ofPattern("M/d HH:mm")

/** 오늘이면 "23:35 기준", 다른 날이면 "10/27 23:35 기준" */
private fun fetchedAtText(snapshot: UsageSnapshot): String {
    val zone = ZoneId.systemDefault()
    val fetched = snapshot.fetchedAt.atZone(zone)
    val format = if (fetched.toLocalDate() == LocalDate.now(zone)) timeFormat else dateTimeFormat
    return "${format.format(fetched)} 기준"
}
