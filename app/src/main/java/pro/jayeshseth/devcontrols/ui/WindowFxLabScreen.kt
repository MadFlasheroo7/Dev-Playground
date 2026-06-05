package pro.jayeshseth.devcontrols.ui

import android.graphics.RuntimeShader
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Window FX Lab (category A) — the two flagship visual tricks an unrooted app can do
 * to its own windows:
 *
 *  - **Blur** — a panel whose window sets `FLAG_BLUR_BEHIND` + `blurBehindRadius`, so
 *    the lab content behind it is gaussian-blurred (frosted glass). The exact step up
 *    from the System Bar Sheet's flat scrim. Gated by `isCrossWindowBlurEnabled`.
 *  - **Shader** — an AGSL `RuntimeShader` driven by a `time` uniform and painted with
 *    a `ShaderBrush`; a live GPU fragment shader (API 33+).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowFxLabScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Window FX Lab") },
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
                FilterChip(tab == 0, { tab = 0 }, { Text("Blur behind") })
                FilterChip(tab == 1, { tab = 1 }, { Text("AGSL shader") })
            }
            when (tab) {
                0 -> BlurTab()
                else -> ShaderTab()
            }
        }
    }
}

/* ----------------------------- Blur behind ----------------------------- */

@Composable
private fun BlurTab() {
    val context = LocalContext.current
    val wm = remember { context.getSystemService(WindowManager::class.java) }
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (wm?.isCrossWindowBlurEnabled == true)

    var showPanel by remember { mutableStateOf(false) }
    var radius by remember { mutableFloatStateOf(60f) }

    Box(Modifier.fillMaxSize()) {
        // Busy background so the blur is obvious.
        Column(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1A237E), Color(0xFFAD1457), Color(0xFFF9A825))
                    )
                )
                .padding(16.dp)
        ) {
            Text("Background content", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            repeat(8) {
                Text(
                    "row $it — open the blur panel to frost this behind it",
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { showPanel = true }) { Text("Open blur panel") }
            Spacer(Modifier.height(8.dp))
            if (!blurSupported) {
                Text(
                    "isCrossWindowBlurEnabled = false on this device/state — blur may not render " +
                        "(needs Android 12+, the ‘Allow window-level blurs’ developer setting on, and " +
                        "no battery-saver). The panel still opens; it just won't frost.",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showPanel) {
        Dialog(
            onDismissRequest = { showPanel = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BlurBehindWindow(radiusDp = radius.toInt())
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(0.86f)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Blur-behind panel", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("blurBehindRadius = ${radius.toInt()} px", fontFamily = FontFamily.Monospace)
                        Slider(value = radius, onValueChange = { radius = it }, valueRange = 0f..150f)
                        Text(
                            "The panel window carries FLAG_BLUR_BEHIND; everything behind it is blurred " +
                                "by the radius above. The card itself is translucent so the frost shows through.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { showPanel = false }) { Text("Close") }
                    }
                }
            }
        }
    }
}

/** Applies FLAG_BLUR_BEHIND + radius to the host Dialog's window. */
@Composable
private fun BlurBehindWindow(radiusDp: Int) {
    val view = LocalView.current
    val density = LocalDensity.current
    val window: Window? = (view.parent as? DialogWindowProvider)?.window
    val radiusPx = with(density) { radiusDp.dp.toPx() }.toInt()
    SideEffect {
        window?.let { w ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val lp = w.attributes
                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                lp.blurBehindRadius = radiusPx
                w.attributes = lp
                runCatching { w.setBackgroundBlurRadius(radiusPx) }
            }
            // A faint dim so there's contrast even where blur is unsupported.
            w.setDimAmount(0.1f)
        }
    }
}

/* ------------------------------ AGSL shader ----------------------------- */

private const val AGSL_SRC = """
uniform float2 iResolution;
uniform float iTime;
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float r = 0.5 + 0.5 * sin(iTime + uv.x * 6.2831);
    float g = 0.5 + 0.5 * sin(iTime * 1.3 + uv.y * 6.2831 + 2.0);
    float b = 0.5 + 0.5 * sin(iTime * 0.7 + (uv.x + uv.y) * 6.2831 + 4.0);
    return half4(r, g, b, 1.0);
}
"""

@Composable
private fun ShaderTab() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Box(Modifier.fillMaxSize().padding(24.dp)) {
            Text("AGSL RuntimeShader needs Android 13 (API 33) or newer.")
        }
        return
    }
    val shader = remember { RuntimeShader(AGSL_SRC) }
    val brush = remember { ShaderBrush(shader) }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var start = 0L
        while (true) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                time = (now - start) / 1_000_000_000f
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Live AGSL fragment shader — animated by a `time` uniform, painted via ShaderBrush.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(20.dp))
                .drawWithCache {
                    shader.setFloatUniform("iResolution", size.width, size.height)
                    onDrawBehind {
                        shader.setFloatUniform("iTime", time)
                        drawRect(brush)
                    }
                }
        )
    }
}
