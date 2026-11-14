package com.bassengine.control.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassengine.control.ui.EngineViewModel
import com.bassengine.control.ui.bassTotalDb
import com.bassengine.control.ui.components.GlassCard
import com.bassengine.control.ui.components.GlowSlider
import com.bassengine.control.ui.components.MetricTile
import com.bassengine.control.ui.components.SectionHeader
import com.bassengine.control.ui.formatDb
import com.bassengine.control.ui.theme.LocalPalette
import com.bassengine.core.BassConfig
import com.bassengine.core.BassEngineConfig
import com.bassengine.core.DspMath
import com.bassengine.core.ResolvedConfig
import com.bassengine.core.ResponseMath
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun BassScreen(config: BassEngineConfig, vm: EngineViewModel) {
    val p = LocalPalette.current
    val b = config.bass
    val total = bassTotalDb(b.preampDb, b.boost, b.adaptive, b.subImpact, b.subwoofer)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Text(
            "低频引擎",
            color = p.text,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            "LOW END · 五个独立模块 + 动态压缩",
            color = p.accent3,
            fontSize = 11.sp,
            letterSpacing = 1.3.sp,
            modifier = Modifier.padding(top = 3.dp)
        )

        GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp), accent = p.accent2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("LOW FREQUENCY ENGINE", color = p.muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
                    Text(
                        if (b.enabled) "低频处理已开启" else "低频处理已旁路",
                        color = if (b.enabled) p.text else p.muted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(
                    checked = b.enabled,
                    onCheckedChange = {
                        vm.updateBass { c -> c.copy(enabled = it) }
                        vm.log("低频引擎 · ${if (it) "开启" else "关闭"}")
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = p.accent2,
                        uncheckedThumbColor = p.muted,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("低频总量", formatDb(total), p.accent3, Modifier.weight(1f))
                MetricTile("预增益", formatDb(b.preampDb), p.accent1, Modifier.weight(1f))
                MetricTile("Q 值", String.format(Locale.US, "%.2f", b.q), p.accent2, Modifier.weight(1f))
            }
            BassCurve(
                config = config,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .padding(top = 10.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("20Hz", "200Hz", "1kHz", "16kHz").forEach {
                    Text(it, color = p.muted.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        }

        SectionHeader("低频模块", "按需组合，不会默认全部拉满")
        FeatureRow(
            title = "低音增强",
            desc = "低频量感与力度提升，收得快不拖泥带水",
            checked = b.boost,
            accent = p.accent3,
            onChecked = {
                vm.updateBass { c -> c.copy(boost = it) }
                vm.log("低音增强 · ${if (it) "开" else "关"}")
            }
        )
        FeatureRow(
            title = "虚拟低频",
            desc = "动态低音压缩 + 多频段推动，小音量也有下潜",
            checked = b.virtualBass,
            accent = p.accent1,
            onChecked = {
                vm.updateBass { c -> c.copy(virtualBass = it) }
                vm.log("虚拟低频 · ${if (it) "开" else "关"}")
            }
        )
        FeatureRow(
            title = "低频自适应",
            desc = "轻量低频补偿；全局模式读不到实时音量，为固定近似",
            checked = b.adaptive,
            accent = p.accent2,
            onChecked = {
                vm.updateBass { c -> c.copy(adaptive = it) }
                vm.log("低频自适应 · ${if (it) "开" else "关"}")
            }
        )
        FeatureRow(
            title = "超低频冲击",
            desc = "20–45Hz 影院级下潜，观影 / 电音开启",
            checked = b.subImpact,
            accent = p.accent1,
            onChecked = {
                vm.updateBass { c -> c.copy(subImpact = it) }
                vm.log("超低频冲击 · ${if (it) "开" else "关"}")
            }
        )
        FeatureRow(
            title = "低音炮模式（1620 榨干）",
            desc = "V 型轰头曲线 + 动态低音全开，动次打次拉满",
            checked = b.subwoofer,
            accent = Color(0xFFFF7A45),
            onChecked = {
                vm.updateBass { c -> c.copy(subwoofer = it) }
                vm.log("低音炮模式 · ${if (it) "开" else "关"}")
            }
        )

        if (b.subwoofer) {
            Text(
                "低音炮模式会明显推高低频与高频两端，请先将媒体音量降到 50% 以下再开启；安全限幅会保护扬声器，但长时间大音量仍可能损伤听力。",
                color = Color(0xFFFFB74D),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFF7A45).copy(alpha = 0.08f))
                    .padding(12.dp)
            )
        }

        SectionHeader("参数", "预增益与 Q 值对整个低频链生效")
        GlassCard(Modifier.fillMaxWidth(), accent = p.accent3) {
            GlowSlider(
                label = "预增益 PRE-GAIN",
                valueText = formatDb(b.preampDb),
                value = b.preampDb,
                range = -10f..10f,
                accent = p.accent3,
                onChange = { vm.setPreamp(it, false) },
                onFinished = { vm.log("预增益 · ${formatDb(vm.config.value.bass.preampDb)}") }
            )
            Spacer(Modifier.height(14.dp))
            GlowSlider(
                label = "低音 Q 值 BASS Q",
                valueText = String.format(Locale.US, "%.2f", b.q),
                value = b.q,
                range = 0.5f..3.0f,
                accent = p.accent2,
                onChange = { vm.setBassQ(it, false) },
                onFinished = { vm.log("低音 Q 值 · ${String.format(Locale.US, "%.2f", vm.config.value.bass.q)}") }
            )
            Text(
                "Q 值越高，低频收束越紧、冲击点越集中；2.8 接近紧致鼓点，1.2 更宽松。所有增益最终都会经过 -1 dBFS 安全限幅。",
                color = p.muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun FeatureRow(
    title: String,
    desc: String,
    checked: Boolean,
    accent: Color,
    onChecked: (Boolean) -> Unit
) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(title, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = p.muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
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
private fun BassCurve(config: BassEngineConfig, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val resolved = ResolvedConfig(config.bands, config.bass, config.surround, true, config.presetId, "")
    val filters = ResponseMath.filters(resolved)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        for (db in listOf(-12, -6, 0, 6, 12)) {
            val y = h * (1f - (db + 18f) / 36f)
            drawLine(
                color = if (db == 0) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }
        val path = Path()
        val steps = 120
        for (i in 0..steps) {
            val x = w * i / steps
            val freq = 20.0 * 20.0.pow(x / w.toDouble())
            val db = DspMath.cascadeDb(filters, freq, 48_000) + config.bass.preampDb
            val y = h * (1f - ((db.toFloat() + 18f) / 36f)).coerceIn(0.02f, 0.98f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = p.accent3.copy(alpha = 0.18f), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
        drawPath(
            path,
            brush = Brush.horizontalGradient(listOf(p.accent3, p.accent2)),
            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(w * (log10(200.0 / 20.0) / log10(20.0)).toFloat(), 0f),
            end = Offset(w * (log10(200.0 / 20.0) / log10(20.0)).toFloat(), h),
            strokeWidth = 1f
        )
    }
}
