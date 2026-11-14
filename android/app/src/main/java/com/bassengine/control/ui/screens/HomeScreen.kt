package com.bassengine.control.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassengine.control.BuildConfig
import com.bassengine.control.audio.OutputRoute
import com.bassengine.control.global.GlobalStatus
import com.bassengine.control.ui.EngineViewModel
import com.bassengine.control.ui.components.BandSlider
import com.bassengine.control.ui.components.EqCurve
import com.bassengine.control.ui.components.GlassCard
import com.bassengine.control.ui.components.MetricTile
import com.bassengine.control.ui.components.Pill
import com.bassengine.control.ui.components.SectionHeader
import com.bassengine.control.ui.theme.LocalPalette
import com.bassengine.control.util.shareText
import com.bassengine.core.BassEngineConfig
import com.bassengine.core.DEVICE_BLUETOOTH
import com.bassengine.core.DEVICE_HEADPHONE
import com.bassengine.core.Preset
import com.bassengine.core.Presets
import org.json.JSONArray
import org.json.JSONObject

private val presetColors = listOf(
    0xFF7C4DFF, 0xFF4F8CFF, 0xFF00E5FF, 0xFF00E5A0,
    0xFFFFD166, 0xFFFF8A65, 0xFFFF3D81, 0xFFB0BEC5
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    config: BassEngineConfig,
    global: GlobalStatus,
    route: OutputRoute,
    now: Long,
    vm: EngineViewModel
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val globalActive = global.active()
    val engineActive = globalActive
    val beatSeconds = if (global.updated > 0) ((now - global.updated).coerceAtLeast(0L) / 1000L) else -1L
    var actionPreset by remember { mutableStateOf<Preset?>(null) }
    var renameTarget by remember { mutableStateOf<Preset?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showSave by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var saveColor by remember { mutableLongStateOf(presetColors.first()) }

    val customPresets = config.customPresets.map {
        Preset(it.id, it.name, "自定义曲线", it.color, it.bands, false)
    }
    val customSlot = Preset(
        Presets.CUSTOM,
        "自定义 CUSTOM",
        "自己调参并保存曲线",
        0xFFB0BEC5,
        config.bands,
        true
    )
    val presets = Presets.all + customSlot + customPresets

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("BASS ENGINE", color = p.text, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(
                    "GLOBAL DSP · 全局低音引擎",
                    color = p.accent3,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Pill("bassengine-v${BuildConfig.VERSION_NAME}", p.accent2)
        }

        GlassCard(Modifier.fillMaxWidth().padding(top = 18.dp), accent = p.accent2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DSP ENGINE", color = p.muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (engineActive) Color(0xFF00E5A0) else Color(0xFFFF5C7A))
                        )
                        Text(
                            if (globalActive) "全局 DSP 已连接" else "引擎未运行",
                            color = if (engineActive) Color(0xFF00E5A0) else Color(0xFFFF5C7A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 7.dp)
                        )
                        Text(
                            if (global.effectId > 0) " · 效果 #${global.effectId}" else "",
                            color = p.muted,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = config.engineOn,
                    onCheckedChange = { vm.setEngineOn(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = p.accent2,
                        uncheckedThumbColor = p.muted,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val routeColor = when (route.key) {
                    DEVICE_BLUETOOTH -> p.accent2
                    DEVICE_HEADPHONE -> p.accent3
                    else -> p.accent1
                }
                Pill("输出 · ${route.label}", routeColor)
                Text(
                    route.name,
                    color = p.muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("效果 ID", if (global.effectId > 0) "#${global.effectId}" else "—", p.accent3, Modifier.weight(1f))
                MetricTile("守护进程", if (global.pid > 0) global.pid.toString() else "—", p.accent1, Modifier.weight(1f))
                MetricTile("心跳", when { beatSeconds < 0 -> "—"; beatSeconds <= 1 -> "刚刚"; else -> "${beatSeconds}s" }, p.accent2, Modifier.weight(1f))
            }
            if (!engineActive) {
                Text(
                    "KernelSU 全局模块未运行。装好模块并重启后，守护进程会把系统 DynamicsProcessing 挂到 session 0，所有 App 共用这一条 DSP 链。",
                    color = p.muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else if (globalActive) {
                Text(
                    "全局效果已挂载 · PID ${global.pid} · 所有 App 共用这一条 DSP 链",
                    color = p.muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            if (!globalActive && global.error.isNotBlank()) {
                Text(
                    "最近错误：${global.error.take(100)}",
                    color = Color(0xFFFFB74D),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        SectionHeader("预设 SOUND PRESETS", "长按卡片可重命名、改色、导出或删除")
        presets.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        selected = config.presetId == preset.id,
                        onClick = {
                            vm.selectPreset(preset.id)
                            Toast.makeText(context, "预设 · ${preset.name}", Toast.LENGTH_SHORT).show()
                        },
                        onLongClick = { actionPreset = preset },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        SectionHeader("10 段图形均衡器", "20Hz – 16kHz · 拖动实时改变频响")
        GlassCard(Modifier.fillMaxWidth(), accent = p.accent3) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    Presets.nameFor(config, config.presetId),
                    color = p.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text("±15 dB", color = p.muted, fontSize = 11.sp)
            }
            EqCurve(
                bands = config.bands,
                bass = config.bass,
                surround = config.surround,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp)
                    .padding(top = 8.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                config.bands.forEachIndexed { index, value ->
                    BandSlider(
                        index = index,
                        value = value,
                        modifier = Modifier.weight(1f),
                        accent = p.accent3,
                        onChange = { vm.setBand(index, it) },
                        onFinished = { vm.log("EQ 调节 · ${index + 1} 段") }
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { vm.resetBands() }) {
                    Text("重置曲线", color = p.muted, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        saveName = "自定义 ${config.customPresets.size + 1}"
                        saveColor = presetColors.first()
                        showSave = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent2),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("保存为自定义", fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("保存自定义预设") },
            text = {
                Column {
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        label = { Text("预设名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("封面色", color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
                    ColorRow(selected = saveColor, onSelect = { saveColor = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.saveCustomPreset(saveName, saveColor)
                    showSave = false
                    Toast.makeText(context, "已保存自定义预设", Toast.LENGTH_SHORT).show()
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("取消") } }
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名预设") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = renameTarget
                    if (target != null) vm.renameCustomPreset(target.id, renameText)
                    renameTarget = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } }
        )
    }

    actionPreset?.let { preset ->
        val isCustom = !preset.builtIn
        AlertDialog(
            onDismissRequest = { actionPreset = null },
            title = { Text(preset.name) },
            text = {
                Column {
                    Text(
                        "曲线 ${preset.bands.joinToString(" / ") { String.format("%.1f", it) }} dB",
                        color = p.muted,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    if (isCustom) {
                        DialogAction("重命名") {
                            renameText = preset.name
                            renameTarget = preset
                            actionPreset = null
                        }
                    }
                    DialogAction("导出预设") {
                        exportPreset(context, preset)
                        vm.log("导出预设 · ${preset.name}")
                        actionPreset = null
                    }
                    if (isCustom) {
                        DialogAction("删除预设", danger = true) {
                            vm.deleteCustomPreset(preset.id)
                            actionPreset = null
                        }
                    } else {
                        Text("内置预设不可删除，可另存为自定义后再编辑。", color = p.muted, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionPreset = null }) { Text("关闭") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetCard(
    preset: Preset,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    val accent = Color(preset.color)
    val brush = if (selected) {
        Brush.linearGradient(listOf(p.accent2.copy(alpha = 0.92f), p.accent1.copy(alpha = 0.68f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.075f), Color.White.copy(alpha = 0.025f)))
    }
    Box(
        modifier
            .height(102.dp)
            .clip(shape)
            .background(brush)
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = 0.35f) else accent.copy(alpha = 0.28f),
                shape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else accent)
                )
                Spacer(Modifier.weight(1f))
                if (selected) {
                    Text("当前", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                preset.name,
                color = p.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                preset.subtitle,
                color = if (selected) Color.White.copy(alpha = 0.82f) else p.muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun ColorRow(selected: Long, onSelect: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        presetColors.forEach { color ->
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = if (color == selected) 3.dp else 0.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .clickable { onSelect(color) }
            )
        }
    }
}

@Composable
private fun DialogAction(text: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        color = if (danger) Color(0xFFFF5C7A) else Color(0xFFB9C6FF),
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp)
    )
}

private fun exportPreset(context: android.content.Context, preset: Preset) {
    val obj = JSONObject().apply {
        put("type", "bassengine_preset")
        put("name", preset.name)
        put("color", preset.color)
        val arr = JSONArray()
        preset.bands.forEach { arr.put(it.toDouble()) }
        put("bands", arr)
        put("frequencies", "20/125/250/500/650/1k/2k/4k/8k/16k")
    }
    shareText(context, obj.toString(2), "BASS ENGINE 预设 · ${preset.name}")
}
