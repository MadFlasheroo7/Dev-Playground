package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pro.jayeshseth.bottomnav.BottomNavigationBar
import pro.jayeshseth.bottomnav.NavigationBarLayout
import pro.jayeshseth.bottomnav.SegmentedButtonRow
import pro.jayeshseth.bottomnav.SegmentedButtonLayout
import pro.jayeshseth.bottomnav.rememberBottomNavBarState

/**
 * Interactive playground for [BottomNavigationBar] and [NavigationBarLayout].
 *
 * All spring-physics, tween configurations, and appearance parameters are exposed
 * as sliders so you can tweak the feel in real time.
 *
 * In this design, the animation transitions are executed from the client's end.
 * The core components are completely passive, exposing their animatable properties
 * inside the state holder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedControlLabScreen(onBack: () -> Unit) {
    var items               by remember { mutableStateOf(listOf("Day", "Week", "Month", "Year")) }
    var sel                 by remember { mutableIntStateOf(0) }
    var instantDragMode     by remember { mutableStateOf(false) }
    var selectedVariant     by remember { mutableIntStateOf(0) } // 0: Standard Box, 1: Custom Layout, 2: Custom Content
    var isScrollable        by remember { mutableStateOf(false) }
    var useStretchyAnimation by remember { mutableStateOf(false) }
    var useTweenAnimation    by remember { mutableStateOf(false) }

    // ── Physics & appearance params wired to playground sliders ──────────────
    var dampingRatio       by remember { mutableFloatStateOf(0.62f) }
    var stiffness          by remember { mutableFloatStateOf(400f) }
    var tweenDuration      by remember { mutableFloatStateOf(400f) }
    var maxScaleMultiplier by remember { mutableFloatStateOf(1.16f) }
    var wobbleAmplitude    by remember { mutableFloatStateOf(0.035f) }
    var cornerRadiusDp     by remember { mutableFloatStateOf(28f) }
    var barHeightDp        by remember { mutableFloatStateOf(56f) }

    // Hoisted animation state holder from the library package
    val state = rememberBottomNavBarState(initialIndex = sel)

    // Remember the index where the drag started to prevent retraction during active slide
    var dragStartIndex by remember { mutableStateOf<Float?>(null) }

    // Monitor drag state changes and capture the starting position
    LaunchedEffect(state.isDragging) {
        if (state.isDragging) {
            dragStartIndex = state.selectedIndex.toFloat()
        } else {
            dragStartIndex = null
        }
    }

    // Synchronize UI-driven selectedIndex changes back to state selection
    LaunchedEffect(sel) {
        state.selectedIndex = sel
    }

    // Synchronize state-driven (drag-release) selections back to our local UI state
    LaunchedEffect(state.selectedIndex) {
        sel = state.selectedIndex
    }

    // ── Client-side Drag Interpolation ──────────────────────────────────────────
    // Listens to gesture position updates during drag and updates the pill boundaries.
    LaunchedEffect(state.position.value, state.isDragging, dragStartIndex) {
        val target = state.position.value
        val start = dragStartIndex
        if (state.isDragging && start != null) {
            if (useStretchyAnimation) {
                // Drag stretch: leading edge snaps to finger, trailing edge is locked to start index
                if (target > start) {
                    state.rightPosition.snapTo(target)
                    state.leftPosition.snapTo(start)
                } else {
                    state.leftPosition.snapTo(target)
                    state.rightPosition.snapTo(start)
                }
            } else {
                // Drag slide: snap both edges instantly to follow finger in lockstep
                state.leftPosition.snapTo(target)
                state.rightPosition.snapTo(target)
            }
        }
    }

    // ── Client-side Transition Animations ──────────────────────────────────────
    // Listens to programmatic selection changes and animates the pill boundaries.
    LaunchedEffect(state.selectedIndex, state.isDragging) {
        if (!state.isDragging) {
            val target = state.selectedIndex.toFloat()
            
            if (useTweenAnimation) {
                // Tween Animation Mode
                val duration = tweenDuration.toInt()
                if (useStretchyAnimation) {
                    val currentLeft = state.leftPosition.value

                    // Keep target position in sync
                    launch {
                        state.position.animateTo(
                            targetValue = target,
                            animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                        )
                    }

                    if (target > currentLeft) {
                        // Moving Right: right boundary moves fast, left boundary follows with a delayed tween
                        launch {
                            state.rightPosition.animateTo(
                                targetValue = target,
                                animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                            )
                        }
                        launch {
                            state.leftPosition.animateTo(
                                targetValue = target,
                                animationSpec = tween(
                                    durationMillis = duration,
                                    delayMillis = (duration * 0.3f).toInt(),
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    } else {
                        // Moving Left: left boundary moves fast, right boundary follows with a delayed tween
                        launch {
                            state.leftPosition.animateTo(
                                targetValue = target,
                                animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                            )
                        }
                        launch {
                            state.rightPosition.animateTo(
                                targetValue = target,
                                animationSpec = tween(
                                    durationMillis = duration,
                                    delayMillis = (duration * 0.3f).toInt(),
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    }
                } else {
                    // Normal sliding mode with tween
                    val tweenSpec = tween<Float>(durationMillis = duration, easing = FastOutSlowInEasing)
                    launch { state.position.animateTo(target, tweenSpec) }
                    launch { state.leftPosition.animateTo(target, tweenSpec) }
                    launch { state.rightPosition.animateTo(target, tweenSpec) }
                }
            } else {
                // Spring Animation Mode
                if (useStretchyAnimation) {
                    val currentLeft = state.leftPosition.value

                    // Keep target position in sync
                    launch {
                        state.position.animateTo(target, spring(dampingRatio = dampingRatio, stiffness = stiffness))
                    }

                    if (target > currentLeft) {
                        // Moving Right: right boundary moves fast, left boundary follows with a slower spring
                        launch {
                            state.rightPosition.animateTo(target, spring(dampingRatio = dampingRatio, stiffness = stiffness))
                        }
                        launch {
                            state.leftPosition.animateTo(target, spring(dampingRatio = dampingRatio, stiffness = stiffness * 0.45f))
                        }
                    } else {
                        // Moving Left: left boundary moves fast, right boundary follows with a slower spring
                        launch {
                            state.leftPosition.animateTo(target, spring(dampingRatio = dampingRatio, stiffness = stiffness))
                        }
                        launch {
                            state.rightPosition.animateTo(target, spring(dampingRatio = dampingRatio, stiffness = stiffness * 0.45f))
                        }
                    }
                } else {
                    // Normal sliding mode with spring
                    val springSpec = spring<Float>(dampingRatio = dampingRatio, stiffness = stiffness)
                    launch { state.position.animateTo(target, springSpec) }
                    launch { state.leftPosition.animateTo(target, springSpec) }
                    launch { state.rightPosition.animateTo(target, springSpec) }
                }
            }
        }
    }

    // ── Pill overshoot squish/scale transition ──────────────────────────────────
    LaunchedEffect(state.selectedIndex) {
        launch {
            state.scale.animateTo(0.88f, spring(stiffness = stiffness))
            state.scale.animateTo(maxScaleMultiplier, spring(dampingRatio = dampingRatio, stiffness = stiffness))
            state.scale.animateTo(1.06f, spring(dampingRatio = 0.75f, stiffness = stiffness))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bottom Navigation Bar") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Live component preview ────────────────────────────────────────
            when (selectedVariant) {
                1 -> {
                    NavigationBarLayout(
                        state              = state,
                        wobbleAmplitude    = wobbleAmplitude,
                        cornerRadius       = cornerRadiusDp.dp,
                        height             = barHeightDp.dp,
                        instantDragMode    = instantDragMode,
                        isScrollable       = isScrollable,
                    ) {
                        items.forEachIndexed { i, label ->
                            item(
                                label    = label,
                                onClick  = { sel = i },
                            )
                        }
                    }
                }
                2 -> {
                    val icons = listOf("☀️", "📅", "🌙", "⭐", "🚀", "🎨", "⚡", "🔥")
                    val customItems = items.mapIndexed { idx, name ->
                        (icons.getOrNull(idx) ?: "📌") to name
                    }
                    SegmentedButtonRow(
                        state              = state,
                        itemCount          = customItems.size,
                        wobbleAmplitude    = wobbleAmplitude,
                        cornerRadius       = cornerRadiusDp.dp,
                        height             = barHeightDp.dp,
                        instantDragMode    = instantDragMode,
                        isScrollable       = isScrollable,
                        onItemSelected     = { sel = it }
                    ) {
                        for ((i, pair) in customItems.withIndex()) {
                            val (icon, label) = pair
                            val isSelected = sel == i
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable { sel = i },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = icon,
                                        fontSize = if (isSelected) 17.sp else 14.sp
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
                3 -> {
                    val icons = listOf("⚡", "🎨", "🚀", "💎", "⭐", "🔥", "🔮", "🌈")
                    val customItems = items.mapIndexed { idx, name ->
                        (icons.getOrNull(idx) ?: "📍") to name
                    }
                    SegmentedButtonLayout(
                        state              = state,
                        itemCount          = customItems.size,
                        wobbleAmplitude    = wobbleAmplitude,
                        cornerRadius       = cornerRadiusDp.dp,
                        height             = barHeightDp.dp,
                        instantDragMode    = instantDragMode,
                        isScrollable       = isScrollable,
                        onItemSelected     = { sel = it }
                    ) {
                        for ((i, pair) in customItems.withIndex()) {
                            val (icon, label) = pair
                            val isSelected = sel == i
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable { sel = i },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = icon,
                                        fontSize = if (isSelected) 17.sp else 14.sp
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    BottomNavigationBar(
                        state              = state,
                        wobbleAmplitude    = wobbleAmplitude,
                        cornerRadius       = cornerRadiusDp.dp,
                        height             = barHeightDp.dp,
                        instantDragMode    = instantDragMode,
                        isScrollable       = isScrollable,
                    ) {
                        items.forEachIndexed { i, label ->
                            item(
                                label    = label,
                                onClick  = { sel = i },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Variant Selector Cards
            Text(
                "Component Variant",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val variantLabels = listOf("V1 (Box)", "V2 (Layout)", "V3 (Custom Row)", "V4 (Custom Layout)")
                for ((index, label) in variantLabels.withIndex()) {
                    val isSelected = selectedVariant == index
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedVariant = index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            // Add / Remove items control row
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nav Items (${items.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Dynamically add or remove tabs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (items.size > 1) {
                                val newItems = items.dropLast(1)
                                items = newItems
                                if (sel >= newItems.size) {
                                    sel = newItems.size - 1
                                }
                            }
                        },
                        enabled = items.size > 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("➖ Remove")
                    }
                    Button(
                        onClick = {
                            if (items.size < 8) {
                                val nextLabels = listOf("Tab 5", "Tab 6", "Tab 7", "Tab 8")
                                val newLabel = nextLabels.getOrNull(items.size - 4) ?: "Tab ${items.size + 1}"
                                items = items + newLabel
                            }
                        },
                        enabled = items.size < 8
                    ) {
                        Text("➕ Add Item")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Selected: ${items[sel]}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(28.dp))
            Text(
                "Playground Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            // ── Sliders card ─────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Stretchy animation toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Stretchy Animation Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Stretch the pill indicator between tabs during transitions and drag gestures.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(checked = useStretchyAnimation, onCheckedChange = { useStretchyAnimation = it })
                    }

                    // Tween / Spring animation selector toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Use Tween Animation",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Toggle between Tween (FastOutSlowIn) and Spring physics spec.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(checked = useTweenAnimation, onCheckedChange = { useTweenAnimation = it })
                    }

                    if (useTweenAnimation) {
                        PlaygroundSlider(
                            label      = "Tween Duration",
                            valueLabel = "${tweenDuration.toInt()} ms",
                            value      = tweenDuration,
                            onValueChange = { tweenDuration = it },
                            range      = 100f..1200f,
                        )
                    } else {
                        PlaygroundSlider(
                            label     = "Damping Ratio (Bounciness)",
                            valueLabel = when {
                                dampingRatio < 0.4f -> "Highly Jiggly (%.2f)".format(dampingRatio)
                                dampingRatio < 0.7f -> "Medium Bouncy (%.2f)".format(dampingRatio)
                                else                -> "Subtle (%.2f)".format(dampingRatio)
                            },
                            value      = dampingRatio,
                            onValueChange = { dampingRatio = it },
                            range      = 0.15f..1.0f,
                        )
                        PlaygroundSlider(
                            label      = "Stiffness (Speed)",
                            valueLabel = "${stiffness.toInt()} ms",
                            value      = stiffness,
                            onValueChange = { stiffness = it },
                            range      = 80f..1200f,
                        )
                    }

                    PlaygroundSlider(
                        label      = "Pill Bulge Scale",
                        valueLabel = "%.2fx".format(maxScaleMultiplier),
                        value      = maxScaleMultiplier,
                        onValueChange = { maxScaleMultiplier = it },
                        range      = 1.0f..1.4f,
                    )
                    PlaygroundSlider(
                        label      = "Wobble Amplitude (Sliding Jiggle)",
                        valueLabel = "%.1f%%".format(wobbleAmplitude * 1000 / 10f),
                        value      = wobbleAmplitude,
                        onValueChange = { wobbleAmplitude = it },
                        range      = 0.0f..0.08f,
                    )
                    PlaygroundSlider(
                        label      = "Corner Radius",
                        valueLabel = "${cornerRadiusDp.toInt()} dp",
                        value      = cornerRadiusDp,
                        onValueChange = { cornerRadiusDp = it },
                        range      = 0f..28f,
                    )
                    PlaygroundSlider(
                        label      = "Bar Height",
                        valueLabel = "${barHeightDp.toInt()} dp",
                        value      = barHeightDp,
                        onValueChange = { barHeightDp = it },
                        range      = 40f..80f,
                    )

                    // Instant drag mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Instant Slide Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Drag starts on first touch instead of long-press.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(checked = instantDragMode, onCheckedChange = { instantDragMode = it })
                    }

                    // Horizontal scrollable mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Horizontal Scrollable Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Enable smooth horizontal scrolling when tabs overflow screen bounds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(checked = isScrollable, onCheckedChange = { isScrollable = it })
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaygroundSlider(
    label: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(label,      fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(valueLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
