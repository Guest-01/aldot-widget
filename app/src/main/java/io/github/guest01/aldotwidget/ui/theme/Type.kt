package io.github.guest01.aldotwidget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak

/**
 * 한국어 줄바꿈: 기본값은 글자 단위로 끊어서 "보관돼/요."처럼 마지막 글자만 다음 줄로 넘어가기도 한다.
 * 어절(띄어쓰기) 단위로 끊게 한다 (웹의 word-break: keep-all과 같은 효과, Android 13+에서 적용).
 */
private val KoreanLineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)

// 어절 단위 규칙은 글자의 언어가 한국어일 때만 동작해서, 폰 언어가 영어여도 한국어로 다루게 한다
private fun TextStyle.korean() = copy(lineBreak = KoreanLineBreak, localeList = LocaleList("ko-KR"))

// 글자 크기·굵기는 화면마다 시안 값을 직접 쓰고, 여기서는 줄바꿈 규칙만 모든 기본 스타일에 넣는다.
val Typography = Typography().run {
    copy(
        displayLarge = displayLarge.korean(),
        displayMedium = displayMedium.korean(),
        displaySmall = displaySmall.korean(),
        headlineLarge = headlineLarge.korean(),
        headlineMedium = headlineMedium.korean(),
        headlineSmall = headlineSmall.korean(),
        titleLarge = titleLarge.korean(),
        titleMedium = titleMedium.korean(),
        titleSmall = titleSmall.korean(),
        bodyLarge = bodyLarge.korean(),
        bodyMedium = bodyMedium.korean(),
        bodySmall = bodySmall.korean(),
        labelLarge = labelLarge.korean(),
        labelMedium = labelMedium.korean(),
        labelSmall = labelSmall.korean(),
    )
}
