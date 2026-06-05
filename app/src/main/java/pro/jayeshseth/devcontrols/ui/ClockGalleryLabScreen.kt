package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/** Clock gallery — analog, binary, and word clocks, all live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockGalleryLabScreen(onBack: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { withFrameNanos { now = System.currentTimeMillis() } } }
    val cal = remember(now / 1000) { Calendar.getInstance().apply { timeInMillis = now } }
    val h = cal.get(Calendar.HOUR_OF_DAY); val mn = cal.get(Calendar.MINUTE); val s = cal.get(Calendar.SECOND)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Clock Gallery") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Analog
            val onSurf = MaterialTheme.colorScheme.onSurface; val prim = MaterialTheme.colorScheme.primary
            Canvas(Modifier.size(180.dp)) {
                val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2 * 0.9f
                drawCircle(onSurf.copy(alpha = 0.2f), r, c, style = Stroke(3f))
                fun hand(frac: Float, len: Float, w: Float, col: Color) {
                    val a = frac * 2 * Math.PI.toFloat() - Math.PI.toFloat() / 2
                    drawLine(col, c, Offset(c.x + cos(a) * r * len, c.y + sin(a) * r * len), w, cap = StrokeCap.Round)
                }
                hand((h % 12 + mn / 60f) / 12f, 0.5f, 8f, onSurf)
                hand((mn + s / 60f) / 60f, 0.75f, 5f, onSurf)
                hand(s / 60f, 0.9f, 2f, prim)
            }
            Spacer(Modifier.height(20.dp))
            // Binary clock
            Text("Binary", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BinaryColumnPair(h); BinaryColumnPair(mn); BinaryColumnPair(s)
            }
            Spacer(Modifier.height(20.dp))
            // Word clock
            Text(wordTime(h, mn), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BinaryColumnPair(value: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        BinaryColumn(value / 10); BinaryColumn(value % 10)
    }
}

@Composable
private fun BinaryColumn(digit: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (bit in 3 downTo 0) {
            val on = (digit shr bit) and 1 == 1
            Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp))
                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}

private fun wordTime(h: Int, m: Int): String {
    val nums = arrayOf("twelve","one","two","three","four","five","six","seven","eight","nine","ten","eleven")
    val hr = h % 12
    val mm = when {
        m == 0 -> return "${nums[hr].replaceFirstChar { it.uppercase() }} o'clock"
        m == 15 -> "quarter past ${nums[hr]}"
        m == 30 -> "half past ${nums[hr]}"
        m == 45 -> "quarter to ${nums[(hr + 1) % 12]}"
        m < 30 -> "$m past ${nums[hr]}"
        else -> "${60 - m} to ${nums[(hr + 1) % 12]}"
    }
    return mm.replaceFirstChar { it.uppercase() }
}
