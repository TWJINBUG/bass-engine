package com.bassengine.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassengine.control.ui.components.GlassCard
import com.bassengine.control.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SetupStep(val title: String, val hint: String, val ok: Boolean)

private fun sh(command: String): String {
    for (binary in listOf("/system/bin/su", "su")) {
        try {
            val proc = ProcessBuilder(binary, "-c", command)
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            if (out.isNotBlank()) return out.trim()
        } catch (_: Throwable) {
        }
    }
    return ""
}

private suspend fun probe(): List<SetupStep> = withContext(Dispatchers.IO) {
    val uid = sh("id").let { if (it.contains("uid=0")) "0" else "" }
    val rootOk = uid == "0"
    val moduleOk = rootOk && sh("test -d /data/adb/modules/bassengine_global && echo yes").contains("yes")
    val pid = if (rootOk) sh("cat /data/adb/bassengine/daemon.pid 2>/dev/null") else ""
    val daemonOk = rootOk && pid.isNotEmpty() && sh("kill -0 $pid 2>/dev/null && echo yes").contains("yes")
    val effectOk = rootOk && sh(
        "grep -c '\"attached\":true' /sdcard/Android/data/com.bassengine.control/files/global_status.json 2>/dev/null"
    ).toIntOrNull()?.let { it > 0 } ?: false
    listOf(
        SetupStep("Root 授权", if (rootOk) "已获得 root" else "需要在权限弹窗里允许 BASS ENGINE", rootOk),
        SetupStep("KernelSU 模块", if (moduleOk) "bassengine_global 已安装" else "去 KernelSU 管理器安装模块后重启", moduleOk),
        SetupStep("全局守护进程", if (daemonOk) "运行中（PID $pid）" else "模块装好后重启手机，或手动执行 service.sh", daemonOk),
        SetupStep("DSP 已挂载", if (effectOk) "session 0 已接管" else "守护进程起来后会自动挂载", effectOk)
    )
}

@Composable
fun SetupScreen(onDone: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var steps by remember { mutableStateOf<List<SetupStep>>(emptyList()) }
    var checking by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }

    fun refresh() {
        checking = true
        scope.launch {
            steps = probe()
            checking = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val allOk = steps.isNotEmpty() && steps.all { it.ok }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Text("BASS ENGINE", color = p.text, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text("第一次使用 · 先接上全局引擎", color = p.accent3, fontSize = 12.sp, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(18.dp))

        GlassCard(Modifier.fillMaxWidth(), accent = p.accent2) {
            steps.forEach { step ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 7.dp)) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (step.ok) Color(0xFF00E5A0) else Color(0xFFFFB74D))
                    )
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(step.title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(step.hint, color = p.muted, fontSize = 10.sp)
                    }
                }
            }
            if (checking) {
                Text("检测中…", color = p.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
            if (message.isNotBlank()) {
                Text(message, color = p.accent1, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        GlassCard(Modifier.fillMaxWidth(), accent = p.accent1) {
            Text("授权方式", color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "点下面的按钮会调用 su，手机上会弹出 root 授权窗口，选「允许」；" +
                    "然后再点一次「重新检测」。模块和守护进程需要在 KernelSU 里装好模块并重启。",
                color = p.muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    scope.launch {
                        val out = withContext(Dispatchers.IO) { sh("id") }
                        message = if (out.contains("uid=0")) "Root 已授权" else "没拿到 root，请检查权限弹窗"
                        steps = probe()
                        checking = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = p.accent2),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) { Text("请求 Root 授权", fontSize = 12.sp) }
            TextButton(onClick = { refresh() }, modifier = Modifier.weight(1f)) {
                Text("重新检测", color = p.accent3, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (allOk) "" else "暂时跳过，先看看界面",
                color = p.muted,
                fontSize = 11.sp
            )
        }
        Button(
            onClick = onDone,
            enabled = allOk,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E5A0),
                contentColor = Color(0xFF062018),
                disabledContainerColor = Color.White.copy(alpha = 0.08f),
                disabledContentColor = p.muted
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp)
        ) {
            Text(if (allOk) "进入 App" else "全部就绪后即可进入", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
