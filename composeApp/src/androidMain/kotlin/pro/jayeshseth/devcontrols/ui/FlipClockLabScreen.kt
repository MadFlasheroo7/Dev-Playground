package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Flip-clock / odometer. A live HH:MM:SS clock where each digit rolls (slides up/out,
 * new slides in) only when it actually changes, via per-digit [AnimatedContent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipClockLabScreen(onBack: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(250) }
    }
    val totalSec = (now / 1000)
    val h = ((totalSec / 3600) % 24).toInt()
    val m = ((totalSec / 60) % 60).toInt()
    val s = (totalSec % 60).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flip Clock") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Digit(h / 10); Digit(h % 10)
                Colon()
                Digit(m / 10); Digit(m % 10)
                Colon()
                Digit(s / 10); Digit(s % 10)
            }
        }
    }
}

@Composable
private fun Digit(value: Int) {
    Box(
        Modifier.padding(3.dp).size(width = 54.dp, height = 84.dp)
            .background(Color(0xFF15151E), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically(tween(380)) { it } togetherWith slideOutVertically(tween(380)) { -it })
            },
            label = "digit"
        ) { v ->
            Text("$v", color = Color(0xFF18FFFF), fontSize = 52.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Colon() {
    Text(":", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}
