package pro.jayeshseth.devcontrols.ui.visualfx

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/** Gooey nav bar — a liquid indicator that stretches as it springs between tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GooeyNavBarScreen(onBack: () -> Unit) {
    val tabs = listOf("🏠", "🔍", "❤", "🔔", "👤")
    var selected by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gooey Nav Bar") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(tabs[selected], fontSize = 96.sp)
            }
            val target by animateFloatAsState(selected.toFloat(), spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "blob")
            val accent = MaterialTheme.colorScheme.primary
            Box(
                Modifier.fillMaxWidth().height(72.dp).background(MaterialTheme.colorScheme.surfaceVariant)
                    .drawBehind {
                        val slot = size.width / tabs.size
                        val x = (target + 0.5f) * slot
                        val sel = (selected + 0.5f) * slot
                        val stretch = abs(sel - x)
                        val w = 54f + stretch
                        drawRoundRect(accent, topLeft = Offset(x - w / 2, size.height * 0.2f), size = Size(w, size.height * 0.6f), cornerRadius = CornerRadius(size.height * 0.3f))
                    }
            ) {
                Row(Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { i, t ->
                        Box(Modifier.weight(1f).fillMaxSize().clickable { selected = i }, contentAlignment = Alignment.Center) {
                            Text(t, fontSize = 24.sp, fontWeight = if (selected == i) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}
