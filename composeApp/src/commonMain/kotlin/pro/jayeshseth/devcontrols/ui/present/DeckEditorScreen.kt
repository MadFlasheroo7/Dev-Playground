package pro.jayeshseth.devcontrols.ui.present

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.devcontrols.platform.ExportKind
import pro.jayeshseth.devcontrols.platform.rememberDeckExporter
import pro.jayeshseth.devcontrols.platform.rememberDeckFileIo
import pro.jayeshseth.devcontrols.platform.rememberToaster
import pro.jayeshseth.devcontrols.util.DeckJson
import kotlin.math.roundToInt

private val COLOR_PRESETS = listOf(0xFFFFFFFF, 0xFFB9C0D8, 0xFF4C6FFF, 0xFF22FFAA, 0xFFFFC857, 0xFFFF5C8A, 0xFF101225)
private val BG_PRESETS = listOf(
    listOf(0xFF101225, 0xFF1B1E3A), listOf(0xFF0E1A14, 0xFF143026), listOf(0xFF1A0F22, 0xFF2E1640),
    listOf(0xFF20121A, 0xFF3A1E2A), listOf(0xFF0B0B0B, 0xFF1F1F1F),
)

/** Track B — live deck editor: drag/resize elements, edit props, present, save/open, and export. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeckEditorScreen(onBack: () -> Unit) {
    var deck by remember { mutableStateOf(sampleDeck()) }
    var current by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var idSeq by remember { mutableIntStateOf(100) }
    var presenting by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    val fileIo = rememberDeckFileIo()
    val exporter = rememberDeckExporter()
    val toast = rememberToaster()

    if (presenting) { PresentScreen(onBack = { presenting = false }, deck = deck); return }

    val slide = deck.slides[current]
    val selected = slide.elements.firstOrNull { it.id == selectedId }

    fun setSlides(list: List<Slide>) { deck = deck.copy(slides = list) }
    fun updateSlide(t: (Slide) -> Slide) = setSlides(deck.slides.toMutableList().also { it[current] = t(it[current]) })
    fun updateElement(el: SlideElement) = updateSlide { s -> s.copy(elements = s.elements.map { if (it.id == el.id) el else it }) }
    fun addElement(e: SlideElement) { updateSlide { it.copy(elements = it.elements + e) }; selectedId = e.id }
    fun doExport(kind: ExportKind) {
        val ex = exporter ?: return
        exporting = true
        ex.export(kind, deck) { msg -> exporting = false; toast(msg) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deck Editor") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = {
                        fileIo.open { bytes ->
                            val parsed = bytes?.let { runCatching { DeckJson.read(it) }.getOrNull() }
                            if (parsed != null && parsed.slides.isNotEmpty()) {
                                deck = parsed; current = 0; selectedId = null
                                toast("Imported \"${parsed.title}\" (${parsed.slides.size} slides)")
                            } else toast("Couldn't read that deck file")
                        }
                    }) { Text("Open") }
                    TextButton(onClick = { fileIo.save("${deck.title}.${DeckJson.EXTENSION}", DeckJson.toBundle(deck)) }) { Text("Save") }
                    TextButton(onClick = { presenting = true }) { Text("Present") }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            // ---- canvas ----
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                SlideBackground(slide)
                BoxWithConstraints(
                    Modifier.fillMaxSize().pointerInput(current) { detectTapGestures { selectedId = null } }
                ) {
                    val wPx = constraints.maxWidth.toFloat(); val hPx = constraints.maxHeight.toFloat()
                    slide.elements.forEach { el ->
                        EditableElement(el, wPx, hPx, el.id == selectedId, { selectedId = el.id }, ::updateElement)
                    }
                }
            }

            // ---- filmstrip ----
            LazyRow(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(deck.slides) { i, s ->
                    Box(
                        Modifier.size(72.dp, 40.dp).clip(RoundedCornerShape(6.dp))
                            .background(Brush.linearGradient(s.bgColors.map { Color(it) }))
                            .border(2.dp, if (i == current) Color(0xFF4C6FFF) else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .pointerInput(Unit) { detectTapGestures { current = i; selectedId = null } },
                        contentAlignment = Alignment.Center
                    ) { Text("${i + 1}", color = Color.White, fontSize = 12.sp) }
                }
                item {
                    OutlinedButton(onClick = {
                        val ns = Slide("s${idSeq++}", listOf(0xFF101225, 0xFF1B1E3A), TransitionIn.FADE, emptyList())
                        val newList = deck.slides + ns
                        setSlides(newList); current = newList.lastIndex; selectedId = null
                    }) { Text("+ Slide") }
                }
            }

            // ---- property panel ----
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (selected != null) {
                    ElementProps(selected, ::updateElement, onDelete = {
                        updateSlide { s -> s.copy(elements = s.elements.filter { it.id != selected.id }) }; selectedId = null
                    })
                } else {
                    SlideProps(
                        slide, ::updateSlide,
                        onAdd = { type ->
                            val id = "e${idSeq++}"
                            addElement(
                                when (type) {
                                    ElementType.TITLE -> SlideElement(id, type, 0.1f, 0.4f, 0.8f, 0.16f, text = "Title", colorArgb = 0xFFFFFFFF, fontSize = 40, weight = 800, centerAlign = true)
                                    ElementType.BODY -> SlideElement(id, type, 0.1f, 0.4f, 0.8f, 0.2f, text = "Body text", colorArgb = 0xFFB9C0D8, fontSize = 22)
                                    ElementType.BULLETS -> SlideElement(id, type, 0.1f, 0.3f, 0.8f, 0.5f, bullets = listOf("First point", "Second point"), colorArgb = 0xFFD7F5E6, fontSize = 24, entrance = Entrance.STAGGER)
                                    ElementType.SHAPE -> SlideElement(id, type, 0.4f, 0.4f, 0.2f, 0.2f, colorArgb = 0xFF4C6FFF, shape = ShapeKind.CIRCLE)
                                    else -> SlideElement(id, ElementType.SUBTITLE, 0.1f, 0.4f, 0.8f, 0.1f, text = "Subtitle", colorArgb = 0xFFB9C0D8, fontSize = 20)
                                }
                            )
                        },
                        onDeleteSlide = if (deck.slides.size > 1) {
                            {
                                val newList = deck.slides.filterIndexed { i, _ -> i != current }
                                setSlides(newList)
                                current = current.coerceAtMost(newList.lastIndex).coerceAtLeast(0)
                            }
                        } else null,
                        onExportPdf = { doExport(ExportKind.PDF) },
                        onExportPng = { doExport(ExportKind.PNG) },
                        onExportPptx = { doExport(ExportKind.PPTX) },
                        exporting = exporting,
                        canExport = exporter != null,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditableElement(el: SlideElement, wPx: Float, hPx: Float, selected: Boolean, onSelect: () -> Unit, onChange: (SlideElement) -> Unit) {
    val density = LocalDensity.current
    val ref = rememberUpdatedState(el)
    var mod = Modifier
        .offset { IntOffset((el.x * wPx).roundToInt(), (el.y * hPx).roundToInt()) }
        .size(with(density) { (el.w * wPx).toDp() }, with(density) { (el.h * hPx).toDp() })
    if (selected) mod = mod.border(1.5.dp, Color(0xFF4C6FFF))
    mod = mod
        .pointerInput(el.id) { detectTapGestures { onSelect() } }
        .pointerInput(el.id) {
            detectDragGestures(onDragStart = { onSelect() }) { change, drag ->
                change.consume()
                val c = ref.value
                onChange(c.copy(
                    x = (c.x + drag.x / wPx).coerceIn(0f, 1f - c.w),
                    y = (c.y + drag.y / hPx).coerceIn(0f, 1f - c.h),
                ))
            }
        }

    Box(mod, contentAlignment = if (el.centerAlign) Alignment.Center else Alignment.CenterStart) {
        when (el.type) {
            ElementType.TITLE, ElementType.SUBTITLE, ElementType.BODY ->
                Text(el.text ?: "", color = Color(el.colorArgb), fontSize = el.fontSize.sp, fontWeight = FontWeight(el.weight),
                    textAlign = if (el.centerAlign) TextAlign.Center else TextAlign.Start)
            ElementType.BULLETS ->
                Column { el.bullets.forEach { Text("•  $it", color = Color(el.colorArgb), fontSize = el.fontSize.sp, modifier = Modifier.padding(vertical = 4.dp)) } }
            ElementType.SHAPE ->
                Box(Modifier.fillMaxSize().clip(if (el.shape == ShapeKind.CIRCLE) CircleShape else RoundedCornerShape(14.dp)).background(Color(el.colorArgb)))
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(Color(0xFF4C6FFF))
                    .pointerInput(el.id) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            val c = ref.value
                            onChange(c.copy(
                                w = (c.w + drag.x / wPx).coerceIn(0.05f, 1f - c.x),
                                h = (c.h + drag.y / hPx).coerceIn(0.04f, 1f - c.y),
                            ))
                        }
                    }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ElementProps(el: SlideElement, onChange: (SlideElement) -> Unit, onDelete: () -> Unit) {
    Text("Element: ${el.type}", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    if (el.type == ElementType.BULLETS) {
        OutlinedTextField(
            value = el.bullets.joinToString("\n"),
            onValueChange = { onChange(el.copy(bullets = it.split("\n"))) },
            label = { Text("Bullets (one per line)") }, modifier = Modifier.fillMaxWidth()
        )
    } else if (el.type != ElementType.SHAPE) {
        OutlinedTextField(
            value = el.text ?: "", onValueChange = { onChange(el.copy(text = it)) },
            label = { Text("Text") }, modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(12.dp))
    if (el.type != ElementType.SHAPE) {
        Text("Font size: ${el.fontSize}")
        Slider(el.fontSize.toFloat(), { onChange(el.copy(fontSize = it.roundToInt())) }, valueRange = 10f..72f)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(el.weight >= 600, { onChange(el.copy(weight = if (el.weight >= 600) 400 else 800)) }, { Text("Bold") })
            FilterChip(el.centerAlign, { onChange(el.copy(centerAlign = !el.centerAlign)) }, { Text("Center") })
        }
        Spacer(Modifier.height(8.dp))
        Text("Entrance")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Entrance.entries.forEach { e -> FilterChip(el.entrance == e, { onChange(el.copy(entrance = e)) }, { Text(e.name) }) }
        }
    }

    if (el.type == ElementType.SHAPE) {
        Text("Shape")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ShapeKind.entries.forEach { s -> FilterChip(el.shape == s, { onChange(el.copy(shape = s)) }, { Text(s.name) }) }
        }
    }

    Spacer(Modifier.height(12.dp))
    Text("Color")
    Swatches(el.colorArgb) { onChange(el.copy(colorArgb = it)) }

    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = el.sharedKey ?: "", onValueChange = { onChange(el.copy(sharedKey = it.ifBlank { null })) },
        label = { Text("Shared key (morph across slides)") }, modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = onDelete) { Text("Delete element") }
    Spacer(Modifier.height(40.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlideProps(
    slide: Slide, updateSlide: ((Slide) -> Slide) -> Unit, onAdd: (ElementType) -> Unit,
    onDeleteSlide: (() -> Unit)?, onExportPdf: () -> Unit, onExportPng: () -> Unit, onExportPptx: () -> Unit,
    exporting: Boolean, canExport: Boolean,
) {
    Text("Add element", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(ElementType.TITLE, ElementType.SUBTITLE, ElementType.BODY, ElementType.BULLETS, ElementType.SHAPE).forEach { t ->
            AssistChip(onClick = { onAdd(t) }, label = { Text(t.name) })
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("Transition in")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TransitionIn.entries.forEach { tr -> FilterChip(slide.transitionIn == tr, { updateSlide { it.copy(transitionIn = tr) } }, { Text(tr.name) }) }
    }

    Spacer(Modifier.height(16.dp))
    Text("Background")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BG_PRESETS.forEach { bg ->
            Box(
                Modifier.size(54.dp, 32.dp).clip(RoundedCornerShape(6.dp))
                    .background(Brush.linearGradient(bg.map { Color(it) }))
                    .border(if (slide.bgColors == bg) 2.dp else 1.dp, if (slide.bgColors == bg) Color(0xFF4C6FFF) else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                    .pointerInput(Unit) { detectTapGestures { updateSlide { it.copy(bgColors = bg) } } }
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("Code background (SkSL/AGSL)", style = MaterialTheme.typography.titleMedium)
    Text(
        "Write a shader for an animated background. Uniforms: float2 iResolution, float iTime. " +
            "Falls back to the gradient if empty or it doesn't compile.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        pro.jayeshseth.devcontrols.platform.ShaderPresets.list.forEach { (name, src) ->
            AssistChip(onClick = { updateSlide { it.copy(bgShader = src) } }, label = { Text(name) })
        }
        if (!slide.bgShader.isNullOrBlank()) {
            AssistChip(onClick = { updateSlide { it.copy(bgShader = null) } }, label = { Text("Clear") })
        }
    }
    OutlinedTextField(
        value = slide.bgShader ?: "",
        onValueChange = { updateSlide { s -> s.copy(bgShader = it.ifBlank { null }) } },
        label = { Text("Shader source") },
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp),
        modifier = Modifier.fillMaxWidth().height(160.dp)
    )

    if (canExport) {
        Spacer(Modifier.height(20.dp))
        Text("Export", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExportPdf, enabled = !exporting) { Text("Export PDF") }
            Button(onClick = onExportPptx, enabled = !exporting) { Text("Export PPTX") }
            OutlinedButton(onClick = onExportPng, enabled = !exporting) { Text("Export PNGs") }
        }
        if (exporting) { Spacer(Modifier.height(8.dp)); Text("Exporting…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }

    onDeleteSlide?.let {
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = it) { Text("Delete this slide") }
    }
    Spacer(Modifier.height(8.dp))
    Text("Tip: tap an element to edit it; drag to move, drag the blue dot to resize. Give two elements on different slides the same shared key to morph between them.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(40.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(selected: Long, onPick: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        COLOR_PRESETS.forEach { c ->
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Color(c))
                    .border(if (c == selected) 3.dp else 1.dp, if (c == selected) Color(0xFF4C6FFF) else Color(0x55FFFFFF), CircleShape)
                    .pointerInput(Unit) { detectTapGestures { onPick(c) } }
            )
        }
    }
}
