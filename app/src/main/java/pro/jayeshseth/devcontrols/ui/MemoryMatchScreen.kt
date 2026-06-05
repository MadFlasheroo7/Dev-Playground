package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Memory match — flip two cards; matching pairs stay revealed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryMatchScreen(onBack: () -> Unit) {
    val emojis = listOf("🍎", "🚀", "🎲", "🎸", "🐬", "🌵", "⚡", "🍕")
    val cards = remember { mutableStateListOf<String>() }
    val revealed = remember { mutableStateListOf<Boolean>() }
    val matched = remember { mutableStateListOf<Boolean>() }
    var first by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }
    var moves by remember { mutableIntStateOf(0) }
    var resetN by remember { mutableIntStateOf(0) }

    LaunchedEffect(resetN) {
        val deck = (emojis + emojis).shuffled()
        cards.clear(); cards.addAll(deck)
        revealed.clear(); revealed.addAll(List(16) { false })
        matched.clear(); matched.addAll(List(16) { false })
        first = -1; busy = false; moves = 0
    }

    suspend fun onPick(i: Int) {
        if (busy || revealed[i] || matched[i]) return
        revealed[i] = true
        if (first == -1) { first = i } else {
            moves++
            busy = true
            if (cards[first] == cards[i]) { matched[first] = true; matched[i] = true; first = -1; busy = false }
            else { delay(700); revealed[first] = false; revealed[i] = false; first = -1; busy = false }
        }
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Match") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = { resetN++ }) { Text("New") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            val done = matched.isNotEmpty() && matched.all { it }
            Text(if (done) "Solved in $moves moves!" else "Moves: $moves", style = MaterialTheme.typography.titleMedium)
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                for (r in 0..3) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0..3) {
                            val i = r * 4 + c
                            if (i >= cards.size) return@Row
                            val face = revealed[i] || matched[i]
                            val flip by animateFloatAsState(if (face) 1f else 0f, label = "flip")
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).padding(4.dp).clip(RoundedCornerShape(12.dp))
                                    .graphicsLayer { scaleX = if (flip > 0.5f) 1f else 1f }
                                    .background(if (matched[i]) Color(0x3369F0AE) else if (face) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary)
                                    .clickable { scope.launch { onPick(i) } },
                                contentAlignment = Alignment.Center
                            ) { if (face) Text(cards[i], fontSize = 30.sp) }
                        }
                    }
                }
            }
        }
    }
}
