package io.github.guest01.aldotwidget.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.guest01.aldotwidget.BuildConfig
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.ui.main.MaxContentWidth
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.update.CheckState
import io.github.guest01.aldotwidget.update.UpdateManager
import io.github.guest01.aldotwidget.update.UpdateText
import io.github.guest01.aldotwidget.update.UpdateUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 저장소 주소. 'GitHub에서 보기' */
private const val REPO_URL = "https://github.com/Guest-01/aldot-widget"

/**
 * 앱 정보 (설정 → 앱 정보). 시안 "수동 확인 후보 B".
 * 이름·버전 아래에 업데이트 확인 상태와 버튼 하나, 그 아래 오픈소스 라이선스와 GitHub 링크.
 * 새 버전이 있으면 버튼이 '업데이트'가 되고, 누르면 업데이트 창(UpdateSheet)이 올라온다.
 */
@Composable
fun AboutScreen(
    update: UpdateUiState,
    updates: UpdateManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** 새 버전 알림을 눌러 들어왔으면 업데이트 창을 바로 띄운다 */
    openSheetOnStart: Boolean = false,
) {
    val colors = AldotTheme.colors
    val context = LocalContext.current
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
                ScreenHeader("앱 정보", onBack)

                Column(Modifier.fillMaxWidth().padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(76.dp)
                    Text(
                        context.getString(R.string.app_name), fontSize = 22.sp, lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold, color = colors.text, modifier = Modifier.padding(top = 16.dp),
                    )
                    Text("버전 ${BuildConfig.VERSION_NAME}", fontSize = 14.sp, lineHeight = 20.sp, color = colors.sub)
                }

                UpdateCard(
                    update,
                    onCheck = updates::checkNow,
                    onOpenUpdate = { sheetOpen = true },
                    modifier = Modifier.padding(top = 28.dp),
                )

                Column(Modifier.padding(top = 20.dp)) {
                    HorizontalDivider(thickness = 1.dp, color = colors.line)
                    LinkLine("오픈소스 라이선스", icon = R.drawable.ic_chevron_right, onClick = { showLicenses = true })
                    HorizontalDivider(thickness = 1.dp, color = colors.divider)
                    LinkLine(
                        "GitHub에서 보기", sub = "MIT 라이선스로 코드를 모두 공개해요", icon = R.drawable.ic_external_link,
                        onClick = { openUrl(context, REPO_URL) },
                    )
                }
            }

            Text(
                "LG U+ 및 알닷의 공식 앱이 아니에요.",
                fontSize = 12.sp, lineHeight = 18.sp, color = colors.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 20.dp),
            )
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
}

/** 앱 아이콘 (런처 아이콘과 같은 그림). 아이콘 그림은 108 캔버스의 가운데 72만 보이게 만들어져 있어서 키워서 자른다 */
@Composable
internal fun AppIcon(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.29f)).background(Color(0xFF1B64DA)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.ic_launcher_foreground), contentDescription = null,
            modifier = Modifier.requiredSize(size * 1.5f),
        )
    }
}

/** 확인 상태 + 버튼 하나 */
@Composable
private fun UpdateCard(
    update: UpdateUiState,
    onCheck: () -> Unit,
    onOpenUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AldotTheme.colors
    val available = update.available
    val checking = update.check == CheckState.CHECKING
    val look = when {
        checking -> CardLook("확인하는 중…", "GitHub에서 새 버전을 찾고 있어요", colors.text, spinner = true)
        available != null -> CardLook(
            "새 버전 ${UpdateText.subject(available.version)} 나왔어요",
            listOfNotNull(UpdateText.size(available.size), UpdateText.date(available.released)).joinToString(" · "),
            colors.accent,
        )
        update.check == CheckState.FAILED -> CardLook("확인하지 못했어요", "인터넷 연결을 확인하고 다시 시도해 주세요", colors.warn)
        update.lastCheckedAt > 0 -> CardLook("최신 버전이에요", checkedText(update), colors.accent, check = true)
        else -> CardLook("업데이트 확인", "아직 확인하지 않았어요", colors.text)
    }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.segment)
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.heightIn(min = 28.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (look.spinner) CircularProgressIndicator(Modifier.size(18.dp), color = colors.accent, strokeWidth = 2.5.dp)
            if (look.check) Icon(painterResource(R.drawable.ic_circle_check), contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            Text(
                look.title, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, color = look.ink,
                modifier = Modifier.padding(start = if (look.spinner || look.check) 8.dp else 0.dp),
            )
        }
        Text(look.sub, fontSize = 13.sp, lineHeight = 19.sp, color = colors.muted, textAlign = TextAlign.Center)
        if (available != null && !checking) {
            WideButton("업데이트", primary = true, onClick = onOpenUpdate, modifier = Modifier.padding(top = 14.dp))
        } else {
            WideButton(
                if (update.check == CheckState.FAILED) "다시 확인" else "지금 확인",
                primary = false, enabled = !checking, onClick = onCheck, modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

private data class CardLook(val title: String, val sub: String, val ink: Color, val spinner: Boolean = false, val check: Boolean = false)

/** "오늘 09:14에 자동으로 확인했어요" / "방금 확인했어요" */
private fun checkedText(update: UpdateUiState, now: Long = System.currentTimeMillis()): String {
    if (update.checkedByUser && now - update.lastCheckedAt < 60_000) return "방금 확인했어요"
    val zone = ZoneId.systemDefault()
    val at = Instant.ofEpochMilli(update.lastCheckedAt).atZone(zone)
    val today = LocalDate.now(zone)
    val day = when (at.toLocalDate()) {
        today -> "오늘"
        today.minusDays(1) -> "어제"
        else -> "${at.monthValue}월 ${at.dayOfMonth}일"
    }
    val how = if (update.checkedByUser) "" else "자동으로 "
    return "$day %02d:%02d에 ${how}확인했어요".format(at.hour, at.minute)
}

/** 카드와 업데이트 창의 넓은 버튼. primary: 테마 색 채움, 아니면 옅은 테마 색 바탕 */
@Composable
internal fun WideButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    val colors = AldotTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (primary) colors.button else colors.stage,
        modifier = modifier.fillMaxWidth().heightIn(min = height),
    ) {
        Box(Modifier.heightIn(min = height), contentAlignment = Alignment.Center) {
            Text(
                label, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold,
                color = when {
                    primary -> Color.White
                    enabled -> colors.accent
                    else -> colors.accent.copy(alpha = 0.45f)
                },
            )
        }
    }
}

@Composable
private fun LinkLine(label: String, icon: Int, onClick: () -> Unit, sub: String? = null) {
    val colors = AldotTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (sub == null) 56.dp else 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, fontSize = 15.sp, lineHeight = 20.sp, color = colors.text)
            if (sub != null) Text(sub, fontSize = 13.sp, lineHeight = 18.sp, color = colors.sub, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(painterResource(icon), contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
    }
}

internal fun openUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // 브라우저가 없는 폰은 거의 없다. 있으면 아무 일도 일어나지 않는다
    }
}

/** 설정 목록의 '앱 정보' 줄 오른쪽: 새 버전이 있으면 '업데이트' 칩, 없으면 지금 버전 */
@Composable
internal fun AboutLineTrailing(hasUpdate: Boolean) {
    val colors = AldotTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (hasUpdate) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(colors.stage).heightIn(min = 28.dp).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("업데이트", fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        } else {
            Text(BuildConfig.VERSION_NAME, fontSize = 14.sp, lineHeight = 20.sp, color = colors.sub)
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.muted,
            modifier = Modifier.padding(start = 8.dp).size(16.dp),
        )
    }
}
