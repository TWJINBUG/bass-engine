package com.bassengine.control.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bassengine.core.THEME_DARK
import com.bassengine.core.THEME_LIQUID
import com.bassengine.core.THEME_MIDNIGHT
import com.bassengine.core.THEME_NEON
import com.bassengine.core.THEME_TITANIUM

data class Palette(
    val id: String,
    val label: String,
    val bg: Color,
    val bgDeep: Color,
    val card: Color,
    val text: Color,
    val muted: Color,
    val accent1: Color,
    val accent2: Color,
    val accent3: Color
)

val Palettes = linkedMapOf(
    THEME_DARK to Palette(
        THEME_DARK, "暗黑",
        Color(0xFF070A12), Color(0xFF0B1020), Color(0xFF121726),
        Color(0xFFF5F7FF), Color(0xFF8A93A8),
        Color(0xFF4F8CFF), Color(0xFF7C4DFF), Color(0xFF00E5FF)
    ),
    THEME_MIDNIGHT to Palette(
        THEME_MIDNIGHT, "极夜",
        Color(0xFF03050A), Color(0xFF060B18), Color(0xFF0B1222),
        Color(0xFFEAF1FF), Color(0xFF6F7C96),
        Color(0xFF3155FF), Color(0xFF00B3FF), Color(0xFF8A5CFF)
    ),
    THEME_NEON to Palette(
        THEME_NEON, "霓虹橙",
        Color(0xFF0B0705), Color(0xFF160C08), Color(0xFF1C120D),
        Color(0xFFFFF4EA), Color(0xFFA08B7C),
        Color(0xFFFF6B35), Color(0xFFFF3D81), Color(0xFFFFC857)
    ),
    THEME_TITANIUM to Palette(
        THEME_TITANIUM, "钛金",
        Color(0xFF0B0C0F), Color(0xFF111318), Color(0xFF171A20),
        Color(0xFFF0F3F8), Color(0xFF8C96A6),
        Color(0xFFC0C6D4), Color(0xFF8E9AAF), Color(0xFFE8EDF5)
    ),
    THEME_LIQUID to Palette(
        THEME_LIQUID, "液晶",
        Color(0xFF041018), Color(0xFF071C28), Color(0xFF0A2433),
        Color(0xFFF2FCFF), Color(0xFF93B4C6),
        Color(0xFF5FE0FF), Color(0xFF6E8CFF), Color(0xFFC9F6FF)
    )
)

val LocalPalette = staticCompositionLocalOf { Palettes.getValue(THEME_DARK) }

@Composable
fun BassTheme(themeId: String, content: @Composable () -> Unit) {
    val p = Palettes[themeId] ?: Palettes.getValue(THEME_DARK)
    val scheme = darkColorScheme(
        primary = p.accent2,
        onPrimary = Color.White,
        secondary = p.accent1,
        onSecondary = Color.White,
        tertiary = p.accent3,
        background = p.bg,
        onBackground = p.text,
        surface = p.card,
        onSurface = p.text,
        surfaceVariant = p.card,
        onSurfaceVariant = p.muted,
        outline = Color.White.copy(alpha = 0.14f)
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
fun BassBackground() {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.bgDeep, p.bg, p.bgDeep)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(p.accent2.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.05f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.15f, size.height * 0.05f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(p.accent3.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.42f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.95f, size.height * 0.42f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(p.accent1.copy(alpha = 0.09f), Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.92f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.3f, size.height * 0.92f)
            )
        }
    }
}
