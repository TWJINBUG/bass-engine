package com.bassengine.control.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassengine.control.ui.EngineViewModel
import com.bassengine.control.ui.components.GlassCard
import com.bassengine.control.ui.components.GlowSlider
import com.bassengine.control.ui.components.SectionHeader
import com.bassengine.control.ui.theme.LocalPalette
import com.bassengine.core.BassEngineConfig
import com.bassengine.core.SURROUND_CINEMA
import com.bassengine.core.SURROUND_NEAR
import com.bassengine.core.SURROUND_WIDE
import com.bassengine.core.SurroundConfig

@Composable
fun SurroundScreen(config: BassEngineConfig, vm: EngineViewModel) {
    val p = LocalPalette.current
    val s = config.surround

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Text(
            "环绕模式",
            color = p.text,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            "SPATIAL · EQ 模拟空间感",
            color = p.accent3,
            fontSize = 11.sp,
            letterSpacing = 1.3.sp,
            modifier = Modifier.padding(top = 3.dp)
        )

        GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp), accent = p.accent2) {
            SpatialVisual(
                mode = s.mode,
                intensity = s.intensity,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Text(
                modeTitle(s.mode),
                color = p.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "强度 ${s.intensity}% · ${modeHint(s.mode)}",
                color = p.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        SectionHeader("三选一", "点击卡片切换空间算法")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            ModeCard(
                title = "宽场",
                tag = "音乐",
                selected = s.mode == SURROUND_WIDE,
                accent = p.accent1,
                modifier = Modifier.weight(1f)
            ) {
                vm.updateSurround { it.copy(mode = SURROUND_WIDE) }
                vm.log("环绕模式 · 宽场（音乐）")
            }
            ModeCard(
                title = "影院",
                tag = "影视",
                selected = s.mode == SURROUND_CINEMA,
                accent = p.accent2,
                modifier = Modifier.weight(1f)
            ) {
                vm.updateSurround { it.copy(mode = SURROUND_CINEMA) }
                vm.log("环绕模式 · 影院（影视）")
            }
            ModeCard(
                title = "近场",
                tag = "人声",
                selected = s.mode == SURROUND_NEAR,
                accent = p.accent3,
                modifier = Modifier.weight(1f)
            ) {
                vm.updateSurround { it.copy(mode = SURROUND_NEAR) }
                vm.log("环绕模式 · 近场（人声）")
            }
        }

        SectionHeader("空间细节", "只做轻量 EQ 染色，不加混响、不改变原始音色取向")
        DetailRow("空间更通透", "抬升 10kHz 空气感，减少闷感", s.air, p.accent3) {
            vm.updateSurround { c -> c.copy(air = it) }
            vm.log("空间更通透 · ${if (it) "开" else "关"}")
        }
        DetailRow("结像更清晰", "2.8kHz 轻微聚焦，人声口型更稳", s.focus, p.accent2) {
            vm.updateSurround { c -> c.copy(focus = it) }
            vm.log("结像更清晰 · ${if (it) "开" else "关"}")
        }
        DetailRow("人声清晰增益", "500Hz–2kHz 提亮，歌词对白听得更清", s.vocal, Color(0xFF00E5A0)) {
            vm.updateSurround { c -> c.copy(vocal = it) }
            vm.log("人声清晰增益 · ${if (it) "开" else "关"}")
        }

        SectionHeader("环绕强度", "越大空气感和中频抬升越明显，建议 40%–70%")
        GlassCard(Modifier.fillMaxWidth(), accent = p.accent2) {
            GlowSlider(
                label = "SPATIAL WIDTH",
                valueText = "${s.intensity}%",
                value = s.intensity.toFloat(),
                range = 40f..70f,
                accent = p.accent2,
                onChange = { vm.setSurroundIntensity(it) },
                onFinished = { vm.log("环绕强度 · ${vm.config.value.surround.intensity}%") }
            )
            if (s.intensity > 65) {
                Text(
                    "当前强度较高，高频和齿音会被一起抬起来；建议回调到 60% 以下。",
                    color = Color(0xFFFFB74D),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            Text(
                "环绕通过 EQ 模拟空间感，耳机 / 立体声扬声器效果最佳；单声道设备只会得到音色变化。",
                color = p.muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

private fun modeTitle(mode: String): String = when (mode) {
    SURROUND_CINEMA -> "影院（影视） · 包围感优先"
    SURROUND_NEAR -> "近场（人声） · 贴耳与清晰度"
    else -> "宽场（音乐） · 横向展开"
}

private fun modeHint(mode: String): String = when (mode) {
    SURROUND_CINEMA -> "适合看剧、看电影"
    SURROUND_NEAR -> "适合人声、播客、ASMR"
    else -> "适合流行、电子"
}

@Composable
private fun ModeCard(
    title: String,
    tag: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(listOf(accent.copy(alpha = 0.32f), accent.copy(alpha = 0.10f)))
                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
            )
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 14.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (selected) accent else p.muted)
        )
        Spacer(Modifier.height(12.dp))
        Text(title, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(tag, color = if (selected) accent else p.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun DetailRow(title: String, desc: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = p.muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = p.muted,
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun SpatialVisual(mode: String, intensity: Int, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val transition = rememberInfiniteTransition(label = "spatial")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "pulse"
    )
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension * 0.14f
        val spread = size.minDimension * (0.18f + intensity / 100f * 0.28f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(p.accent2.copy(alpha = 0.18f), Color.Transparent),
                center = center,
                radius = spread * 1.6f
            ),
            radius = spread * 1.6f,
            center = center
        )
        for (i in 0..3) {
            val r = base + spread * i / 3f
            drawCircle(
                color = when (mode) {
                    SURROUND_CINEMA -> p.accent2.copy(alpha = 0.22f * pulse)
                    SURROUND_NEAR -> p.accent3.copy(alpha = 0.26f * pulse)
                    else -> p.accent1.copy(alpha = 0.22f * pulse)
                },
                radius = r,
                center = center,
                style = Stroke(width = (1.2f + i * 0.3f).dp.toPx())
            )
        }
        drawCircle(Color.White.copy(alpha = 0.9f), 3.dp.toPx(), center)
    }
}
