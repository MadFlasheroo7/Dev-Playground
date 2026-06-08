package pro.jayeshseth.devcontrols.ui.toys

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt
import kotlin.random.Random

private val ANSWERS = listOf("It is certain", "Without a doubt", "Yes definitely", "Most likely", "Ask again later",
    "Cannot predict now", "Don't count on it", "My reply is no", "Very doubtful", "Signs point to yes", "Outlook good", "Better not tell you now")

/** Magic 8-Ball — shake the phone or tap to reveal an answer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Magic8BallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var answer by remember { mutableStateOf("Ask a question") }
    fun shake() { answer = ANSWERS[Random.nextInt(ANSWERS.size)] }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / 9.81f
                if (g > 2.2f) shake()
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Magic 8-Ball") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            Box(Modifier.size(280.dp).clip(CircleShape).background(Color(0xFF0B0B0B)).clickable { shake() }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(150.dp).clip(CircleShape).background(Color(0xFF1A237E)), contentAlignment = Alignment.Center) {
                    AnimatedContent(answer, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "8") {
                        Text(it, color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Shake the phone or tap the ball.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
