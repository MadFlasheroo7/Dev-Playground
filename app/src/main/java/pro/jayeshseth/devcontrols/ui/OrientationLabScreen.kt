package pro.jayeshseth.devcontrols.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Orientation Lab — force the activity's requested orientation to each mode. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrientationLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val config = LocalConfiguration.current

    DisposableEffect(Unit) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    val modes = listOf(
        "Portrait" to ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        "Landscape" to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        "Reverse portrait" to ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
        "Reverse landscape" to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        "Sensor" to ActivityInfo.SCREEN_ORIENTATION_SENSOR,
        "Lock current" to ActivityInfo.SCREEN_ORIENTATION_LOCKED,
        "Full sensor" to ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
        "Unspecified" to ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Orientation Lab") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text("Current: ${config.screenWidthDp} × ${config.screenHeightDp} dp", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { (label, mode) ->
                    Button(onClick = { activity?.requestedOrientation = mode }) { Text(label) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Reset to Unspecified on leaving the screen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
