package pro.jayeshseth.devcontrols.ui.timedate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val ZONES = listOf(
    "Local" to TimeZone.getDefault().id, "New York" to "America/New_York", "London" to "Europe/London",
    "Mumbai" to "Asia/Kolkata", "Tokyo" to "Asia/Tokyo", "Sydney" to "Australia/Sydney",
    "Dubai" to "Asia/Dubai", "San Francisco" to "America/Los_Angeles", "Berlin" to "Europe/Berlin"
)

/** World clock — live times across major time zones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(onBack: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("World Clock") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        LazyColumn(Modifier.padding(inner).fillMaxSize().padding(horizontal = 16.dp)) {
            items(ZONES) { (label, zone) ->
                val tz = TimeZone.getTimeZone(zone)
                val time = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = tz }.format(Date(now))
                val day = SimpleDateFormat("EEE d MMM", Locale.US).apply { timeZone = tz }.format(Date(now))
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(day, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(time, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.headlineSmall)
                }
                HorizontalDivider()
            }
        }
    }
}
