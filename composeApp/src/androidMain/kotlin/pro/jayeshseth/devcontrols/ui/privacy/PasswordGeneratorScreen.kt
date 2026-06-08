package pro.jayeshseth.devcontrols.ui.privacy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import java.security.SecureRandom
import kotlin.math.ln

/** Password generator — charset toggles, length, entropy + strength meter, copy. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PasswordGeneratorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var len by remember { mutableFloatStateOf(16f) }
    var lower by remember { mutableStateOf(true) }
    var upper by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    val rng = remember { SecureRandom() }

    fun charset(): String = buildString {
        if (lower) append("abcdefghijkmnpqrstuvwxyz")
        if (upper) append("ABCDEFGHJKLMNPQRSTUVWXYZ")
        if (digits) append("23456789")
        if (symbols) append("!@#\$%^&*-_=+?")
    }
    fun gen(): String { val cs = charset(); if (cs.isEmpty()) return ""; return (0 until len.toInt()).map { cs[rng.nextInt(cs.length)] }.joinToString("") }
    var pw by remember { mutableStateOf(gen()) }

    val entropy = remember(pw) { if (charset().isEmpty()) 0.0 else len.toInt() * (ln(charset().length.toDouble()) / ln(2.0)) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Password Generator") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text(pw.ifEmpty { "pick a charset" }, fontFamily = FontFamily.Monospace, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = { pw = gen() }) { Text("Generate") }
                Spacer(Modifier.fillMaxWidth(0.04f))
                OutlinedButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("pw", pw))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                }) { Text("Copy") }
            }
            Spacer(Modifier.height(16.dp))
            Text("Length: ${len.toInt()}")
            Slider(value = len, onValueChange = { len = it; pw = gen() }, valueRange = 6f..40f)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(lower, { lower = !lower; pw = gen() }, { Text("a-z") })
                FilterChip(upper, { upper = !upper; pw = gen() }, { Text("A-Z") })
                FilterChip(digits, { digits = !digits; pw = gen() }, { Text("0-9") })
                FilterChip(symbols, { symbols = !symbols; pw = gen() }, { Text("!@#") })
            }
            Spacer(Modifier.height(16.dp))
            Text("Entropy: ${entropy.toInt()} bits — ${strength(entropy)}")
            LinearProgressIndicator(progress = { (entropy / 128.0).coerceIn(0.0, 1.0).toFloat() }, modifier = Modifier.fillMaxWidth(),
                color = if (entropy < 50) Color(0xFFE53935) else if (entropy < 80) Color(0xFFFFB300) else Color(0xFF43A047))
        }
    }
}

private fun strength(e: Double) = when { e < 40 -> "weak"; e < 64 -> "fair"; e < 90 -> "strong"; else -> "very strong" }
