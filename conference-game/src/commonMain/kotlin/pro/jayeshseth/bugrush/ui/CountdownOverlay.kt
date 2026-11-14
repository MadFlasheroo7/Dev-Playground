package pro.jayeshseth.bugrush.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.bugrush.audio.SoundEffect
import pro.jayeshseth.bugrush.audio.SoundPlayer
import kotlinx.coroutines.delay

@Composable
fun CountdownOverlay(
    soundPlayer: SoundPlayer,
    onCountdownComplete: () -> Unit
) {
    var countStep by remember { mutableIntStateOf(3) }
    val scaleAnim = remember { Animatable(2.2f) }
    val alphaAnim = remember { Animatable(0.2f) }

    LaunchedEffect(Unit) {
        // Step 3
        countStep = 3
        soundPlayer.playSfx(SoundEffect.LASER_PEW)
        scaleAnim.snapTo(2.4f)
        alphaAnim.snapTo(0.3f)
        scaleAnim.animateTo(1.0f, tween(300, easing = FastOutSlowInEasing))
        alphaAnim.animateTo(1.0f, tween(200))
        delay(450)

        // Step 2
        countStep = 2
        soundPlayer.playSfx(SoundEffect.LASER_PEW)
        scaleAnim.snapTo(2.4f)
        alphaAnim.snapTo(0.3f)
        scaleAnim.animateTo(1.0f, tween(300, easing = FastOutSlowInEasing))
        alphaAnim.animateTo(1.0f, tween(200))
        delay(450)

        // Step 1
        countStep = 1
        soundPlayer.playSfx(SoundEffect.LASER_PEW)
        scaleAnim.snapTo(2.4f)
        alphaAnim.snapTo(0.3f)
        scaleAnim.animateTo(1.0f, tween(300, easing = FastOutSlowInEasing))
        alphaAnim.animateTo(1.0f, tween(200))
        delay(450)

        // Step 0: RUN!
        countStep = 0
        soundPlayer.playSfx(SoundEffect.UPGRADE_UNLOCKED)
        scaleAnim.snapTo(2.6f)
        alphaAnim.snapTo(0.5f)
        scaleAnim.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        alphaAnim.animateTo(1.0f, tween(150))
        delay(600)

        onCountdownComplete()
    }

    val textColor = when (countStep) {
        3 -> Color(0xFF00E5FF)
        2 -> Color(0xFFFFD600)
        1 -> Color(0xFFFF5252)
        else -> Color(0xFF00E676)
    }

    val displayText = when (countStep) {
        3 -> "3"
        2 -> "2"
        1 -> "1"
        else -> "RUN!"
    }

    val subtitle = when (countStep) {
        0 -> "⚡ INITIATING TELEPORT BEAM-IN • SHIELD ENGAGED!"
        else -> "GET READY FOR SWARM DEPLOYMENT..."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC060A10)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scaleAnim.value)
        ) {
            Text(
                text = displayText,
                color = textColor.copy(alpha = alphaAnim.value),
                fontSize = if (countStep == 0) 90.sp else 115.sp,
                fontWeight = FontWeight.Black,
                fontFamily = ArcadeTheme.pixelHeader,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = subtitle,
                color = Color.White.copy(alpha = alphaAnim.value * 0.85f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ArcadeTheme.terminalBody,
                letterSpacing = 2.sp
            )
        }
    }
}
