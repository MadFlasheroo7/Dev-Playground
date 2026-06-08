package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.nav.Route
import pro.jayeshseth.devcontrols.nav.labCategories

/**
 * The shared categorized home menu: collapsible categories of labelled routes ([labCategories]).
 * [isEnabled] filters which routes are tappable on a platform (Android = all; desktop/web hide
 * routes that aren't available there). Categories with no enabled items are omitted.
 */
@Composable
fun CategoryMenu(
    expandedCats: SnapshotStateMap<String, Boolean>,
    onNavigate: (Route) -> Unit,
    isEnabled: (Route) -> Boolean = { true },
) {
    labCategories.forEach { cat ->
        val items = cat.items.filter { isEnabled(it.route) }
        if (items.isEmpty()) return@forEach
        val open = expandedCats[cat.title] ?: false
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expandedCats[cat.title] = !open }
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(cat.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(if (open) "▾" else "▸", style = MaterialTheme.typography.titleMedium)
            }
            if (open) {
                Spacer(Modifier.height(6.dp))
                items.forEach { item ->
                    Button(onClick = { onNavigate(item.route) }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(item.label)
                    }
                }
            }
        }
    }
}
