package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Animated PIN pad — dots fill as you type; a full 4-digit PIN flashes then clears. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinPadLabScreen(onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var flash by remember { mutableStateOf(false) }

    LaunchedEffect(pin) {
        if (pin.length == 4) { flash = true; delay(450); pin = ""; flash = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PIN Pad") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.size(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                repeat(4) { i ->
                    val filled = i < pin.length
                    val sc by animateFloatAsState(if (filled) 1.15f else 1f, label = "dot")
                    Box(
                        Modifier.size(22.dp).scale(sc).clip(CircleShape)
                            .background(
                                when {
                                    flash -> Color(0xFF69F0AE)
                                    filled -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                }
            }
            Spacer(Modifier.size(40.dp))
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    row.forEach { key ->
                        Box(
                            Modifier.size(72.dp).clip(CircleShape)
                                .background(if (key.isEmpty()) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = key.isNotEmpty() && !flash) {
                                    when (key) {
                                        "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        else -> if (pin.length < 4) pin += key
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(key, fontSize = 26.sp, fontWeight = FontWeight.Medium) }
                    }
                }
            }
        }
    }
}
