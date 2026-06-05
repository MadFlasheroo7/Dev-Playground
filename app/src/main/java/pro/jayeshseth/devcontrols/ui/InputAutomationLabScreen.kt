package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.ui.adbtools.AdbToolScaffold

/**
 * Input & Automation — inject taps/swipes/text/keyevents via `input`, capture
 * coordinates from a pad, record a macro and replay it, and dump the live view tree
 * with `uiautomator`. UID-2000 shell.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputAutomationLabScreen(onBack: () -> Unit) {
    AdbToolScaffold("Input & Automation", onBack, scrollable = false) { runner ->
        val scope = rememberCoroutineScope()
        var tapX by remember { mutableStateOf("500") }
        var tapY by remember { mutableStateOf("1000") }
        var textToType by remember { mutableStateOf("") }
        val macro = remember { mutableStateListOf<String>() }
        var dump by remember { mutableStateOf("") }

        fun send(cmd: String, record: Boolean = true) {
            runner.run(cmd)
            if (record) macro.add(cmd)
        }

        Spacer(Modifier.height(8.dp))
        // Coordinate capture pad
        Text("Tap the pad to capture coordinates (≈ device pixels):", style = MaterialTheme.typography.bodySmall)
        Box(
            Modifier.fillMaxWidth().height(140.dp).padding(vertical = 6.dp)
                .background(Color(0xFF22252E), RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    detectTapGestures { o -> tapX = o.x.toInt().toString(); tapY = o.y.toInt().toString() }
                }
        ) {
            Text("x=$tapX  y=$tapY", Modifier.padding(12.dp), color = Color.White, fontFamily = FontFamily.Monospace)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(tapX, { tapX = it }, label = { Text("x") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(tapY, { tapY = it }, label = { Text("y") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            Button(onClick = { send("input tap ${tapX.trim()} ${tapY.trim()}") }, modifier = Modifier.padding(top = 8.dp)) { Text("Tap") }
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(textToType, { textToType = it }, label = { Text("Text to type") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = {
                if (textToType.isNotEmpty()) send("input text ${textToType.trim().replace(" ", "%s")}")
            }, modifier = Modifier.padding(top = 8.dp)) { Text("Type") }
        }

        Spacer(Modifier.height(6.dp))
        Text("Key events", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val keys = listOf("HOME" to 3, "BACK" to 4, "RECENTS" to 187, "POWER" to 26,
                "VOL+" to 24, "VOL-" to 25, "ENTER" to 66, "TAB" to 61, "MENU" to 82)
            keys.forEach { (name, code) ->
                AssistChip(onClick = { send("input keyevent $code") }, label = { Text(name) }, enabled = !runner.busy)
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Macro (${macro.size})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(top = 8.dp))
            OutlinedButton(onClick = { macro.clear() }) { Text("Clear") }
            Button(onClick = {
                val steps = macro.toList()
                scope.launch { for (s in steps) { runner.exec(s); delay(400) } }
            }, enabled = macro.isNotEmpty() && !runner.busy) { Text("Replay") }
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
            items(macro) { Text("• $it", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        OutlinedButton(onClick = {
            scope.launch {
                runner.exec("uiautomator dump /sdcard/window_dump.xml")
                dump = runner.exec("cat /sdcard/window_dump.xml").output.take(4000)
            }
        }, modifier = Modifier.padding(vertical = 6.dp)) { Text("Dump view tree (uiautomator)") }
        if (dump.isNotEmpty()) {
            Text(dump, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}
