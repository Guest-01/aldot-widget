package io.github.guest01.aldotwidget.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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

/**
 * 설정 화면 (시안 "앱 화면" ⑥ 카드형). 위에 계정 카드, 위젯 꾸미기 카드, 동작 상태 카드(켜짐/꺼짐 칩), 정보.
 * 갱신 주기와 테마 색은 줄을 누르면 아래에서 올라오는 시트에서 고른다.
 */
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
    partnerName: String?,
    onAddWidget: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AldotTheme.colors
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }

    // 내용이 짧으면 맨 아래 안내를 바닥에 붙이고, 길면(큰 글자 등) 전체를 스크롤한다
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                // 아이콘 버튼은 누르는 영역(48dp)이 아이콘보다 커서, 아이콘이 아래 글자들과 왼쪽 줄이 맞도록 당긴다
                IconButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp).offset(x = (-12).dp)) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "뒤로", tint = colors.text)
                }
                Text(
                    "설정", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp, color = colors.text,
                    modifier = Modifier.padding(top = 4.dp),
                )

                AccountCard(phoneMasked, partnerName, onLogout = { confirmLogout = true }, Modifier.padding(top = 18.dp))

                Card(Modifier.padding(top = 12.dp)) {
                    IconLine(R.drawable.ic_clock, "자동 갱신 주기", onClick = { sheet = SettingsSheet.INTERVAL }) {
                        Text(refreshInterval.label, fontSize = 14.sp, color = colors.sub)
                        Chevron()
                    }
                    IconLine(R.drawable.ic_palette, "테마 색", onClick = { sheet = SettingsSheet.COLOR }) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(colors.accent))
                        Text(themeColor.label, fontSize = 14.sp, color = colors.sub, modifier = Modifier.padding(start = 8.dp))
                        Chevron()
                    }
                    IconLine(R.drawable.ic_widgets, "홈 화면에 위젯 추가", onClick = onAddWidget) {
                        Chevron()
                    }
                }

                Card(Modifier.padding(top = 12.dp)) {
                    IconLine(R.drawable.ic_moon, "밤에도 로그인 유지") { StatusChip(batteryOn, onFixBattery) }
                    IconLine(R.drawable.ic_bell, "로그인 만료 알림") { StatusChip(notificationsOn, onFixNotifications) }
                }

                Column(Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)) {
                    PlainLine("오픈소스 라이선스", onClick = { showLicenses = true }) { Chevron() }
                    PlainLine("버전") { Text(appVersion(), fontSize = 14.sp, color = colors.muted) }
                }
            }
            Text(
                "LG U+ 및 알닷의 공식 앱이 아니에요.",
                fontSize = 12.sp, color = colors.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 24.dp),
            )
        }
    }

    when (sheet) {
        SettingsSheet.INTERVAL -> IntervalSheet(refreshInterval, onSelect = { onRefreshIntervalChange(it); sheet = null }, onDismiss = { sheet = null })
        SettingsSheet.COLOR -> ColorSheet(themeColor, onThemeColorChange, onDismiss = { sheet = null })
        null -> Unit
    }

    if (showLicenses) LicensesDialog(onDismiss = { showLicenses = false })

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = colors.bg,
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

private enum class SettingsSheet { INTERVAL, COLOR }

/** 맨 위 계정 카드: 번호, 파트너사, 로그아웃 */
@Composable
private fun AccountCard(phoneMasked: String?, partnerName: String?, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.segment).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(colors.stage), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_user), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(phoneMasked ?: "알닷 계정", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Text(listOfNotNull("알닷 계정", partnerName).joinToString(" · "), fontSize = 13.sp, color = colors.sub)
        }
        Surface(
            onClick = onLogout,
            shape = CircleShape,
            color = colors.bg,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier.padding(start = 8.dp).heightIn(min = 36.dp),
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text("로그아웃", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.sub)
            }
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AldotTheme.colors.segment).padding(vertical = 4.dp),
        content = content,
    )
}

/** 카드 안 한 줄: 옅은 테마 색 칸에 아이콘, 이름, 오른쪽 값. onClick이 있으면 줄 전체를 누를 수 있다 */
@Composable
private fun IconLine(icon: Int, label: String, onClick: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit) {
    val colors = AldotTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.stage), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        }
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.text, modifier = Modifier.weight(1f).padding(start = 12.dp))
        trailing()
    }
}

/** 카드 밖의 가벼운 줄 (라이선스, 버전) */
@Composable
private fun PlainLine(label: String, onClick: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, color = AldotTheme.colors.sub, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun Chevron() {
    Icon(
        painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = AldotTheme.colors.muted,
        modifier = Modifier.padding(start = 4.dp).size(16.dp),
    )
}

/** 켜짐(테마 색) / 꺼짐 · 켜기(경고 색, 누르면 켜러 간다) */
@Composable
private fun StatusChip(on: Boolean, onFix: () -> Unit) {
    val colors = AldotTheme.colors
    val ink = if (on) colors.accent else colors.warn
    val background = if (on) colors.stage else colors.warnBg
    Row(
        Modifier
            // 꺼져 있을 때만 누를 수 있는 버튼. 칩은 30dp지만 누르는 영역은 48dp로 넓힌다
            .then(if (on) Modifier else Modifier.minimumInteractiveComponentSize())
            .clip(CircleShape)
            .background(background)
            .then(if (on) Modifier else Modifier.clickable(onClickLabel = "켜기", role = Role.Button, onClick = onFix))
            .heightIn(min = 30.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(ink))
        Text(if (on) "켜짐" else "꺼짐 · 켜기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ink, modifier = Modifier.padding(start = 5.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalSheet(selected: RefreshInterval, onSelect: (RefreshInterval) -> Unit, onDismiss: () -> Unit) {
    val colors = AldotTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.bg) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            SheetTitle("자동 갱신 주기")
            Column(Modifier.padding(top = 8.dp).selectableGroup()) {
                RefreshInterval.entries.forEach { interval ->
                    val on = interval == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(selected = on, onClick = { onSelect(interval) }, role = Role.RadioButton),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${interval.label}마다", fontSize = 16.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            color = colors.text, modifier = Modifier.weight(1f),
                        )
                        if (on) Icon(painterResource(R.drawable.ic_check), contentDescription = "선택됨", tint = colors.accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
            SheetNote("위젯이 이 주기로 갱신돼요. 폰 상태에 따라 조금 늦어질 수 있어요.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorSheet(selected: ThemeColor, onSelect: (ThemeColor) -> Unit, onDismiss: () -> Unit) {
    val colors = AldotTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.bg) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            SheetTitle("테마 색")
            // 고르는 즉시 앱과 위젯이 그 색으로 바뀌어서, 바뀐 모습을 보며 고를 수 있게 시트는 열어 둔다
            Row(Modifier.padding(top = 12.dp).offset(x = (-8).dp), verticalAlignment = Alignment.CenterVertically) {
                ThemeColorPicker(selected, onSelect, Modifier.weight(1f))
                Text(selected.label, fontSize = 14.sp, color = colors.sub)
            }
            SheetNote("앱과 홈 화면 위젯에 함께 적용돼요.")
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AldotTheme.colors.text)
}

@Composable
private fun SheetNote(text: String) {
    Text(text, fontSize = 13.sp, lineHeight = 20.sp, color = AldotTheme.colors.muted, modifier = Modifier.padding(top = 12.dp))
}

/** 앱에 넣은 오픈소스 중 표시 의무가 있는 것. 아이콘은 Lucide(ISC)를 쓴다 */
@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    val colors = AldotTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        title = { Text("오픈소스 라이선스") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Lucide 아이콘", fontWeight = FontWeight.Bold, color = colors.text)
                Text(LUCIDE_LICENSE, fontSize = 12.sp, lineHeight = 18.sp, color = colors.sub, modifier = Modifier.padding(top = 6.dp))
                Text("Android Jetpack, OkHttp, Kotlin", fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.padding(top = 16.dp))
                Text("Apache License 2.0", fontSize = 12.sp, color = colors.sub, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기", color = colors.accent) } },
    )
}

private const val LUCIDE_LICENSE = """ISC License

Copyright (c) for portions of Lucide are held by Cole Bemis 2013-2022 as part of Feather (MIT). All other copyright (c) for Lucide are held by Lucide Contributors 2022.

Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE."""

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
            phoneMasked = "010-12**-34**",
            partnerName = "알뜰폰",
            onAddWidget = {},
            onLogout = {},
            onBack = {},
        )
    }
}
