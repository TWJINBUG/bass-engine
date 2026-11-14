package com.bassengine.control.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.bassengine.control.ui.formatDbShort
import com.bassengine.control.ui.freqLabel
import com.bassengine.control.ui.theme.LocalPalette
import com.bassengine.core.DspMath

@Composable
fun BandSlider(
    index: Int,
    value: Float,
    modifier: Modifier = Modifier,
    accent: Color = LocalPalette.current.accent3,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    val p = LocalPalette.current
    var heightPx by remember { mutableFloatStateOf(1f) }
    val pulse by rememberInfiniteTransition(label = "eqPulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "eqPulseAlpha"
    )

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            formatDbShort(value),
            color = if (value >= 0) p.text else p.muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(148.dp)
                .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(heightPx) {
                    detectDragGestures(
                        onDragStart = { offset -> onChange(fromY(offset.y, heightPx)); },
                        onDrag = { change, _ ->
                            onChange(fromY(change.position.y, heightPx))
                            change.consume()
                        },
                        onDragEnd = { onFinished() },
                        onDragCancel = { onFinished() }
                    )
                }
                .pointerInput(heightPx) {
                    detectTapGestures { offset ->
                        onChange(fromY(offset.y, heightPx))
                        onFinished()
                    }
                }
        ) {
            Canvas(Modifier.fillMaxWidth().height(148.dp)) {
                val cx = size.width / 2f
                val pad = 8.dp.toPx()
                val usable = size.height - pad * 2
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.07f),
                    topLeft = Offset(cx - 2.5.dp.toPx(), pad),
                    size = Size(5.dp.toPx(), usable),
                    cornerRadius = CornerRadius(2.5.dp.toPx())
                )
                val f = ((value + 15f) / 30f).coerceIn(0f, 1f)
                val thumbY = pad + usable * (1f - f)
                val zeroY = pad + usable * 0.5f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(p.accent3.copy(alpha = 0.9f), p.accent2.copy(alpha = 0.55f)),
                        startY = thumbY, endY = zeroY
                    ),
                    topLeft = Offset(cx - 2.5.dp.toPx(), minOf(thumbY, zeroY)),
                    size = Size(5.dp.toPx(), kotlin.math.abs(zeroY - thumbY).coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(2.5.dp.toPx())
                )
                drawLine(
                    Color.White.copy(alpha = 0.16f),
                    Offset(cx - 7.dp.toPx(), zeroY),
                    Offset(cx + 7.dp.toPx(), zeroY),
                    strokeWidth = 1f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.30f * pulse), Color.Transparent),
                        center = Offset(cx, thumbY),
                        radius = (18.dp.toPx() * pulse)
                    ),
                    radius = 18.dp.toPx() * pulse,
                    center = Offset(cx, thumbY)
                )
                drawCircle(Color.White, 4.5.dp.toPx(), Offset(cx, thumbY))
                drawCircle(accent, 2.dp.toPx(), Offset(cx, thumbY))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(freqLabel(DspMath.FREQUENCIES[index]), color = p.muted, fontSize = 9.sp)
    }
}

private fun fromY(y: Float, height: Float): Float {
    val pad = 8f
    val usable = (height - pad * 2).coerceAtLeast(1f)
    val f = (1f - ((y - pad) / usable)).coerceIn(0f, 1f)
    return f * 30f - 15f
}
