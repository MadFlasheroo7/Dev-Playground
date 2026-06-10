package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import kotlin.math.cos
import kotlin.math.sin

/** Turtle graphics REPL — type a tiny language (F n, L deg, R deg, repeat k [ … ]) and watch it draw. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurtleReplLabScreen(onBack: () -> Unit) {
    var src by remember { mutableStateOf("repeat 36 [ F 60 R 100 F 30 L 80 ]") }
    val tokens = remember(src) { tokenize(src) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Turtle REPL") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(src, { src = it }, Modifier.fillMaxWidth().padding(12.dp),
                label = { Text("turtle program") }, textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            Text("Commands: F/B n · L/R deg · repeat k [ … ]", Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxSize().background(Color(0xFF07100A)).drawBehind {
                val segs = ArrayList<Pair<Offset, Offset>>()
                var x = size.width / 2f; var y = size.height / 2f; var ang = -90f
                fun run(list: List<Tok>) {
                    var i = 0
                    while (i < list.size) {
                        val t = list[i]
                        when (t) {
                            is Tok.Cmd -> {
                                when (t.name) {
                                    "F", "B" -> {
                                        val d = if (t.name == "B") -t.arg else t.arg
                                        val r = ((ang.toDouble()) * kotlin.math.PI / 180.0)
                                        val nx = x + (cos(r) * d).toFloat(); val ny = y + (sin(r) * d).toFloat()
                                        segs.add(Offset(x, y) to Offset(nx, ny)); x = nx; y = ny
                                    }
                                    "R" -> ang += t.arg
                                    "L" -> ang -= t.arg
                                }
                                i++
                            }
                            is Tok.Repeat -> { repeat(t.count) { run(t.body) }; i++ }
                        }
                    }
                }
                run(tokens)
                segs.forEach { drawLine(Color(0xFF7CFF9E), it.first, it.second, 2f) }
            })
        }
    }
}

private sealed class Tok {
    class Cmd(val name: String, val arg: Float) : Tok()
    class Repeat(val count: Int, val body: List<Tok>) : Tok()
}

private fun tokenize(src: String): List<Tok> {
    val words = src.replace("[", " [ ").replace("]", " ] ").trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    var pos = 0
    fun parse(): List<Tok> {
        val out = ArrayList<Tok>()
        while (pos < words.size) {
            val w = words[pos]
            when {
                w == "]" -> { pos++; return out }
                w.equals("repeat", true) -> {
                    pos++; val count = words.getOrNull(pos)?.toIntOrNull() ?: 0; pos++
                    if (words.getOrNull(pos) == "[") pos++
                    out.add(Tok.Repeat(count, parse()))
                }
                w.uppercase() in setOf("F", "B", "L", "R") -> {
                    val arg = words.getOrNull(pos + 1)?.toFloatOrNull() ?: 0f
                    out.add(Tok.Cmd(w.uppercase(), arg)); pos += 2
                }
                else -> pos++
            }
        }
        return out
    }
    return parse()
}
