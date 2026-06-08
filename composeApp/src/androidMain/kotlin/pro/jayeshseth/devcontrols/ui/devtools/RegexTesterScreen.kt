package pro.jayeshseth.devcontrols.ui.devtools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Regex tester — live match list + groups against a test string. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexTesterScreen(onBack: () -> Unit) {
    var pattern by remember { mutableStateOf("\\b(\\w+)@(\\w+\\.\\w+)\\b") }
    var test by remember { mutableStateOf("contact a@b.com or dev@x.io for info") }

    val result = remember(pattern, test) {
        runCatching {
            val re = Regex(pattern)
            re.findAll(test).map { m -> "\"${m.value}\"  groups=${m.groupValues.drop(1)}  @${m.range.first}" }.toList()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Regex Tester") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(pattern, { pattern = it }, label = { Text("Pattern") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(test, { test = it }, label = { Text("Test string") }, modifier = Modifier.fillMaxWidth().height(120.dp))
            Spacer(Modifier.height(12.dp))
            result.fold(
                onSuccess = { matches ->
                    Text("${matches.size} match(es)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    LazyColumn(Modifier.fillMaxWidth()) { items(matches) { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp)) } }
                },
                onFailure = { Text("Invalid regex: ${it.message}", color = Color(0xFFE53935)) }
            )
        }
    }
}
