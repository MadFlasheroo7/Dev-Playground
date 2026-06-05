package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

/** Newton's cradle — pendulums that transfer momentum end to end. Tap to swing the left ball. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewtonsCradleLabScreen(onBack: () -> Unit) {
    val count = 5
    val angle = remember { FloatArray(count) }
    val vel = remember { FloatArray(count) }
    var tick by remember { mutableIntStateOf(0) }
    var kicked by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        angle[0] = -0.8f
        val g = 0.012f
        while (true) {
            withFrameNanos {
                for (i in 0 until count) { vel[i] += -g * sin(angle[i]); vel[i] *= 0.9995f; angle[i] += vel[i] }
                // collision: adjacent balls touching at bottom transfer velocity
                for (i in 0 until count - 1) {
                    if (angle[i] > angle[i + 1] - 0.001f && angle[i] >= -0.02f && angle[i + 1] <= 0.02f) {
                        if (vel[i] > 0 && vel[i + 1] <= vel[i]) { val t = vel[i]; vel[i] = vel[i + 1]; vel[i + 1] = t }
                    }
                }
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Newton's Cradle") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF11131A))
                .pointerInput(Unit) { detectTapGestures { for (i in 0 until count) vel[i] = 0f; angle[0] = -0.9f; angle.fill(0f, 1, count); kicked++ } }
                .drawBehind {
                    tick
                    val topY = size.height * 0.2f
                    val len = size.height * 0.45f
                    val rad = 24f
                    val spacing = rad * 2f
                    val startX = size.width / 2f - spacing * (count - 1) / 2f
                    for (i in 0 until count) {
                        val pivotX = startX + i * spacing
                        val bx = pivotX + sin(angle[i]) * len
                        val by = topY + cos(angle[i]) * len
                        drawLine(Color(0xFF888FA0), Offset(pivotX, topY), Offset(bx, by), 2f)
                        drawCircle(Color(0xFFB0BEC5), rad, Offset(bx, by))
                        drawCircle(Color(0x55000000), rad, Offset(bx, by), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                    }
                })
        }
    }
}
