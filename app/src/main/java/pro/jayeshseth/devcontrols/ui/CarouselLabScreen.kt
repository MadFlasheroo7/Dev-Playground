package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

/** Carousel — a HorizontalPager whose centre card scales up and side cards shrink + fade. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarouselLabScreen(onBack: () -> Unit) {
    val colors = listOf(
        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF5C6BC0), Color(0xFF29B6F6),
        Color(0xFF66BB6A), Color(0xFFFFCA28), Color(0xFFFF7043)
    )
    val pager = rememberPagerState(pageCount = { colors.size })

    Scaffold(
        topBar = { TopAppBar(title = { Text("Carousel") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = 64.dp),
                pageSpacing = 16.dp,
                modifier = Modifier.fillMaxWidth().height(360.dp)
            ) { page ->
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue
                val scale = 1f - (offset * 0.18f).coerceIn(0f, 0.35f)
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = 1f - offset.coerceIn(0f, 0.5f) }
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors[page]),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${page + 1}", color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Swipe — the centred card grows; neighbours recede.",
                Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
