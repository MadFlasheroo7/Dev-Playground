package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class NeonBrushStyle { CLASSIC, PULSE, FLICKER, SPARKS }

data class NeonStroke(
    val points: List<Offset>,
    val color: Color,
    val style: NeonBrushStyle,
    val size: Float,
    val intensity: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonGlowBoardScreen(onBack: () -> Unit) {
    val strokes = remember { mutableStateListOf<NeonStroke>() }
    val redoStack = remember { mutableStateListOf<NeonStroke>() }

    val currentPoints = remember { mutableStateListOf<Offset>() }
    var selectedColor by remember { mutableStateOf(Color(0xFFFF2A85)) } // Neon Pink default
    var selectedStyle by remember { mutableStateOf(NeonBrushStyle.CLASSIC) }
    var brushSize by remember { mutableStateOf(10f) }
    var glowIntensity by remember { mutableStateOf(1.0f) }

    // Pulse transition
    val infiniteTransition = rememberInfiniteTransition()
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Flicker randomizer
    val flickerFactor by produceState(1.0f) {
        while (true) {
            delay((80..400).random().toLong())
            value = if (kotlin.random.Random.nextFloat() < 0.12f) 0.15f else 1.0f
        }
    }

    val neonColors = listOf(
        Color(0xFFFF2A85), // Neon Pink
        Color(0xFF05FFC5), // Neon Cyan
        Color(0xFF39FF14), // Neon Green
        Color(0xFFFF9933), // Neon Orange
        Color(0xFFBD00FF), // Neon Purple
        Color(0xFFFFE600), // Neon Yellow
        Color(0xFF00E1FF)  // Electric Blue
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neon Glow Drawing Board") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    OutlinedButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                redoStack.add(strokes.removeLast())
                            }
                        },
                        enabled = strokes.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Undo")
                    }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                strokes.add(redoStack.removeLast())
                            }
                        },
                        enabled = redoStack.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Redo")
                    }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = {
                            strokes.clear()
                            redoStack.clear()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF08080C))
        ) {
            // Control panel
            Card(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Brush Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Color presets
                    Text("Neon Colors", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                neonColors.take(4).forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                2.dp,
                                                if (selectedColor == color) Color.White else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable { selectedColor = color }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                neonColors.drop(4).forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                2.dp,
                                                if (selectedColor == color) Color.White else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable { selectedColor = color }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Brush Styles
                    Text("Emission Style", style = MaterialTheme.typography.bodyMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { selectedStyle = NeonBrushStyle.CLASSIC },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedStyle == NeonBrushStyle.CLASSIC) selectedColor.copy(alpha = 0.15f) else Color.Transparent
                                )
                            ) {
                                Text("Classic", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { selectedStyle = NeonBrushStyle.PULSE },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedStyle == NeonBrushStyle.PULSE) selectedColor.copy(alpha = 0.15f) else Color.Transparent
                                )
                            ) {
                                Text("Pulse", fontSize = 10.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { selectedStyle = NeonBrushStyle.FLICKER },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedStyle == NeonBrushStyle.FLICKER) selectedColor.copy(alpha = 0.15f) else Color.Transparent
                                )
                            ) {
                                Text("Flicker", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { selectedStyle = NeonBrushStyle.SPARKS },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedStyle == NeonBrushStyle.SPARKS) selectedColor.copy(alpha = 0.15f) else Color.Transparent
                                )
                            ) {
                                Text("Sparks", fontSize = 10.sp)
                            }
                        }
                    }

                    HorizontalDivider()

                    // Brush Size
                    Text("Brush Size: ${brushSize.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 4f..35f
                    )

                    // Glow Intensity
                    Text("Glow Intensity: ${(glowIntensity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = glowIntensity,
                        onValueChange = { glowIntensity = it },
                        valueRange = 0.2f..1.5f
                    )
                }
            }

            // Canvas Workspace
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF050508))
                    .border(1.dp, Color(0x1F6C7BFF), RoundedCornerShape(16.dp))
                    .pointerInput(selectedColor, selectedStyle, brushSize, glowIntensity) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                redoStack.clear()
                                currentPoints.clear()
                                currentPoints.add(offset)
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    strokes.add(
                                        NeonStroke(
                                            points = currentPoints.toList(),
                                            color = selectedColor,
                                            style = selectedStyle,
                                            size = brushSize,
                                            intensity = glowIntensity
                                        )
                                    )
                                    currentPoints.clear()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentPoints.add(change.position)
                            }
                        )
                    }
            ) {
                // Neon Canvas Drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw grid helper lines
                    val gridColor = Color(0x0F6C7BFF)
                    for (x in 0..size.width.toInt() step 60) {
                        drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                    }
                    for (y in 0..size.height.toInt() step 60) {
                        drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                    }

                    // Helper to draw a neon layered path
                    fun drawNeonPath(points: List<Offset>, color: Color, style: NeonBrushStyle, baseSize: Float, intensity: Float) {
                        if (points.size < 2) return

                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val mid = Offset((points[i - 1].x + points[i].x) / 2f, (points[i - 1].y + points[i].y) / 2f)
                                quadraticTo(points[i - 1].x, points[i - 1].y, mid.x, mid.y)
                            }
                            lineTo(points.last().x, points.last().y)
                        }

                        // Determine dynamic alpha modifiers
                        val activeGlow = when (style) {
                            NeonBrushStyle.PULSE -> pulseFactor * intensity
                            NeonBrushStyle.FLICKER -> flickerFactor * intensity
                            else -> intensity
                        }

                        // Pass 1: Outer soft glow (very thick, very transparent)
                        drawPath(
                            path = path,
                            color = color.copy(alpha = 0.08f * activeGlow),
                            style = Stroke(
                                width = baseSize * 4f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Pass 2: Inner halo glow (medium thick)
                        drawPath(
                            path = path,
                            color = color.copy(alpha = 0.25f * activeGlow),
                            style = Stroke(
                                width = baseSize * 2.2f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Pass 3: Neon tube core (white/tinted bright color, narrow)
                        drawPath(
                            path = path,
                            color = color.copy(alpha = 0.75f * activeGlow),
                            style = Stroke(
                                width = baseSize * 1.1f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Pass 4: Hot white filament center
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = 0.95f * activeGlow),
                            style = Stroke(
                                width = baseSize * 0.35f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Spark style rendering: Draw floating embers along the path
                        if (style == NeonBrushStyle.SPARKS) {
                            points.forEachIndexed { index, pt ->
                                if (index % 5 == 0) {
                                    val offsetMultiplier = (index % 3 - 1) * baseSize
                                    drawCircle(
                                        color = color.copy(alpha = 0.4f * activeGlow),
                                        radius = 4f,
                                        center = Offset(pt.x + offsetMultiplier, pt.y + offsetMultiplier)
                                    )
                                }
                            }
                        }
                    }

                    // Render historical strokes
                    strokes.forEach { stroke ->
                        drawNeonPath(stroke.points, stroke.color, stroke.style, stroke.size, stroke.intensity)
                    }

                    // Render current active stroke
                    if (currentPoints.isNotEmpty()) {
                        drawNeonPath(currentPoints.toList(), selectedColor, selectedStyle, brushSize, glowIntensity)
                    }
                }
            }
        }
    }
}
