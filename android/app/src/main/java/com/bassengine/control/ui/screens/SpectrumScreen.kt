package com.bassengine.control.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.bassengine.control.audio.SpectrumAnalyzer
import com.bassengine.control.data.LogEntry
import com.bassengine.control.ui.EngineViewModel
import com.bassengine.control.ui.bassTotalDb
import com.bassengine.control.ui.components.GlassCard
import com.bassengine.control.ui.components.MetricTile
import com.bassengine.control.ui.components.SectionHeader
import com.bassengine.control.ui.components.SpectrumView
import com.bassengine.control.ui.formatDb
import com.bassengine.control.ui.theme.LocalPalette
import com.bassengine.control.util.shareText
import com.bassengine.core.BassEngineConfig
import com.bassengine.core.Presets

@Composable
fun SpectrumScreen(
    config: BassEngineConfig,
    logs: List<LogEntry>,
    vm: EngineViewModel
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    var bands by remember { mutableStateOf(FloatArray(SpectrumAnalyzer.BANDS)) }
    var micOn by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    val analyzer = remember { SpectrumAnalyzer { bands = it } }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (analyzer.start()) {
                micOn = true
                vm.log("频谱模式 · 麦克风 MIC")
            } else {
                Toast.makeText(context, "麦克风初始化失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "需要麦克风权限才能显示实时频谱", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestMic() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (analyzer.start()) {
                micOn = true
                vm.log("频谱模式 · 麦克风 MIC")
            } else {
                Toast.makeText(context, "麦克风初始化失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.stop()
            micOn = false
        }
    }

    val am = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val volumePct = runCatching {
        am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 /
            am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }.getOrDefault(0)
    val total = bassTotalDb(
        config.bass.preampDb, config.bass.boost,
        config.bass.adaptive, config.bass.subImpact, config.bass.subwoofer
    )

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
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("实时频谱", color = p.text, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("SPECTRUM · MIC ANALYSER", color = p.accent3, fontSize = 11.sp, letterSpacing = 1.3.sp)
            }
            TextButton(onClick = { fullscreen = true }) {
                Text("全屏", color = p.accent3, fontSize = 12.sp)
            }
        }

        GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp), accent = p.accent3) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MIC INPUT", color = p.muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
                    Text(
                        if (micOn) "实时采集 · 56 bands" else "未开启麦克风",
                        color = p.text,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Switch(
                    checked = micOn,
                    onCheckedChange = { on ->
                        if (on) requestMic() else {
                            analyzer.stop()
                            micOn = false
                            bands = FloatArray(SpectrumAnalyzer.BANDS)
                            vm.log("频谱模式 · 关闭")
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = p.accent3,
                        uncheckedThumbColor = p.muted,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
            }
            if (micOn) {
                SpectrumView(
                    bands = bands,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(top = 12.dp)
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "打开开关后显示 56 段实时频谱",
                        color = p.muted,
                        fontSize = 11.sp
                    )
                }
            }
            if (!micOn) {
                Button(
                    onClick = { requestMic() },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent2),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("开启麦克风频谱", fontSize = 12.sp)
                }
            }
        }


        SectionHeader("输出状态", "来自系统音量与当前引擎配置")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricTile("媒体音量", "$volumePct%", p.accent1, Modifier.weight(1f))
            MetricTile("低频总量", formatDb(total), p.accent3, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricTile("环绕强度", "${config.surround.intensity}%", p.accent2, Modifier.weight(1f))
            MetricTile("低频自适应", if (config.bass.adaptive) "已开启" else "已关闭", if (config.bass.adaptive) Color(0xFF00E5A0) else p.muted, Modifier.weight(1f))
        }

        SectionHeader("运行日志", "预设切换、预增益、Q 值、频谱模式等")
        GlassCard(Modifier.fillMaxWidth(), accent = p.accent2) {
            if (logs.isEmpty()) {
                Text("暂无日志", color = p.muted, fontSize = 12.sp)
            } else {
                logs.takeLast(7).reversed().forEach { entry ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(entry.timeText(), color = p.accent3, fontSize = 10.sp)
                        Text(
                            entry.message,
                            color = p.muted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    val text = vm.exportLogs()
                    if (text.isBlank()) {
                        Toast.makeText(context, "暂无日志可导出", Toast.LENGTH_SHORT).show()
                    } else {
                        shareText(context, text, "BASS ENGINE 运行日志")
                    }
                }) { Text("导出日志", color = p.accent3, fontSize = 12.sp) }
                TextButton(onClick = {
                    vm.clearLogs()
                    Toast.makeText(context, "日志已清空", Toast.LENGTH_SHORT).show()
                }) { Text("清空", color = p.muted, fontSize = 12.sp) }
                Spacer(Modifier.weight(1f))
                Text("${logs.size}/200", color = p.muted, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF03050A), Color(0xFF0A0718)))
                    )
                    .padding(20.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                Presets.nameFor(config, config.presetId),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("BASS ENGINE · VISUAL", color = p.accent3, fontSize = 10.sp, letterSpacing = 2.sp)
                        }
                        TextButton(onClick = { fullscreen = false }) {
                            Text("退出全屏", color = p.accent3, fontSize = 12.sp)
                        }
                    }
                    SpectrumView(
                        bands = bands,
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 24.dp)
                    )
                    Text(
                        if (micOn) "MIC · 实时" else "开启麦克风后显示实时频谱",
                        color = p.muted,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
