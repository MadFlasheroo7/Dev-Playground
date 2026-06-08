package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars

/** IME Insets — a message list that pins to the keyboard, with live inset readout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImeInsetsLabScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    val imeDp = with(density) { imeBottom.toDp() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("IME Insets") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().imePadding()) {
            Text("IME inset: ${imeDp.value.toInt()} dp ($imeBottom px) · nav ${navBottom}px",
                Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)) {
                items((1..30).toList()) { i ->
                    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("Message $i — the input below rides up with the keyboard.",
                            Modifier.fillMaxWidth().padding(8.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth().padding(12.dp),
                label = { Text("Type — watch the inset value") })
        }
    }
}
