package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 3D transform playground. A premium Credit Card (NEXUS PLATINUM) driven by `Modifier.graphicsLayer`
 * with a dynamic holographic sheen that slides across the card surface as it tilts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Transform3DLabScreen(onBack: () -> Unit) {
    var rotX by remember { mutableFloatStateOf(10f) }
    var rotY by remember { mutableFloatStateOf(-15f) }
    var rotZ by remember { mutableFloatStateOf(0f) }
    var cam by remember { mutableFloatStateOf(12f) }
    var flipped by remember { mutableStateOf(false) }

    val flipDeg by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "flip"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3D Card Parallax") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                contentAlignment = Alignment.Center
            ) {
                val effectiveY = rotY + flipDeg
                val showingBack = ((abs(effectiveY) % 360f) in 90f..270f)

                Box(
                    Modifier
                        .size(320.dp, 200.dp) // Standard credit card aspect ratio
                        .graphicsLayer {
                            rotationX = rotX
                            rotationY = effectiveY
                            rotationZ = rotZ
                            cameraDistance = cam * density
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (showingBack)
                                Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
                            else
                                Brush.linearGradient(listOf(Color(0xFF1F1C2C), Color(0xFF928DAB)))
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, drag ->
                                    change.consume()
                                    rotY += drag.x * 0.3f
                                    rotX -= drag.y * 0.3f
                                }
                            )
                        }
                        .drawWithContent {
                            drawContent()
                            // Holographic Sheen Layer
                            val sheenOffset = (rotY * 5f) + (rotX * 5f)
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0f),
                                        Color.White.copy(alpha = 0.08f),
                                        Color(0xFF00C9FF).copy(alpha = 0.15f),
                                        Color(0xFF7F00FF).copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0f)
                                    ),
                                    start = Offset(sheenOffset - 150f, 0f),
                                    end = Offset(sheenOffset + 250f, size.height)
                                )
                            )
                        }
                ) {
                    // Counter-flip the back content so it isn't mirror-reversed
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { if (showingBack) rotationY = 180f }
                    ) {
                        if (showingBack) {
                            // Back of the Credit Card
                            Column(modifier = Modifier.fillMaxSize()) {
                                Spacer(Modifier.height(24.dp))
                                // Magnetic Stripe
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .background(Color(0xFF111111))
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Signature strip
                                    Box(
                                        Modifier
                                            .width(160.dp)
                                            .height(28.dp)
                                            .background(Color.White.copy(alpha = 0.8f))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("848", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            // Front of the Credit Card (NEXUS PLATINUM)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                // Top Row: Card Title & Contactless Icon
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "NEXUS PLATINUM",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 1.5.sp
                                    )
                                    Text("📶", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                                }

                                // Golden Chip
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .size(36.dp, 28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFFFE000), Color(0xFF799F0C))
                                            )
                                        )
                                )

                                // Card Details
                                Column(
                                    modifier = Modifier.align(Alignment.BottomStart)
                                ) {
                                    Text(
                                        "•••• •••• •••• 8848",
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "JAYESH SETH",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            "12/30",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { flipped = !flipped },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Flip Card")
                }
                Button(
                    onClick = {
                        rotX = 10f
                        rotY = -15f
                        rotZ = 0f
                        cam = 12f
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Reset Tilt")
                }
            }

            Spacer(Modifier.height(8.dp))
            Slider3D("rotationX (Tilt)", rotX, -60f..60f) { rotX = it }
            Slider3D("rotationY (Pan)", rotY, -60f..60f) { rotY = it }
            Slider3D("Perspective", cam, 4f..25f) { cam = it }

            Spacer(Modifier.height(8.dp))
            Text(
                "Drag the card in 3D to watch the holographic light sheen reflect across the surface.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun Slider3D(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label = ${value.toInt()}°", style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onValue, valueRange = range)
    }
}
