package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AccordionItemData(
    val id: Int,
    val title: String,
    val content: String
)

/**
 * Reusable Accordion / Expandable List component.
 * Supports single-expand (accordion) and multi-expand modes, custom headers, and body composables.
 */
@Composable
fun Accordion(
    items: List<AccordionItemData>,
    modifier: Modifier = Modifier,
    allowMultipleOpen: Boolean = false,
    headerContent: @Composable (item: AccordionItemData, isOpen: Boolean) -> Unit = { item, isOpen ->
        val rotation by animateFloatAsState(if (isOpen) 180f else 0f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "⌄",
                modifier = Modifier.rotate(rotation),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    },
    bodyContent: @Composable (item: AccordionItemData) -> Unit = { item ->
        Text(
            text = item.content,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
) {
    val haptic = LocalHapticFeedback.current
    var openItemId by remember { mutableStateOf<Int?>(null) }
    val openItemIds = remember { mutableStateListOf<Int>() }

    Column(modifier = modifier) {
        items.forEach { item ->
            val isOpen = if (allowMultipleOpen) {
                openItemIds.contains(item.id)
            } else {
                openItemId == item.id
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                // Header tap targets
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (allowMultipleOpen) {
                                if (isOpen) openItemIds.remove(item.id) else openItemIds.add(item.id)
                            } else {
                                openItemId = if (isOpen) null else item.id
                            }
                        }
                ) {
                    headerContent(item, isOpen)
                }

                // Body expansion with visibility animation
                AnimatedVisibility(
                    visible = isOpen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    bodyContent(item)
                }
            }
        }
    }
}

/** Accordion lab screen showcasing the reusable Accordion component with config controls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccordionLabScreen(onBack: () -> Unit) {
    val faqItems = remember {
        listOf(
            AccordionItemData(1, "What makes a component reusable?", "A reusable component is self-contained. It exposes parameters (like Modifiers, custom layouts, and states) and communicates back via event lambdas without depending on external screen state."),
            AccordionItemData(2, "Does this support multiplatform?", "Yes! Since it is written entirely in Jetpack Compose, it compiles natively to Android, iOS, Desktop, and Web (Wasm)."),
            AccordionItemData(3, "How does the height animation work?", "It uses `Modifier.animateContentSize` combined with a spring physics spec, which dynamically measures the layout bounds and animates changes smoothly."),
            AccordionItemData(4, "Can I customize the header and body?", "Absolutely. The Accordion component takes custom composable lambdas for both `headerContent` and `bodyContent` so you can style them however you want.")
        )
    }

    var allowMultipleOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accordion / Collapse") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                "Reusable Accordion Component",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "A flexible collapsing list component. You can toggle between accordion and multi-expand modes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            // Configuration card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Multiple Open", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("If disabled, opening one item collapses the others.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = allowMultipleOpen,
                        onCheckedChange = { allowMultipleOpen = it }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Reusable Accordion
            Accordion(
                items = faqItems,
                allowMultipleOpen = allowMultipleOpen
            )
        }
    }
}
