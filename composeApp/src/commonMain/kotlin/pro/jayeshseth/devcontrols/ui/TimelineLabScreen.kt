package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Vertical timeline — node rail with a connecting line and event cards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineLabScreen(onBack: () -> Unit) {
    val events = listOf(
        "Order placed" to "09:12", "Packed" to "10:40", "Shipped" to "14:05",
        "Out for delivery" to "08:30", "Delivered" to "11:58"
    )
    val colors = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D), Color(0xFFFFC371), Color(0xFFFF8008), Color(0xFF69F0AE))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        LazyColumn(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            itemsIndexed(events) { i, (title, time) ->
                Row(Modifier.fillMaxWidth().height(96.dp)) {
                    // rail
                    Column(Modifier.width(40.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(20.dp).clip(CircleShape).background(colors[i % colors.size]))
                        if (i < events.lastIndex) Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(title, fontWeight = FontWeight.Bold)
                            Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
