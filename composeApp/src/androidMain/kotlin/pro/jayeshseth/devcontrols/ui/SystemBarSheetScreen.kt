package pro.jayeshseth.devcontrols.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Demo + reusable "child window tints the system bars" implementations.
 *
 * The reliable way — and the way Material's `ModalBottomSheet` does it — is NOT a
 * Compose [androidx.compose.ui.window.Dialog] (that is a *floating* window and won't
 * paint into the status/nav bar strips, leaving them an opaque black). Instead we add
 * our own non-floating window straight to the [WindowManager] sized `MATCH_PARENT`
 * with `FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS`, so the whole screen — bars
 * included — belongs to the sheet and our scrim tints them. Bar-icon appearance is set
 * on the host window's [WindowInsetsControllerCompat] and restored on dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemBarSheetScreen(onBack: () -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var lightIcons by remember { mutableStateOf(false) }
    var scrimChoice by remember { mutableStateOf(0) }

    val scrims = listOf(
        "Dim (black 40%)" to Color.Black.copy(alpha = 0.4f),
        "Indigo tint" to Color(0xFF3F51B5).copy(alpha = 0.5f),
        "Crimson tint" to Color(0xFFD32F2F).copy(alpha = 0.5f),
        "Clear (no tint)" to Color.Transparent,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Bar Sheet") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Child-window bar tinting", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The sheet/dialog is its own full-screen WindowManager window (like " +
                            "ModalBottomSheet), so the scrim covers the status & navigation bars and " +
                            "the bar icons flip — while the Activity's own bars stay put.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Light bar icons (dark content)", Modifier.weight(1f))
                Switch(lightIcons, { lightIcons = it })
            }

            Spacer(Modifier.height(8.dp))
            Text("Scrim", style = MaterialTheme.typography.titleSmall)
            scrims.forEachIndexed { i, (label, _) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = scrimChoice == i, onClick = { scrimChoice = i })
                    Text(label)
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = { showSheet = true }, Modifier.fillMaxWidth()) { Text("Show bottom sheet") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showDialog = true }, Modifier.fillMaxWidth()) { Text("Show dialog") }
        }
    }

    if (showSheet) {
        BarTintBottomSheet(
            onDismiss = { showSheet = false },
            scrim = scrims[scrimChoice].second,
            lightBarIcons = lightIcons,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Custom bottom sheet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "This window owns the bars while it's up — the scrim above extends across the " +
                        "status and navigation bars, no black strips.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showSheet = false }) { Text("Close") }
            }
        }
    }

    if (showDialog) {
        BarTintDialog(
            onDismiss = { showDialog = false },
            scrim = scrims[scrimChoice].second,
            lightBarIcons = lightIcons,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Custom dialog", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Same window trick, centered instead of bottom-anchored.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showDialog = false }) { Text("Close") }
            }
        }
    }
}

/* --------------------------------------------------------------------------- */
/* Core: a full-screen overlay hosted in its own WindowManager window.         */
/* --------------------------------------------------------------------------- */

@Composable
private fun RawWindowOverlay(
    onDismiss: () -> Unit,
    lightBarIcons: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() } ?: return
    val parentComposition = rememberCompositionContext()
    val currentContent by androidx.compose.runtime.rememberUpdatedState(content)
    val currentDismiss by androidx.compose.runtime.rememberUpdatedState(onDismiss)

    DisposableEffect(Unit) {
        val wm = activity.windowManager
        val hostWindow = activity.window

        // ComponentActivity implements all three owners; fall back to the decor view's.
        val decor = hostWindow.decorView
        val lifecycleOwner = (activity as? androidx.lifecycle.LifecycleOwner)
            ?: decor.findViewTreeLifecycleOwner()
        val vmStoreOwner = (activity as? androidx.lifecycle.ViewModelStoreOwner)
            ?: decor.findViewTreeViewModelStoreOwner()
        val savedStateOwner = (activity as? androidx.savedstate.SavedStateRegistryOwner)
            ?: decor.findViewTreeSavedStateRegistryOwner()

        val composeView = ComposeView(activity).apply {
            // Inherit the host's lifecycle / state owners so Compose can run in this window.
            lifecycleOwner?.let { setViewTreeLifecycleOwner(it) }
            vmStoreOwner?.let { setViewTreeViewModelStoreOwner(it) }
            savedStateOwner?.let { setViewTreeSavedStateRegistryOwner(it) }
            setParentCompositionContext(parentComposition)
            isFocusableInTouchMode = true
            setContent { currentContent() }
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    currentDismiss(); true
                } else false
            }
        }

        val lp = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
            token = hostWindow.decorView.windowToken
            // Cover the whole screen INCLUDING the bar strips; this is the bit a floating
            // dialog can't do.
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            dimAmount = 0f
        }

        // Drive the host window's bar icons; the panel's scrim provides the colour.
        val controller = WindowInsetsControllerCompat(hostWindow, hostWindow.decorView)
        controller.isAppearanceLightStatusBars = lightBarIcons
        controller.isAppearanceLightNavigationBars = lightBarIcons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                hostWindow.isStatusBarContrastEnforced = false
                hostWindow.isNavigationBarContrastEnforced = false
            }
        }

        runCatching { wm.addView(composeView, lp) }
        composeView.requestFocus()

        onDispose {
            runCatching { wm.removeView(composeView) }
            composeView.disposeComposition()
            // Restore the app's default bar-icon appearance (dark theme → light icons off).
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
}

/** A custom bottom sheet that tints the system bars exactly like ModalBottomSheet. */
@Composable
fun BarTintBottomSheet(
    onDismiss: () -> Unit,
    scrim: Color,
    lightBarIcons: Boolean,
    content: @Composable () -> Unit,
) {
    RawWindowOverlay(onDismiss, lightBarIcons) {
        var visible by remember { mutableStateOf(false) }
        DisposableEffect(Unit) { visible = true; onDispose { } }
        Box(Modifier.fillMaxSize()) {
            // Full-screen tap target to dismiss…
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )
            // …but the tint is inset out of the status/nav bar strips, leaving the bars
            // completely transparent (clear) while the content area dims.
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .background(scrim)
            )
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(280)) { it },
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 10.dp, bottom = 6.dp)
                                .size(width = 40.dp, height = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        content()
                    }
                }
            }
        }
    }
}

/** A custom centered dialog with the same bar-tinting behaviour. */
@Composable
fun BarTintDialog(
    onDismiss: () -> Unit,
    scrim: Color,
    lightBarIcons: Boolean,
    content: @Composable () -> Unit,
) {
    RawWindowOverlay(onDismiss, lightBarIcons) {
        Box(Modifier.fillMaxSize()) {
            // Full-screen tap target to dismiss…
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )
            // …but the tint is inset out of the status/nav bar strips, leaving the bars
            // completely transparent (clear) while the content area dims.
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .background(scrim)
            )
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            ) { content() }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
