package pro.jayeshseth.devcontrols.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** Cutout & Insets visualizer — draws the display cutout + system-bar inset rects. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutoutInsetsLabScreen(onBack: () -> Unit) {
    val view = LocalView.current
    val insets = ViewCompat.getRootWindowInsets(view)
    val cutout = insets?.displayCutout
    val bars = insets?.getInsets(WindowInsetsCompat.Type.systemBars())
    val ime = insets?.getInsets(WindowInsetsCompat.Type.ime())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Cutout & Insets") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Box(Modifier.fillMaxSize().drawBehind {
                // system bar inset bands
                bars?.let {
                    val c = Color(0x3300C2FF)
                    drawRect(c, Offset(0f, 0f), Size(size.width, it.top.toFloat()))
                    drawRect(c, Offset(0f, size.height - it.bottom), Size(size.width, it.bottom.toFloat()))
                    drawRect(c, Offset(0f, 0f), Size(it.left.toFloat(), size.height))
                    drawRect(c, Offset(size.width - it.right, 0f), Size(it.right.toFloat(), size.height))
                }
                // cutout bounding rects
                cutout?.boundingRects?.forEach { r ->
                    drawRect(Color(0x55FF3366), Offset(r.left.toFloat(), r.top.toFloat()),
                        Size(r.width().toFloat(), r.height().toFloat()))
                }
            }) {
                Column(Modifier.padding(20.dp)) {
                    Text("System bars inset (px):", style = MaterialTheme.typography.titleSmall)
                    Text("top=${bars?.top} bottom=${bars?.bottom} left=${bars?.left} right=${bars?.right}", fontFamily = FontFamily.Monospace)
                    Text("IME inset bottom=${ime?.bottom}", fontFamily = FontFamily.Monospace)
                    Text("Cutout rects: ${cutout?.boundingRects?.size ?: 0}", style = MaterialTheme.typography.titleSmall)
                    cutout?.boundingRects?.forEachIndexed { i, r ->
                        Text("  #$i $r", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                    if (cutout == null) Text("No display cutout reported on this device/orientation.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
