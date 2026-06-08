package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Person(val name: String, val age: Int, val city: String)

/** Sortable data table — sticky header, click a column to sort, zebra rows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTableLabScreen(onBack: () -> Unit) {
    val base = remember {
        listOf(
            Person("Ada", 36, "London"), Person("Linus", 54, "Portland"), Person("Grace", 85, "New York"),
            Person("Alan", 41, "Manchester"), Person("Margaret", 70, "Boston"), Person("Dennis", 70, "Murray Hill"),
            Person("Ken", 80, "Berkeley"), Person("Barbara", 78, "Ohio"), Person("Tim", 68, "San Diego")
        )
    }
    var sortCol by remember { mutableIntStateOf(0) }
    var asc by remember { mutableStateOf(true) }

    val rows = remember(sortCol, asc) {
        val c = when (sortCol) {
            0 -> compareBy<Person> { it.name }
            1 -> compareBy { it.age }
            else -> compareBy { it.city }
        }
        base.sortedWith(if (asc) c else c.reversed())
    }

    fun arrow(i: Int) = if (sortCol == i) (if (asc) " ▲" else " ▼") else ""

    Scaffold(
        topBar = { TopAppBar(title = { Text("Data Table") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 12.dp, vertical = 12.dp)) {
                HeaderCell("Name${arrow(0)}", 2f) { if (sortCol == 0) asc = !asc else { sortCol = 0; asc = true } }
                HeaderCell("Age${arrow(1)}", 1f) { if (sortCol == 1) asc = !asc else { sortCol = 1; asc = true } }
                HeaderCell("City${arrow(2)}", 2f) { if (sortCol == 2) asc = !asc else { sortCol = 2; asc = true } }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(rows.withIndex().toList()) { (idx, p) ->
                    Row(Modifier.fillMaxWidth()
                        .background(if (idx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Text(p.name, Modifier.weight(2f))
                        Text("${p.age}", Modifier.weight(1f))
                        Text(p.city, Modifier.weight(2f))
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float, onClick: () -> Unit) {
    Text(text, Modifier.weight(weight).clickable(onClick = onClick), fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer)
}
