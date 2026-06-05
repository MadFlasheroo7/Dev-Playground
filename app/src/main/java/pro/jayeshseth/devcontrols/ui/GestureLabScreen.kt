package pro.jayeshseth.devcontrols.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Insets
import android.os.CancellationSignal
import android.view.WindowInsets as PlatformWindowInsets
import android.view.WindowInsetsAnimationControlListener
import android.view.WindowInsetsAnimationController
import android.view.animation.LinearInterpolator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

/**
 * Gesture Lab — three live demos of what an unprivileged app can and can't do with
 * Android's gesture navigation:
 *
 *  1. **Insets & exclusion** — visualise the `systemGestures` (overridable, 200dp
 *     cap) vs `mandatorySystemGestures` (the home-pill region you can never claim)
 *     inset regions, and feel `Modifier.systemGestureExclusion()` win the back-swipe.
 *  2. **Nav-bar inset control** — hide/show the bars and finger-drive the nav-bar
 *     inset animation via the platform `WindowInsetsController.controlWindowInsetsAnimation`.
 *  3. **Icon-morph** — the launcher "window springs back to its icon" effect, done
 *     legally for *in-app* content with a shared-element transition. The real
 *     cross-app version is platform-signature-only (see the note on that tab).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureLabScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gesture Lab") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(tab == 0, { tab = 0 }, { Text("Insets") })
                FilterChip(tab == 1, { tab = 1 }, { Text("Nav bar") })
                FilterChip(tab == 2, { tab = 2 }, { Text("Icon morph") })
            }
            when (tab) {
                0 -> InsetsSection()
                1 -> NavBarSection()
                else -> IconMorphSection()
            }
        }
    }
}

/* ---------------------------------------------------------------------------- */
/* 1. Gesture insets + exclusion rects                                          */
/* ---------------------------------------------------------------------------- */

@Composable
private fun InsetsSection() {
    val density = LocalDensity.current
    val ld = LocalLayoutDirection.current
    val sys = WindowInsets.systemGestures
    val mandatory = WindowInsets.mandatorySystemGestures

    fun WindowInsets.edgesDp(): String {
        val l = with(density) { getLeft(density, ld).toDp().value.roundToInt() }
        val t = with(density) { getTop(density).toDp().value.roundToInt() }
        val r = with(density) { getRight(density, ld).toDp().value.roundToInt() }
        val b = with(density) { getBottom(density).toDp().value.roundToInt() }
        return "L $l · T $t · R $r · B $b  (dp)"
    }

    var excludeOn by remember { mutableStateOf(true) }
    var offset by remember { mutableStateOf(IntOffset(0, 0)) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Reported gesture insets", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("systemGestures:    ${sys.edgesDp()}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("mandatorySystemGestures: ${mandatory.edgesDp()}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "systemGestures = the back-swipe edges you may override (capped at 200dp per " +
                        "edge). mandatorySystemGestures = the home/recents region you can never claim. " +
                        "Values may read 0 unless the activity is edge-to-edge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Apply systemGestureExclusion to the box", Modifier.weight(1f))
            Switch(excludeOn, { excludeOn = it })
        }
        Text(
            "Drag the box to the LEFT or RIGHT edge. With exclusion ON your drag wins; " +
                "OFF, the system eats it as a Back gesture.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxSize()) {
            val dragMod = Modifier
                .offset { offset }
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        offset = IntOffset(
                            offset.x + drag.x.roundToInt(),
                            offset.y + drag.y.roundToInt()
                        )
                    }
                }
            Box(if (excludeOn) dragMod.systemGestureExclusion() else dragMod) {
                Text("drag me", Modifier.align(Alignment.Center), color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

/* ---------------------------------------------------------------------------- */
/* 2. Nav-bar inset control                                                     */
/* ---------------------------------------------------------------------------- */

@Composable
private fun NavBarSection() {
    val view = LocalView.current
    val context = LocalContext.current
    val activity = remember { context.findActivity() }

    var fraction by remember { mutableStateOf(1f) }
    // Holds the live animation controller between onReady and slider updates.
    val controllerHolder = remember { arrayOfNulls<WindowInsetsAnimationController>(1) }

    fun compat() = activity?.window?.let { WindowCompat.getInsetsController(it, view) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Show / hide system bars", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        compat()?.hide(WindowInsetsCompat.Type.systemBars())
                    }) { Text("Hide bars") }
                    OutlinedButton(onClick = {
                        compat()?.show(WindowInsetsCompat.Type.systemBars())
                    }) { Text("Show bars") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    compat()?.systemBarsBehavior =
                        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }) { Text("Behavior: transient-swipe") }
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Finger-drive the nav-bar inset", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Grab control, then drag the slider: 1 = bar shown, 0 = bar hidden. This is the " +
                        "exact controlWindowInsetsAnimation() API a launcher uses to drag the bar under " +
                        "your finger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val controller = activity?.window?.decorView?.windowInsetsController ?: return@Button
                    runCatching {
                        controller.controlWindowInsetsAnimation(
                            PlatformWindowInsets.Type.navigationBars(),
                            -1L,
                            LinearInterpolator(),
                            CancellationSignal(),
                            object : WindowInsetsAnimationControlListener {
                                override fun onReady(c: WindowInsetsAnimationController, types: Int) {
                                    controllerHolder[0] = c
                                }
                                override fun onFinished(c: WindowInsetsAnimationController) {
                                    controllerHolder[0] = null
                                }
                                override fun onCancelled(c: WindowInsetsAnimationController?) {
                                    controllerHolder[0] = null
                                }
                            }
                        )
                    }
                }) { Text("Grab nav bar") }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = fraction,
                    onValueChange = { f ->
                        fraction = f
                        val c = controllerHolder[0] ?: return@Slider
                        val shown = c.shownStateInsets
                        val hidden = c.hiddenStateInsets
                        val lerped = Insets.of(
                            lerp(hidden.left, shown.left, f),
                            lerp(hidden.top, shown.top, f),
                            lerp(hidden.right, shown.right, f),
                            lerp(hidden.bottom, shown.bottom, f),
                        )
                        runCatching { c.setInsetsAndAlpha(lerped, 1f, f) }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        controllerHolder[0]?.finish(true); controllerHolder[0] = null; fraction = 1f
                    }) { Text("Finish → shown") }
                    OutlinedButton(onClick = {
                        controllerHolder[0]?.finish(false); controllerHolder[0] = null; fraction = 0f
                    }) { Text("Finish → hidden") }
                }
            }
        }
    }
}

private fun lerp(a: Int, b: Int, f: Float): Int = (a + (b - a) * f).roundToInt()

/* ---------------------------------------------------------------------------- */
/* 3. Icon-morph (launcher-style shared-element transition)                     */
/* ---------------------------------------------------------------------------- */

private data class Tile(val id: Int, val label: String, val color: Color)

private val tiles = listOf(
    Tile(0, "Mail", Color(0xFFE57373)),
    Tile(1, "Maps", Color(0xFF64B5F6)),
    Tile(2, "Music", Color(0xFFFFB74D)),
    Tile(3, "Photos", Color(0xFF81C784)),
    Tile(4, "Notes", Color(0xFFBA68C8)),
    Tile(5, "Clock", Color(0xFF4DB6AC)),
    Tile(6, "Wallet", Color(0xFF90A4AE)),
    Tile(7, "Camera", Color(0xFFF06292)),
    Tile(8, "Files", Color(0xFF7986CB)),
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun IconMorphSection() {
    var selected by remember { mutableStateOf<Tile?>(null) }
    // Spring tuned to roughly match QuickStep's close-to-icon feel.
    val boundsSpring = spring<androidx.compose.ui.geometry.Rect>(
        dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow
    )

    Column(Modifier.fillMaxSize()) {
        Text(
            "Tap a tile → it grows to a full screen from its own bounds. Press Back inside → it " +
                "springs back into the same tile. This is a shared-element transition on in-app content. " +
                "Doing this to a real foreign app's window (what the system launcher does on swipe-home) " +
                "needs CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS — a signature permission, not ADB-grantable.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SharedTransitionLayout(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(spring(stiffness = Spring.StiffnessHigh)) togetherWith fadeOut() },
                label = "morph"
            ) { current ->
                if (current == null) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(tiles, key = { it.id }) { tile ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .sharedBounds(
                                            rememberSharedContentState("bg-${tile.id}"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            boundsTransform = { _, _ -> boundsSpring }
                                        )
                                        .size(72.dp)
                                        .background(tile.color, RoundedCornerShape(18.dp))
                                        .clickableTile { selected = tile }
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(tile.label, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Box(
                        Modifier
                            .sharedBounds(
                                rememberSharedContentState("bg-${current.id}"),
                                animatedVisibilityScope = this@AnimatedContent,
                                boundsTransform = { _, _ -> boundsSpring }
                            )
                            .fillMaxSize()
                            .background(current.color)
                            .clickableTile { selected = null }
                    ) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(current.label, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("tap anywhere / Back to shrink to icon", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
    // Hardware/predictive Back collapses the detail too.
    androidx.activity.compose.BackHandler(enabled = selected != null) { selected = null }
}

/* small helper so we don't pull in the whole clickable ripple machinery twice */
private fun Modifier.clickableTile(onClick: () -> Unit): Modifier =
    this.pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
