package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Speed-dial FAB — the main button rotates and fans out labelled mini-FABs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedDialFabLabScreen(onBack: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (open) 45f else 0f, label = "rot")
    val items = listOf("Share" to Color(0xFF00C9FF), "Edit" to Color(0xFF92FE9D), "Delete" to Color(0xFFFF5F6D))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speed-Dial FAB") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            Column(
                Modifier.align(Alignment.BottomEnd).padding(24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items.forEachIndexed { i, (label, color) ->
                    AnimatedVisibility(visible = open, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = Color.White,
                                modifier = Modifier.background(Color(0xCC222230), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp))
                            androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
                            Box(Modifier.size(48.dp).background(color, CircleShape).clickable { open = false },
                                contentAlignment = Alignment.Center) {
                                Text(label.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Box(Modifier.size(64.dp).background(Color(0xFF7F00FF), CircleShape).clickable { open = !open },
                    contentAlignment = Alignment.Center) {
                    Text("+", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.rotate(rot))
                }
            }
        }
    }
}
