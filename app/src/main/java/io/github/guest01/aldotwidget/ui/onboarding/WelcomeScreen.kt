package io.github.guest01.aldotwidget.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.ui.main.MaxContentWidth
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.ui.theme.AldotWidgetTheme

/**
 * 처음 설치했을 때 로그인 전에 보이는 화면 (시안 ①).
 * 무엇을 하는 앱인지와 비공식 앱이라는 점을 먼저 알리고 로그인으로 보낸다.
 */
@Composable
fun WelcomeScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AldotTheme.colors
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = MaxContentWidth).fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 24.dp)) {
            Text(
                "비공식 앱", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.sub,
                modifier = Modifier.clip(CircleShape).background(colors.segment).padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Text(
                accentTitle("통신사 앱 없이\n홈 화면에서 ", "남은 데이터", " 확인"),
                fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp,
                color = colors.text, modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "알닷 계정으로 로그인하면 데이터·통화·문자 남은 양을 홈 화면 위젯으로 보여 드려요.",
                fontSize = 15.sp, lineHeight = 24.sp, color = colors.sub, modifier = Modifier.padding(top = 12.dp),
            )
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                WidgetIllustration()
            }
            PrimaryButton("알닷 계정으로 시작하기", onStart)
            Text(
                "LG U+ 및 알닷의 공식 앱이 아니에요.\n알닷 사이트에 공개된 내 사용량을 대신 조회해요.",
                fontSize = 12.sp, lineHeight = 18.sp, color = colors.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
        }
    }
}

/** 홈 화면에 놓인 위젯을 흉내 낸 그림. 실제 위젯과 같은 색을 쓴다. */
@Composable
private fun WidgetIllustration() {
    val colors = AldotTheme.colors
    // 높이는 내용에 맞춘다 (고정하면 글자 크기 설정이 큰 폰에서 카드가 잘린다)
    Column(
        Modifier.width(300.dp).clip(RoundedCornerShape(32.dp)).background(colors.stage).padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
                .background(colors.raised).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("알뜰폰 · 20:08 기준", fontSize = 12.sp, color = colors.muted, modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
            }
            IllustrationBar("데이터", "5.7 GB 남음", 0.38f)
            IllustrationBar("음성", "212분 남음", 0.71f)
            IllustrationBar("문자", "88건 남음", 0.88f)
        }
        Column(
            Modifier.width(160.dp).shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                .background(colors.raised).padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("데이터", fontSize = 11.sp, color = colors.muted)
            Text("5.7 GB", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colors.text)
            Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(5.dp).clip(CircleShape).background(colors.line)) {
                Box(Modifier.fillMaxWidth(0.38f).fillMaxHeight().clip(CircleShape).background(colors.accent))
            }
        }
    }
}

@Composable
private fun IllustrationBar(label: String, value: String, ratio: Float) {
    val colors = AldotTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, fontSize = 13.sp, color = colors.sub, modifier = Modifier.weight(1f))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.line)) {
            Box(Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(CircleShape).background(colors.accent))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WelcomeScreenPreview() {
    AldotWidgetTheme { WelcomeScreen(onStart = {}) }
}
