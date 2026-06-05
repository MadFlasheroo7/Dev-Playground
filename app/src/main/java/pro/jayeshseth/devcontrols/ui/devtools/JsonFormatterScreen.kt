package pro.jayeshseth.devcontrols.ui.devtools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

/** JSON formatter — pretty-print + validate any JSON. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonFormatterScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("""{"name":"Dev Controls","screens":140,"tags":["ui","adb"]}""") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun format() {
        val t = input.trim()
        val r = runCatching { if (t.startsWith("[")) JSONArray(t).toString(2) else JSONObject(t).toString(2) }
        output = r.getOrElse { it.message ?: "Invalid JSON" }
        error = r.isFailure
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("JSON Formatter") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(input, { input = it }, label = { Text("JSON input") }, modifier = Modifier.fillMaxWidth().height(160.dp))
            Spacer(Modifier.height(8.dp))
            Button(onClick = { format() }) { Text("Format & validate") }
            Spacer(Modifier.height(12.dp))
            if (output.isNotEmpty()) Text(output, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                color = if (error) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface)
        }
    }
}
