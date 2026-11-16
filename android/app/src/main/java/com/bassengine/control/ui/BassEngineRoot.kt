package com.bassengine.control.ui

import android.content.Context

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import kotlin.math.roundToInt

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
    val shape = RoundedCornerShape(50)
    val density = LocalDensity.current
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val blobX = remember { Animatable(0f) }
    val target = dragIndex ?: selected

    LaunchedEffect(target, rowSize.width) {
        if (rowSize.width == 0) return@LaunchedEffect
        val itemW = rowSize.width / navItems.size.toFloat()
        val x = itemW * (target + 0.5f) - itemW * 0.86f / 2f
        if (!blobX.isRunning && blobX.value == 0f) blobX.snapTo(x)
        else blobX.animateTo(x, spring(dampingRatio = 0.42f, stiffness = 430f))
    }

    val glide by animateFloatAsState(
        targetValue = if (blobX.isRunning) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.32f, stiffness = 480f),
        label = "blobGlide"
    )
    val blobWidth = with(density) { (rowSize.width / navItems.size.toFloat() * 0.86f).toDp() }
    val blobHeight = with(density) { rowSize.height.toDp() }

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = p.accent2.copy(alpha = 0.22f)
            )
            .clip(shape)
            .background(Color(0xE60A0C12))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.035f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape
            )
    ) {
        // 镜面高光：顶部一条细亮线 + 玻璃内部的斜向反光
        Canvas(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)) {
            drawRect(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.55f),
                        Color.Transparent
                    )
                )
            )
        }
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.03f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )
        }

        Box(Modifier.fillMaxWidth().padding(6.dp)) {
            if (rowSize.width > 0) {
                Box(
                    Modifier
                        .offset { IntOffset(blobX.value.roundToInt(), 0) }
                        .size(width = blobWidth, height = blobHeight)
                        .graphicsLayer {
                            // 移动时沿速度方向拉长，停下回弹，做出“液体”感
                            scaleX = 1f + 0.10f * glide
                            scaleY = 1f - 0.12f * glide
                        }
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(50),
                            clip = false,
                            ambientColor = Color.Transparent,
                            spotColor = Color.White.copy(alpha = 0.28f)
                        )
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.30f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.035f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.52f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            ),
                            RoundedCornerShape(50)
                        )
                ) {
                    // 胶囊内的镜面反光
                    Canvas(Modifier.matchParentSize()) {
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.42f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.30f, size.height * 0.14f),
                                radius = size.width * 0.55f
                            ),
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, size.height * 0.72f)
                        )
                        // 斜向镜面条纹
                        val bandW = size.width * 0.22f
                        drawRect(
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.22f),
                                    Color.Transparent
                                ),
                                start = Offset(size.width * 0.30f, 0f),
                                end = Offset(size.width * 0.55f, size.height)
                            )
                        )
                        // 顶部 1px 高光
                        drawLine(
                            color = Color.White.copy(alpha = 0.55f),
                            start = Offset(size.width * 0.18f, 1f),
                            end = Offset(size.width * 0.82f, 1f),
                            strokeWidth = 1.2f
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { rowSize = it.size }
                    .pointerInput(navItems.size) {
                        detectTapGestures(
                            onLongPress = { },
                            onTap = { offset ->
                                val index = ((offset.x / size.width) * navItems.size)
                                    .toInt().coerceIn(0, navItems.lastIndex)
                                onSelect(index)
                            }
                        )
                    }
                    .pointerInput(navItems.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                dragIndex = ((offset.x / size.width) * navItems.size)
                                    .toInt().coerceIn(0, navItems.lastIndex)
                            },
                            onDrag = { change, _ ->
                                dragIndex = ((change.position.x / size.width) * navItems.size)
                                    .toInt().coerceIn(0, navItems.lastIndex)
                            },
                            onDragEnd = {
                                dragIndex?.let(onSelect)
                                dragIndex = null
                            },
                            onDragCancel = { dragIndex = null }
                        )
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    val active = selected == index
                    val hovered = dragIndex == index
                    val highlighted = active || hovered
                    val tint by animateColorAsState(
                        if (highlighted) p.accent3 else p.muted.copy(alpha = 0.80f),
                        label = "navTint"
                    )
                    val labelColor by animateColorAsState(
                        if (highlighted) p.accent3 else p.muted,
                        label = "navLabel"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (highlighted) 1.08f else 1f,
                        animationSpec = spring(dampingRatio = 0.35f, stiffness = 600f),
                        label = "navIconScale"
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            Modifier.padding(vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(item.icon),
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                            )
                            Text(
                                item.label,
                                color = labelColor,
                                fontSize = 10.sp,
                                fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Medium,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
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
