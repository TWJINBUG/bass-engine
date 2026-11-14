package com.bassengine.control.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bassengine.control.R
import com.bassengine.control.ui.screens.BassScreen
import com.bassengine.control.ui.screens.HomeScreen
import com.bassengine.control.ui.screens.SettingsScreen
import com.bassengine.control.ui.screens.SpectrumScreen
import com.bassengine.control.ui.screens.SurroundScreen
import com.bassengine.control.ui.theme.BassBackground
import com.bassengine.control.ui.theme.BassTheme
import com.bassengine.control.ui.theme.LocalPalette
import kotlinx.coroutines.delay

private data class NavItem(val label: String, val icon: Int)

private val navItems = listOf(
    NavItem("主页", R.drawable.ic_tab_home),
    NavItem("低频", R.drawable.ic_tab_bass),
    NavItem("环绕", R.drawable.ic_tab_eq),
    NavItem("频谱", R.drawable.ic_tab_spectrum),
    NavItem("设置", R.drawable.ic_tab_settings)
)

@Composable
fun BassEngineRoot() {
    val vm: EngineViewModel = viewModel()
    val config by vm.config.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val global by vm.global.collectAsStateWithLifecycle()
    val route by vm.route.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(config.lastTab.coerceIn(0, 4)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            now = System.currentTimeMillis()
        }
    }

    BassTheme(config.theme) {
        Box(Modifier.fillMaxSize()) {
            BassBackground()
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    EngineNavBar(selected = tab) { index ->
                        tab = index
                        vm.update { it.copy(lastTab = index) }
                    }
                }
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when (tab) {
                        0 -> HomeScreen(config, global, route, now, vm)
                        1 -> BassScreen(config, vm)
                        2 -> SurroundScreen(config, vm)
                        3 -> SpectrumScreen(config, logs, vm)
                        else -> SettingsScreen(config, logs, vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineNavBar(selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(16.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF20E1420), Color(0xF2090D16))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.14f),
                        p.accent2.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEachIndexed { index, item ->
            val active = selected == index
            val tint by animateColorAsState(
                if (active) p.accent3 else p.muted.copy(alpha = 0.72f),
                label = "navTint"
            )
            val labelColor by animateColorAsState(
                if (active) p.text else p.muted,
                label = "navLabel"
            )
            val pillColor by animateColorAsState(
                if (active) p.accent2.copy(alpha = 0.20f) else Color.Transparent,
                label = "navPill"
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(pillColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(index) }
                        .padding(horizontal = 17.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        item.label,
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}

