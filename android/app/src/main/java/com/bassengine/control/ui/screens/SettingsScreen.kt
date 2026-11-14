package com.bassengine.control.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.bassengine.control.audio.TestTonePlayer
import com.bassengine.control.data.LogEntry
import com.bassengine.control.ui.EngineViewModel
import com.bassengine.control.ui.components.GlassCard
import com.bassengine.control.ui.components.Pill
import com.bassengine.control.ui.components.SectionHeader
import com.bassengine.control.ui.components.SmallChip
import com.bassengine.control.ui.theme.LocalPalette
import com.bassengine.control.ui.theme.Palettes
import com.bassengine.control.util.copyText
import com.bassengine.control.util.shareText
import com.bassengine.core.BassEngineConfig
import com.bassengine.core.DEVICE_AUTO
import com.bassengine.core.DEVICE_BLUETOOTH
import com.bassengine.core.DEVICE_HEADPHONE
import com.bassengine.core.DEVICE_SPEAKER
import com.bassengine.core.DeviceProfile
import com.bassengine.core.DeviceTuning
import com.bassengine.core.SCHEDULE_OFF
import com.bassengine.core.SCHEDULE_SLEEP
import com.bassengine.core.SURROUND_CINEMA
import com.bassengine.core.SURROUND_NEAR
import com.bassengine.core.SURROUND_WIDE
import com.bassengine.core.Preset
import com.bassengine.core.Presets

@Composable
fun SettingsScreen(
    config: BassEngineConfig,
    logs: List<LogEntry>,
    vm: EngineViewModel
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val global by vm.global.collectAsStateWithLifecycle()
    val tuning = remember(config.device.lockedModel) {
        DeviceTuning.detect(Build.MODEL, Build.MANUFACTURER, config.device.lockedModel)
    }
    var editProfile by remember { mutableStateOf<String?>(null) }
    var showRulePicker by remember { mutableStateOf(false) }
    var pendingRulePackage by remember { mutableStateOf<String?>(null) }
    var showImport by remember { mutableStateOf(false) }
    val tonePlayer = remember { TestTonePlayer() }

    DisposableEffect(Unit) {
        onDispose { tonePlayer.stop() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Text(
            "设置",
            color = p.text,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            "SYSTEM · 主题 / 档案 / 自动化",
            color = p.accent3,
            fontSize = 11.sp,
            letterSpacing = 1.3.sp,
            modifier = Modifier.padding(top = 3.dp)
        )

        SectionHeader("主题", "一键换肤，面板与图表同步变化")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Palettes.values.forEach { palette ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { vm.setTheme(palette.id) }
                        .padding(6.dp)
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.accent1, palette.accent2, palette.accent3)
                                )
                            )
                            .border(
                                width = if (config.theme == palette.id) 2.5.dp else 0.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                    Text(
                        palette.label,
                        color = if (config.theme == palette.id) p.text else p.muted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }

        SectionHeader("机型调音档案", "按设备识别，可手动锁定")
        GlassCard(Modifier.fillMaxWidth(), accent = p.accent1) {
            Text(tuning.title, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                tuning.detail,
                color = p.muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("锁定本机档案", color = p.text, fontSize = 12.sp)
                    Text("关闭后按当前识别结果自动适配", color = p.muted, fontSize = 10.sp)
                }
                Switch(
                    checked = config.device.lockedModel != null,
                    onCheckedChange = { locked ->
                        vm.update {
                            it.copy(device = it.device.copy(lockedModel = if (locked) Build.MODEL else null))
                        }
                        vm.log(if (locked) "锁定机型档案 · ${Build.MODEL}" else "解锁机型档案")
                    },
                    colors = switchColors(p)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    vm.selectPreset(config.presetId)
                    Toast.makeText(context, "已重新应用当前预设", Toast.LENGTH_SHORT).show()
                }) { Text("重新应用预设", color = p.accent3, fontSize = 12.sp) }
                Spacer(Modifier.weight(1f))
                Pill(
                    if (tuning.known) "已收录机型" else "通用曲线",
                    if (tuning.known) Color(0xFF00E5A0) else p.muted
                )
            }
        }

        val route by vm.route.collectAsStateWithLifecycle()
        SectionHeader(
            "设备 Profile",
            "当前输出 · ${route.label} · ${route.name}\n耳机 / 扬声器 / 蓝牙各自记忆一套预设"
        )
        DeviceModeChips(config, vm)
        Spacer(Modifier.height(8.dp))
        ProfileRow("扬声器", config.device.profiles[DEVICE_SPEAKER], p.accent1) { editProfile = DEVICE_SPEAKER }
        ProfileRow("耳机", config.device.profiles[DEVICE_HEADPHONE], p.accent3) { editProfile = DEVICE_HEADPHONE }
        ProfileRow("蓝牙耳机", config.device.profiles[DEVICE_BLUETOOTH], p.accent2) { editProfile = DEVICE_BLUETOOTH }

        SectionHeader("应用自动场景", "按 App 记录想用的预设")
        if (config.appRules.isEmpty()) {
            Text("未添加规则。音乐 App 可指定宽场，视频 App 指定影院，游戏指定 GAME。", color = p.muted, fontSize = 11.sp, lineHeight = 16.sp)
        } else {
            config.appRules.forEach { rule ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.035f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(appLabel(context, rule.packageName), color = p.text, fontSize = 12.sp)
                        Text(
                            "${rule.packageName} → ${Presets.nameFor(config, rule.presetId)}",
                            color = p.muted,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = {
                        vm.update { it.copy(appRules = it.appRules.filterNot { r -> r.packageName == rule.packageName }) }
                        vm.log("删除应用规则 · ${rule.packageName}")
                    }) { Text("移除", color = p.muted, fontSize = 11.sp) }
                }
            }
        }
        TextButton(onClick = { showRulePicker = true }) {
            Text("+ 添加应用规则", color = p.accent3, fontSize = 12.sp)
        }

        SectionHeader("定时模式", "夜听自动切助眠或关闭引擎")
        GlassCard(Modifier.fillMaxWidth(), accent = p.accent2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("启用定时", color = p.text, fontSize = 13.sp)
                    Text(
                        if (config.schedule.enabled) "${config.schedule.start} → ${config.schedule.end}" else "当前关闭",
                        color = p.muted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = config.schedule.enabled,
                    onCheckedChange = {
                        vm.update { c -> c.copy(schedule = c.schedule.copy(enabled = it)) }
                        vm.log("定时模式 · ${if (it) "开启" else "关闭"}")
                    },
                    colors = switchColors(p)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { pickTime(context, config.schedule.start) { t ->
                        vm.update { c -> c.copy(schedule = c.schedule.copy(start = t)) }
                        vm.log("定时开始 · $t")
                    } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("开始 ${config.schedule.start}", fontSize = 11.sp) }
                OutlinedButton(
                    onClick = { pickTime(context, config.schedule.end) { t ->
                        vm.update { c -> c.copy(schedule = c.schedule.copy(end = t)) }
                        vm.log("定时结束 · $t")
                    } },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("结束 ${config.schedule.end}", fontSize = 11.sp) }
            }
            Row(Modifier.padding(top = 8.dp)) {
                SmallChip("助眠曲线", config.schedule.action == SCHEDULE_SLEEP, p.accent3) {
                    vm.update { it.copy(schedule = it.schedule.copy(action = SCHEDULE_SLEEP)) }
                }
                SmallChip("关闭引擎", config.schedule.action == SCHEDULE_OFF, p.accent2) {
                    vm.update { it.copy(schedule = it.schedule.copy(action = SCHEDULE_OFF)) }
                }
            }
        }

        SectionHeader("配置导入 / 导出", "EQ + 低频 + 环绕 + 自动化全部打包")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { shareText(context, vm.exportJson(), "BASS ENGINE 配置") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("导出 JSON", fontSize = 11.sp) }
            OutlinedButton(
                onClick = {
                    copyText(context, "BASS ENGINE 分享码", vm.shareCode())
                    Toast.makeText(context, "分享码已复制", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("复制分享码", fontSize = 11.sp) }
            OutlinedButton(
                onClick = { showImport = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("导入", fontSize = 11.sp) }
        }

        SectionHeader("试听工具", "调参时一键对比，输出为低电平测试信号")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val ok = tonePlayer.start(TestTonePlayer.Mode.PINK)
                    Toast.makeText(context, if (ok) "粉红噪声播放中" else "音频输出初始化失败", Toast.LENGTH_SHORT).show()
                    if (ok) vm.log("试听 · 粉红噪声")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent2),
                shape = RoundedCornerShape(14.dp)
            ) { Text("粉红噪声", fontSize = 11.sp) }
            Button(
                onClick = {
                    val ok = tonePlayer.start(TestTonePlayer.Mode.SWEEP)
                    Toast.makeText(context, if (ok) "20Hz–20kHz 扫频播放中" else "音频输出初始化失败", Toast.LENGTH_SHORT).show()
                    if (ok) vm.log("试听 · 扫频")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent1),
                shape = RoundedCornerShape(14.dp)
            ) { Text("扫频测试", fontSize = 11.sp) }
            OutlinedButton(
                onClick = { tonePlayer.stop(); vm.log("试听 · 停止") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("停止", fontSize = 11.sp) }
        }

        SectionHeader("安全保护", "所有输出都会经过峰值限制")
        GlassCard(Modifier.fillMaxWidth(), accent = Color(0xFF00E5A0)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("自动限幅防削波", color = p.text, fontSize = 13.sp)
                    Text("阈值 -1 dBFS，硬削波保护始终开启", color = p.muted, fontSize = 10.sp)
                }
                Switch(
                    checked = config.limiter,
                    onCheckedChange = { vm.setLimiter(it) },
                    colors = switchColors(p)
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val beat = if (global.updated > 0)
                    ((System.currentTimeMillis() - global.updated).coerceAtLeast(0L) / 1000L)
                else -1L
                StatChip("效果 ID", if (global.effectId > 0) "#${global.effectId}" else "—", p.accent3, Modifier.weight(1f))
                StatChip("守护进程", if (global.pid > 0) global.pid.toString() else "—", p.accent1, Modifier.weight(1f))
                StatChip("心跳", when { beat < 0 -> "—"; beat <= 1 -> "刚刚"; else -> "${beat}s" }, p.accent2, Modifier.weight(1f))
            }
            if (global.error.isNotBlank()) {
                Text("最近错误：${global.error.take(140)}", color = Color(0xFFFFB74D), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        SectionHeader("关于", null)
        GlassCard(Modifier.fillMaxWidth(), accent = p.accent2) {
            Text("BASS ENGINE · bassengine-v${BuildConfig.VERSION_NAME}", color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "KernelSU 全局模块 + 控制 App。root 守护进程把系统 DynamicsProcessing 挂到 audio session 0，所有 App 共用一条 DSP 链；不改内核、不写分区、不注入任何 App 进程。",
                color = p.muted,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("全局守护进程由 KernelSU 模块在开机时拉起", color = p.muted, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text("${logs.size} 条日志", color = p.muted, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(30.dp))
    }

    editProfile?.let { key ->
        val profile = config.device.profiles[key] ?: DeviceProfile()
        DeviceProfileDialog(
            title = when (key) {
                DEVICE_HEADPHONE -> "耳机 Profile"
                DEVICE_BLUETOOTH -> "蓝牙 Profile"
                else -> "扬声器 Profile"
            },
            presetId = profile.presetId,
            bassEnabled = profile.bassEnabled,
            surroundMode = profile.surroundMode,
            initialIntensity = profile.intensity,
            config = config,
            onDismiss = { editProfile = null },
            onSave = { updated ->
                vm.update { c ->
                    c.copy(device = c.device.copy(profiles = c.device.profiles + (key to updated)))
                }
                vm.log("更新设备 Profile · $key")
                editProfile = null
            }
        )
    }

    if (showRulePicker) {
        AppPickerDialog(
            onDismiss = { showRulePicker = false },
            onPick = { pkg ->
                showRulePicker = false
                pendingRulePackage = pkg
            }
        )
    }

    pendingRulePackage?.let { pkg ->
        PresetPickerDialog(
            title = "为 ${appLabel(context, pkg)} 选择预设",
            config = config,
            onDismiss = { pendingRulePackage = null },
            onPick = { id ->
                vm.update { c ->
                    c.copy(appRules = c.appRules.filterNot { it.packageName == pkg } + com.bassengine.core.AppRule(pkg, id))
                }
                vm.log("应用规则 · $pkg → ${Presets.nameFor(config, id)}")
                pendingRulePackage = null
            }
        )
    }

    if (showImport) {
        ImportDialog(
            vm = vm,
            onDismiss = { showImport = false }
        )
    }
}

@Composable
private fun DeviceModeChips(config: BassEngineConfig, vm: EngineViewModel) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth()) {
        SmallChip("自动", config.device.mode == DEVICE_AUTO, p.accent3) {
            vm.update { it.copy(device = it.device.copy(mode = DEVICE_AUTO)) }
            vm.log("设备模式 · 自动")
        }
        SmallChip("扬声器", config.device.mode == DEVICE_SPEAKER, p.accent1) {
            vm.update { it.copy(device = it.device.copy(mode = DEVICE_SPEAKER)) }
            vm.log("设备模式 · 扬声器")
        }
        SmallChip("耳机", config.device.mode == DEVICE_HEADPHONE, p.accent3) {
            vm.update { it.copy(device = it.device.copy(mode = DEVICE_HEADPHONE)) }
            vm.log("设备模式 · 耳机")
        }
        SmallChip("蓝牙", config.device.mode == DEVICE_BLUETOOTH, p.accent2) {
            vm.update { it.copy(device = it.device.copy(mode = DEVICE_BLUETOOTH)) }
            vm.log("设备模式 · 蓝牙")
        }
    }
}

private fun surroundLabel(mode: String?): String = when (mode) {
    "cinema" -> "影院"
    "near" -> "近场"
    else -> "宽场"
}

@Composable
private fun ProfileRow(label: String, profile: DeviceProfile?, accent: Color, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(label, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                "${Presets.byId(profile?.presetId ?: "")?.name ?: "自定义"} · 低音${if (profile?.bassEnabled == true) "开" else "关"} · ${surroundLabel(profile?.surroundMode)}",
                color = p.muted,
                fontSize = 10.sp
            )
        }
        Text("编辑", color = p.accent3, fontSize = 11.sp)
    }
}

@Composable
private fun StatChip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = p.muted, fontSize = 9.sp)
        Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun switchColors(p: com.bassengine.control.ui.theme.Palette) = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = p.accent2,
    uncheckedThumbColor = p.muted,
    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
)

private fun pickTime(context: Context, initial: String, onPicked: (String) -> Unit) {
    val parts = initial.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 23
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(context, { _, hour, minute ->
        onPicked(String.format("%02d:%02d", hour, minute))
    }, h, m, true).show()
}

private fun appLabel(context: Context, pkg: String): String {
    return try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Throwable) {
        pkg
    }
}

private data class AppEntry(val packageName: String, val label: String)

@Composable
private fun AppPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val apps = remember {
        runCatching {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
        }.getOrDefault(emptyList())
    }
    val filtered = apps.filter { query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择应用") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("搜索") },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(Modifier.heightIn(max = 330.dp).padding(top = 8.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app.packageName) }
                                .padding(vertical = 9.dp, horizontal = 4.dp)
                        ) {
                            Column {
                                Text(app.label, fontSize = 13.sp)
                                Text(app.packageName, fontSize = 9.sp, color = LocalPalette.current.muted)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun PresetPickerDialog(
    title: String,
    config: BassEngineConfig,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val p = LocalPalette.current
    val items = Presets.all + config.customPresets.map {
        Preset(it.id, it.name, "自定义", it.color, it.bands, false)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 15.sp) },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(items, key = { it.id }) { preset ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(preset.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(preset.color))
                        )
                        Text(
                            preset.name,
                            color = p.text,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DeviceProfileDialog(
    title: String,
    presetId: String,
    bassEnabled: Boolean,
    surroundMode: String,
    initialIntensity: Int,
    config: BassEngineConfig,
    onDismiss: () -> Unit,
    onSave: (DeviceProfile) -> Unit
) {
    val p = LocalPalette.current
    var selectedPreset by remember { mutableStateOf(presetId) }
    var bass by remember { mutableStateOf(bassEnabled) }
    var mode by remember { mutableStateOf(surroundMode) }
    var intensity by remember { mutableStateOf(initialIntensity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 15.sp) },
        text = {
            Column {
                Text("预设", color = p.muted, fontSize = 10.sp)
                LazyColumn(Modifier.heightIn(max = 190.dp).padding(vertical = 6.dp)) {
                    items(Presets.all, key = { it.id }) { preset ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedPreset = preset.id }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset.color))
                            )
                            Text(
                                preset.name,
                                color = if (selectedPreset == preset.id) p.text else p.muted,
                                fontSize = 13.sp,
                                fontWeight = if (selectedPreset == preset.id) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("低音开关", color = p.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Switch(checked = bass, onCheckedChange = { bass = it }, colors = switchColors(p))
                }
                Text("环绕模式", color = p.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                Row {
                    SmallChip("宽场", mode == SURROUND_WIDE, p.accent1) { mode = SURROUND_WIDE }
                    SmallChip("影院", mode == SURROUND_CINEMA, p.accent2) { mode = SURROUND_CINEMA }
                    SmallChip("近场", mode == SURROUND_NEAR, p.accent3) { mode = SURROUND_NEAR }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(DeviceProfile(selectedPreset, bass, mode, intensity))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ImportDialog(vm: EngineViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入配置") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("粘贴 JSON 或 BASS7: 分享码") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "导入会覆盖当前全部调音参数；主题与自动化规则也会一并恢复。",
                    color = LocalPalette.current.muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ok = vm.importConfig(text)
                Toast.makeText(context, if (ok) "配置已导入" else "格式无效，导入失败", Toast.LENGTH_SHORT).show()
                if (ok) onDismiss()
            }) { Text("导入") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
