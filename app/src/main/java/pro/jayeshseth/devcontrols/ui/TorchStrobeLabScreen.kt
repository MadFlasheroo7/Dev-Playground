package pro.jayeshseth.devcontrols.ui

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Torch & Strobe — toggle the camera flash via [CameraManager.setTorchMode], strobe it
 * at an adjustable rate, or use the screen as a white light. No CAMERA permission needed
 * for torch control on modern Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorchStrobeLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val torchId = remember {
        runCatching { cm.cameraIdList.firstOrNull { id -> cm.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } }.getOrNull()
    }
    var on by remember { mutableStateOf(false) }
    var strobe by remember { mutableStateOf(false) }
    var hz by remember { mutableFloatStateOf(8f) }
    var screenLight by remember { mutableStateOf(false) }

    fun setTorch(state: Boolean) { torchId?.let { runCatching { cm.setTorchMode(it, state) } } }

    DisposableEffect(Unit) { onDispose { setTorch(false) } }
    LaunchedEffect(on, strobe, hz) {
        if (strobe) {
            val half = (500f / hz.coerceAtLeast(1f)).toLong()
            while (true) { setTorch(true); delay(half); setTorch(false); delay(half) }
        } else setTorch(on)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Torch & Strobe") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize().background(if (screenLight) Color.White else MaterialTheme.colorScheme.background)) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                if (torchId == null) Text("No flash unit found on this device.", color = MaterialTheme.colorScheme.error)
                Row2("Torch", on && !strobe) { on = it }
                Row2("Strobe", strobe) { strobe = it }
                Text("Strobe rate: ${hz.toInt()} Hz", color = if (screenLight) Color.Black else MaterialTheme.colorScheme.onBackground)
                Slider(value = hz, onValueChange = { hz = it }, valueRange = 1f..20f)
                Spacer(Modifier.height(12.dp))
                Row2("Screen light (white)", screenLight) { screenLight = it }
                Spacer(Modifier.height(8.dp))
                Text("⚠ Rapid strobe can trigger photosensitive epilepsy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (screenLight) Color(0xFF884400) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Row2(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
