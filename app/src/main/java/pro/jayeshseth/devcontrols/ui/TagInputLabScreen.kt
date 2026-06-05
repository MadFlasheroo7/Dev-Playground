package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/** Tag/chip input — type to add, comma/enter commits, backspace on empty removes last, suggestions. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagInputLabScreen(onBack: () -> Unit) {
    val tags = remember { mutableStateListOf("compose", "android") }
    var input by remember { mutableStateOf("") }
    val suggestions = listOf("kotlin", "shader", "agsl", "canvas", "sensor", "adb", "ui", "animation")

    fun add(t: String) { val v = t.trim().trimEnd(','); if (v.isNotEmpty() && v !in tags) tags.add(v) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tag Input") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    InputChip(selected = false, onClick = { tags.remove(tag) }, label = { Text(tag) },
                        trailingIcon = { Text("✕") })
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { v ->
                    if (v.endsWith(",")) { add(v); input = "" } else input = v
                },
                label = { Text("Add a tag") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add(input); input = "" })
            )
            Spacer(Modifier.height(16.dp))
            Text("Suggestions", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.filter { it !in tags }.forEach { s ->
                    AssistChip(onClick = { add(s) }, label = { Text("+ $s") })
                }
            }
        }
    }
}
