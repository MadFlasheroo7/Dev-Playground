package pro.jayeshseth.devcontrols.ui.present

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Renders one slide. Reused by present mode (and later the editor + export). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SlideRenderer(slide: Slide, shared: SharedTransitionScope, av: AnimatedVisibilityScope, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize().background(Brush.linearGradient(slide.bgColors.map { Color(it) }))) {
        val wPx = constraints.maxWidth.toFloat(); val hPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        slide.elements.forEach { el -> key(el.id) { ElementView(el, wPx, hPx, density, shared, av) } }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ElementView(el: SlideElement, wPx: Float, hPx: Float, density: Density, shared: SharedTransitionScope, av: AnimatedVisibilityScope) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(420)) }

    var mod: Modifier = Modifier
        .offset { IntOffset((el.x * wPx).roundToInt(), (el.y * hPx).roundToInt()) }
        .size(with(density) { (el.w * wPx).toDp() }, with(density) { (el.h * hPx).toDp() })
    if (el.sharedKey != null) {
        mod = with(shared) { mod.sharedElement(rememberSharedContentState(el.sharedKey), av) }
    }
    mod = mod.graphicsLayer {
        when (el.entrance) {
            Entrance.FADE_UP -> { alpha = enter.value; translationY = (1f - enter.value) * hPx * 0.04f }
            Entrance.SCALE_IN -> { alpha = enter.value; val s = 0.85f + 0.15f * enter.value; scaleX = s; scaleY = s }
            else -> {}
        }
    }

    Box(mod, contentAlignment = if (el.centerAlign) Alignment.Center else Alignment.CenterStart) {
        when (el.type) {
            ElementType.TITLE, ElementType.SUBTITLE, ElementType.BODY ->
                Text(el.text ?: "", color = Color(el.colorArgb), fontSize = el.fontSize.sp, fontWeight = FontWeight(el.weight),
                    textAlign = if (el.centerAlign) TextAlign.Center else TextAlign.Start)
            ElementType.BULLETS ->
                Column { el.bullets.forEachIndexed { i, b -> BulletRow(b, i, el) } }
            ElementType.SHAPE ->
                Box(Modifier.fillMaxSize().clip(if (el.shape == ShapeKind.CIRCLE) CircleShape else RoundedCornerShape(18.dp)).background(Color(el.colorArgb)))
        }
    }
}

@Composable
private fun BulletRow(text: String, index: Int, el: SlideElement) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(index * 110L); a.animateTo(1f, tween(320)) }
    Row(Modifier.graphicsLayer { alpha = a.value; translationX = (1f - a.value) * 40f }.padding(vertical = 7.dp)) {
        Text("•  ", color = Color(el.colorArgb), fontSize = el.fontSize.sp)
        Text(text, color = Color(el.colorArgb), fontSize = el.fontSize.sp)
    }
}
