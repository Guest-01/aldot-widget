package io.github.guest01.aldotwidget.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
 * 위: 뒤로와 건너뛰기, 진행 막대와 단계 숫자, 제목, 설명. 가운데: 화면마다 다른 내용. 아래: 큰 버튼.
 * 가운데 내용은 남는 높이의 가운데에 두고, 화면이 낮으면 그 부분만 스크롤된다.
 */
@Composable
internal fun OnboardingPage(
    position: StepPosition,
    title: AnnotatedString,
    body: String,
    skipLabel: String,
    onSkip: () -> Unit,
    /** 이전 단계가 있을 때만. 없으면 그 자리를 비워 둔다 (건너뛰기·막대 위치는 그대로) */
    onBack: (() -> Unit)?,
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
                if (onBack != null) BackButton(onBack)
                Spacer(Modifier.weight(1f))
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

/** 진행 막대와 끝의 "2 / 4": 지난 단계와 지금 단계는 테마 색, 남은 단계는 회색 */
@Composable
internal fun StepProgress(position: StepPosition, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(position.total) { i ->
                Box(
                    Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i < position.number) colors.accent else colors.line),
                )
            }
        }
        Text("${position.number} / ${position.total}", fontSize = 13.sp, color = colors.muted, modifier = Modifier.padding(start = 10.dp))
    }
}

/** 단계 화면 왼쪽 위의 뒤로 */
@Composable
private fun BackButton(onBack: () -> Unit) {
    // 아이콘 버튼은 누르는 영역(48dp)이 아이콘보다 커서, 아이콘이 왼쪽 여백과 줄이 맞도록 당긴다
    IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "뒤로", tint = AldotTheme.colors.text)
    }
}

/** 로그인 화면 위쪽: 다른 단계와 같은 머리 (뒤로, 진행 막대) */
@Composable
internal fun LoginStepHeader(position: StepPosition, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
        }
        StepProgress(position, Modifier.padding(top = 8.dp))
    }
}

/** 그림 속 글자 크기. 읽으라는 글자가 아니라 그림의 일부라서, 폰의 글자 크기 설정과 상관없이 고정한다. */
@Composable
internal fun pictureSp(size: Number): TextUnit = with(LocalDensity.current) { size.toFloat().dp.toSp() }

/**
 * 그림 속 글자는 줄 간격을 글꼴 기본값으로. 앱 글자 스타일의 줄 간격(24sp)을 그대로 쓰면
 * 작은 글자도 한 줄이 24sp를 차지해서 그림 속 위젯 내용이 넘친다.
 */
@Composable
internal fun PictureTextStyle(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified), content = content)
}

/** 위젯 그림 속 고리 (기본은 남은 38%). 가운데 내용은 content로 */
@Composable
internal fun PictureRing(size: Dp, stroke: Dp, ratio: Float = 0.38f, content: @Composable () -> Unit = {}) {
    val colors = AldotTheme.colors
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = stroke.toPx()
            val topLeft = Offset(width / 2, width / 2)
            val arcSize = Size(this.size.width - width, this.size.height - width)
            drawArc(colors.line, 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
            drawArc(colors.accent, -90f, 360f * ratio, false, topLeft, arcSize, style = Stroke(width, cap = StrokeCap.Round))
        }
        content()
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
