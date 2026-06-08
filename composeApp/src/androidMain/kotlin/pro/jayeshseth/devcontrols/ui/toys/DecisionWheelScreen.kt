package pro.jayeshseth.devcontrols.ui.toys

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Decision wheel — spin to pick one of the options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionWheelScreen(onBack: () -> Unit) {
    val options = listOf("Yes", "No", "Maybe", "Ask again", "Definitely", "Nope", "Sure", "Later")
    val angle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf("") }
    val colors = listOf(0xFFEF5350, 0xFFAB47BC, 0xFF5C6BC0, 0xFF29B6F6, 0xFF66BB6A, 0xFFFFCA28, 0xFFFF7043, 0xFF26A69A).map { Color(it) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Decision Wheel") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.size(20.dp))
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(300.dp)) {
                    val sweep = 360f / options.size
                    rotate(angle.value) {
                        options.forEachIndexed { i, opt ->
                            drawArc(colors[i % colors.size], -90f + i * sweep, sweep, true)
                            // label
                            val mid = Math.toRadians((-90f + i * sweep + sweep / 2).toDouble())
                            val r = size.minDimension / 2 * 0.6
                            val x = size.width / 2 + Math.cos(mid).toFloat() * r.toFloat()
                            val y = size.height / 2 + Math.sin(mid).toFloat() * r.toFloat()
                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE; textAlign = android.graphics.Paint.Align.CENTER; textSize = 34f; isFakeBoldText = true
                            }
                            drawContext.canvas.nativeCanvas.drawText(opt, x, y, paint)
                        }
                    }
                }
                Text("▼", Modifier.align(Alignment.TopCenter), fontSize = 28.sp, color = Color.Black)
            }
            Spacer(Modifier.size(20.dp))
            Button(onClick = {
                scope.launch {
                    val spin = 360f * (4 + Random.nextInt(3)) + Random.nextFloat() * 360f
                    angle.animateTo(angle.value + spin, tween(2200))
                    val sweep = 360f / options.size
                    val norm = ((360f - (angle.value % 360f)) % 360f)
                    result = options[((norm + sweep / 2) / sweep).toInt() % options.size]
                }
            }) { Text("Spin") }
            Spacer(Modifier.size(16.dp))
            if (result.isNotEmpty()) Text(result, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
