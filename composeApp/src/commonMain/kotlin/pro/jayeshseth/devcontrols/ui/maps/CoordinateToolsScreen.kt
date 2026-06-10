package pro.jayeshseth.devcontrols.ui.maps

import pro.jayeshseth.devcontrols.util.fmt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Coordinate tools — distance, bearing, midpoint between two points + DMS conversion. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinateToolsScreen(onBack: () -> Unit) {
    var aLat by remember { mutableStateOf("19.0760") }
    var aLon by remember { mutableStateOf("72.8777") }
    var bLat by remember { mutableStateOf("28.6139") }
    var bLon by remember { mutableStateOf("77.2090") }

    val a = aLat.toDoubleOrNull() to aLon.toDoubleOrNull()
    val b = bLat.toDoubleOrNull() to bLon.toDoubleOrNull()
    val valid = a.first != null && a.second != null && b.first != null && b.second != null

    Scaffold(
        topBar = { TopAppBar(title = { Text("Coordinate Tools") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Point A", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row { OutlinedTextField(aLat, { aLat = it }, label = { Text("lat") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); OutlinedTextField(aLon, { aLon = it }, label = { Text("lon") }, singleLine = true, modifier = Modifier.weight(1f)) }
            Spacer(Modifier.height(8.dp))
            Text("Point B", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row { OutlinedTextField(bLat, { bLat = it }, label = { Text("lat") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); OutlinedTextField(bLon, { bLon = it }, label = { Text("lon") }, singleLine = true, modifier = Modifier.weight(1f)) }
            Spacer(Modifier.height(16.dp))
            if (valid) {
                val dist = GeoMath.distanceMeters(a.first!!, a.second!!, b.first!!, b.second!!)
                val brg = GeoMath.bearing(a.first!!, a.second!!, b.first!!, b.second!!)
                val mid = GeoMath.midpoint(a.first!!, a.second!!, b.first!!, b.second!!)
                Result("Distance", GeoMath.fmtDist(dist))
                Result("Initial bearing", "${fmt(brg, 1)}° (${compass(brg)})")
                Result("Midpoint", "${fmt(mid.first, 5)}, ${fmt(mid.second, 5)}")
                Result("A in DMS", "${GeoMath.toDms(a.first!!, true)}  ${GeoMath.toDms(a.second!!, false)}")
                Result("B in DMS", "${GeoMath.toDms(b.first!!, true)}  ${GeoMath.toDms(b.second!!, false)}")
            } else Text("Enter valid decimal coordinates.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Result(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
    }
}

private fun compass(b: Double): String {
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
    return dirs[((b + 22.5) / 45).toInt() % 8]
}
