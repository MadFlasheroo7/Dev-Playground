package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pro.jayeshseth.bottomsheet.BottomSheetScaffoldState
import pro.jayeshseth.bottomsheet.BottomSheetState
import pro.jayeshseth.bottomsheet.CustomBottomSheetScaffold
import pro.jayeshseth.bottomsheet.CustomModalBottomSheet
import pro.jayeshseth.bottomsheet.DefaultBottomSheetDragHandle
import pro.jayeshseth.bottomsheet.SheetAnimationConfig
import pro.jayeshseth.bottomsheet.SheetDimension
import pro.jayeshseth.bottomsheet.SheetDragHandle
import pro.jayeshseth.bottomsheet.SheetDragHandleStyle
import pro.jayeshseth.bottomsheet.SheetValue
import pro.jayeshseth.bottomsheet.rememberBottomSheetScaffoldState
import pro.jayeshseth.bottomsheet.rememberBottomSheetState
import pro.jayeshseth.bottomsheet.sheetEdgeGlow
import kotlin.math.roundToInt

private enum class SheetMode { Modal, Scaffold }
private enum class AnimMode { Preset, SpringPhysics, TweenTiming }
private enum class AnimPreset { BouncySpring, SmoothSpring, FastTween, SlowTween }
private enum class ScrimColorPreset(val label: String, val color: Color) {
    Black("Black", Color.Black),
    Navy("Navy", Color(0xFF0D1B2A)),
    Violet("Violet", Color(0xFF1B0B2E)),
    Warm("Warm", Color(0xFF261C14)),
    Charcoal("Charcoal", Color(0xFF1F2421)),
    Indigo("Indigo", Color(0xFF1A1B41))
}

/**
 * Interactive playground demonstrating all bottom sheet capabilities built from scratch:
 * - Bespoke anchor and physics engine with velocity flings and rubber-band bounce.
 * - Coordinated nested scrolling with LazyColumn.
 * - Sizing with flexible SheetDimension (Dp, Fraction, WrapContent).
 * - Independent toggles for swipe-to-dismiss and scrim dismissal.
 * - Dynamic system status bar contrast switching.
 * - Android 14/15+ predictive back scaling and corner radius morphing.
 * - Shared transition element support (in-tree overlay without jumps or clipping).
 * - Dynamic content height observer.
 * - Pluggable shader glow and styling.
 * - Extensive customization controls: drag handle styles, scrim alpha/colors, physics specs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun BottomSheetLabScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(SheetMode.Modal) }

    // Configuration states
    var swipeToDismiss by remember { mutableStateOf(true) }
    var scrimDismiss by remember { mutableStateOf(true) }
    var enablePartial by remember { mutableStateOf(true) }
    var statusBarTint by remember { mutableStateOf(true) }
    var edgeGlowEnabled by remember { mutableStateOf(true) }
    var useWindowPopup by remember { mutableStateOf(false) }

    // Customization states
    var dragHandleStyle by remember { mutableStateOf(SheetDragHandleStyle.Pill) }
    var scrimAlpha by remember { mutableFloatStateOf(0.5f) }
    var scrimColorPreset by remember { mutableStateOf(ScrimColorPreset.Black) }

    // Animation physics states
    var animMode by remember { mutableStateOf(AnimMode.Preset) }
    var animPreset by remember { mutableStateOf(AnimPreset.BouncySpring) }
    var springDamping by remember { mutableFloatStateOf(Spring.DampingRatioMediumBouncy) }
    var springStiffness by remember { mutableFloatStateOf(Spring.StiffnessMediumLow) }
    var tweenDurationMs by remember { mutableFloatStateOf(350f) }

    // Sizing fractions
    var maxHeightFraction by remember { mutableFloatStateOf(0.90f) }
    var partialHeightFraction by remember { mutableFloatStateOf(0.55f) }
    var peekHeightDp by remember { mutableFloatStateOf(80f) }

    // Dynamic items list for dynamic height observer demo
    val dynamicItems = remember { mutableStateListOf("Card 1: Analytics", "Card 2: Performance") }

    // Nested Container Trap demo states
    var nestedSheetBreakout by remember { mutableStateOf(false) }
    var showMaterialSheet by remember { mutableStateOf(false) }
    val nestedSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    // Animation configuration dynamically computed from selected controls
    val animationConfig = remember(animMode, animPreset, springDamping, springStiffness, tweenDurationMs) {
        when (animMode) {
            AnimMode.Preset -> when (animPreset) {
                AnimPreset.BouncySpring -> SheetAnimationConfig(
                    enterSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    snapSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                    exitSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                )
                AnimPreset.SmoothSpring -> SheetAnimationConfig(
                    enterSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                    snapSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                    exitSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                )
                AnimPreset.FastTween -> SheetAnimationConfig(
                    enterSpec = tween(durationMillis = 220),
                    snapSpec = tween(durationMillis = 200),
                    exitSpec = tween(durationMillis = 220)
                )
                AnimPreset.SlowTween -> SheetAnimationConfig(
                    enterSpec = tween(durationMillis = 600),
                    snapSpec = tween(durationMillis = 550),
                    exitSpec = tween(durationMillis = 600)
                )
            }
            AnimMode.SpringPhysics -> SheetAnimationConfig(
                enterSpec = spring(dampingRatio = springDamping, stiffness = springStiffness),
                snapSpec = spring(dampingRatio = springDamping, stiffness = springStiffness),
                exitSpec = spring(dampingRatio = springDamping, stiffness = springStiffness)
            )
            AnimMode.TweenTiming -> SheetAnimationConfig(
                enterSpec = tween(durationMillis = tweenDurationMs.roundToInt()),
                snapSpec = tween(durationMillis = (tweenDurationMs * 0.85f).roundToInt()),
                exitSpec = tween(durationMillis = tweenDurationMs.roundToInt())
            )
        }
    }

    // Modal sheet state
    val modalSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        animationConfig = animationConfig
    )

    // Scaffold state
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Collapsed,
            skipHiddenState = true,
            animationConfig = animationConfig
        )
    )

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val sharedTransitionScope = this

        if (mode == SheetMode.Modal) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Bottom Sheet Lab") },
                            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                            actions = {
                                FilterChip(
                                    selected = false,
                                    onClick = { mode = SheetMode.Scaffold },
                                    label = { Text("Switch to Scaffold") }
                                )
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Main screen controls
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hero Trigger Card (In-Tree Shared Element Transition when !useWindowPopup)
                        val isSharedTransitionActive = !useWindowPopup
                        if (isSharedTransitionActive) {
                            val isBaseCardVisible = modalSheetState.targetValue == SheetValue.Hidden
                            AnimatedVisibility(
                                visible = isBaseCardVisible,
                                enter = fadeIn(tween(220)),
                                exit = fadeOut(tween(180))
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .sharedBounds(
                                            sharedContentState = rememberSharedContentState("hero-card-container"),
                                            animatedVisibilityScope = this@AnimatedVisibility,
                                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(20.dp))
                                        )
                                        .clickable {
                                            scope.launch { modalSheetState.expand() }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .sharedElement(
                                                    sharedContentState = rememberSharedContentState("hero-card-icon"),
                                                    animatedVisibilityScope = this@AnimatedVisibility
                                                )
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("⚡", fontSize = 24.sp)
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "Custom Modal Sheet Engine",
                                                modifier = Modifier.sharedElement(
                                                    sharedContentState = rememberSharedContentState("hero-card-title"),
                                                    animatedVisibilityScope = this@AnimatedVisibility
                                                ),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Shared Transition Active • Tap to Expand",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Window Popup Mode Card (no cross-window lookahead registration)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { modalSheetState.expand() }
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("⚡", fontSize = 24.sp)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Custom Modal Sheet Engine",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Window Popup Active (Cross-window lookahead disabled) • Tap to Expand",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { scope.launch { modalSheetState.expand() } },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Expand Sheet")
                            }
                            if (enablePartial) {
                                OutlinedButton(
                                    onClick = { scope.launch { modalSheetState.partialExpand() } },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Partial (${(partialHeightFraction * 100).roundToInt()}%)")
                                }
                            }
                        }

                        // Configuration Controls Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Engine Configuration & Customization", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                                // Toggles
                                SettingToggle(title = "Swipe to Dismiss", checked = swipeToDismiss, onCheckedChange = { swipeToDismiss = it })
                                SettingToggle(title = "Scrim Tap to Dismiss", checked = scrimDismiss, onCheckedChange = { scrimDismiss = it })
                                SettingToggle(title = "Partial Expand Anchor", checked = enablePartial, onCheckedChange = { enablePartial = it })
                                SettingToggle(title = "Dynamic Status Bar Tinting", checked = statusBarTint, onCheckedChange = { statusBarTint = it })
                                SettingToggle(title = "Shader Top Edge Glow", checked = edgeGlowEnabled, onCheckedChange = { edgeGlowEnabled = it })
                                SettingToggle(title = "Use Window Popup (Material style)", checked = useWindowPopup, onCheckedChange = { useWindowPopup = it })

                                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                                // Drag Handle Style Selector
                                Text("Drag Handle Style", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SheetDragHandleStyle.entries.forEach { style ->
                                        FilterChip(
                                            selected = dragHandleStyle == style,
                                            onClick = { dragHandleStyle = style },
                                            label = { Text(style.name, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                                // Scrim Controls
                                Text("Scrim Opacity: ${(scrimAlpha * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Slider(
                                    value = scrimAlpha,
                                    onValueChange = { scrimAlpha = it },
                                    valueRange = 0.0f..1.0f
                                )

                                Text("Scrim Color", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ScrimColorPreset.entries.forEach { preset ->
                                        FilterChip(
                                            selected = scrimColorPreset == preset,
                                            onClick = { scrimColorPreset = preset },
                                            label = { Text(preset.label, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                                // Height Fractions
                                Text("Max (Expanded) Height: ${(maxHeightFraction * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Slider(
                                    value = maxHeightFraction,
                                    onValueChange = { newMax ->
                                        maxHeightFraction = newMax
                                        if (partialHeightFraction >= newMax - 0.05f) {
                                            partialHeightFraction = (newMax - 0.05f).coerceAtLeast(0.20f)
                                        }
                                    },
                                    valueRange = 0.40f..1.0f
                                )

                                if (enablePartial) {
                                    val maxPartial = (maxHeightFraction - 0.05f).coerceAtLeast(0.25f)
                                    val clampedPartial = partialHeightFraction.coerceIn(0.20f, maxPartial)
                                    Text("Partial Height: ${(clampedPartial * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Slider(
                                        value = clampedPartial,
                                        onValueChange = { partialHeightFraction = it.coerceIn(0.20f, maxPartial) },
                                        valueRange = 0.20f..maxPartial
                                    )
                                }

                                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                                // Animation Spec Controls
                                Text("Animation Physics Mode", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AnimMode.entries.forEach { modeChoice ->
                                        FilterChip(
                                            selected = animMode == modeChoice,
                                            onClick = { animMode = modeChoice },
                                            label = { Text(modeChoice.name, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                when (animMode) {
                                    AnimMode.Preset -> {
                                        Text("Preset Curve", style = MaterialTheme.typography.bodySmall)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AnimPreset.entries.forEach { preset ->
                                                FilterChip(
                                                    selected = animPreset == preset,
                                                    onClick = { animPreset = preset },
                                                    label = { Text(preset.name, fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                    AnimMode.SpringPhysics -> {
                                        val dampingPercent = (springDamping * 100).roundToInt()
                                        Text("Spring Damping Ratio: ${dampingPercent / 100f} (${if (springDamping < 0.4f) "Very Bouncy" else if (springDamping < 0.8f) "Bouncy" else "No Bounce"})", style = MaterialTheme.typography.bodySmall)
                                        Slider(
                                            value = springDamping,
                                            onValueChange = { springDamping = it },
                                            valueRange = 0.15f..1.0f
                                        )

                                        Text("Spring Stiffness: ${springStiffness.roundToInt()} (${if (springStiffness < 200f) "Gentle" else if (springStiffness < 800f) "Medium" else "Snappy"})", style = MaterialTheme.typography.bodySmall)
                                        Slider(
                                            value = springStiffness,
                                            onValueChange = { springStiffness = it },
                                            valueRange = 50f..2000f
                                        )
                                    }
                                    AnimMode.TweenTiming -> {
                                        Text("Tween Duration: ${tweenDurationMs.roundToInt()} ms", style = MaterialTheme.typography.bodySmall)
                                        Slider(
                                            value = tweenDurationMs,
                                            onValueChange = { tweenDurationMs = it },
                                            valueRange = 100f..1500f
                                        )
                                    }
                                }
                            }
                        }

                        // Architecture Features Overview Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Engine Architecture Highlights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                ArchitectureBullet("In-Tree Shared Transitions", "Zero window detachment allows SharedTransitionLayout to morph cards across screen and sheet boundaries without jump or clipping.")
                                ArchitectureBullet("Accurate Layout Placement", "Modifier.offset tracks dynamic physics synchronously so child coordinate systems reflect exact screen positions.")
                                ArchitectureBullet("Directional Fling Physics", "Release velocity determines whether to snap, expand, or dismiss smoothly with rubber-band bounds.")
                                ArchitectureBullet("Coordinated Nested Scroll", "Inner LazyColumn hands off pre-scroll to sheet, and post-scroll to collapse seamlessly.")
                                ArchitectureBullet("Predictive Back Morphing", "Interactively scales, shifts downward, and morphs top corner radii during Android 14/15 back swipes.")
                            }
                        }

                        // Nested Container Trap Showcase Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🪤 Nested Container Trap Demo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                Text(
                                    "This card demonstrates why Window Popup exists. The sandbox below is clipped to 220.dp with rounded corners:",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                SettingToggle(
                                    title = "useWindowPopup = true (Break out of box)",
                                    checked = nestedSheetBreakout,
                                    onCheckedChange = { nestedSheetBreakout = it }
                                )

                                // The Mini Clipped Sandbox Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF1E1E2E))
                                        .border(1.5.dp, if (nestedSheetBreakout) Color(0xFF81C784) else Color(0xFFFFB74D), RoundedCornerShape(20.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            if (nestedSheetBreakout) "🚀 Mode: WINDOW POPUP (Breakout)" else "📦 Mode: IN-TREE (Trapped inside box)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (nestedSheetBreakout) Color(0xFF81C784) else Color(0xFFFFB74D)
                                        )
                                        Text(
                                            if (nestedSheetBreakout) "Sheet will escape this box and cover the full screen" else "Sheet & scrim will be clipped and boxed inside here!",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { scope.launch { nestedSheetState.expand() } },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (nestedSheetBreakout) Color(0xFF2E7D32) else Color(0xFFE65100)
                                                )
                                            ) {
                                                Text(if (nestedSheetBreakout) "Custom (Breakout)" else "Custom (Trapped)")
                                            }
                                            OutlinedButton(
                                                onClick = { showMaterialSheet = true }
                                            ) {
                                                Text("Open M3 Sheet")
                                            }
                                        }
                                    }

                                    // Official Material 3 ModalBottomSheet called from inside this same 220dp box
                                    if (showMaterialSheet) {
                                        androidx.compose.material3.ModalBottomSheet(
                                            onDismissRequest = { showMaterialSheet = false },
                                            containerColor = Color(0xFF1E1E2E)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    "🎨 Official Material 3 ModalBottomSheet",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    "Notice: Material 3 always creates an OS Dialog Window, so even though it was invoked inside the 220dp clipped box, it breaks out and covers the full screen.\n\nOur custom sheet with useWindowPopup = true now replicates this exact same behavior.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 12.sp
                                                )
                                                Button(onClick = { showMaterialSheet = false }) {
                                                    Text("Dismiss Material 3 Sheet")
                                                }
                                            }
                                        }
                                    }

                                    // The Nested Bottom Sheet called directly inside this clipped Box!
                                    CustomModalBottomSheet(
                                        onDismissRequest = { scope.launch { nestedSheetState.hide() } },
                                        modifier = Modifier.fillMaxSize(),
                                        sheetState = nestedSheetState,
                                        useWindowPopup = nestedSheetBreakout,
                                        containerColor = Color(0xFF2A2A3E),
                                        scrimColor = Color.Black.copy(alpha = 0.6f)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                if (nestedSheetBreakout) "🎉 Full Window Overlay!" else "⚠️ Trapped Inside The Box!",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                if (nestedSheetBreakout)
                                                    "Because useWindowPopup = true, this sheet escaped the 220dp clipped box and covers the entire screen like Material 3."
                                                else
                                                    "Because useWindowPopup = false, this sheet cannot escape its parent Box! Its scrim and height are stuck inside the 220dp container.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp
                                            )
                                            Button(onClick = { scope.launch { nestedSheetState.hide() } }) {
                                                Text("Dismiss Mini Sheet")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom Modal Bottom Sheet drawn above Scaffold and TopAppBar
                CustomModalBottomSheet(
                    onDismissRequest = { /* Dismissed */ },
                    modifier = Modifier.fillMaxSize(),
                    sheetState = modalSheetState,
                    maxHeight = SheetDimension.Fraction(maxHeightFraction),
                    partiallyExpandedHeight = if (enablePartial) SheetDimension.Fraction(partialHeightFraction.coerceAtMost(maxHeightFraction - 0.05f)) else null,
                    swipeToDismissEnabled = swipeToDismiss,
                    scrimDismissEnabled = scrimDismiss,
                    scrimColor = scrimColorPreset.color.copy(alpha = scrimAlpha),
                    dragHandle = { SheetDragHandle(style = dragHandleStyle) },
                    statusBarTintEnabled = statusBarTint,
                    animationConfig = animationConfig,
                    containerColor = Color(0xFF161622),
                    sheetModifier = if (edgeGlowEnabled) {
                        Modifier.sheetEdgeGlow(color = Color(0xFF4FC3F7), glowHeight = 24.dp, intensity = 0.85f)
                    } else Modifier,
                    useWindowPopup = useWindowPopup
                ) {
                    SheetBodyContent(
                        sheetState = modalSheetState,
                        dynamicItems = dynamicItems,
                        sharedTransitionScope = if (useWindowPopup) null else sharedTransitionScope,
                        onClose = { scope.launch { modalSheetState.hide() } }
                    )
                }
            }
        } else {
            // Scaffold Variant
            CustomBottomSheetScaffold(
                scaffoldState = scaffoldState,
                peekHeight = SheetDimension.Dp(peekHeightDp.dp),
                maxHeight = SheetDimension.Fraction(maxHeightFraction),
                partiallyExpandedHeight = if (enablePartial) SheetDimension.Fraction(partialHeightFraction.coerceAtMost(maxHeightFraction - 0.05f)) else null,
                swipeToDismissEnabled = false,
                dragHandle = { SheetDragHandle(style = dragHandleStyle) },
                animationConfig = animationConfig,
                statusBarTintEnabled = statusBarTint,
                containerColor = Color(0xFF161622),
                sheetModifier = if (edgeGlowEnabled) {
                    Modifier.sheetEdgeGlow(color = Color(0xFFA5D6A7), glowHeight = 24.dp, intensity = 0.85f)
                } else Modifier,
                topBar = {
                    TopAppBar(
                        title = { Text("Scaffold Bottom Sheet") },
                        navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                        actions = {
                            FilterChip(
                                selected = false,
                                onClick = { mode = SheetMode.Modal },
                                label = { Text("Switch to Modal") }
                            )
                        }
                    )
                },
                sheetContent = {
                    SheetBodyContent(
                        sheetState = scaffoldState.bottomSheetState,
                        dynamicItems = dynamicItems,
                        sharedTransitionScope = sharedTransitionScope,
                        onClose = { scope.launch { scaffoldState.bottomSheetState.collapse() } }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Persistent Scaffold Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "The sheet stays visible at peek height (${peekHeightDp.toInt()} dp) while base content remains interactive.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Expand")
                        }
                        Button(
                            onClick = { scope.launch { scaffoldState.bottomSheetState.collapse() } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Collapse to Peek")
                        }
                    }

                    // Peek Height Slider
                    Text("Peek Height: ${peekHeightDp.toInt()} dp", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = peekHeightDp,
                        onValueChange = { peekHeightDp = it },
                        valueRange = 40f..160f
                    )

                    // Trigger Snackbar
                    Button(
                        onClick = {
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar("Snackbar displayed above persistent sheet!")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Scaffold Snackbar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SheetBodyContent(
    sheetState: BottomSheetState,
    dynamicItems: MutableList<String>,
    sharedTransitionScope: SharedTransitionScope?,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Shared Element Header Destination (active when in-tree, fallback card when in Window Popup mode)
        if (sharedTransitionScope != null) {
            with(sharedTransitionScope) {
                val isSheetCardVisible = sheetState.targetValue != SheetValue.Hidden
                AnimatedVisibility(
                    visible = isSheetCardVisible,
                    enter = fadeIn(tween(220)),
                    exit = fadeOut(tween(180))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState("hero-card-container"),
                                animatedVisibilityScope = this@AnimatedVisibility,
                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                            ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("hero-card-icon"),
                                        animatedVisibilityScope = this@AnimatedVisibility
                                    )
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Custom Modal Sheet Engine",
                                    modifier = Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState("hero-card-title"),
                                        animatedVisibilityScope = this@AnimatedVisibility
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Shared transition morph active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF81D4FA)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Standard Card for Window Popup mode (separate OS window hierarchy)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Custom Modal Sheet Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Window Popup Active (Cross-window lookahead disabled)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF81D4FA)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Telemetry HUD Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF222233)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LIVE ENGINE TELEMETRY", style = MaterialTheme.typography.labelSmall, color = Color(0xFF81D4FA), fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClose) { Text("Dismiss", color = Color(0xFFFF8A80), fontSize = 12.sp) }
                }
                TelemetryRow("Current Anchor", sheetState.currentValue.name)
                TelemetryRow("Target Anchor", sheetState.targetValue.name)
                TelemetryRow("Offset (px)", "${sheetState.offset?.toInt() ?: 0} px")
                TelemetryRow("Expansion Progress", "${(sheetState.progress * 100).toInt()}%")
                TelemetryRow("Predictive Back", if (sheetState.isPredictiveBackActive) "ACTIVE (${(sheetState.predictiveBackProgress * 100).toInt()}%)" else "Idle")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Dynamic Height Observer Demo Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dynamic Content Observer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { dynamicItems.add("Card ${dynamicItems.size + 1}: Dynamic") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("+ Add", fontSize = 12.sp)
                }
                if (dynamicItems.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { dynamicItems.removeLastOrNull() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("- Remove", fontSize = 12.sp)
                    }
                }
            }
        }

        // Display dynamic items
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dynamicItems.forEach { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A2A3E))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(item, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Nested Scroll Verification List (LazyColumn)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            "Scroll up expands sheet first; scroll down when at top collapses sheet.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(Modifier.height(6.dp))

        // Nested Scroll List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(30) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1E2F))
                        .border(1.dp, Color(0xFF33334D), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("List Element #${index + 1}", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Drag or Scroll ↕", color = Color(0xFF64B5F6), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color(0xFFFFD54F))
    }
}

@Composable
private fun SettingToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ArchitectureBullet(title: String, description: String) {
    Column {
        Text("• $title", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
