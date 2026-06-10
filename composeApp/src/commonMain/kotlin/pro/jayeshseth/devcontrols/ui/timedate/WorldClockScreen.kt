package pro.jayeshseth.devcontrols.ui.timedate

import pro.jayeshseth.devcontrols.util.fmt
import pro.jayeshseth.devcontrols.util.hex2
import pro.jayeshseth.devcontrols.util.pad
import pro.jayeshseth.devcontrols.util.nowMillis

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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime

private val ZONES = listOf(
    "Local" to TimeZone.currentSystemDefault().id, "New York" to "America/New_York", "London" to "Europe/London",
    "Mumbai" to "Asia/Kolkata", "Tokyo" to "Asia/Tokyo", "Sydney" to "Australia/Sydney",
    "Dubai" to "Asia/Dubai", "San Francisco" to "America/Los_Angeles", "Berlin" to "Europe/Berlin"
)
private val DOW = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val MON = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** World clock — live times across major time zones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(onBack: () -> Unit) {
    var now by remember { mutableLongStateOf(nowMillis()) }
    LaunchedEffect(Unit) { while (true) { now = nowMillis(); delay(1000) } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("World Clock") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        LazyColumn(Modifier.padding(inner).fillMaxSize().padding(horizontal = 16.dp)) {
            items(ZONES) { (label, zone) ->
                val ldt = Instant.fromEpochMilliseconds(now).toLocalDateTime(TimeZone.of(zone))
                val time = "${ldt.hour.pad(2)}:${ldt.minute.pad(2)}:${ldt.second.pad(2)}"
                val day = "${DOW[ldt.dayOfWeek.isoDayNumber - 1]} ${ldt.dayOfMonth} ${MON[ldt.monthNumber - 1]}"
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
