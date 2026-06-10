package pro.jayeshseth.devcontrols.ui

import pro.jayeshseth.devcontrols.util.fmt
import pro.jayeshseth.devcontrols.util.hex2
import pro.jayeshseth.devcontrols.util.pad
import pro.jayeshseth.devcontrols.util.nowMillis

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/** Wheel time picker — two snapping wheels (hours + minutes) with a centred selection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelPickerLabScreen(onBack: () -> Unit) {
    var hour by remember { mutableIntStateOf(0) }
    var minute by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Wheel Picker") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Wheel(count = 24, onSelected = { hour = it }, modifier = Modifier.width(90.dp))
                Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                Wheel(count = 60, onSelected = { minute = it }, modifier = Modifier.width(90.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Selected: %02d:%02d".format(hour, minute), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun Wheel(count: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val itemH = 48.dp
    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val center by remember {
        derivedStateOf {
            val info = state.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) 0
            else {
                val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2f) - mid) }?.index ?: 0
            }
        }
    }
    LaunchedEffect(center) { onSelected(center.coerceIn(0, count - 1)) }

    Box(modifier.height(itemH * 5), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemH * 2),
            modifier = Modifier.fillMaxSize()
        ) {
            items((0 until count).toList()) { i ->
                val selected = i == center
                Box(Modifier.fillMaxWidth().height(itemH), contentAlignment = Alignment.Center) {
                    Text((i).pad(2), textAlign = TextAlign.Center,
                        fontSize = if (selected) 28.sp else 20.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(Modifier.fillMaxWidth().padding(bottom = itemH), color = MaterialTheme.colorScheme.outlineVariant)
        HorizontalDivider(Modifier.fillMaxWidth().padding(top = itemH), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
