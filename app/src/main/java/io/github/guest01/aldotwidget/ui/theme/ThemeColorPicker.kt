package io.github.guest01.aldotwidget.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.guest01.aldotwidget.data.ThemeColor

/**
 * 테마 색 동그라미들 (첫 실행 안내의 위젯 단계와 설정에서 쓴다).
 * 동그라미는 지금 화면(밝게·어둡게)에서 실제로 막대·글자에 쓰일 색으로 보인다.
 * 고른 색에는 바깥 고리를 둘러서 색만으로 구분하지 않게 한다.
 */
@Composable
fun ThemeColorPicker(selected: ThemeColor, onSelect: (ThemeColor) -> Unit, modifier: Modifier = Modifier) {
    val dark = AldotTheme.colors.isDark
    Row(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        ThemeColor.entries.forEach { color ->
            val on = color == selected
            val fill = Color(if (dark) color.night else color.day)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .selectable(selected = on, onClick = { onSelect(color) }, role = Role.RadioButton)
                    .semantics { contentDescription = color.label },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .then(if (on) Modifier.border(2.dp, fill, CircleShape) else Modifier)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(fill),
                )
            }
        }
    }
}
