package io.github.guest01.aldotwidget.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.guest01.aldotwidget.data.Quota
import io.github.guest01.aldotwidget.data.RefreshInterval
import io.github.guest01.aldotwidget.data.UsageFormat
import io.github.guest01.aldotwidget.data.UsageSnapshot
import io.github.guest01.aldotwidget.data.sampleSnapshot
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val fetchedAtFormat = DateTimeFormatter.ofPattern("M월 d일 HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun UsageScreen(
    snapshot: UsageSnapshot,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onAddWidget: () -> Unit,
    refreshInterval: RefreshInterval,
    onRefreshIntervalChange: (RefreshInterval) -> Unit,
    showBatteryWarning: Boolean,
    onFixBattery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(snapshot)
        if (showBatteryWarning) {
            BatteryWarningBanner(onFix = onFixBattery)
        }
        QuotaCard("데이터", snapshot.data, UsageFormat::dataKb)
        QuotaCard("음성통화", snapshot.voice, UsageFormat::voiceSeconds)
        QuotaCard("문자", snapshot.sms, UsageFormat::smsCount)

        Text(
            text = "마지막 갱신 ${fetchedAtFormat.format(snapshot.fetchedAt)}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (error != null) {
            Text(
                text = "갱신 실패: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRefresh, enabled = !refreshing) {
                Text(if (refreshing) "갱신 중…" else "새로고침")
            }
            TextButton(onClick = onLogout) { Text("로그아웃") }
        }
        TextButton(onClick = onAddWidget) { Text("홈 화면에 위젯 추가") }
        RefreshIntervalSection(refreshInterval, onRefreshIntervalChange)
        Text(
            text = "알닷 실시간 사용량은 참고용이며 전산 반영이 늦을 수 있습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RefreshIntervalSection(selected: RefreshInterval, onSelect: (RefreshInterval) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("자동 갱신 주기", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RefreshInterval.entries.forEach { interval ->
                FilterChip(
                    selected = interval == selected,
                    onClick = { onSelect(interval) },
                    label = { Text(interval.label) },
                )
            }
        }
        Text(
            text = "홈 화면 위젯이 이 주기로 갱신돼요. 휴대전화 상태에 따라 조금 늦어질 수 있어요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 배터리 예외가 없으면 밤에 로그인이 풀린다(실기기 확인: 절전 중 통신이 막혀 연장 실패).
 * 위젯이 제대로 동작하는 데 필요한 설정이라 닫을 수 없게 하고, 설정되면 저절로 사라진다.
 */
@Composable
private fun BatteryWarningBanner(onFix: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "밤에 로그인이 풀릴 수 있어요",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    "배터리 사용을 '제한 없음'으로 허용해 주세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            TextButton(onClick = onFix) { Text("허용하기") }
        }
    }
}

@Composable
private fun Header(snapshot: UsageSnapshot) {
    Column {
        Text(
            text = listOfNotNull(snapshot.partnerName, snapshot.phoneMasked).joinToString(" · "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        snapshot.planName?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        val period = listOfNotNull(snapshot.periodStart, snapshot.periodEnd).joinToString(" ~ ")
        val remain = snapshot.remainDays?.let { "${it}일 남음" }
        Text(
            text = listOf(period, remain).filter { !it.isNullOrBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun QuotaCard(title: String, quota: Quota?, format: (Long) -> String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (quota == null) {
                Text("정보 없음", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            Text(
                text = "${format(quota.remaining)} 남음",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { quota.remainingRatio },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${format(quota.used)} 사용 · 총 ${format(quota.total)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("사용량을 불러오지 못했어요", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
        Button(onClick = onRetry) { Text("다시 시도") }
    }
}

// Android Studio의 Compose Preview에서 기기 없이 화면을 볼 수 있게 하는 예시 데이터
@Preview(showBackground = true)
@Composable
private fun UsageScreenPreview() {
    AldotWidgetTheme {
        UsageScreen(
            snapshot = sampleSnapshot(),
            refreshing = false,
            error = null,
            onRefresh = {},
            onLogout = {},
            onAddWidget = {},
            refreshInterval = RefreshInterval.DEFAULT,
            onRefreshIntervalChange = {},
            showBatteryWarning = true,
            onFixBattery = {},
        )
    }
}
