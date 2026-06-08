package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.random.Random

/** Matrix rain — columns of falling glyphs with a bright leading head. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixRainLabScreen(onBack: () -> Unit) {
    val cell = 28f
    val charset = "ｱｲｳｴｵｶｷｸ0123456789ABCDEF"
    var size by remember { mutableStateOf(IntSize.Zero) }
    var heads by remember { mutableStateOf(FloatArray(0)) }
    var speeds by remember { mutableStateOf(FloatArray(0)) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(size) {
        if (size == IntSize.Zero) return@LaunchedEffect
        val n = (size.width / cell).toInt() + 1
        heads = FloatArray(n) { Random.nextFloat() * size.height }
        speeds = FloatArray(n) { 6f + Random.nextFloat() * 12f }
        while (true) {
            withFrameNanos {
                for (i in heads.indices) { heads[i] += speeds[i]; if (heads[i] > size.height + cell * 12) heads[i] = -Random.nextFloat() * 400f }
                tick++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Matrix Rain") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black).onSizeChanged { size = it }.drawBehind {
                tick
                val paint = android.graphics.Paint().apply { textSize = cell * 0.9f; isAntiAlias = true }
                val canvas = drawContext.canvas.nativeCanvas
                for (col in heads.indices) {
                    val x = col * cell
                    val headRow = (heads[col] / cell).toInt()
                    for (k in 0 until 14) {
                        val row = headRow - k
                        val y = row * cell
                        if (y < -cell || y > size.height + cell) continue
                        val ch = charset[(col * 31 + row * 17 + tick / 4) % charset.length].toString()
                        paint.color = if (k == 0) android.graphics.Color.rgb(0xCC, 0xFF, 0xCC)
                        else android.graphics.Color.argb((220 - k * 16).coerceAtLeast(20), 0x33, 0xFF, 0x55)
                        canvas.drawText(ch, x, y, paint)
                    }
                }
            })
        }
    }
}
