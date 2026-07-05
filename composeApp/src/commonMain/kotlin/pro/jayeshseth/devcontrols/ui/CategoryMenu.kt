package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Includes a real-time search field at the top.
 * Pinned features are displayed at the top for quick access.
 */
@Composable
fun CategoryMenu(
    expandedCats: SnapshotStateMap<String, Boolean>,
    onNavigate: (Route) -> Unit,
    isEnabled: (Route) -> Boolean = { true },
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Load pinned routes from cross-platform persistent storage
    var pinnedRoutes by remember {
        mutableStateOf(
            pro.jayeshseth.devcontrols.platform.PersistentStorage.getString("pinned_features", "")
                .split(",")
                .filter { it.isNotEmpty() }
                .toSet()
        )
    }

    val onTogglePin: (Route) -> Unit = { route ->
        val newPins = if (pinnedRoutes.contains(route.name)) {
            pinnedRoutes - route.name
        } else {
            pinnedRoutes + route.name
        }
        pinnedRoutes = newPins
        pro.jayeshseth.devcontrols.platform.PersistentStorage.saveString("pinned_features", newPins.joinToString(","))
    }

    val onClearAllPins: () -> Unit = {
        pinnedRoutes = emptySet()
        pro.jayeshseth.devcontrols.platform.PersistentStorage.saveString("pinned_features", "")
    }

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Search features...") },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "✕",
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { searchQuery = "" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )

        val query = searchQuery.trim()

        if (query.isEmpty()) {
            // --- 📌 Pinned Features Section ---
            val pinnedItems = labCategories.flatMap { it.items }
                .filter { pinnedRoutes.contains(it.route.name) && isEnabled(it.route) }
                .distinctBy { it.route }

            if (pinnedItems.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📌 Pinned Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Clear All",
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onClearAllPins() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    
                    // Render pinned items in a 2-column grid
                    val chunked = pinnedItems.chunked(2)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .clickable { onNavigate(item.route) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "✕",
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onTogglePin(item.route) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
            }

            // --- Collapsible Categories List ---
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
                            val isPinned = pinnedRoutes.contains(item.route.name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                    .clickable { onNavigate(item.route) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "›",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = if (isPinned) "★" else "☆",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onTogglePin(item.route) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isPinned) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- Search Results List ---
            val matchedItems = mutableListOf<Pair<String, pro.jayeshseth.devcontrols.nav.LabItem>>()
            labCategories.forEach { cat ->
                cat.items.forEach { item ->
                    if (isEnabled(item.route)) {
                        val matchesLabel = item.label.contains(query, ignoreCase = true)
                        val matchesCategory = cat.title.contains(query, ignoreCase = true)
                        if (matchesLabel || matchesCategory) {
                            matchedItems.add(cat.title to item)
                        }
                    }
                }
            }

            if (matchedItems.isEmpty()) {
                Text(
                    text = "No matching features found.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                matchedItems.forEach { (catTitle, item) ->
                    val isPinned = pinnedRoutes.contains(item.route.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigate(item.route) }
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(catTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isPinned) "★" else "☆",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onTogglePin(item.route) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isPinned) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("➔", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
