package pro.jayeshseth.devcontrols.ui.maps

import pro.jayeshseth.devcontrols.util.fmt
import pro.jayeshseth.devcontrols.util.pad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetIn
import kotlinx.datetime.toLocalDateTime
import pro.jayeshseth.devcontrols.util.nowMillis
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/** Sun & Moon — sunrise/sunset/solar-noon, sun altitude, day length, and moon phase for a location. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunMoonScreen(onBack: () -> Unit) {
    var latS by remember { mutableStateOf("19.0760") }
    var lonS by remember { mutableStateOf("72.8777") }
    val lat = latS.toDoubleOrNull(); val lon = lonS.toDoubleOrNull()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sun & Moon") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Row { OutlinedTextField(latS, { latS = it }, label = { Text("Latitude") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); OutlinedTextField(lonS, { lonS = it }, label = { Text("Longitude") }, singleLine = true, modifier = Modifier.weight(1f)) }
            Spacer(Modifier.height(16.dp))
            if (lat == null || lon == null) { Text("Enter a valid location.", color = MaterialTheme.colorScheme.error); return@Column }

            val tz = TimeZone.currentSystemDefault()
            val nowInstant = Clock.System.now()
            val ldt = nowInstant.toLocalDateTime(tz)
            val n = ldt.dayOfYear
            val tzMin = nowInstant.offsetIn(tz).totalSeconds / 60.0
            val decl = -23.44 * cos(((360.0 / 365 * (n + 10)) * kotlin.math.PI / 180.0))
            val b = ((360.0 / 365 * (n - 81)) * kotlin.math.PI / 180.0)
            val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
            val solarNoon = 720 - 4 * lon - eot + tzMin // minutes from local midnight
            val cosHa = (sin(((-0.833) * kotlin.math.PI / 180.0)) - sin(((lat) * kotlin.math.PI / 180.0)) * sin(((decl) * kotlin.math.PI / 180.0))) / (cos(((lat) * kotlin.math.PI / 180.0)) * cos(((decl) * kotlin.math.PI / 180.0)))
            val polar = cosHa !in -1.0..1.0
            val ha = if (polar) 0.0 else ((acos(cosHa)) * 180.0 / kotlin.math.PI)
            val sunrise = solarNoon - 4 * ha; val sunset = solarNoon + 4 * ha
            val nowMin = ldt.hour * 60 + ldt.minute.toDouble()
            val haNow = (((nowMin - solarNoon) / 4) * kotlin.math.PI / 180.0)
            val altNow = (asin(sin(((lat) * kotlin.math.PI / 180.0)) * sin(((decl) * kotlin.math.PI / 180.0)) + cos(((lat) * kotlin.math.PI / 180.0)) * cos(((decl) * kotlin.math.PI / 180.0)) * cos(haNow))) * 180.0 / kotlin.math.PI

            // moon phase
            val jd = nowMillis() / 86400000.0 + 2440587.5
            val phase = (((jd - 2451550.1) / 29.530588853) % 1 + 1) % 1
            val illum = (1 - cos(2 * kotlin.math.PI * phase)) / 2

            fun hhmm(min: Double): String { val m = ((min % 1440) + 1440) % 1440; return "${(m / 60).toInt().pad(2)}:${(m % 60).toInt().pad(2)}" }

            // sun arc
            Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                val w = size.width; val h = size.height; val baseY = h - 10
                drawLine(Color(0xFF555B6E), Offset(0f, baseY), Offset(w, baseY), 2f)
                val path = androidx.compose.ui.graphics.Path()
                var x = 0f; path.moveTo(0f, baseY)
                while (x <= w) { val t = x / w; val arc = sin(t * kotlin.math.PI).toFloat(); path.lineTo(x, baseY - arc * (h - 30)); x += 6f }
                drawPath(path, Color(0x55FFD54F), style = Stroke(3f))
                // current sun pos by time fraction between sunrise..sunset
                if (!polar && nowMin in sunrise..sunset) {
                    val t = ((nowMin - sunrise) / (sunset - sunrise)).toFloat()
                    val arc = sin(t * kotlin.math.PI).toFloat()
                    drawCircle(Color(0xFFFFC107), 14f, Offset(t * w, baseY - arc * (h - 30)))
                }
            }
            Spacer(Modifier.height(12.dp))
            Info("Sunrise", if (polar) "—" else hhmm(sunrise))
            Info("Solar noon", hhmm(solarNoon))
            Info("Sunset", if (polar) "—" else hhmm(sunset))
            Info("Day length", if (polar) "polar day/night" else hhmm(sunset - sunrise))
            Info("Sun altitude now", "${fmt(altNow, 1)}°")
            Spacer(Modifier.height(8.dp))
            Info("Moon phase", "${moonName(phase)} · ${(illum * 100).toInt()}% lit ${moonEmoji(phase)}")
            Spacer(Modifier.height(8.dp))
            Text("Approximate (NOAA day-of-year solar model); within ~a few minutes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
    }
}

private fun moonName(p: Double) = when {
    p < 0.03 || p > 0.97 -> "New moon"; p < 0.22 -> "Waxing crescent"; p < 0.28 -> "First quarter"
    p < 0.47 -> "Waxing gibbous"; p < 0.53 -> "Full moon"; p < 0.72 -> "Waning gibbous"
    p < 0.78 -> "Last quarter"; else -> "Waning crescent"
}
private fun moonEmoji(p: Double) = when {
    p < 0.03 || p > 0.97 -> "🌑"; p < 0.22 -> "🌒"; p < 0.28 -> "🌓"; p < 0.47 -> "🌔"
    p < 0.53 -> "🌕"; p < 0.72 -> "🌖"; p < 0.78 -> "🌗"; else -> "🌘"
}
