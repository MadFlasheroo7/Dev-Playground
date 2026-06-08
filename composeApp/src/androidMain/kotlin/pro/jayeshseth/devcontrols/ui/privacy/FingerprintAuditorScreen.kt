package pro.jayeshseth.devcontrols.ui.privacy

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.DisplayMetrics
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import java.util.Locale
import java.util.TimeZone

/** Fingerprint Auditor — everything an app can learn about you with ZERO permissions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerprintAuditorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val signals = remember { gatherSignals(context) }
    val fingerprint = remember(signals) {
        val joined = signals.joinToString("|") { it.second }
        MessageDigest.getInstance("SHA-256").digest(joined.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Fingerprint Auditor") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        LazyColumn(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Device fingerprint", style = MaterialTheme.typography.labelMedium)
                        Text(fingerprint, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Derived from ${signals.size} permission-free signals — apps can build this to track you across installs.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(signals) { (k, v) ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(v, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

private fun gatherSignals(context: Context): List<Pair<String, String>> {
    val dm: DisplayMetrics = context.resources.displayMetrics
    val cfg = context.resources.configuration
    val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensors = sm.getSensorList(Sensor.TYPE_ALL).map { it.name }
    return buildList {
        add("Model" to "${Build.MANUFACTURER} ${Build.MODEL}")
        add("Device / board" to "${Build.DEVICE} / ${Build.BOARD}")
        add("Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        add("Build fingerprint" to Build.FINGERPRINT)
        add("Hardware" to Build.HARDWARE)
        add("Supported ABIs" to Build.SUPPORTED_ABIS.joinToString())
        add("Screen" to "${dm.widthPixels}×${dm.heightPixels} @ ${dm.densityDpi}dpi (${dm.density}x)")
        add("Font scale" to "${cfg.fontScale}")
        add("Locale" to Locale.getDefault().toString())
        add("Timezone" to TimeZone.getDefault().id)
        add("Locales (config)" to cfg.locales.toLanguageTags())
        add("Night mode" to "${cfg.uiMode}")
        add("Sensor count" to "${sensors.size}")
        add("Sensors" to sensors.joinToString(", ").take(400))
        add("Processors" to "${Runtime.getRuntime().availableProcessors()}")
        add("Max memory" to "${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB")
        add("Boot id (since boot ms)" to "${android.os.SystemClock.elapsedRealtime()}")
        add("Kernel" to System.getProperty("os.version").orEmpty())
    }
}
