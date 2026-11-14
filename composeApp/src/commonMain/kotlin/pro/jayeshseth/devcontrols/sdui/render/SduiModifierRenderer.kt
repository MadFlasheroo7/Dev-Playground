package pro.jayeshseth.devcontrols.sdui.render

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.sdui.model.SduiModifier
import pro.jayeshseth.devcontrols.sdui.state.SduiActionRunner

/**
 * Transforms SDUI declarative modifier models into Compose Modifiers,
 * resolving reactive expressions against [SduiRenderContext.store].
 */
object SduiModifierRenderer {

    @Composable
    fun render(
        baseModifier: Modifier = Modifier,
        modifiers: List<SduiModifier>,
        context: SduiRenderContext
    ): Modifier {
        var result = baseModifier
        val store = context.store

        modifiers.forEach { mod ->
            result = when (mod) {
                is SduiModifier.Padding -> {
                    val all = mod.all?.let { store.evalFloat(it, 0f) }
                    if (all != null && all > 0f) {
                        result.padding(all.dp)
                    } else {
                        val h = mod.horizontal?.let { store.evalFloat(it, 0f) } ?: 0f
                        val v = mod.vertical?.let { store.evalFloat(it, 0f) } ?: 0f
                        val start = mod.start?.let { store.evalFloat(it, h) } ?: h
                        val top = mod.top?.let { store.evalFloat(it, v) } ?: v
                        val end = mod.end?.let { store.evalFloat(it, h) } ?: h
                        val bottom = mod.bottom?.let { store.evalFloat(it, v) } ?: v
                        result.padding(start.dp, top.dp, end.dp, bottom.dp)
                    }
                }
                is SduiModifier.Size -> {
                    var m = result
                    if (mod.fillMaxSize != null) {
                        val frac = store.evalFloat(mod.fillMaxSize, 1f)
                        m = m.fillMaxSize(frac)
                    } else {
                        if (mod.fillMaxWidth != null) {
                            val frac = store.evalFloat(mod.fillMaxWidth, 1f)
                            m = m.fillMaxWidth(frac)
                        }
                        if (mod.fillMaxHeight != null) {
                            val frac = store.evalFloat(mod.fillMaxHeight, 1f)
                            m = m.fillMaxHeight(frac)
                        }
                    }
                    if (mod.width != null) {
                        val w = store.evalFloat(mod.width, -1f)
                        if (w > 0f) m = m.width(w.dp)
                    }
                    if (mod.height != null) {
                        val h = store.evalFloat(mod.height, -1f)
                        if (h > 0f) m = m.height(h.dp)
                    }
                    if (mod.aspectRatio != null) {
                        val ratio = store.evalFloat(mod.aspectRatio, 1f)
                        m = m.aspectRatio(ratio)
                    }
                    m
                }
                is SduiModifier.Background -> {
                    val shape = parseShape("rounded", mod.cornerRadius ?: 0f)
                    if (!mod.gradientColors.isNullOrEmpty()) {
                        val colors = mod.gradientColors.map { SduiColorParser.parse(store.evalString(it), Color.Transparent) }
                        val brush = when (mod.gradientType.lowercase()) {
                            "radialgradient" -> Brush.radialGradient(colors)
                            else -> Brush.linearGradient(colors)
                        }
                        result.background(brush = brush, shape = shape)
                    } else {
                        val color = SduiColorParser.parse(store.evalString(mod.color), Color.Transparent)
                        result.background(color = color, shape = shape)
                    }
                }
                is SduiModifier.Border -> {
                    val w = store.evalFloat(mod.width, 1f)
                    val color = SduiColorParser.parse(store.evalString(mod.color), Color.Gray)
                    val radius = store.evalFloat(mod.cornerRadius, 0f)
                    val shape = parseShape(mod.shape, radius)
                    result.border(w.dp, color, shape)
                }
                is SduiModifier.Clip -> {
                    val radius = store.evalFloat(mod.cornerRadius, 0f)
                    val shape = parseShape(mod.shape, radius)
                    result.clip(shape)
                }
                is SduiModifier.Shadow -> {
                    val elevation = store.evalFloat(mod.elevation, 4f)
                    val radius = store.evalFloat(mod.cornerRadius, 0f)
                    val shape = parseShape(mod.shape, radius)
                    val ambient = SduiColorParser.parse(store.evalString(mod.ambientColor), Color.Black.copy(alpha = 0.2f))
                    val spot = SduiColorParser.parse(store.evalString(mod.spotColor), Color.Black.copy(alpha = 0.4f))
                    result.shadow(
                        elevation = elevation.dp,
                        shape = shape,
                        clip = mod.clip,
                        ambientColor = ambient,
                        spotColor = spot
                    )
                }
                is SduiModifier.Blur -> {
                    val radius = store.evalFloat(mod.radius, 10f)
                    result.blur(radius.dp)
                }
                is SduiModifier.GraphicsLayer -> {
                    val sX = mod.scaleX?.let { store.evalFloat(it, 1f) } ?: 1f
                    val sY = mod.scaleY?.let { store.evalFloat(it, 1f) } ?: 1f
                    val rX = mod.rotationX?.let { store.evalFloat(it, 0f) } ?: 0f
                    val rY = mod.rotationY?.let { store.evalFloat(it, 0f) } ?: 0f
                    val rZ = mod.rotationZ?.let { store.evalFloat(it, 0f) } ?: 0f
                    val tX = mod.translationX?.let { store.evalFloat(it, 0f) } ?: 0f
                    val tY = mod.translationY?.let { store.evalFloat(it, 0f) } ?: 0f
                    val a = mod.alpha?.let { store.evalFloat(it, 1f) } ?: 1f
                    val pX = mod.transformOriginX?.let { store.evalFloat(it, 0.5f) } ?: 0.5f
                    val pY = mod.transformOriginY?.let { store.evalFloat(it, 0.5f) } ?: 0.5f

                    result.graphicsLayer {
                        scaleX = sX
                        scaleY = sY
                        rotationX = rX
                        rotationY = rY
                        rotationZ = rZ
                        translationX = tX
                        translationY = tY
                        alpha = a
                        transformOrigin = TransformOrigin(pX, pY)
                        clip = mod.clip
                    }
                }
                is SduiModifier.Clickable -> {
                    result.clickable(
                        enabled = true,
                        onClick = {
                            context.actionRunner.execute(mod.actions)
                        }
                    )
                }
                is SduiModifier.GestureDrag -> {
                    result.pointerInput(mod.targetStateKey) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val currentOffset = store.evalFloat(mod.targetStateKey, 0f)
                                val delta = when (mod.orientation.lowercase()) {
                                    "vertical" -> dragAmount.y
                                    else -> dragAmount.x
                                }
                                val newOffset = (currentOffset + delta).coerceIn(mod.minOffset, mod.maxOffset)
                                store.set(mod.targetStateKey, newOffset)
                            },
                            onDragEnd = {
                                if (mod.snapBack) {
                                    store.set(mod.targetStateKey, 0f)
                                }
                            }
                        )
                    }
                }
                is SduiModifier.AnimateContentSize -> {
                    val spec = SduiActionRunner.toComposeIntSizeAnimationSpec(mod.animation)
                    result.animateContentSize(animationSpec = spec)
                }
                is SduiModifier.Unknown -> {
                    // Gracefully skip unknown modifier
                    result
                }
            }
        }

        return result
    }

    fun parseShape(shape: String, cornerRadius: Float): Shape {
        return when (shape.lowercase()) {
            "circle" -> CircleShape
            "cutcorner" -> CutCornerShape(cornerRadius.dp)
            else -> RoundedCornerShape(cornerRadius.dp)
        }
    }
}
