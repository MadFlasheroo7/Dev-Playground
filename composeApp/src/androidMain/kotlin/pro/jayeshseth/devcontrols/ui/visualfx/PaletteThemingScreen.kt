package pro.jayeshseth.devcontrols.ui.visualfx

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Palette theming — pick a photo, extract dominant colors, re-theme a sample UI live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteThemingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var swatches by remember { mutableStateOf<List<Color>>(listOf(Color(0xFF6750A4), Color(0xFF625B71), Color(0xFF7D5260))) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) runCatching {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            val bmp = ImageDecoder.decodeBitmap(src) { d, _, _ -> d.setTargetSize(64, 64); d.isMutableRequired = false }
            swatches = extractPalette(bmp)
        }
    }

    val primary = swatches.getOrElse(0) { Color(0xFF6750A4) }
    val secondary = swatches.getOrElse(1) { primary }
    val tertiary = swatches.getOrElse(2) { secondary }
    fun on(c: Color) = if (c.luminance() > 0.5f) Color.Black else Color.White

    Scaffold(
        topBar = { TopAppBar(title = { Text("Palette Theming") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().background(primary.copy(alpha = 0.12f)).padding(16.dp)) {
            Button(onClick = { pick.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Pick a photo") }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                swatches.forEach { c -> Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(c)) }
            }
            Spacer(Modifier.height(20.dp))
            // sample themed UI
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(primary).padding(20.dp)) {
                Text("Themed Card", color = on(primary), fontWeight = FontWeight.Bold, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Text("Colors extracted from your photo.", color = on(primary).copy(alpha = 0.8f))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(secondary).padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Action", color = on(secondary)) }
                    Box(Modifier.clip(RoundedCornerShape(50)).background(tertiary).padding(horizontal = 16.dp, vertical = 8.dp)) { Text("More", color = on(tertiary)) }
                }
            }
        }
    }
}

private fun extractPalette(bmp: Bitmap): List<Color> {
    val buckets = HashMap<Int, IntArray>() // key → [count, rSum, gSum, bSum]
    val w = bmp.width; val h = bmp.height
    for (y in 0 until h) for (x in 0 until w) {
        val p = bmp.getPixel(x, y)
        val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
        val key = (r shr 5 shl 6) or (g shr 5 shl 3) or (b shr 5)
        val acc = buckets.getOrPut(key) { IntArray(4) }
        acc[0]++; acc[1] += r; acc[2] += g; acc[3] += b
    }
    return buckets.values.sortedByDescending { it[0] }
        .map { Color(it[1] / it[0], it[2] / it[0], it[3] / it[0]) }
        .filter { it.luminance() in 0.06f..0.94f }
        .take(4)
        .ifEmpty { listOf(Color(0xFF6750A4)) }
}
