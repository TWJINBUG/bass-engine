package com.bassengine.control.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bassengine.control.ui.theme.LocalPalette

@Composable
fun SpectrumView(
    bands: FloatArray,
    modifier: Modifier = Modifier,
    showReflection: Boolean = true
) {
    val p = LocalPalette.current
    Canvas(modifier) {
        if (bands.isEmpty()) return@Canvas
        val gap = 2.5.dp.toPx()
        val barWidth = ((size.width - gap * (bands.size - 1)) / bands.size).coerceAtLeast(1f)
        val bottom = size.height * if (showReflection) 0.84f else 0.98f
        bands.forEachIndexed { i, value ->
            val x = i * (barWidth + gap)
            val h = (size.height * 0.78f * value.coerceIn(0f, 1f)).coerceAtLeast(2.dp.toPx())
            val top = bottom - h
            val color = when {
                i < bands.size / 3 -> p.accent3
                i < bands.size * 2 / 3 -> p.accent1
                else -> p.accent2
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.55f)),
                    startY = top,
                    endY = bottom
                ),
                topLeft = Offset(x, top),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
            drawRoundRect(
                color = color.copy(alpha = 0.16f),
                topLeft = Offset(x - 1.5.dp.toPx(), top - 1.5.dp.toPx()),
                size = Size(barWidth + 3.dp.toPx(), h + 3.dp.toPx()),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
            if (showReflection) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.18f), Color.Transparent),
                        startY = bottom,
                        endY = size.height
                    ),
                    topLeft = Offset(x, bottom),
                    size = Size(barWidth, h * 0.35f),
                    cornerRadius = CornerRadius(barWidth / 2f)
                )
            }
        }
    }
}
