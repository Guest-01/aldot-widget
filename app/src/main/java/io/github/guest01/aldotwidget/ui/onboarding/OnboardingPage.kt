package io.github.guest01.aldotwidget.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.ui.main.MaxContentWidth
import io.github.guest01.aldotwidget.ui.theme.AldotTheme

/** 단계 표시 "2 / 4"와 진행 막대. 로그인부터 위젯 고르기까지 이어서 센다 */
data class StepPosition(val number: Int, val total: Int)

/** 제목 일부만 테마 색으로. 예: accentTitle("로그인이 풀리면\n", "알려 드릴까요?") */
@Composable
internal fun accentTitle(before: String, accent: String, after: String = ""): AnnotatedString {
    val color = AldotTheme.colors.accent
    return buildAnnotatedString {
        append(before)
        withStyle(SpanStyle(color = color)) { append(accent) }
        append(after)
    }
}

/**
 * 첫 실행 안내 한 장의 틀 (시안 ③~⑤).
 * 위: 단계 표시와 건너뛰기, 진행 막대, 제목, 설명. 가운데: 화면마다 다른 내용. 아래: 큰 버튼.
 * 가운데 내용은 남는 높이의 가운데에 두고, 화면이 낮으면 그 부분만 스크롤된다.
 */
@Composable
internal fun OnboardingPage(
    position: StepPosition,
    title: AnnotatedString,
    body: String,
    skipLabel: String,
    onSkip: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AldotTheme.colors
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = MaxContentWidth).fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${position.number} / ${position.total}", fontSize = 13.sp, color = colors.muted, modifier = Modifier.weight(1f))
                // 글자 버튼은 누르는 영역이 글자보다 넓어서, 글자가 오른쪽 여백과 줄이 맞도록 민다
                TextButton(onClick = onSkip, modifier = Modifier.offset(x = 12.dp)) {
                    Text(skipLabel, fontSize = 15.sp, color = colors.sub)
                }
            }
            StepProgress(position, Modifier.padding(top = 8.dp))
            Text(
                title, fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp,
                color = colors.text, modifier = Modifier.padding(top = 24.dp),
            )
            Text(body, fontSize = 15.sp, lineHeight = 24.sp, color = colors.sub, modifier = Modifier.padding(top = 12.dp))

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content,
                )
            }

            PrimaryButton(primaryLabel, onPrimary)
            if (footnote != null) {
                Text(
                    footnote, fontSize = 12.sp, color = colors.muted, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
            }
        }
    }
}

/** 진행 막대: 지난 단계와 지금 단계는 테마 색, 남은 단계는 회색 */
@Composable
internal fun StepProgress(position: StepPosition, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(position.total) { i ->
            Box(
                Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i < position.number) colors.accent else colors.line),
            )
        }
    }
}

/** 로그인 화면 위쪽: 뒤로, "1 / 4", 진행 막대 */
@Composable
internal fun LoginStepHeader(position: StepPosition, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "뒤로", tint = colors.text)
            }
            Box(Modifier.weight(1f))
            Text("${position.number} / ${position.total}", fontSize = 13.sp, color = colors.muted)
        }
        StepProgress(position, Modifier.padding(top = 8.dp))
    }
}

@Composable
internal fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
