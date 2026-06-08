package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

private class Mesh(val verts: Array<FloatArray>, val faces: Array<IntArray>)

private fun cube() = Mesh(
    arrayOf(floatArrayOf(-1f,-1f,-1f),floatArrayOf(1f,-1f,-1f),floatArrayOf(1f,1f,-1f),floatArrayOf(-1f,1f,-1f),
        floatArrayOf(-1f,-1f,1f),floatArrayOf(1f,-1f,1f),floatArrayOf(1f,1f,1f),floatArrayOf(-1f,1f,1f)),
    arrayOf(intArrayOf(0,1,2,3),intArrayOf(5,4,7,6),intArrayOf(4,0,3,7),intArrayOf(1,5,6,2),intArrayOf(4,5,1,0),intArrayOf(3,2,6,7))
)
private fun octa() = Mesh(
    arrayOf(floatArrayOf(1f,0f,0f),floatArrayOf(-1f,0f,0f),floatArrayOf(0f,1f,0f),floatArrayOf(0f,-1f,0f),floatArrayOf(0f,0f,1f),floatArrayOf(0f,0f,-1f)),
    arrayOf(intArrayOf(0,2,4),intArrayOf(2,1,4),intArrayOf(1,3,4),intArrayOf(3,0,4),intArrayOf(2,0,5),intArrayOf(1,2,5),intArrayOf(3,1,5),intArrayOf(0,3,5))
)

/** Flat-shaded solids — rotating polyhedra with back-face culling + Lambert lighting (painter's order). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SolidsLabScreen(onBack: () -> Unit) {
    var shape by remember { mutableIntStateOf(0) }
    val mesh = remember(shape) { if (shape == 0) cube() else octa() }
    var ay by remember { mutableFloatStateOf(0f) }
    var ax by remember { mutableFloatStateOf(0.5f) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { ay += 0.012f; ax += 0.006f } } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Solids") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(shape == 0, { shape = 0 }, { Text("Cube") })
                FilterChip(shape == 1, { shape = 1 }, { Text("Octahedron") })
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF06060E)).drawBehind {
                val cx = size.width / 2f; val cy = size.height / 2f
                val scale = minOf(cx, cy) * 0.6f
                val rv = mesh.verts.map { val (x, y, z) = rotate(it[0], it[1], it[2], ax, ay); floatArrayOf(x, y, z) }
                val lightDir = floatArrayOf(0.4f, -0.5f, -0.76f)
                // painter's: sort faces by avg z desc (far first)
                val order = mesh.faces.indices.sortedByDescending { fi -> mesh.faces[fi].map { rv[it][2] }.average() }
                for (fi in order) {
                    val f = mesh.faces[fi]
                    val a = rv[f[0]]; val b = rv[f[1]]; val c = rv[f[2]]
                    // normal
                    val ux = b[0]-a[0]; val uy = b[1]-a[1]; val uz = b[2]-a[2]
                    val vx = c[0]-a[0]; val vy = c[1]-a[1]; val vz = c[2]-a[2]
                    var nx = uy*vz - uz*vy; var ny = uz*vx - ux*vz; var nz = ux*vy - uy*vx
                    val nl = sqrt(nx*nx+ny*ny+nz*nz); if (nl==0f) continue; nx/=nl; ny/=nl; nz/=nl
                    if (nz > 0) continue // back-face cull (viewer looks -z)
                    val lambert = (-(nx*lightDir[0]+ny*lightDir[1]+nz*lightDir[2])).coerceIn(0.15f, 1f)
                    val path = Path()
                    f.forEachIndexed { k, vi -> val sx = cx + rv[vi][0]*scale; val sy = cy + rv[vi][1]*scale; if (k==0) path.moveTo(sx, sy) else path.lineTo(sx, sy) }
                    path.close()
                    drawPath(path, Color(red = 0.2f*lambert+0.05f, green = 0.7f*lambert, blue = 0.9f*lambert, alpha = 1f))
                }
            })
        }
    }
}
