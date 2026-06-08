package pro.jayeshseth.devcontrols.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/** QR generator — encodes text to a QR code with zxing-core, rendered as a bitmap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrToolsLabScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("https://github.com") }
    val qr: ImageBitmap? = remember(text) { if (text.isBlank()) null else generateQr(text, 640) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Generator") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Text / URL") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(280.dp).clip(RoundedCornerShape(16.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                qr?.let { Image(bitmap = it, contentDescription = "QR", modifier = Modifier.size(256.dp)) }
                    ?: Text("Enter text", color = Color.Gray)
            }
            Spacer(Modifier.height(12.dp))
            Text("Live QR for the text above.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun generateQr(text: String, size: Int): ImageBitmap? = runCatching {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) for (y in 0 until size) {
        bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
    bmp.asImageBitmap()
}.getOrNull()
