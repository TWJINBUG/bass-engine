package com.bassengine.control.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class NavItem(val label: String, val icon: Int)

private val navItems = listOf(
    NavItem("主页", R.drawable.ic_tab_home),
    NavItem("低频", R.drawable.ic_tab_bass),
    NavItem("环绕", R.drawable.ic_tab_eq),
    NavItem("频谱", R.drawable.ic_tab_spectrum),
    NavItem("设置", R.drawable.ic_tab_settings)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BassEngineRoot() {
    val vm: EngineViewModel = viewModel()
    val config by vm.config.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val global by vm.global.collectAsStateWithLifecycle()
    val route by vm.route.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val pagerState = rememberPagerState(
        initialPage = config.lastTab.coerceIn(0, navItems.lastIndex)
    ) { navItems.size }
    val scope = rememberCoroutineScope()
    val tab = pagerState.currentPage

    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            now = System.currentTimeMillis()
        }
    }

    // Persist the selected tab only once the swipe settles.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .collect { (page, scrolling) ->
                if (!scrolling && page != vm.config.value.lastTab) {
                    vm.update { it.copy(lastTab = page) }
                }
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
                        scope.launch {
                            pagerState.animateScrollToPage(
                                index,
                                animationSpec = tween(320, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                }
            ) { padding ->
                HorizontalPager(
                    state = pagerState,
                    beyondBoundsPageCount = 1,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) { page ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val distance = (pagerState.currentPage - page) +
                                    pagerState.currentPageOffsetFraction
                                val d = distance.absoluteValue.coerceIn(0f, 1f)
                                alpha = 1f - d * 0.22f
                                val scale = 1f - d * 0.03f
                                scaleX = scale
                                scaleY = scale
                                translationY = d * 10.dp.toPx()
                            }
                    ) {
                        when (page) {
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
}

@Composable
private fun EngineNavBar(selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(30.dp)
    val sheenTransition = rememberInfiniteTransition(label = "navSheen")
    val sheen by sheenTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "navSheenShift"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = p.accent2.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.115f),
                        Color.White.copy(alpha = 0.045f)
                    )
                )
            )
            .background(
                Brush.verticalGradient(
                    listOf(p.bg.deepen(0.35f).copy(alpha = 0.62f), p.bgDeep.copy(alpha = 0.78f))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.30f),
                        p.accent2.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val x = w * (sheen * 1.4f - 0.2f)
            drawRect(
                Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    start = Offset(x - w * 0.22f, 0f),
                    end = Offset(x + w * 0.22f, h)
                )
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
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
                val iconScale by animateFloatAsState(
                    targetValue = if (active) 1.08f else 1f,
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    label = "navIconScale"
                )
                val pillBrush = if (active) {
                    Brush.verticalGradient(
                        listOf(
                            p.accent2.copy(alpha = 0.26f),
                            p.accent2.copy(alpha = 0.10f)
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(pillBrush)
                            .then(
                                if (active) {
                                    Modifier.border(
                                        1.dp,
                                        p.accent3.copy(alpha = 0.18f),
                                        RoundedCornerShape(18.dp)
                                    )
                                } else Modifier
                            )
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
                            modifier = Modifier
                                .size(21.dp)
                                .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
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
}

private fun Color.deepen(factor: Float): Color = Color(
    red = red * (1f - factor),
    green = green * (1f - factor),
    blue = blue * (1f - factor),
    alpha = alpha
)
