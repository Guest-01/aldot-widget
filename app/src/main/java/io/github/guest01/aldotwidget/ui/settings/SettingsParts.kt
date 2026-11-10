package io.github.guest01.aldotwidget.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.guest01.aldotwidget.BuildConfig
import io.github.guest01.aldotwidget.R
import io.github.guest01.aldotwidget.ui.theme.AldotTheme
import io.github.guest01.aldotwidget.update.UpdateUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 저장소 주소. 'GitHub에서 보기' */
internal const val REPO_URL = "https://github.com/Guest-01/aldot-widget"

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

/** 업데이트 창의 넓은 버튼. primary: 테마 색 채움, 아니면 옅은 테마 색 바탕 */
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

/** 마지막으로 확인한 때: "오늘 09:14에 자동으로 확인했어요" */
internal fun checkedText(update: UpdateUiState): String {
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

/**
 * 'GitHub 새 이슈' 주소. 앱 버전·Android 버전·기기 모델을 미리 채운다 (사용량·번호 같은 개인정보는 넣지 않는다).
 * 이슈는 공개 글이라 본문 맨 위에 그 점을 알린다.
 */
internal fun issueUrl(): String {
    val body = """
        |<!-- 이 글은 누구나 볼 수 있어요. 전화번호나 사용량이 보이는 캡처는 가려서 올려 주세요. -->
        |
        |### 어떤 화면에서 무슨 일이 있었나요?
        |
        |
        |### 기기 정보 (자동으로 채워졌어요)
        |- 앱 버전: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
        |- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
        |- 기기: ${Build.MANUFACTURER} ${Build.MODEL}
    """.trimMargin()
    return Uri.parse("$REPO_URL/issues/new").buildUpon()
        .appendQueryParameter("body", body)
        .build().toString()
}

internal fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // 브라우저가 없는 폰은 거의 없다. 있으면 아무 일도 일어나지 않는다
    }
}
