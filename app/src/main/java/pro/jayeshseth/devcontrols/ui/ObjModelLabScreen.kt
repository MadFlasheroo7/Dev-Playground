package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

// Icosahedron in Wavefront OBJ (v + triangular f), parsed at runtime.
private const val OBJ = """
v 0 -0.5257 0.8507
v 0.8507 0 0.5257
v 0.8507 0 -0.5257
v -0.8507 0 -0.5257
v -0.8507 0 0.5257
v -0.5257 0.8507 0
v 0.5257 0.8507 0
v 0.5257 -0.8507 0
v -0.5257 -0.8507 0
v 0 -0.5257 -0.8507
v 0 0.5257 -0.8507
v 0 0.5257 0.8507
f 1 2 8
f 1 8 9
f 1 9 5
f 1 5 12
f 1 12 2
f 2 12 7
f 2 7 3
f 2 3 8
f 3 7 11
f 3 11 10
f 3 10 8
f 8 10 9
f 9 10 4
f 9 4 5
f 5 4 6
f 5 6 12
f 12 6 7
f 7 6 11
f 11 6 4
f 11 4 10
"""

/** OBJ model loader — parses an embedded Wavefront OBJ (icosahedron) and renders it shaded. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjModelLabScreen(onBack: () -> Unit) {
    val model = remember {
        val verts = ArrayList<FloatArray>(); val faces = ArrayList<IntArray>()
        OBJ.lineSequence().forEach { line ->
            val tok = line.trim().split(Regex("\\s+"))
            when (tok.firstOrNull()) {
                "v" -> verts.add(floatArrayOf(tok[1].toFloat(), tok[2].toFloat(), tok[3].toFloat()))
                "f" -> faces.add(IntArray(tok.size - 1) { tok[it + 1].substringBefore('/').toInt() - 1 })
            }
        }
        verts.toTypedArray() to faces.toTypedArray()
    }
    var ay by remember { mutableFloatStateOf(0f) }
    var ax by remember { mutableFloatStateOf(0.4f) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { ay += 0.012f; ax += 0.004f } } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("OBJ Model") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Text("${model.first.size} verts · ${model.second.size} faces (icosahedron.obj)",
                Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxSize().background(Color(0xFF06060E)).drawBehind {
                val cx = size.width / 2f; val cy = size.height / 2f; val scale = minOf(cx, cy) * 0.8f
                val rv = model.first.map { val (x, y, z) = rotate(it[0], it[1], it[2], ax, ay); floatArrayOf(x, y, z) }
                val light = floatArrayOf(0.3f, -0.4f, -0.85f)
                val order = model.second.indices.sortedByDescending { fi -> model.second[fi].map { rv[it][2] }.average() }
                for (fi in order) {
                    val f = model.second[fi]
                    val a = rv[f[0]]; val b = rv[f[1]]; val c = rv[f[2]]
                    var nx = (b[1]-a[1])*(c[2]-a[2]) - (b[2]-a[2])*(c[1]-a[1])
                    var ny = (b[2]-a[2])*(c[0]-a[0]) - (b[0]-a[0])*(c[2]-a[2])
                    var nz = (b[0]-a[0])*(c[1]-a[1]) - (b[1]-a[1])*(c[0]-a[0])
                    val nl = sqrt(nx*nx+ny*ny+nz*nz); if (nl == 0f) continue; nx/=nl; ny/=nl; nz/=nl
                    if (nz > 0) continue
                    val lam = (-(nx*light[0]+ny*light[1]+nz*light[2])).coerceIn(0.15f, 1f)
                    val path = Path()
                    f.forEachIndexed { k, vi -> val sx = cx + rv[vi][0]*scale; val sy = cy + rv[vi][1]*scale; if (k==0) path.moveTo(sx, sy) else path.lineTo(sx, sy) }
                    path.close()
                    drawPath(path, Color(red = 0.85f*lam, green = 0.45f*lam, blue = 0.2f*lam + 0.05f, alpha = 1f))
                }
            })
        }
    }
}
