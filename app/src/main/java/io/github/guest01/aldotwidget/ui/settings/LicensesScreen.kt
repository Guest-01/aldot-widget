package io.github.guest01.aldotwidget.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.data.LicenseGroup
import io.github.guest01.aldotwidget.data.licenseGroups
import io.github.guest01.aldotwidget.ui.main.MaxContentWidth
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 오픈소스 라이선스 (설정 → 오픈소스 라이선스).
 * 목록은 빌드할 때 자동으로 만들어진 res/raw/aboutlibraries.json에서 읽는다. 라이선스별로 묶고,
 * 묶음 머리를 누르면 원문을 아래에서 올라오는 창으로 보여 준다.
 */
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    val context = LocalContext.current
    // 100개 가까운 목록을 읽고 묶는 일이라 화면을 멈추지 않게 따로 한다
    val groups by produceState<List<LicenseGroup>?>(null) {
        value = withContext(Dispatchers.IO) {
            context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { licenseGroups(it.readText()) }
        }
    }
    var openedLicense by rememberSaveable { mutableStateOf<String?>(null) }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = MaxContentWidth).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            item {
                // 아이콘 버튼은 누르는 영역(48dp)이 아이콘보다 커서, 아이콘이 아래 글자들과 왼쪽 줄이 맞도록 당긴다
                IconButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp).offset(x = (-12).dp)) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "뒤로", tint = colors.text)
                }
                Text(
                    "오픈소스 라이선스", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp,
                    color = colors.text, modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "이 앱은 아래 오픈소스 소프트웨어로 만들었어요. 라이선스를 누르면 전문을 볼 수 있어요.",
                    fontSize = 14.sp, lineHeight = 21.sp, color = colors.sub, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
            val loaded = groups
            if (loaded == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = colors.accent)
                    }
                }
            } else {
                loaded.forEach { group ->
                    item(key = "header-${group.name}") {
                        GroupHeader(group, onOpen = { openedLicense = group.name })
                    }
                    items(group.libraries, key = { "${group.name}-${it.id}" }) { library ->
                        val last = library == group.libraries.last()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(if (last) RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp) else RoundedCornerShape(0.dp))
                                .background(colors.segment)
                                .padding(horizontal = 16.dp),
                        ) {
                            Column(Modifier.padding(vertical = 10.dp)) {
                                Text(library.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                                Text(
                                    listOfNotNull(library.id, library.version).joinToString(" · "),
                                    fontSize = 12.sp, color = colors.muted,
                                )
                            }
                            if (!last) HorizontalDivider(thickness = 1.dp, color = colors.divider)
                        }
                    }
                }
            }
        }
    }

    val opened = groups?.firstOrNull { it.name == openedLicense }
    if (opened != null) LicenseTextSheet(opened, onDismiss = { openedLicense = null })
}

/** 묶음 머리: 라이선스 이름, 몇 개인지, 전문 보기 */
@Composable
private fun GroupHeader(group: LicenseGroup, onOpen: () -> Unit) {
    val colors = AldotTheme.colors
    Row(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(colors.segment)
            .clickable(enabled = group.text != null, onClickLabel = "전문 보기", role = Role.Button, onClick = onOpen)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(group.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Text("라이브러리 ${group.libraries.size}개", fontSize = 12.sp, color = colors.muted)
        }
        if (group.text != null) {
            Text("전문 보기", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
        }
    }
    HorizontalDivider(thickness = 1.dp, color = colors.divider)
}

/**
 * 원문은 80자마다 줄을 끊어 둔 텍스트라 폰 폭에서는 들쭉날쭉하게 접힌다.
 * 빈 줄로 나뉜 문단은 그대로 두고, 문단 안의 줄바꿈과 들여쓰기만 공백 하나로 합친다.
 */
internal fun reflow(text: String): String =
    text.split(Regex("\\n\\s*\\n"))
        .joinToString("\n\n") { it.trim().replace(Regex("\\s+"), " ") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseTextSheet(group: LicenseGroup, onDismiss: () -> Unit) {
    val colors = AldotTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bg,
    ) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(group.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
            Text(
                reflow(group.text.orEmpty()),
                fontSize = 12.sp, lineHeight = 18.sp, color = colors.sub,
                modifier = Modifier.padding(top = 12.dp, bottom = 32.dp).verticalScroll(rememberScrollState()),
            )
        }
    }
}
