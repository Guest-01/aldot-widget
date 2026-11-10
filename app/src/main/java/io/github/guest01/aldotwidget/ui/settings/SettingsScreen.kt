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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.guest01.aldotwidget.BuildConfig
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.data.RefreshInterval
import io.github.guest01.aldotwidget.data.ThemeColor
import io.github.guest01.aldotwidget.ui.main.MaxContentWidth
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.ui.theme.ThemeColorPicker
import io.github.guest01.aldotwidget.update.CheckState
import io.github.guest01.aldotwidget.update.UpdateManager
import io.github.guest01.aldotwidget.update.UpdateText
import io.github.guest01.aldotwidget.update.UpdateUiState

/**
 * 설정 화면 (시안 "설정 새 후보" ② 앱 머리 + 두 묶음).
 * 맨 위 앱 카드에 이름·버전과 업데이트 확인, 그 아래 '위젯 동작'(갱신 주기, 밤 로그인 유지, 만료 알림)과
 * '꾸미기'(테마 색) 두 묶음, 링크 버튼 두 개(GitHub, 오픈소스 라이선스), 맨 아래 로그아웃.
 * 꺼진 항목은 그 줄의 '켜기'와 묶음 제목 옆 칩으로 알린다.
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
    update: UpdateUiState,
    updates: UpdateManager,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** 새 버전 알림을 눌러 들어왔으면 업데이트 창을 바로 띄운다 */
    openSheetOnStart: Boolean = false,
) {
    val colors = AldotTheme.colors
    val context = LocalContext.current
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    // 앱을 막 띄운 경우 찾아 둔 새 버전을 저장소에서 읽는 데 잠깐 걸려서, 읽히면 띄운다
    val hasAvailable = update.available != null
    LaunchedEffect(openSheetOnStart, hasAvailable) { if (openSheetOnStart && hasAvailable) sheetOpen = true }

    // 설치 허용을 켜러 설정에 갔다가 돌아오면 이어서 받는다
    LifecycleResumeEffect(Unit) {
        updates.continueIfPermitted()
        onPauseOrDispose { }
    }

    if (showLicenses) {
        BackHandler { showLicenses = false }
        LicensesScreen(onBack = { showLicenses = false }, modifier = modifier)
        return
    }

    val offCount = listOf(batteryOn, notificationsOn).count { !it }

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

                AppCard(
                    update,
                    onCheck = updates::checkNow,
                    onOpenUpdate = { sheetOpen = true },
                    modifier = Modifier.padding(top = 12.dp),
                )

                SectionTitle(
                    "위젯 동작", badge = if (offCount > 0) "${offCount}가지 확인 필요" else null,
                    modifier = Modifier.padding(top = 24.dp),
                )
                ActionRow(R.drawable.ic_refresh, "자동 갱신") {
                    SegmentedPicker(RefreshInterval.entries, refreshInterval, { it.label }, onRefreshIntervalChange)
                }
                StatusRow(R.drawable.ic_moon, "밤에도 로그인 유지", batteryOn, onFixBattery)
                StatusRow(R.drawable.ic_bell, "로그인 만료 알림", notificationsOn, onFixNotifications)

                SectionTitle("꾸미기", modifier = Modifier.padding(top = 20.dp))
                ActionRow(R.drawable.ic_palette, "테마 색") {
                    // 동그라미(24dp)는 누르는 영역(44dp) 가운데 있어서, 마지막 동그라미가 오른쪽 줄에 맞도록 민다
                    ThemeColorPicker(themeColor, onThemeColorChange, Modifier.offset(x = 10.dp))
                }

                SectionTitle("도움", modifier = Modifier.padding(top = 20.dp))
                LinkRow(
                    R.drawable.ic_message_alert, "문제 알리기", "GitHub에 앱·기기 정보를 채워 열어요",
                    onClick = { openUrl(context, issueUrl()) },
                )

                // 큰 글자에서 한쪽만 두 줄이 돼도 두 버튼 높이를 맞춘다
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp).height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LinkButton("GitHub에서 보기", R.drawable.ic_external_link, { openUrl(context, REPO_URL) }, Modifier.weight(1f).fillMaxHeight())
                    LinkButton("오픈소스 라이선스", R.drawable.ic_chevron_right, { showLicenses = true }, Modifier.weight(1f).fillMaxHeight())
                }
                Text(
                    "MIT 라이선스로 코드를 모두 공개해요",
                    fontSize = 12.sp, lineHeight = 18.sp, color = colors.muted, modifier = Modifier.padding(top = 8.dp),
                )
            }

            Footer(onLogout = { confirmLogout = true }, Modifier.padding(top = 28.dp, bottom = 20.dp))
        }
    }

    val available = update.available
    if (sheetOpen && available != null) {
        UpdateSheet(
            info = available,
            install = update.install,
            updates = updates,
            onOpenBrowser = { openUrl(context, updates.apkUrl(available)) },
            onDismiss = {
                sheetOpen = false
                updates.resetInstallState()
            },
        )
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
 * 맨 위 앱 카드: 아이콘·이름·버전, 아래 줄에 업데이트 확인 상태와 버튼 하나.
 * 새 버전이 있으면 카드가 옅은 테마 색이 되고 버튼이 '업데이트'(누르면 업데이트 창)로 바뀐다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppCard(update: UpdateUiState, onCheck: () -> Unit, onOpenUpdate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    val available = update.available
    val checking = update.check == CheckState.CHECKING
    val status = when {
        checking -> CardStatus("확인하는 중…", colors.sub)
        available != null -> CardStatus("새 버전 ${UpdateText.subject(available.version)} 나왔어요", colors.accent, bold = true)
        update.check == CheckState.FAILED -> CardStatus("확인하지 못했어요", colors.warn, bold = true)
        // 이번에 직접 확인했으면 결과를, 아니면 언제(자동으로/직접) 확인했는지를 보여 준다
        update.lastCheckedAt > 0 && update.checkedByUser && update.checkedThisSession ->
            CardStatus("최신 버전이에요", colors.accent, bold = true, check = true)
        update.lastCheckedAt > 0 -> CardStatus(checkedText(update), colors.sub)
        else -> CardStatus("아직 확인하지 않았어요", colors.sub)
    }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(if (available != null) colors.stage else colors.segment)
            .padding(18.dp),
    ) {
        Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
            AppIcon(52.dp)
            Column(Modifier.padding(start = 14.dp)) {
                Text(
                    stringResource(R.string.app_name), fontSize = 18.sp, lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold, color = colors.text,
                )
                Text("버전 ${BuildConfig.VERSION_NAME} · 비공식 앱", fontSize = 13.sp, lineHeight = 18.sp, color = colors.sub)
            }
        }
        HorizontalDivider(
            thickness = 1.dp, modifier = Modifier.padding(top = 14.dp),
            color = if (available != null) colors.accent.copy(alpha = 0.15f) else colors.line,
        )
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.padding(end = 10.dp).heightIn(min = 48.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (status.check) {
                    Icon(
                        painterResource(R.drawable.ic_check), contentDescription = null, tint = status.ink,
                        modifier = Modifier.padding(end = 4.dp).size(15.dp),
                    )
                }
                Text(
                    status.text, fontSize = 13.sp, lineHeight = 18.sp, color = status.ink,
                    fontWeight = if (status.bold) FontWeight.Bold else FontWeight.Normal,
                )
            }
            if (available != null && !checking) {
                PillButton("업데이트", filled = true, onClick = onOpenUpdate)
            } else {
                PillButton("업데이트 확인", filled = false, onClick = onCheck, busy = checking)
            }
        }
    }
}

private data class CardStatus(val text: String, val ink: Color, val bold: Boolean = false, val check: Boolean = false)

/** 앱 카드의 작은 버튼. 확인 중에는 글자 대신 도는 표시를 보여 주고(폭은 그대로) 누를 수 없다 */
@Composable
private fun PillButton(label: String, filled: Boolean, onClick: () -> Unit, busy: Boolean = false) {
    val colors = AldotTheme.colors
    Surface(
        onClick = onClick,
        enabled = !busy,
        shape = CircleShape,
        color = when {
            filled -> colors.button
            colors.isDark -> Color.White.copy(alpha = 0.10f)
            else -> colors.bg
        },
        modifier = Modifier.alpha(if (busy) 0.7f else 1f),
    ) {
        Box(Modifier.heightIn(min = 36.dp).padding(horizontal = if (filled) 16.dp else 14.dp), contentAlignment = Alignment.Center) {
            Text(
                label, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                color = if (filled) Color.White else colors.accent,
                modifier = Modifier.alpha(if (busy) 0f else 1f),
            )
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = colors.accent, strokeWidth = 2.5.dp)
        }
    }
}

/** 묶음 제목 (작은 흐린 글자). badge가 있으면 옆에 경고 칩 */
@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier, badge: String? = null) {
    val colors = AldotTheme.colors
    Row(modifier.semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = colors.muted)
        if (badge != null) {
            Box(
                Modifier.padding(start = 8.dp).clip(CircleShape).background(colors.warnBg).padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(badge, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = colors.warn)
            }
        }
    }
}

/**
 * 묶음 안 한 줄: 아이콘 받침 + 이름은 왼쪽, 고르는 것(또는 상태)은 오른쪽.
 * 큰 글자 등으로 한 줄에 다 안 들어가면 오른쪽 것이 이름 아래로 내려간다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionRow(
    icon: Int,
    label: String,
    modifier: Modifier = Modifier,
    warn: Boolean = false,
    trailing: @Composable () -> Unit,
) {
    val colors = AldotTheme.colors
    FlowRow(
        modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.padding(end = 12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (warn) colors.warnBg else colors.segment),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = if (warn) colors.warn else colors.sub, modifier = Modifier.size(17.dp))
            }
            Text(
                label, fontSize = 15.sp, lineHeight = 20.sp, color = colors.text,
                modifier = Modifier.padding(start = 12.dp).wrapContentHeight(Alignment.CenterVertically),
            )
        }
        trailing()
    }
}

/** 켜짐/꺼짐 줄. 켜져 있으면 "✓ 켜짐", 꺼져 있으면 "켜기" 버튼 */
@Composable
private fun StatusRow(icon: Int, label: String, on: Boolean, onFix: () -> Unit) {
    val colors = AldotTheme.colors
    ActionRow(
        icon, label, warn = !on,
        // 화면 읽기: "밤에도 로그인 유지, 꺼짐" 한 번에 읽고, 켜기 버튼은 따로 누를 수 있게 둔다
        modifier = Modifier.semantics(mergeDescendants = true) { stateDescription = if (on) "켜짐" else "꺼짐" },
    ) {
        if (on) {
            Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                Text("켜짐", fontSize = 14.sp, lineHeight = 20.sp, color = colors.sub, modifier = Modifier.padding(start = 4.dp))
            }
        } else {
            FixButton(onFix)
        }
    }
}

/** 꺼진 줄의 "켜기": 바탕색 버튼에 경고 색 글자와 옅은 경고 색 테두리 */
@Composable
private fun FixButton(onClick: () -> Unit) {
    val colors = AldotTheme.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.bg,
        border = BorderStroke(1.dp, colors.warn.copy(alpha = if (colors.isDark) 0.45f else 0.28f)),
    ) {
        Box(Modifier.heightIn(min = 34.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text("켜기", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = colors.warn)
        }
    }
}

/** 묶음 안에서 누르면 다른 곳으로 가는 줄: 아이콘 받침 + 이름과 설명 + 바깥 링크 표시 */
@Composable
private fun LinkRow(icon: Int, label: String, sub: String, onClick: () -> Unit) {
    val colors = AldotTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.segment),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = colors.sub, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(label, fontSize = 15.sp, lineHeight = 20.sp, color = colors.text)
            Text(sub, fontSize = 12.sp, lineHeight = 17.sp, color = colors.muted, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(painterResource(R.drawable.ic_external_link), contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
    }
}

/** GitHub · 오픈소스 라이선스 버튼 (회색 바탕, 나란히 두 개) */
@Composable
private fun LinkButton(label: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = colors.segment, modifier = modifier) {
        Row(
            Modifier.fillMaxSize().heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = colors.text,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false),
            )
            Icon(painterResource(icon), contentDescription = null, tint = colors.muted, modifier = Modifier.padding(start = 4.dp).size(14.dp))
        }
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
