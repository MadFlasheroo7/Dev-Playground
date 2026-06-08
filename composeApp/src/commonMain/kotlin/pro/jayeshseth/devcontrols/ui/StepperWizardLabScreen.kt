package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stepper / wizard — a 4-step flow with a progress rail and animated step transitions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepperWizardLabScreen(onBack: () -> Unit) {
    val titles = listOf("Account", "Profile", "Preferences", "Done")
    var step by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stepper / Wizard") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                titles.forEachIndexed { i, _ ->
                    val done = i <= step
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Text("${i + 1}", color = if (done) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                    if (i < titles.lastIndex) {
                        val w by animateDpAsState(if (i < step) 60.dp else 60.dp, label = "seg")
                        Box(Modifier.height(3.dp).width(w)
                            .background(if (i < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            AnimatedContent(
                targetState = step,
                transitionSpec = { (slideInHorizontally { it } togetherWith slideOutHorizontally { -it }) },
                label = "step"
            ) { s ->
                Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(titles[s], fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Step ${s + 1} content goes here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { if (step > 0) step-- }, enabled = step > 0) { Text("Back") }
                Button(onClick = { if (step < titles.lastIndex) step++ else step = 0 }) {
                    Text(if (step < titles.lastIndex) "Next" else "Finish")
                }
            }
        }
    }
}
