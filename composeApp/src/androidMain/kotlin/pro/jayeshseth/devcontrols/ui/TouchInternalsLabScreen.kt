package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Touch & stylus internals visualizer. A raw `awaitPointerEventScope` loop reads every
 * active [androidx.compose.ui.input.pointer.PointerInputChange] each frame and draws a
 * ring per contact — radius scaled by **pressure**, coloured by **pointer type**
 * (touch / stylus / mouse), labelled with its pointer **id**. Multi-touch shows every
 * finger at once; a pressure-sensitive stylus modulates the ring size.
 */
private data class Contact(val id: Long, val pos: Offset, val pressure: Float, val type: PointerType)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchInternalsLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Touch Internals") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }

        Column(Modifier.padding(inner).fillMaxSize()) {
            Text(
                "Touch / drag with one or more fingers (or a stylus). Ring size = pressure, " +
                    "colour = pointer type.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF101014))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                contacts = event.changes.filter { it.pressed }.map {
                                    Contact(it.id.value, it.position, it.pressure, it.type)
                                }
                            }
                        }
                    }
                    .drawBehind {
                        contacts.forEach { c ->
                            val color = when (c.type) {
                                PointerType.Stylus -> Color(0xFFE040FB)
                                PointerType.Mouse -> Color(0xFFFFD740)
                                else -> Color(0xFF18FFFF)
                            }
                            val r = 40f + c.pressure.coerceIn(0f, 1f) * 160f
                            drawCircle(color, radius = r, center = c.pos, style = Stroke(width = 5f))
                            drawCircle(color.copy(alpha = 0.25f), radius = 26f, center = c.pos)
                        }
                    }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("active pointers: ${contacts.size}", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    contacts.forEach { c ->
                        Text(
                            "  id=${c.id} type=${c.type} p=${"%.2f".format(c.pressure)} (${c.pos.x.toInt()},${c.pos.y.toInt()})",
                            color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
