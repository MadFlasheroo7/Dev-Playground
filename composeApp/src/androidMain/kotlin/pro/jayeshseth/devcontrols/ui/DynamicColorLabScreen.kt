package pro.jayeshseth.devcontrols.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dynamic-color lab. Pick an image; we extract its dominant colours by frequency
 * (downscale → quantize RGB into buckets → average the busiest ones — no `Palette`
 * dependency) and build a quick Material-ish scheme live, themed onto a sample card.
 * The "Material You from your wallpaper" trick, done by hand.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicColorLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var image by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var swatches by remember { mutableStateOf<List<Color>>(emptyList()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        image = uri
    }

    LaunchedEffect(image) {
        val uri = image ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val src = ImageDecoder.createSource(context.contentResolver, uri)
                val bmp = ImageDecoder.decodeBitmap(src) { d, _, _ ->
                    d.setTargetSampleSize(4)
                    // HARDWARE bitmaps can't be read with getPixel(); force software so
                    // extractColors() can sample them.
                    d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                preview = bmp.asImageBitmap()
                swatches = extractColors(bmp, 6)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dynamic Color") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Button(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(if (image == null) "Pick an image" else "Pick another") }

            preview?.let {
                Spacer(Modifier.height(12.dp))
                Image(
                    bitmap = it,
                    contentDescription = "Source",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                )
            }

            if (swatches.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Extracted palette", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    swatches.forEach { c ->
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(c))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Sample theme", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                val primary = swatches[0]
                val secondary = swatches.getOrElse(1) { primary }
                val bg = swatches.minByOrNull { it.luminance() } ?: primary
                SampleCard(primary, secondary, bg)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Colours are frequency-ranked from a downscaled copy of the image. This is the same " +
                    "idea as dynamic theming from the wallpaper, computed without the Palette library.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SampleCard(primary: Color, secondary: Color, bg: Color) {
    val onBg = if (bg.luminance() > 0.5f) Color.Black else Color.White
    val onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).padding(16.dp)) {
        Column {
            Text("Headline", color = onBg, fontWeight = FontWeight.Bold)
            Text("Body text themed from the extracted palette.", color = onBg.copy(alpha = 0.8f))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(primary).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Primary", color = onPrimary)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(secondary).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Secondary", color = if (secondary.luminance() > 0.5f) Color.Black else Color.White)
                }
            }
        }
    }
}

/** Frequency-based dominant-colour extraction (no Palette lib). */
private fun extractColors(bitmap: Bitmap, count: Int): List<Color> {
    val w = 48
    val h = 48
    val scaled = Bitmap.createScaledBitmap(bitmap, w, h, false)
    // key -> [count, rSum, gSum, bSum]
    val buckets = HashMap<Int, IntArray>()
    for (y in 0 until h) {
        for (x in 0 until w) {
            val px = scaled.getPixel(x, y)
            val r = android.graphics.Color.red(px)
            val g = android.graphics.Color.green(px)
            val b = android.graphics.Color.blue(px)
            val key = (r shr 4 shl 8) or (g shr 4 shl 4) or (b shr 4)
            val acc = buckets.getOrPut(key) { IntArray(4) }
            acc[0]++; acc[1] += r; acc[2] += g; acc[3] += b
        }
    }
    return buckets.values
        .sortedByDescending { it[0] }
        .take(count)
        .map { Color(it[1] / it[0], it[2] / it[0], it[3] / it[0]) }
}
