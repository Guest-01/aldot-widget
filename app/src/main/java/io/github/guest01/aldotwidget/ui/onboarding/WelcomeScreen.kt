package io.github.guest01.aldotwidget.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
                // "홈 화면"이 두 줄로 갈라지지 않게 사이를 줄바꿈 없는 공백으로
                "알닷 계정으로 로그인하면 데이터·음성·문자 남은 양을 홈\u00A0화면 위젯으로 보여 드려요.",
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

/**
 * 홈 화면에 놓인 위젯 세 가지(2×2 요약, 1×1 고리, 2×1 고리와 용량)를 홈 화면 칸처럼 배치한 그림.
 * 실제 위젯과 같은 모양·색이다. 빈 칸은 앱 아이콘 자리.
 */
@Composable
private fun WidgetIllustration() {
    val colors = AldotTheme.colors
    val cell = 72.dp
    val gap = 12.dp
    Box(Modifier.size(300.dp).clip(RoundedCornerShape(32.dp)).background(colors.stage), contentAlignment = Alignment.Center) {
        PictureTextStyle {
        Box(Modifier.size(cell * 3 + gap * 2)) {
            MiniWidget(Modifier.size(cell * 2 + gap)) { SquarePicture() }
            Slot(Modifier.offset(x = (cell + gap) * 2).size(cell))
            MiniWidget(Modifier.offset(x = (cell + gap) * 2, y = cell + gap).size(cell)) { TinyPicture() }
            Slot(Modifier.offset(y = (cell + gap) * 2).size(cell))
            MiniWidget(Modifier.offset(x = cell + gap, y = (cell + gap) * 2).size(width = cell * 2 + gap, height = cell)) { CompactPicture() }
        }
        }
    }
}

@Composable
private fun MiniWidget(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.shadow(6.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(AldotTheme.colors.raised),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 앱 아이콘 자리 */
@Composable
private fun Slot(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(AldotTheme.colors.text.copy(alpha = 0.06f)))
}

@Composable
private fun SquarePicture() {
    val colors = AldotTheme.colors
    Column(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("알뜰폰", fontSize = pictureSp(9), color = colors.muted, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, tint = colors.muted, modifier = Modifier.size(11.dp))
        }
        Text("데이터 남음", fontSize = pictureSp(9), color = colors.sub, modifier = Modifier.padding(top = 4.dp))
        Text("5.7 GB", fontSize = pictureSp(20), lineHeight = pictureSp(24), fontWeight = FontWeight.ExtraBold, color = colors.text)
        Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.line)) {
            Box(Modifier.fillMaxWidth(0.38f).fillMaxHeight().clip(CircleShape).background(colors.accent))
        }
        PictureLine("음성", "212분", Modifier.padding(top = 9.dp))
        PictureLine("문자", "88건", Modifier.padding(top = 3.dp))
        Spacer(Modifier.weight(1f))
        Text("20:08 기준", fontSize = pictureSp(8), color = colors.muted)
    }
}

@Composable
private fun PictureLine(label: String, value: String, modifier: Modifier) {
    val colors = AldotTheme.colors
    Row(modifier.fillMaxWidth()) {
        Text(label, fontSize = pictureSp(9), color = colors.sub, modifier = Modifier.weight(1f))
        Text(value, fontSize = pictureSp(9), fontWeight = FontWeight.Bold, color = colors.text)
    }
}

@Composable
private fun TinyPicture() {
    val colors = AldotTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PictureRing(44.dp, 5.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("5.7", fontSize = pictureSp(11), lineHeight = pictureSp(12), fontWeight = FontWeight.ExtraBold, color = colors.text)
                Text("GB", fontSize = pictureSp(7), lineHeight = pictureSp(8), color = colors.sub)
            }
        }
        Text("20:08", fontSize = pictureSp(8), color = colors.muted, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun CompactPicture() {
    val colors = AldotTheme.colors
    Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        PictureRing(48.dp, 5.5.dp) {
            Text("38%", fontSize = pictureSp(10), fontWeight = FontWeight.ExtraBold, color = colors.text)
        }
        Column(Modifier.padding(start = 10.dp)) {
            Text("데이터 남음", fontSize = pictureSp(9), color = colors.sub)
            Text("5.7 GB", fontSize = pictureSp(15), lineHeight = pictureSp(18), fontWeight = FontWeight.ExtraBold, color = colors.text, maxLines = 1)
            Text("20:08 기준", fontSize = pictureSp(8), color = colors.muted)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WelcomeScreenPreview() {
    AldotWidgetTheme { WelcomeScreen(onStart = {}) }
}
