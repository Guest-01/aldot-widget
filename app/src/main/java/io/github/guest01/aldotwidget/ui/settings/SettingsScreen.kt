package io.github.guest01.aldotwidget.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
 * 설정 화면 (시안 "앱 화면" ⑥ 상태 점검형).
 * 맨 위 점검 카드가 "위젯이 잘 돌아가는지"를 한 장으로 보여 주고, 꺼진 게 있으면 그 줄에서 바로 켠다.
 * 갱신 주기와 테마 색은 목록 줄 오른쪽에서 바로 고른다. 로그아웃은 맨 아래 공식 앱 안내와 함께 푸터로 둔다.
 * (앱은 로그인 아이디를 모르고, 번호는 홈의 회선 정보에 있어서 계정 줄은 두지 않는다)
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
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AldotTheme.colors
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }

    // 오픈소스 라이선스는 설정 안의 한 화면. 폰의 뒤로 가기는 설정으로 돌아온다
    if (showLicenses) {
        BackHandler { showLicenses = false }
        LicensesScreen(onBack = { showLicenses = false }, modifier = modifier)
        return
    }

    // 내용이 짧으면 푸터를 바닥에 붙이고, 길면(큰 글자 등) 전체를 스크롤한다
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
                ScreenHeader("설정", onBack)

                StatusCard(
                    refreshInterval, batteryOn, onFixBattery, notificationsOn, onFixNotifications,
                    Modifier.padding(top = 12.dp),
                )

                // 모든 항목을 "이름 왼쪽, 고르는 것 오른쪽" 한 줄로. 꾸미기와 정보가 같은 모양의 한 목록
                Column(Modifier.padding(top = 20.dp)) {
                    HorizontalDivider(thickness = 1.dp, color = colors.line)
                    SettingLine("자동 갱신 주기") {
                        SegmentedPicker(RefreshInterval.entries, refreshInterval, { it.label }, onRefreshIntervalChange)
                    }
                    HorizontalDivider(thickness = 1.dp, color = colors.divider)
                    SettingLine("테마 색") {
                        // 동그라미(24dp)는 누르는 영역(44dp) 가운데 있어서, 마지막 동그라미가 오른쪽 줄에 맞도록 민다
                        ThemeColorPicker(themeColor, onThemeColorChange, Modifier.offset(x = 10.dp))
                    }
                    HorizontalDivider(thickness = 1.dp, color = colors.divider)
                    SettingLine("오픈소스 라이선스", onClick = { showLicenses = true }) {
                        Icon(
                            painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.muted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    HorizontalDivider(thickness = 1.dp, color = colors.divider)
                    SettingLine("버전") { Text(appVersion(), fontSize = 14.sp, color = colors.sub) }
                }
            }

            Footer(onLogout = { confirmLogout = true }, Modifier.padding(top = 28.dp, bottom = 20.dp))
        }
    }

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

/** 설정과 그 안의 화면(오픈소스 라이선스)이 같이 쓰는 머리: 뒤로 + 제목 한 줄 */
@Composable
internal fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    // 아이콘 버튼은 누르는 영역(48dp)이 아이콘보다 커서, 화살표가 아래 내용과 왼쪽 줄이 맞도록 당긴다
    Row(modifier.padding(top = 8.dp).heightIn(min = 48.dp).offset(x = (-12).dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "뒤로", tint = colors.text)
        }
        Text(
            title, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, color = colors.text,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * 점검 카드. 다 켜져 있으면 홈 위쪽처럼 테마 색 바탕에 흰 글자, 하나라도 꺼지면 옅은 경고 바탕에 경고 색 글자.
 * 꺼진 줄에만 "켜기" 버튼이 붙는다 (경고 카드 안이라 테마 색 대신 경고 색 테두리 버튼).
 */
@Composable
private fun StatusCard(
    interval: RefreshInterval,
    batteryOn: Boolean,
    onFixBattery: () -> Unit,
    notificationsOn: Boolean,
    onFixNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AldotTheme.colors
    val offCount = listOf(batteryOn, notificationsOn).count { !it }
    val ok = offCount == 0
    val look = if (ok) {
        StatusLook(bg = colors.button, ink = Color.White, check = Color.White.copy(alpha = 0.85f), line = Color.White.copy(alpha = 0.18f))
    } else {
        StatusLook(
            bg = colors.warnBg, ink = colors.warn, check = colors.sub,
            line = if (colors.isDark) Color.White.copy(alpha = 0.08f) else colors.warn.copy(alpha = 0.12f),
        )
    }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(look.bg)
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp),
    ) {
        Row(Modifier.semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(if (ok) R.drawable.ic_circle_check else R.drawable.ic_error_outline),
                contentDescription = null, tint = look.ink, modifier = Modifier.size(24.dp),
            )
            Text(
                if (ok) "위젯이 잘 돌아가고 있어요" else "${offCount}가지 확인이 필요해요",
                fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, color = look.ink,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Column(Modifier.padding(top = 8.dp)) {
            StatusLine("${interval.label}마다 자동 갱신", on = true, onFix = null, look)
            StatusLine("밤에도 로그인 유지", batteryOn, onFixBattery, look)
            StatusLine("로그인 만료 알림", notificationsOn, onFixNotifications, look)
        }
    }
}

private data class StatusLook(val bg: Color, val ink: Color, val check: Color, val line: Color)

@Composable
private fun StatusLine(label: String, on: Boolean, onFix: (() -> Unit)?, look: StatusLook) {
    HorizontalDivider(thickness = 1.dp, color = look.line)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            // 화면 읽기: "밤에도 로그인 유지, 꺼짐" 한 번에 읽고, 켜기 버튼은 따로 누를 수 있게 둔다
            .semantics(mergeDescendants = true) { stateDescription = if (on) "켜짐" else "꺼짐" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(if (on) R.drawable.ic_check else R.drawable.ic_x), contentDescription = null,
            tint = if (on) look.check else look.ink, modifier = Modifier.size(16.dp),
        )
        Text(
            label, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = if (on) FontWeight.Normal else FontWeight.Bold, color = look.ink,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
        if (!on && onFix != null) FixButton(onFix)
    }
}

/** 꺼진 줄의 "켜기": 바탕색 버튼에 경고 색 글자와 옅은 경고 색 테두리 (시안 A) */
@Composable
private fun FixButton(onClick: () -> Unit) {
    val colors = AldotTheme.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.bg,
        border = BorderStroke(1.dp, colors.warn.copy(alpha = if (colors.isDark) 0.45f else 0.28f)),
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Box(Modifier.heightIn(min = 34.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text("켜기", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = colors.warn)
        }
    }
}

/**
 * 설정 목록 한 줄: 이름은 왼쪽, 고르는 것(또는 값)은 오른쪽. onClick이 있으면 줄 전체를 누를 수 있다.
 * 큰 글자 등으로 한 줄에 다 안 들어가면 오른쪽 것이 이름 아래로 내려간다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingLine(label: String, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label, fontSize = 15.sp, lineHeight = 20.sp, color = AldotTheme.colors.text,
            modifier = Modifier.padding(end = 12.dp).heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically),
        )
        trailing()
    }
}

/**
 * 작은 분할 버튼 (갱신 주기). 고른 칸만 떠 있는 칸.
 * 보이는 칸은 34dp지만 누르는 영역은 48dp로 넓히고, 바탕(40dp)은 그 가운데에 그린다.
 */
@Composable
private fun <T> SegmentedPicker(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val colors = AldotTheme.colors
    Row(
        Modifier
            .selectableGroup()
            .drawBehind {
                val height = 40.dp.toPx()
                drawRoundRect(
                    colors.segment, topLeft = Offset(0f, (size.height - height) / 2), size = Size(size.width, height),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                )
            }
            .padding(horizontal = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .selectable(selected = on, onClick = { onSelect(option) }, role = Role.RadioButton),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .heightIn(min = 34.dp)
                        .then(if (on) Modifier.shadow(2.dp, shape) else Modifier)
                        .clip(shape)
                        .background(if (on) colors.raised else Color.Transparent)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(option), fontSize = 14.sp, lineHeight = 20.sp, maxLines = 1,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        color = if (on) colors.text else colors.sub,
                    )
                }
            }
        }
    }
}

/** 푸터: 가운데 로그아웃 버튼과 공식 앱이 아니라는 안내 */
@Composable
private fun Footer(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(onClick = onLogout, shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, colors.line)) {
            Row(Modifier.heightIn(min = 44.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_log_out), contentDescription = null, tint = colors.warn, modifier = Modifier.size(17.dp))
                Text(
                    "로그아웃", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = colors.warn,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Text(
            "LG U+ 및 알닷의 공식 앱이 아니에요.",
            fontSize = 12.sp, lineHeight = 18.sp, color = colors.muted, textAlign = TextAlign.Center,
        )
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
            onLogout = {},
            onBack = {},
        )
    }
}
