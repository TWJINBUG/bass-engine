package com.bassengine.control.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassengine.control.ui.theme.LocalPalette

@Composable
fun GlowSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    val p = LocalPalette.current
    var widthPx by remember { mutableFloatStateOf(1f) }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = p.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(range, widthPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val pad = 14.dp.toPx()
                            val f = ((offset.x - pad) / (widthPx - 2 * pad)).coerceIn(0f, 1f)
                            onChange(range.start + f * (range.endInclusive - range.start))
                        },
                        onDrag = { change, _ ->
                            val pad = 14.dp.toPx()
                            val f = ((change.position.x - pad) / (widthPx - 2 * pad)).coerceIn(0f, 1f)
                            onChange(range.start + f * (range.endInclusive - range.start))
                            change.consume()
                        },
                        onDragEnd = { onFinished() },
                        onDragCancel = { onFinished() }
                    )
                }
                .pointerInput(range, widthPx) {
                    detectTapGestures { offset ->
                        val pad = 14.dp.toPx()
                        val f = ((offset.x - pad) / (widthPx - 2 * pad)).coerceIn(0f, 1f)
                        onChange(range.start + f * (range.endInclusive - range.start))
                        onFinished()
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val pad = 14.dp.toPx()
                val cy = size.height / 2f
                val usable = size.width - pad * 2f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.075f),
                    topLeft = Offset(pad, cy - 3.dp.toPx()),
                    size = Size(usable, 6.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                val f = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                val thumbX = pad + usable * f
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(p.accent1.copy(alpha = 0.55f), accent, p.accent3)
                    ),
                    topLeft = Offset(pad, cy - 3.dp.toPx()),
                    size = Size(usable * f, 6.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(thumbX, cy),
                        radius = 24.dp.toPx()
                    ),
                    radius = 24.dp.toPx(),
                    center = Offset(thumbX, cy)
                )
                drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(thumbX, cy))
                drawCircle(color = accent, radius = 3.dp.toPx(), center = Offset(thumbX, cy))
            }
        }
    }
}

@Composable
fun SmallChip(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            color = if (selected) Color(0xFF080B14) else p.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
