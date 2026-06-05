package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private class Node(val name: String, val children: List<Node> = emptyList()) { val isLeaf get() = children.isEmpty() }

private val TREE = Node("app", listOf(
    Node("src", listOf(
        Node("main", listOf(
            Node("java", listOf(Node("MainActivity.kt"), Node("ui", listOf(Node("CalendarLabScreen.kt"), Node("TreeBrowserLabScreen.kt"))))),
            Node("res", listOf(Node("values", listOf(Node("strings.xml"), Node("themes.xml"))), Node("drawable"))),
            Node("AndroidManifest.xml")
        )),
        Node("test", listOf(Node("ExampleTest.kt")))
    )),
    Node("build.gradle.kts"),
    Node("proguard-rules.pro")
))

/** Tree / file browser — expandable nested rows with indentation guides. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeBrowserLabScreen(onBack: () -> Unit) {
    val expanded = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>().apply { put("app", true) } }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Tree Browser") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        LazyColumn(Modifier.padding(inner).fillMaxSize().padding(8.dp)) {
            renderNode(this, TREE, 0, "", expanded)
        }
    }
}

private fun renderNode(
    scope: androidx.compose.foundation.lazy.LazyListScope,
    node: Node,
    depth: Int,
    path: String,
    expanded: SnapshotStateMap<String, Boolean>,
) {
    val key = "$path/${node.name}"
    scope.item(key) {
        val open = expanded[key] ?: false
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !node.isLeaf) { expanded[key] = !open }
                .padding(start = (depth * 18).dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (node.isLeaf) "   📄 ${node.name}" else "${if (open) "▾" else "▸"} 📁 ${node.name}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    if ((expanded[key] ?: false)) {
        node.children.forEach { renderNode(scope, it, depth + 1, key, expanded) }
    }
}
