package pro.jayeshseth.devcontrols.ui

import pro.jayeshseth.devcontrols.util.pad
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Reusable 3D Cylinder-style WheelPicker component.
 * Supports customizable items, visible count, perspective bending, and haptic feedback.
 */
@Composable
fun <T> WheelPicker(
    items: List<T>,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 48.dp,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val halfCount = visibleItemCount / 2

    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) 0
            else {
                val centerOffset = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                layoutInfo.visibleItemsInfo.minByOrNull {
                    abs((it.offset + it.size / 2f) - centerOffset)
                }?.index ?: 0
            }
        }
    }

    // Trigger callback and haptic tick on item selection change
    var lastSelectedIndex by remember { mutableIntStateOf(startIndex) }
    LaunchedEffect(selectedIndex) {
        val safeIndex = selectedIndex.coerceIn(0, items.size - 1)
        if (safeIndex != lastSelectedIndex) {
            lastSelectedIndex = safeIndex
            onItemSelected(items[safeIndex])
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItemCount),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * halfCount),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items.size) { index ->
                val isSelected = index == selectedIndex
                
                // Calculate 3D perspective variables based on distance from viewport center
                val graphicsModifier = Modifier.graphicsLayer {
                    val layoutInfo = listState.layoutInfo
                    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                    if (itemInfo != null) {
                        val viewCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                        val itemCenter = itemInfo.offset + itemInfo.size / 2f
                        val distanceFromCenter = itemCenter - viewCenter
                        val maxDistance = itemInfo.size * (halfCount + 0.5f)

                        // Bending angle (rotation around X-axis)
                        val fraction = (distanceFromCenter / maxDistance).coerceIn(-1f, 1f)
                        rotationX = fraction * -45f
                        
                        // Shrink and fade out items rotating away
                        scaleX = 1f - abs(fraction) * 0.25f
                        scaleY = 1f - abs(fraction) * 0.25f
                        alpha = 1f - abs(fraction) * 0.6f
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .then(graphicsModifier),
                    contentAlignment = Alignment.Center
                ) {
                    itemContent(items[index], isSelected)
                }
            }
        }

        // Selection overlay bars
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(bottom = itemHeight),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            thickness = 1.5.dp
        )
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(top = itemHeight),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            thickness = 1.5.dp
        )
    }
}

/** Wheel picker screen showcasing two 3D Cylinder WheelPickers (Hours and Minutes). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelPickerLabScreen(onBack: () -> Unit) {
    var hour by remember { mutableIntStateOf(10) }
    var minute by remember { mutableIntStateOf(30) }

    val hoursList = remember { (0..23).toList() }
    val minutesList = remember { (0..59).toList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3D Wheel Picker") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "3D Cylinder Wheel Picker",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Scroll the wheels to feel the haptic ticks and watch the 3D rotation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(40.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hours Wheel
                WheelPicker(
                    items = hoursList,
                    onItemSelected = { hour = it },
                    startIndex = 10,
                    modifier = Modifier.width(80.dp)
                ) { item, isSelected ->
                    Text(
                        text = item.pad(2),
                        textAlign = TextAlign.Center,
                        fontSize = if (isSelected) 24.sp else 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = ":",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Minutes Wheel
                WheelPicker(
                    items = minutesList,
                    onItemSelected = { minute = it },
                    startIndex = 30,
                    modifier = Modifier.width(80.dp)
                ) { item, isSelected ->
                    Text(
                        text = item.pad(2),
                        textAlign = TextAlign.Center,
                        fontSize = if (isSelected) 24.sp else 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
            
            Text(
                text = "Selected Time: ${hour.pad(2)}:${minute.pad(2)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
