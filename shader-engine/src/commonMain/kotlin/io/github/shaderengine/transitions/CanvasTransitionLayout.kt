package io.github.shaderengine.transitions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.VertexMode
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.shaderengine.presets.CanvasTransitionType
import io.github.shaderengine.presets.TransitionPreset
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Declarative Compose container for Pure Canvas transitions.
 *
 * Runs 100% shader-free using Compose DrawScope geometry, [clipPath], [clipRect],
 * and layer blending with zero AGSL or JNI requirements, ensuring universal
 * compatibility down to Android 5.0 (API 21+), Desktop JVM, iOS, and Web.
 */
@Composable
fun CanvasTransitionLayout(
    progress: Float,
    preset: TransitionPreset,
    modifier: Modifier = Modifier,
    onProgressChange: ((Float) -> Unit)? = null,
    fromContent: @Composable () -> Unit,
    toContent: @Composable () -> Unit
) {
    val animProgress = remember { Animatable(progress) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val layerA = rememberGraphicsLayer()
    val layerB = rememberGraphicsLayer()
    var bitmapA by remember { mutableStateOf<ImageBitmap?>(null) }
    var currentSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(progress) {
        if (!isDragging) {
            animProgress.snapTo(progress)
        }
    }

    val currentP = animProgress.value
    val isTransitioning = currentP > 0.001f && currentP < 0.999f

    // Recapture snapshot for mesh deformation when transition begins
    LaunchedEffect(isTransitioning) {
        if (isTransitioning) {
            try {
                bitmapA = layerA.toImageBitmap()
            } catch (_: Throwable) {
            }
        }
    }
    val canvasType = preset.canvasType ?: CanvasTransitionType.CIRCULAR_IRIS

    Box(
        modifier = modifier
            .onSizeChanged { currentSize = it }
            .pointerInput(preset) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        scope.launch {
                            try {
                                val target = if (animProgress.value >= 0.5f) 1f else 0f
                                animProgress.animateTo(
                                    targetValue = target,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) {
                                    onProgressChange?.invoke(value)
                                }
                            } finally {
                                isDragging = false
                            }
                        }
                    },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { _, dragAmount ->
                        val width = currentSize.width.toFloat().coerceAtLeast(1f)
                        val next = (animProgress.value + dragAmount / width).coerceIn(0f, 1f)
                        scope.launch {
                            animProgress.snapTo(next)
                            onProgressChange?.invoke(next)
                        }
                    }
                )
            }
    ) {
        // Slot A: Outgoing content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (currentP < 0.5f) 1f else 0f)
                .drawWithContent {
                    layerA.record {
                        this@drawWithContent.drawContent()
                    }
                    if (currentP <= 0.001f) {
                        drawLayer(layerA)
                    }
                }
        ) {
            fromContent()
        }

        // Slot B: Incoming content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (currentP >= 0.5f) 1f else 0f)
                .drawWithContent {
                    layerB.record {
                        this@drawWithContent.drawContent()
                    }
                    if (currentP >= 0.999f) {
                        drawLayer(layerB)
                    }
                }
        ) {
            toContent()
        }

        // Transition Overlay: Pure Compose Canvas Geometry Pass
        if (isTransitioning) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
            ) {
                when (canvasType) {
                    CanvasTransitionType.CIRCULAR_IRIS -> {
                        drawLayer(layerA)
                        val maxR = hypot(size.width, size.height) * 0.75f
                        val r = currentP * maxR
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val irisPath = Path().apply {
                            addOval(Rect(center, r))
                        }
                        clipPath(irisPath) {
                            drawLayer(layerB)
                        }
                        // Aperture glowing accent ring
                        drawCircle(
                            color = Color(0xFF38BDF8).copy(alpha = (1f - currentP * 0.3f).coerceIn(0f, 1f)),
                            radius = r,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    CanvasTransitionType.CLOCK_RADAR_SWEEP -> {
                        drawLayer(layerA)
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = hypot(size.width, size.height)
                        val sweep = currentP * 360f
                        val clockPath = Path().apply {
                            moveTo(cx, cy)
                            arcTo(
                                rect = Rect(cx - r, cy - r, cx + r, cy + r),
                                startAngleDegrees = -90f,
                                sweepAngleDegrees = sweep,
                                forceMoveTo = false
                            )
                            close()
                        }
                        clipPath(clockPath) {
                            drawLayer(layerB)
                        }
                        // Radar scanner sweep line
                        val angleRad = (-90f + sweep) * (PI / 180f).toFloat()
                        val endX = cx + cos(angleRad) * r
                        val endY = cy + sin(angleRad) * r
                        drawLine(
                            color = Color(0xFF34D399),
                            start = Offset(cx, cy),
                            end = Offset(endX, endY),
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }

                    CanvasTransitionType.DIAGONAL_SLATS -> {
                        drawLayer(layerA)
                        val diag = hypot(size.width, size.height) * 1.5f
                        val numSlats = 12
                        val slatW = diag / numSlats
                        val slatsPath = Path()
                        for (i in 0 until numSlats) {
                            val delay = (i.toFloat() / numSlats) * 0.35f
                            val localP = ((currentP - delay) / 0.65f).coerceIn(0f, 1f)
                            if (localP <= 0f) continue
                            val w = slatW * localP
                            val x = -diag / 2f + i * slatW
                            slatsPath.addRect(Rect(x, -diag / 2f, x + w, diag / 2f))
                        }
                        val center = Offset(size.width / 2f, size.height / 2f)
                        rotate(degrees = 45f, pivot = center) {
                            clipPath(slatsPath) {
                                rotate(degrees = -45f, pivot = center) {
                                    drawLayer(layerB)
                                }
                            }
                        }
                    }

                    CanvasTransitionType.DIAMOND_GRID -> {
                        drawLayer(layerA)
                        val tileSize = 52.dp.toPx()
                        val cols = (size.width / tileSize).toInt() + 1
                        val rows = (size.height / tileSize).toInt() + 1
                        val maxD = hypot(cols / 2f, rows / 2f).coerceAtLeast(1f)
                        val diamondPath = Path()

                        for (c in 0..cols) {
                            for (r in 0..rows) {
                                val cx = (c + 0.5f) * tileSize
                                val cy = (r + 0.5f) * tileSize
                                val dist = hypot(c - cols / 2f, r - rows / 2f) / maxD
                                val delay = dist * 0.45f
                                val localP = ((currentP - delay) / 0.55f).coerceIn(0f, 1f)
                                if (localP <= 0f) continue

                                val rad = localP * (tileSize * 0.75f)
                                diamondPath.moveTo(cx, cy - rad)
                                diamondPath.lineTo(cx + rad, cy)
                                diamondPath.lineTo(cx, cy + rad)
                                diamondPath.lineTo(cx - rad, cy)
                                diamondPath.close()
                            }
                        }
                        clipPath(diamondPath) {
                            drawLayer(layerB)
                        }
                    }

                    CanvasTransitionType.CURTAIN_SPLIT -> {
                        val dx = currentP * (size.width / 2f)
                        val halfW = size.width / 2f
                        val shadowW = 28.dp.toPx()

                        // 1. Left curtain sliding left
                        clipRect(left = 0f, top = 0f, right = halfW - dx, bottom = size.height) {
                            translate(left = -dx, top = 0f) {
                                drawLayer(layerA)
                            }
                        }

                        // 2. Right curtain sliding right
                        clipRect(left = halfW + dx, top = 0f, right = size.width, bottom = size.height) {
                            translate(left = dx, top = 0f) {
                                drawLayer(layerA)
                            }
                        }

                        // 3. Card B ONLY rendered inside the opening slit (0% visible at start)
                        if (dx > 0.5f) {
                            clipRect(left = halfW - dx, top = 0f, right = halfW + dx, bottom = size.height) {
                                drawLayer(layerB)
                            }
                        }

                        // Depth drop-shadows along curtain seams
                        if (currentP > 0.01f && currentP < 0.99f) {
                            // Left curtain edge shadow
                            val leftEdge = (halfW - dx).coerceAtLeast(0f)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                                    startX = (leftEdge - shadowW).coerceAtLeast(0f),
                                    endX = leftEdge
                                ),
                                topLeft = Offset((leftEdge - shadowW).coerceAtLeast(0f), 0f),
                                size = Size(shadowW, size.height)
                            )
                            // Right curtain edge shadow
                            val rightEdge = (halfW + dx).coerceAtMost(size.width)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                                    startX = rightEdge,
                                    endX = (rightEdge + shadowW).coerceAtMost(size.width)
                                ),
                                topLeft = Offset(rightEdge, 0f),
                                size = Size(shadowW, size.height)
                            )
                        }
                    }

                    CanvasTransitionType.MESH_LIQUID_RIPPLE -> {
                        val bA = bitmapA
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val maxD = hypot(size.width, size.height) / 2f

                        // 1. Draw Card A as base
                        drawLayer(layerA)

                        // 2. Reveal Card B strictly through expanding wavefront (0% at start)
                        val waveFront = currentP * maxD * 1.35f
                        if (waveFront > 1f) {
                            val wavePath = Path().apply {
                                addOval(Rect(cx - waveFront, cy - waveFront, cx + waveFront, cy + waveFront))
                            }
                            clipPath(wavePath) {
                                drawLayer(layerB)
                            }
                        }

                        // 3. Deformed liquid ripple mesh along the wave boundary
                        if (bA != null && bA.width > 0 && bA.height > 0 && currentP > 0.01f && currentP < 0.99f) {
                            val cols = 18
                            val rows = 18
                            val cellW = size.width / cols
                            val cellH = size.height / rows
                            val positions = ArrayList<Offset>((cols + 1) * (rows + 1))
                            val texCoords = ArrayList<Offset>((cols + 1) * (rows + 1))
                            val colors = ArrayList<Color>((cols + 1) * (rows + 1))
                            val indices = ArrayList<Int>(cols * rows * 6)

                            val waveAmp = sin(currentP * PI.toFloat()) * 38.dp.toPx()

                            for (r in 0..rows) {
                                for (c in 0..cols) {
                                    val ox = c * cellW
                                    val oy = r * cellH
                                    val d = hypot(ox - cx, oy - cy)
                                    val angle = if (d > 0.001f) atan2(oy - cy, ox - cx) else 0f
                                    val distFromFront = (d - waveFront) / (maxD * 0.4f)
                                    val waveEnvelope = (1f - kotlin.math.abs(distFromFront)).coerceIn(0f, 1f)
                                    val disp = sin(d * 0.032f - currentP * 12f) * waveAmp * waveEnvelope
                                    val px = ox + cos(angle) * disp
                                    val py = oy + sin(angle) * disp

                                    positions.add(Offset(px, py))
                                    texCoords.add(Offset((c.toFloat() / cols) * bA.width, (r.toFloat() / rows) * bA.height))
                                    colors.add(Color.White)
                                }
                            }

                            for (r in 0 until rows) {
                                for (c in 0 until cols) {
                                    val i0 = r * (cols + 1) + c
                                    val i1 = i0 + 1
                                    val i2 = (r + 1) * (cols + 1) + c
                                    val i3 = i2 + 1
                                    indices.add(i0); indices.add(i1); indices.add(i2)
                                    indices.add(i1); indices.add(i3); indices.add(i2)
                                }
                            }

                            val vertices = Vertices(
                                vertexMode = VertexMode.Triangles,
                                positions = positions,
                                textureCoordinates = texCoords,
                                colors = colors,
                                indices = indices
                            )
                            val meshPaint = Paint().apply {
                                shader = ImageShader(bA)
                                alpha = (sin(currentP * PI.toFloat()) * 0.75f).coerceIn(0f, 1f)
                            }
                            drawContext.canvas.drawVertices(vertices, BlendMode.SrcOver, meshPaint)
                        }
                    }

                    CanvasTransitionType.MESH_SWIRL_VORTEX -> {
                        val bA = bitmapA
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val maxR = hypot(size.width, size.height) * 0.65f
                        val swirlStrength = sin(currentP * PI.toFloat()) * 4.2f

                        // 1. Draw Card A swirling base
                        if (bA != null && bA.width > 0 && bA.height > 0) {
                            val cols = 18
                            val rows = 18
                            val cellW = size.width / cols
                            val cellH = size.height / rows
                            val positions = ArrayList<Offset>((cols + 1) * (rows + 1))
                            val texCoords = ArrayList<Offset>((cols + 1) * (rows + 1))
                            val colors = ArrayList<Color>((cols + 1) * (rows + 1))
                            val indices = ArrayList<Int>(cols * rows * 6)

                            for (r in 0..rows) {
                                for (c in 0..cols) {
                                    val ox = c * cellW
                                    val oy = r * cellH
                                    val dx = ox - cx
                                    val dy = oy - cy
                                    val dist = hypot(dx, dy)
                                    val baseAngle = atan2(dy, dx)
                                    val falloff = (1f - dist / maxR).coerceIn(0f, 1f)
                                    val twist = falloff * falloff * swirlStrength
                                    val newAngle = baseAngle + twist

                                    val px = cx + cos(newAngle) * dist
                                    val py = cy + sin(newAngle) * dist

                                    positions.add(Offset(px, py))
                                    texCoords.add(Offset((c.toFloat() / cols) * bA.width, (r.toFloat() / rows) * bA.height))
                                    colors.add(Color.White)
                                }
                            }

                            for (r in 0 until rows) {
                                for (c in 0 until cols) {
                                    val i0 = r * (cols + 1) + c
                                    val i1 = i0 + 1
                                    val i2 = (r + 1) * (cols + 1) + c
                                    val i3 = i2 + 1
                                    indices.add(i0); indices.add(i1); indices.add(i2)
                                    indices.add(i1); indices.add(i3); indices.add(i2)
                                }
                            }

                            val vertices = Vertices(
                                vertexMode = VertexMode.Triangles,
                                positions = positions,
                                textureCoordinates = texCoords,
                                colors = colors,
                                indices = indices
                            )
                            val meshPaint = Paint().apply {
                                shader = ImageShader(bA)
                                alpha = (1f - currentP).coerceIn(0f, 1f)
                            }
                            drawContext.canvas.drawVertices(vertices, BlendMode.SrcOver, meshPaint)
                        } else {
                            drawLayer(layerA)
                        }

                        // 2. Card B ONLY blooms outward from the vortex center eye (0% at start)
                        val eyeRadius = currentP * maxR * 1.35f
                        if (eyeRadius > 1f) {
                            val eyePath = Path().apply {
                                addOval(Rect(cx - eyeRadius, cy - eyeRadius, cx + eyeRadius, cy + eyeRadius))
                            }
                            clipPath(eyePath) {
                                drawLayer(layerB)
                            }
                        }
                    }

                    CanvasTransitionType.CHROMATIC_RGB_SPLIT -> {
                        val dx = sin(currentP * PI.toFloat()) * 26.dp.toPx()
                        val bounds = Rect(0f, 0f, size.width, size.height)

                        // 1. Draw Card A with lateral RGB splitting
                        val alphaA = (1f - currentP).coerceIn(0f, 1f)
                        if (alphaA > 0.01f) {
                            val redPaint = Paint().apply {
                                colorFilter = ColorFilter.colorMatrix(
                                    ColorMatrix(
                                        floatArrayOf(
                                            1f, 0f, 0f, 0f, 0f,
                                            0f, 0f, 0f, 0f, 0f,
                                            0f, 0f, 0f, 0f, 0f,
                                            0f, 0f, 0f, alphaA, 0f
                                        )
                                    )
                                )
                            }
                            drawContext.canvas.saveLayer(bounds, redPaint)
                            translate(left = -dx, top = 0f) {
                                drawLayer(layerA)
                            }
                            drawContext.canvas.restore()

                            val greenPaint = Paint().apply {
                                colorFilter = ColorFilter.colorMatrix(
                                    ColorMatrix(
                                        floatArrayOf(
                                            0f, 0f, 0f, 0f, 0f,
                                            0f, 1f, 0f, 0f, 0f,
                                            0f, 0f, 0f, 0f, 0f,
                                            0f, 0f, 0f, alphaA, 0f
                                        )
                                    )
                                )
                                blendMode = BlendMode.Plus
                            }
                            drawContext.canvas.saveLayer(bounds, greenPaint)
                            drawLayer(layerA)
                            drawContext.canvas.restore()

                            val bluePaint = Paint().apply {
                                colorFilter = ColorFilter.colorMatrix(
                                    ColorMatrix(
                                        floatArrayOf(
                                            0f, 0f, 0f, 0f, 0f,
                                            0f, 0f, 0f, 0f, 0f,
                                            0f, 0f, 1f, 0f, 0f,
                                            0f, 0f, 0f, alphaA, 0f
                                        )
                                    )
                                )
                                blendMode = BlendMode.Plus
                            }
                            drawContext.canvas.saveLayer(bounds, bluePaint)
                            translate(left = dx, top = 0f) {
                                drawLayer(layerA)
                            }
                            drawContext.canvas.restore()
                        }

                        // 2. Card B strictly fades in according to progress (0.0 at start)
                        val alphaB = (currentP * currentP).coerceIn(0f, 1f)
                        if (alphaB > 0.01f) {
                            val paintB = Paint().apply {
                                alpha = alphaB
                            }
                            drawContext.canvas.saveLayer(bounds, paintB)
                            drawLayer(layerB)
                            drawContext.canvas.restore()
                        }
                    }

                    CanvasTransitionType.PIXEL_BAYER_DITHER -> {
                        drawLayer(layerA)
                        val bayer8 = intArrayOf(
                             0, 32,  8, 40,  2, 34, 10, 42,
                            48, 16, 56, 24, 50, 18, 58, 26,
                            12, 44,  4, 36, 14, 46,  6, 38,
                            60, 28, 52, 20, 62, 30, 54, 22,
                             3, 35, 11, 43,  1, 33,  9, 41,
                            51, 19, 59, 27, 49, 17, 57, 25,
                            15, 47,  7, 39, 13, 45,  5, 37,
                            63, 31, 55, 23, 61, 29, 53, 21
                        )
                        val cellSize = 18.dp.toPx()
                        val cols = (size.width / cellSize).toInt() + 1
                        val rows = (size.height / cellSize).toInt() + 1
                        val ditherPath = Path()

                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                val bayerVal = bayer8[(r % 8) * 8 + (c % 8)] / 64f
                                if (currentP >= bayerVal) {
                                    ditherPath.addRect(
                                        Rect(
                                            c * cellSize,
                                            r * cellSize,
                                            (c + 1) * cellSize,
                                            (r + 1) * cellSize
                                        )
                                    )
                                }
                            }
                        }
                        clipPath(ditherPath) {
                            drawLayer(layerB)
                        }
                    }

                    CanvasTransitionType.HALFTONE_DISSOLVE -> {
                        drawLayer(layerA)
                        val dotSpacing = 20.dp.toPx()
                        val cols = (size.width / dotSpacing).toInt() + 1
                        val rows = (size.height / dotSpacing).toInt() + 1
                        val maxRadius = dotSpacing * 0.85f
                        val halftonePath = Path()

                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                val cx = c * dotSpacing
                                val cy = r * dotSpacing
                                val threshold = (c.toFloat() / cols * 0.6f + r.toFloat() / rows * 0.4f)
                                val localP = ((currentP * 1.5f) - threshold).coerceIn(0f, 1f)
                                if (localP > 0.001f) {
                                    val rad = localP * maxRadius
                                    halftonePath.addOval(Rect(cx - rad, cy - rad, cx + rad, cy + rad))
                                }
                            }
                        }
                        if (currentP > 0.001f) {
                            clipPath(halftonePath) {
                                drawLayer(layerB)
                            }
                        }
                    }

                    CanvasTransitionType.SPIRAL_NAUTILUS -> {
                        drawLayer(layerA)
                        if (currentP > 0.001f) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val maxRadius = kotlin.math.hypot(size.width, size.height) / 2f * 1.2f
                            val maxTheta = 4f * kotlin.math.PI.toFloat()
                            val currentTheta = currentP * maxTheta
                            val spiralPath = Path()

                            spiralPath.moveTo(cx, cy)
                            val steps = 120
                            for (i in 1..steps) {
                                val t = (i.toFloat() / steps) * currentTheta
                                val r = (t / maxTheta) * maxRadius
                                val x = cx + r * kotlin.math.cos(t)
                                val y = cy + r * kotlin.math.sin(t)
                                spiralPath.lineTo(x, y)
                            }
                            spiralPath.lineTo(cx, cy)
                            spiralPath.close()

                            val coreRadius = currentP * maxRadius * 0.6f
                            spiralPath.addOval(Rect(cx - coreRadius, cy - coreRadius, cx + coreRadius, cy + coreRadius))

                            clipPath(spiralPath) {
                                drawLayer(layerB)
                            }
                        }
                    }

                    CanvasTransitionType.DOOR_SWING_3D -> {
                        val halfPI = (kotlin.math.PI / 2.0).toFloat()
                        if (currentP < 0.5f) {
                            val normP = currentP * 2f
                            val scaleX = kotlin.math.cos(normP * halfPI).coerceAtLeast(0.001f)
                            val shadowAlpha = (normP * 0.75f).coerceIn(0f, 1f)

                            if (scaleX < 0.99f) {
                                clipRect(left = size.width * scaleX, top = 0f, right = size.width, bottom = size.height) {
                                    drawLayer(layerB)
                                }
                            }

                            clipRect(left = 0f, top = 0f, right = size.width * scaleX, bottom = size.height) {
                                scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(0f, size.height / 2f)) {
                                    drawLayer(layerA)
                                }
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = shadowAlpha)),
                                        startX = 0f,
                                        endX = size.width * scaleX
                                    ),
                                    topLeft = Offset(0f, 0f),
                                    size = Size(size.width * scaleX, size.height)
                                )
                            }
                        } else {
                            val normP = (currentP - 0.5f) * 2f
                            val scaleX = kotlin.math.sin(normP * halfPI).coerceAtLeast(0.001f)
                            val shadowAlpha = ((1f - normP) * 0.75f).coerceIn(0f, 1f)

                            clipRect(left = 0f, top = 0f, right = size.width * scaleX, bottom = size.height) {
                                scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(0f, size.height / 2f)) {
                                    drawLayer(layerB)
                                }
                                if (shadowAlpha > 0.01f) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = shadowAlpha)),
                                            startX = 0f,
                                            endX = size.width * scaleX
                                        ),
                                        topLeft = Offset(0f, 0f),
                                        size = Size(size.width * scaleX, size.height)
                                    )
                                }
                            }
                        }
                    }

                    CanvasTransitionType.CONCENTRIC_RINGS -> {
                        drawLayer(layerA)
                        if (currentP > 0.001f) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val maxRadius = kotlin.math.hypot(size.width, size.height) / 2f * 1.1f
                            val ringCount = 8
                            val ringStep = maxRadius / ringCount
                            val ringsPath = Path()

                            for (i in 0 until ringCount) {
                                val delay = i * 0.08f
                                val ringP = ((currentP - delay) / (1f - delay)).coerceIn(0f, 1f)
                                if (ringP > 0.001f) {
                                    val rOuter = (i + 1) * ringStep * ringP
                                    val rInner = i * ringStep * ringP
                                    if (rOuter > rInner) {
                                        ringsPath.addOval(Rect(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter))
                                    }
                                }
                            }
                            clipPath(ringsPath) {
                                drawLayer(layerB)
                            }
                        }
                    }

                    CanvasTransitionType.POLYGON_SHATTER -> {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val sectors = 16
                        val sectorAngle = (2.0 * kotlin.math.PI / sectors).toFloat()
                        val maxD = kotlin.math.hypot(size.width, size.height)

                        if (currentP > 0.001f) {
                            val openRadius = currentP * maxD * 0.7f
                            val centerHole = Path().apply {
                                addOval(Rect(cx - openRadius, cy - openRadius, cx + openRadius, cy + openRadius))
                            }
                            clipPath(centerHole) {
                                drawLayer(layerB)
                            }
                        }

                        val alphaA = (1f - currentP * currentP).coerceIn(0f, 1f)
                        if (alphaA > 0.01f) {
                            for (s in 0 until sectors) {
                                val a1 = s * sectorAngle
                                val a2 = (s + 1) * sectorAngle
                                val midA = (a1 + a2) / 2f

                                val flyDist = currentP * currentP * maxD * 0.8f
                                val tx = kotlin.math.cos(midA) * flyDist
                                val ty = kotlin.math.sin(midA) * flyDist
                                val rot = currentP * 45f * if (s % 2 == 0) 1f else -1f

                                val triPath = Path().apply {
                                    moveTo(cx, cy)
                                    lineTo(cx + kotlin.math.cos(a1) * maxD, cy + kotlin.math.sin(a1) * maxD)
                                    lineTo(cx + kotlin.math.cos(a2) * maxD, cy + kotlin.math.sin(a2) * maxD)
                                    close()
                                }

                                clipPath(triPath) {
                                    translate(left = tx, top = ty) {
                                        rotate(degrees = rot, pivot = Offset(cx, cy)) {
                                            drawLayer(layerA)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
