package com.bassengine.control.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassengine.control.ui.theme.LocalPalette

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(24.dp)
    val tint = accent ?: p.accent2
    val base = modifier
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.085f), Color.White.copy(alpha = 0.028f))
            )
        )
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.16f),
                    tint.copy(alpha = 0.32f),
                    Color.White.copy(alpha = 0.04f)
                )
            ),
            shape
        )
    val clickable = if (onClick != null) base.clickable { onClick() } else base
    Column(
        modifier = clickable.padding(contentPadding),
        content = content
    )
}

@Composable
fun SectionHeader(title: String, hint: String? = null, trailing: @Composable (() -> Unit)? = null) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (hint != null) {
                Text(hint, color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun Pill(text: String, color: Color, filled: Boolean = false) {
    val bg = if (filled) color.copy(alpha = 0.9f) else color.copy(alpha = 0.14f)
    val fg = if (filled) Color(0xFF080B14) else color
    Text(
        text,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
fun MetricTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = p.muted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TinyLabel(text: String, color: Color = LocalPalette.current.muted) {
    Text(text, color = color, fontSize = 11.sp, letterSpacing = 1.2.sp)
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}

@Composable
fun Dot(color: Color, size: Int = 7) {
    Spacer(
        Modifier
            .width(size.dp)
            .height(size.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}
