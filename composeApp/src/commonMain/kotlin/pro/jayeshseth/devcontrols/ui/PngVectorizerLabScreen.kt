package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pro.jayeshseth.devcontrols.platform.rememberDeckFileIo
import pro.jayeshseth.devcontrols.vectorize.Mode
import pro.jayeshseth.devcontrols.vectorize.Raster
import pro.jayeshseth.devcontrols.vectorize.VectorResult
import pro.jayeshseth.devcontrols.vectorize.VectorizeParams
import pro.jayeshseth.devcontrols.vectorize.vectorize
import kotlin.math.min

private class SourceImage(val bitmap: ImageBitmap, val argb: IntArray, val w: Int, val h: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PngVectorizerLabScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val fileIo = rememberDeckFileIo()
    val clipboard = LocalClipboardManager.current

    var source by remember { mutableStateOf<SourceImage?>(null) }
    var result by remember { mutableStateOf<VectorResult?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }
    var selectedTab by remember { mutableStateOf(0) } // 0 Preview, 1 SVG source, 2 Logs

    // Tunable parameters (colorCount == 1 => monochrome luminance mode; >= 2 => colour quantize).
    var maxDimension by remember { mutableStateOf(256f) }
    var colorCount by remember { mutableStateOf(4f) }
    var threshold by remember { mutableStateOf(0.5f) }
    var epsilon by remember { mutableStateOf(1.5f) }
    var smoothing by remember { mutableStateOf(false) }

    // Load the procedural sample once so the lab works on every target (no asset plumbing).
    LaunchedEffect(Unit) {
        if (source == null) {
            val bmp = buildSampleBitmap()
            source = withContext(Dispatchers.Default) { bmp.toSource() }
        }
    }

    // Recompute on any input change, debounced; runs off the main thread and auto-cancels supersedes.
    LaunchedEffect(source, maxDimension, colorCount, threshold, epsilon, smoothing) {
        val src = source ?: return@LaunchedEffect
        delay(150)
        isConverting = true
        val params = VectorizeParams(
            maxDimension = maxDimension.toInt(),
            mode = if (colorCount.toInt() <= 1) Mode.LUMINANCE else Mode.QUANTIZE,
            threshold = threshold,
            colorCount = colorCount.toInt(),
            epsilon = epsilon,
            smoothing = smoothing,
        )
        val localLogs = ArrayList<String>()
        val res = withContext(Dispatchers.Default) {
            vectorize(
                Raster(src.w, src.h, src.argb),
                params,
                log = { localLogs.add(it) },
                now = { currentTimeMillis() },
            )
        }
        logs.clear(); logs.addAll(localLogs)
        result = res
        isConverting = false
    }

    fun loadFromDisk() {
        fileIo.open { bytes ->
            if (bytes == null) return@open
            scope.launch {
                val src = withContext(Dispatchers.Default) {
                    runCatching { bytes.decodeToImageBitmap().toSource() }.getOrNull()
                }
                if (src == null) {
                    logs.add("⚠ Could not decode that file as an image")
                } else {
                    source = src
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PNG → SVG Vectorizer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        scope.launch { source = withContext(Dispatchers.Default) { buildSampleBitmap().toSource() } }
                    }) { Text("Sample", color = Color(0xFF60A5FA)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E24),
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF121214)),
        ) {
            val isWide = maxWidth >= 850.dp

            val controls: @Composable (Modifier) -> Unit = { mod ->
                ControlsPanel(
                    modifier = mod,
                    source = source,
                    maxDimension = maxDimension, onMaxDimension = { maxDimension = it },
                    colorCount = colorCount, onColorCount = { colorCount = it },
                    threshold = threshold, onThreshold = { threshold = it },
                    epsilon = epsilon, onEpsilon = { epsilon = it },
                    smoothing = smoothing, onSmoothing = { smoothing = it },
                    onLoad = { loadFromDisk() },
                )
            }

            val output: @Composable (Modifier) -> Unit = { mod ->
                Column(modifier = mod) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                    ) {
                        Tab(selectedTab == 0, { selectedTab = 0 }, text = { Text("Preview", fontSize = 12.sp) })
                        Tab(selectedTab == 1, { selectedTab = 1 }, text = { Text("SVG Source", fontSize = 12.sp) })
                        Tab(selectedTab == 2, { selectedTab = 2 }, text = { Text("Logs / Stats", fontSize = 12.sp) })
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            0 -> PreviewTab(result, isConverting)
                            1 -> SvgSourceTab(
                                result = result,
                                onCopy = { result?.let { clipboard.setText(AnnotatedString(buildSvgDocument(it))) } },
                                onSave = { result?.let { fileIo.save("trace.svg", buildSvgDocument(it).encodeToByteArray()) } },
                            )
                            2 -> LogsTab(result, logs, isConverting)
                        }
                    }
                }
            }

            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    controls(Modifier.weight(0.42f).fillMaxHeight().padding(12.dp))
                    Box(Modifier.fillMaxHeight().width(1.dp).background(Color(0xFF2D2D35)))
                    output(Modifier.weight(0.58f).fillMaxHeight().padding(12.dp))
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    output(Modifier.weight(1f).fillMaxWidth())
                    HorizontalDivider(color = Color(0xFF2D2D35))
                    controls(Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun ControlsPanel(
    modifier: Modifier,
    source: SourceImage?,
    maxDimension: Float, onMaxDimension: (Float) -> Unit,
    colorCount: Float, onColorCount: (Float) -> Unit,
    threshold: Float, onThreshold: (Float) -> Unit,
    epsilon: Float, onEpsilon: (Float) -> Unit,
    smoothing: Boolean, onSmoothing: (Boolean) -> Unit,
    onLoad: () -> Unit,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onLoad,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                modifier = Modifier.weight(1f),
            ) { Text("Load PNG", fontSize = 13.sp) }
        }
        if (source != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
                border = BorderStroke(1.dp, Color(0xFF26262D)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Source: ${source.w} × ${source.h}px",
                    color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        val isMono = colorCount.toInt() <= 1
        LabeledSlider("Colours: ${if (isMono) "monochrome" else colorCount.toInt().toString()}", colorCount, 1f..8f, 7, onColorCount)
        if (isMono) {
            LabeledSlider("Threshold: ${(threshold * 100).toInt()}%", threshold, 0.1f..0.9f, 0, onThreshold)
        }
        LabeledSlider("Detail (ε): ${roundTo(epsilon, 1)}px", epsilon, 0.5f..6f, 0, onEpsilon)
        LabeledSlider("Resolution cap: ${maxDimension.toInt()}px", maxDimension, 64f..512f, 0, onMaxDimension)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Bézier smoothing", color = Color.Gray, fontSize = 13.sp)
            Switch(checked = smoothing, onCheckedChange = onSmoothing)
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun PreviewTab(result: VectorResult?, isConverting: Boolean) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1F)),
        border = BorderStroke(1.dp, Color(0xFF26262D)),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            val res = result
            if (res == null) {
                Text(if (isConverting) "Converting…" else "No image", color = Color.Gray, fontSize = 13.sp)
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val vbW = res.viewBoxW.toFloat()
                    val vbH = res.viewBoxH.toFloat()
                    if (vbW <= 0f || vbH <= 0f) return@Canvas
                    val s = min(size.width / vbW, size.height / vbH)
                    val offX = (size.width - vbW * s) / 2f
                    val offY = (size.height - vbH * s) / 2f
                    withTransform({
                        translate(offX, offY)
                        scale(s, s, pivot = Offset.Zero)
                    }) {
                        res.groups.forEach { group ->
                            drawPath(buildComposePath(group.segments), color = Color(group.fillArgb))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SvgSourceTab(result: VectorResult?, onCopy: () -> Unit, onSave: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onCopy, enabled = result != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26262F)),
                modifier = Modifier.weight(1f),
            ) { Text("Copy", fontSize = 12.sp) }
            Button(
                onClick = onSave, enabled = result != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26262F)),
                modifier = Modifier.weight(1f),
            ) { Text("Save .svg", fontSize = 12.sp) }
        }
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070709)),
            border = BorderStroke(1.dp, Color(0xFF26262D)),
        ) {
            Text(
                text = result?.let { buildSvgDocument(it) } ?: "",
                color = Color(0xFF8AE234),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            )
        }
    }
}

@Composable
private fun LogsTab(result: VectorResult?, logs: List<String>, isConverting: Boolean) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070709)),
        border = BorderStroke(1.dp, Color(0xFF26262D)),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            result?.stats?.let { s ->
                Text(
                    buildString {
                        append("source ${s.srcW}×${s.srcH}  →  scaled ${s.scaledW}×${s.scaledH}\n")
                        append("regions ${s.regionCount}   segments ${s.totalSegments}\n")
                        append("vertices ${s.rawVertexCount} → ${s.simplifiedVertexCount} after simplify\n")
                        append("elapsed ${s.elapsedMs}ms${if (isConverting) "  (converting…)" else ""}")
                    },
                    color = Color(0xFF9CDCFE), fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF26262D))
                Spacer(Modifier.height(8.dp))
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs) { line ->
                    Text(
                        line,
                        color = if (line.startsWith("⚠")) Color(0xFFFFC107) else Color(0xFFBBBBBB),
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// helpers
// ---------------------------------------------------------------------------

private fun ImageBitmap.toSource(): SourceImage {
    val pm = this.toPixelMap()
    val w = width; val h = height
    val argb = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val c = pm[x, y]
            val a = (c.alpha * 255f + 0.5f).toInt()
            val r = (c.red * 255f + 0.5f).toInt()
            val g = (c.green * 255f + 0.5f).toInt()
            val b = (c.blue * 255f + 0.5f).toInt()
            argb[y * w + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    return SourceImage(this, argb, w, h)
}

/** A high-contrast sample drawn procedurally — no bundled assets, works on every target. */
private fun buildSampleBitmap(): ImageBitmap {
    val w = 240; val h = 240
    val bmp = ImageBitmap(w, h)
    val canvas = Canvas(bmp)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(w.toFloat(), h.toFloat()),
    ) {
        drawRect(Color.White, size = size)
        drawCircle(Color(0xFF1E88E5), radius = 62f, center = Offset(82f, 90f))
        drawRoundRect(
            color = Color(0xFFE53935),
            topLeft = Offset(120f, 118f),
            size = Size(96f, 96f),
            cornerRadius = CornerRadius(20f, 20f),
        )
        val tri = Path().apply {
            moveTo(160f, 24f); lineTo(224f, 108f); lineTo(96f, 108f); close()
        }
        drawPath(tri, Color(0xFFFB8C00))
    }
    return bmp
}

private fun buildSvgDocument(result: VectorResult): String {
    val sb = StringBuilder()
    sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ${result.viewBoxW} ${result.viewBoxH}\">\n")
    result.groups.forEach { g ->
        sb.append("  <path fill=\"").append(argbToHex(g.fillArgb)).append("\" d=\"")
            .append(serializeSvgPath(g.segments)).append("\"/>\n")
    }
    sb.append("</svg>")
    return sb.toString()
}

private fun argbToHex(c: Long): String {
    val r = ((c ushr 16) and 0xFF).toInt()
    val g = ((c ushr 8) and 0xFF).toInt()
    val b = (c and 0xFF).toInt()
    fun h(v: Int) = v.toString(16).padStart(2, '0')
    return "#${h(r)}${h(g)}${h(b)}"
}

private fun roundTo(v: Float, dp: Int): String {
    var f = 1
    repeat(dp) { f *= 10 }
    val scaled = (v * f + 0.5f).toInt()
    val whole = scaled / f
    val frac = scaled % f
    return if (dp == 0) whole.toString() else "$whole.${frac.toString().padStart(dp, '0')}"
}
