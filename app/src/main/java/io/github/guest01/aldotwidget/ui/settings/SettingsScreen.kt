package io.github.guest01.aldotwidget.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.data.RefreshInterval
import io.github.guest01.aldotwidget.data.ThemeColor
import io.github.guest01.aldotwidget.ui.main.MaxContentWidth
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme
import io.github.guest01.aldotwidget.ui.theme.ThemeColorPicker

/** 설정 화면 (시안 "앱 화면" ⑥). 자주 쓰지 않는 것들을 홈 화면에서 뺐다. */
@Composable
fun SettingsScreen(
    refreshInterval: RefreshInterval,
    onRefreshIntervalChange: (RefreshInterval) -> Unit,
    themeColor: ThemeColor,
    onThemeColorChange: (ThemeColor) -> Unit,
    batteryOn: Boolean,
    onFixBattery: () -> Unit,
    notificationsOn: Boolean,
    onFixNotifications: () -> Unit,
    phoneMasked: String?,
    onAddWidget: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AldotTheme.colors
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                // 아이콘 버튼은 누르는 영역(48dp)이 아이콘보다 커서, 아이콘이 아래 글자들과 왼쪽 줄이 맞도록 당긴다
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "뒤로", tint = colors.text)
                }
                Text(
                    "설정", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colors.text,
                    modifier = Modifier.offset(x = (-8).dp),
                )
            }

            SectionLabel("자동 갱신 주기")
            IntervalSelector(refreshInterval, onRefreshIntervalChange)
            Text(
                "위젯이 이 주기로 갱신돼요. 폰 상태에 따라 조금 늦어질 수 있어요.",
                fontSize = 12.sp, lineHeight = 18.sp, color = colors.muted, modifier = Modifier.padding(top = 8.dp),
            )

            SectionLabel("테마 색")
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThemeColorPicker(themeColor, onThemeColorChange, Modifier.weight(1f))
                    Text(themeColor.label, fontSize = 14.sp, color = colors.sub)
                }
            }
            Text(
                "앱과 홈 화면 위젯에 함께 적용돼요.",
                fontSize = 12.sp, lineHeight = 18.sp, color = colors.muted, modifier = Modifier.padding(top = 8.dp),
            )

            SectionLabel("동작 상태")
            Card {
                StatusLine("밤에도 로그인 유지", on = batteryOn, onFix = onFixBattery)
                Divider()
                StatusLine("로그인 만료 알림", on = notificationsOn, onFix = onFixNotifications)
                Divider()
                LinkLine("홈 화면 위젯 추가", onAddWidget)
            }

            SectionLabel("계정")
            Card {
                Line {
                    Text("알닷 계정", fontSize = 15.sp, color = colors.text, modifier = Modifier.weight(1f))
                    Text(phoneMasked ?: "-", fontSize = 14.sp, color = colors.sub)
                }
                Divider()
                Line(onClick = { confirmLogout = true }) {
                    Text("로그아웃", fontSize = 15.sp, color = colors.warn)
                }
            }

            SectionLabel("정보")
            Card {
                Line {
                    Text("버전", fontSize = 15.sp, color = colors.text, modifier = Modifier.weight(1f))
                    Text(appVersion(), fontSize = 14.sp, color = colors.sub)
                }
            }

            Text(
                "LG U+ 및 알닷의 공식 앱이 아니에요.",
                fontSize = 12.sp, color = colors.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 24.dp),
            )
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("로그아웃할까요?") },
            text = { Text("이 폰에 저장된 로그인과 사용량을 지워요. 위젯에는 '로그인이 필요해요'가 보여요.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) {
                    Text("로그아웃", color = colors.warn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("취소", color = colors.sub) }
            },
        )
    }
}

/** 30분 / 1시간 중 하나를 고르는 분할 버튼 */
@Composable
private fun IntervalSelector(selected: RefreshInterval, onSelect: (RefreshInterval) -> Unit) {
    val colors = AldotTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.segment)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RefreshInterval.entries.forEach { interval ->
            val on = interval == selected
            Surface(
                color = if (on) colors.raised else colors.segment,
                shape = RoundedCornerShape(10.dp),
                shadowElevation = if (on) 1.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .selectable(selected = on, onClick = { onSelect(interval) }, role = Role.RadioButton),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        interval.label, fontSize = 15.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) colors.text else colors.sub,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AldotTheme.colors.muted,
        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
    )
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AldotTheme.colors.panel),
        content = content,
    )
}

/** 카드 안 한 줄 (높이 56dp). onClick이 있으면 줄 전체를 누를 수 있다. */
@Composable
private fun Line(onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = AldotTheme.colors.divider)
}

/** 켜짐 / 꺼짐(경고 + "켜기") */
@Composable
private fun StatusLine(label: String, on: Boolean, onFix: () -> Unit) {
    val colors = AldotTheme.colors
    Line {
        Text(label, fontSize = 15.sp, color = colors.text, modifier = Modifier.weight(1f))
        if (on) {
            Text("켜짐", fontSize = 14.sp, color = colors.sub)
        } else {
            Icon(painterResource(R.drawable.ic_error_outline), contentDescription = null, tint = colors.warn, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("꺼짐", fontSize = 14.sp, color = colors.warn)
            // 글자 버튼의 최소 폭(58dp)을 없애고(widthIn) 안쪽 여백(12dp)만큼 밀어서, 글자 크기와 상관없이 글자 끝을 다른 줄의 값과 맞춘다
            TextButton(onClick = onFix, modifier = Modifier.offset(x = 12.dp).widthIn(min = 1.dp)) {
                Text("켜기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        }
    }
}

@Composable
private fun LinkLine(label: String, onClick: () -> Unit) {
    val colors = AldotTheme.colors
    Line(onClick = onClick) {
        Text(label, fontSize = 15.sp, color = colors.text, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() {
    AldotWidgetTheme {
        SettingsScreen(
            refreshInterval = RefreshInterval.DEFAULT,
            onRefreshIntervalChange = {},
            themeColor = ThemeColor.DEFAULT,
            onThemeColorChange = {},
            batteryOn = false,
            onFixBattery = {},
            notificationsOn = true,
            onFixNotifications = {},
            phoneMasked = "010-****-****",
            onAddWidget = {},
            onLogout = {},
            onBack = {},
        )
    }
}
