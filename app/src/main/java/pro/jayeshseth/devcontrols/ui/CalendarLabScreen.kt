package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar

/** Calendar with date-range selection, built from scratch on java.util.Calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarLabScreen(onBack: () -> Unit) {
    val today = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var rangeStart by remember { mutableStateOf<Long?>(null) }
    var rangeEnd by remember { mutableStateOf<Long?>(null) }

    fun dayKey(y: Int, m: Int, d: Int) = y * 10000L + m * 100L + d

    val cal = remember(year, month) { Calendar.getInstance().apply { clear(); set(year, month, 1) } }
    val firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthName = arrayOf("January","February","March","April","May","June","July","August","September","October","November","December")[month]

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calendar") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if (month == 0) { month = 11; year-- } else month-- }) { Text("‹") }
                Text("$monthName $year", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { if (month == 11) { month = 0; year++ } else month++ }) { Text("›") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("S","M","T","W","T","F","S").forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val cells = firstDow + daysInMonth
            val rows = (cells + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val idx = r * 7 + c
                        val day = idx - firstDow + 1
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                            if (day in 1..daysInMonth) {
                                val key = dayKey(year, month, day)
                                val inRange = rangeStart != null && rangeEnd != null && key in rangeStart!!..rangeEnd!!
                                val isEnd = key == rangeStart || key == rangeEnd
                                Box(Modifier.fillMaxSize().clip(CircleShape)
                                    .background(when { isEnd -> MaterialTheme.colorScheme.primary; inRange -> MaterialTheme.colorScheme.primaryContainer; else -> Color.Transparent })
                                    .clickable {
                                        when {
                                            rangeStart == null || rangeEnd != null -> { rangeStart = key; rangeEnd = null }
                                            key < rangeStart!! -> { rangeEnd = rangeStart; rangeStart = key }
                                            else -> rangeEnd = key
                                        }
                                    }, contentAlignment = Alignment.Center) {
                                    Text("$day", color = if (isEnd) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(when {
                rangeStart != null && rangeEnd != null -> "Range: ${fmt(rangeStart!!)} → ${fmt(rangeEnd!!)}"
                rangeStart != null -> "Start: ${fmt(rangeStart!!)} (tap an end date)"
                else -> "Tap a start date."
            }, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun fmt(key: Long): String {
    val y = (key / 10000); val m = (key / 100 % 100).toInt() + 1; val d = (key % 100)
    return "%04d-%02d-%02d".format(y, m, d)
}
