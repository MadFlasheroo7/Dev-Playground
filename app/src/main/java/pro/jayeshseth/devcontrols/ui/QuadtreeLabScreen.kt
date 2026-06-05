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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** Quadtree — drifting points; the tree subdivides any cell holding more than capacity. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuadtreeLabScreen(onBack: () -> Unit) {
    val n = 80
    val px = remember { FloatArray(n) }
    val py = remember { FloatArray(n) }
    val vx = remember { FloatArray(n) { (Random.nextFloat() - 0.5f) * 4f } }
    val vy = remember { FloatArray(n) { (Random.nextFloat() - 0.5f) * 4f } }
    var tick by remember { mutableIntStateOf(0) }
    var seeded by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) { withFrameNanos { tick++ } }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Quadtree") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF07070F)).drawBehind {
                tick
                if (seeded == 0) { for (i in 0 until n) { px[i] = Random.nextFloat() * size.width; py[i] = Random.nextFloat() * size.height }; seeded = 1 }
                for (i in 0 until n) {
                    px[i] += vx[i]; py[i] += vy[i]
                    if (px[i] < 0 || px[i] > size.width) vx[i] = -vx[i]
                    if (py[i] < 0 || py[i] > size.height) vy[i] = -vy[i]
                    px[i] = px[i].coerceIn(0f, size.width); py[i] = py[i].coerceIn(0f, size.height)
                }
                val root = QNode(Rect(0f, 0f, size.width, size.height))
                for (i in 0 until n) root.insert(px[i], py[i])
                root.draw(this)
                for (i in 0 until n) drawCircle(Color(0xFF22FFAA), 3f, Offset(px[i], py[i]))
            })
        }
    }
}

private class QNode(val b: Rect, val depth: Int = 0) {
    val pts = ArrayList<Offset>(); var divided = false
    var nw: QNode? = null; var ne: QNode? = null; var sw: QNode? = null; var se: QNode? = null
    fun insert(x: Float, y: Float) {
        if (!b.contains(Offset(x, y))) return
        if (pts.size < 4 || depth > 6) { pts.add(Offset(x, y)); return }
        if (!divided) subdivide()
        nw!!.insert(x, y); ne!!.insert(x, y); sw!!.insert(x, y); se!!.insert(x, y)
    }
    private fun subdivide() {
        val hw = b.width / 2; val hh = b.height / 2
        nw = QNode(Rect(b.left, b.top, b.left + hw, b.top + hh), depth + 1)
        ne = QNode(Rect(b.left + hw, b.top, b.right, b.top + hh), depth + 1)
        sw = QNode(Rect(b.left, b.top + hh, b.left + hw, b.bottom), depth + 1)
        se = QNode(Rect(b.left + hw, b.top + hh, b.right, b.bottom), depth + 1)
        divided = true
    }
    fun draw(scope: DrawScope) {
        scope.drawRect(Color(0x3355AAFF), topLeft = Offset(b.left, b.top), size = Size(b.width, b.height), style = Stroke(1f))
        if (divided) { nw!!.draw(scope); ne!!.draw(scope); sw!!.draw(scope); se!!.draw(scope) }
    }
}
