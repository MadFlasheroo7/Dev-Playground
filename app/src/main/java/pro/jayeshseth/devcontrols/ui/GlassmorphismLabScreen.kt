package pro.jayeshseth.devcontrols.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Glassmorphism gallery. A vibrant, blurred backdrop sits behind translucent cards
 * with hairline borders; the colour bleeds through the frosted panes. Uses
 * `Modifier.blur` for the backdrop (a real gaussian blur on API 31+, graceful no-op
 * below) plus low-alpha fills + light borders for the glass look.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassmorphismLabScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Glassmorphism") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            // colourful, blurred backdrop
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(60.dp) else Modifier)
            ) {
                Box(Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFF1A2980), Color(0xFFE100FF), Color(0xFFFF8008)))
                ))
                Box(Modifier.size(220.dp).graphicsLayer { translationX = 60f; translationY = 120f }
                    .clip(RoundedCornerShape(200.dp)).background(Color(0x9900E5FF)))
                Box(Modifier.size(200.dp).graphicsLayer { translationX = 520f; translationY = 700f }
                    .clip(RoundedCornerShape(200.dp)).background(Color(0x9969F0AE)))
            }

            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Spacer(Modifier.height(40.dp))
                repeat(3) { i ->
                    GlassCard(title = listOf("Frosted panel", "Translucent card", "Glass tile")[i])
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun GlassCard(title: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Backdrop bleeds through the translucent pane.", color = Color.White.copy(alpha = 0.85f))
        }
    }
}
