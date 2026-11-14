package com.bassengine.control.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassengine.control.ui.theme.LocalPalette
import com.bassengine.core.BassConfig
import com.bassengine.core.DspMath
import com.bassengine.core.ResolvedConfig
import com.bassengine.core.ResponseMath
import com.bassengine.core.SurroundConfig
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun EqCurve(
    bands: List<Float>,
    bass: BassConfig,
    surround: SurroundConfig,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true
) {
    val p = LocalPalette.current
    val resolved = remember(bands, bass, surround) {
        ResolvedConfig(bands, bass, surround, true, "custom", "")
    }
    val filters = remember(resolved) { ResponseMath.filters(resolved) }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val top = if (showLabels) 6.dp.toPx() else 0f
        val bottom = h - if (showLabels) 18.dp.toPx() else 0f
        val plotH = (bottom - top).coerceAtLeast(1f)
        val minDb = -18f
        val maxDb = 18f
        fun yFor(db: Double): Float = top + ((maxDb - db.toFloat()) / (maxDb - minDb)) * plotH
        fun xFor(freq: Float): Float {
            val ratio = log10(freq / 20f) / log10(1000f)
            return (ratio * w).coerceIn(0f, w)
        }

        // horizontal reference lines
        for (db in listOf(-12, -6, 0, 6, 12)) {
            val y = yFor(db.toDouble())
            drawLine(
                color = if (db == 0) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.055f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = if (db == 0) 1.2.dp.toPx() else 0.8.dp.toPx()
            )
        }
        // band guides
        DspMath.FREQUENCIES.forEach { f ->
            val x = xFor(f)
            drawLine(
                color = Color.White.copy(alpha = 0.04f),
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = 1f
            )
        }

        // magnitude response, sampled logarithmically
        val line = Path()
        val fill = Path()
        val steps = w.toInt().coerceIn(80, 480)
        var first = true
        for (i in 0..steps) {
            val x = w * i / steps
            val freq = 20.0 * 1000.0.pow((x / w).toDouble())
            val db = DspMath.cascadeDb(filters, freq, 48_000) + bass.preampDb
            val y = yFor(db)
            if (first) {
                line.moveTo(x, y)
                fill.moveTo(x, bottom)
                fill.lineTo(x, y)
                first = false
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(w, bottom)
        fill.close()
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                colors = listOf(p.accent2.copy(alpha = 0.28f), p.accent3.copy(alpha = 0.02f)),
                startY = top,
                endY = bottom
            )
        )
        drawPath(
            line,
            color = p.accent2.copy(alpha = 0.20f),
            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            line,
            brush = Brush.horizontalGradient(listOf(p.accent1, p.accent2, p.accent3)),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // dots on the ten control points
        DspMath.FREQUENCIES.forEachIndexed { i, f ->
            val db = DspMath.cascadeDb(filters, f.toDouble(), 48_000) + bass.preampDb
            val x = xFor(f)
            val y = yFor(db)
            drawCircle(p.accent3.copy(alpha = 0.16f), 7.dp.toPx(), Offset(x, y))
            drawCircle(Color.White, 2.7.dp.toPx(), Offset(x, y))
        }

        if (showLabels) {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(130, 255, 255, 255)
                textSize = 9.sp.toPx()
                isAntiAlias = true
            }
            DspMath.FREQUENCIES.forEach { f ->
                val label = when {
                    f >= 1000f -> "${(f / 1000f).toInt()}k"
                    else -> f.toInt().toString()
                }
                val tw = paint.measureText(label)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    xFor(f) - tw / 2,
                    h - 3.dp.toPx(),
                    paint
                )
            }
        }
    }
}

@Composable
fun CurveLegend(text: String, color: Color = LocalPalette.current.muted) {
    Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Normal)
}
