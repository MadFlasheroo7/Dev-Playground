package pro.jayeshseth.devcontrols.ui.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Range
import android.util.SizeF
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
import kotlin.math.atan
import kotlin.math.hypot

/** Lens Explorer — every camera's CameraCharacteristics: facing, HW level, lenses, FOV, caps. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensExplorerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val cams = remember { enumerateCameras(context) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Lens Explorer") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        LazyColumn(Modifier.padding(inner).fillMaxSize().padding(12.dp)) {
            items(cams) { c ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Camera ${c.id} · ${c.facing}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(c.detail, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

private data class CamInfo(val id: String, val facing: String, val detail: String)

private fun enumerateCameras(context: Context): List<CamInfo> {
    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return runCatching {
        cm.cameraIdList.map { id ->
            val ch = cm.getCameraCharacteristics(id)
            val facing = when (ch.get(CameraCharacteristics.LENS_FACING)) {
                CameraMetadata.LENS_FACING_FRONT -> "Front"; CameraMetadata.LENS_FACING_BACK -> "Back"; else -> "External"
            }
            val level = when (ch.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                else -> "EXTERNAL"
            }
            val focals = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
            val sensor = ch.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: SizeF(0f, 0f)
            val fov = focals.minOrNull()?.let { f -> if (f > 0) Math.toDegrees(2.0 * atan(hypot(sensor.width, sensor.height) / (2 * f).toDouble())).toInt() else 0 } ?: 0
            val iso = ch.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            val exp = ch.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            val zoom = ch.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            val minFocus = ch.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
            val fpsRanges = ch.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.joinToString(", ") { r -> "${r.lower}-${r.upper}" } ?: "?"
            val caps = ch.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: IntArray(0)
            val capNames = buildList {
                if (caps.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)) add("RAW")
                if (caps.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)) add("MANUAL_SENSOR")
                if (caps.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING)) add("MANUAL_POST")
                if (caps.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO)) add("HIGH_SPEED")
                if (caps.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT)) add("DEPTH")
            }
            val map = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val maxJpeg = map?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height }
            val flash = ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

            val detail = buildString {
                appendLine("HW level: $level   flash: $flash")
                appendLine("focal: ${focals.joinToString { "%.1fmm".format(it) }}   FOV ~${fov}°")
                appendLine("sensor: ${"%.1f".format(sensor.width)}×${"%.1f".format(sensor.height)} mm")
                appendLine("max JPEG: ${maxJpeg?.let { "${it.width}×${it.height}" } ?: "?"}")
                appendLine("ISO: ${iso?.let { "${it.lower}-${it.upper}" } ?: "auto-only"}")
                appendLine("exposure(ns): ${exp?.let { "${it.lower}-${it.upper}" } ?: "auto-only"}")
                appendLine("zoom: ${zoom?.let { "${"%.1f".format(it.lower)}-${"%.1f".format(it.upper)}x" } ?: "digital"}")
                appendLine("min focus dist: ${if (minFocus > 0) "%.2f".format(1f / minFocus) + " m" else "fixed/∞"}")
                appendLine("FPS ranges: $fpsRanges")
                append("caps: ${if (capNames.isEmpty()) "basic" else capNames.joinToString(" ")}")
            }
            CamInfo(id, facing, detail)
        }
    }.getOrDefault(emptyList())
}
