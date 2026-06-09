package pro.jayeshseth.devcontrols.ui.present

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.BackHandlerCommon
import pro.jayeshseth.devcontrols.platform.PresentationMode

/** Present mode — full-screen deck with per-slide transitions and shared-element morphs. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PresentScreen(onBack: () -> Unit, deck: Deck = sampleDeck()) {
    var index by remember { mutableIntStateOf(0) }
    fun go(d: Int) { index = (index + d).coerceIn(0, deck.slides.lastIndex) }
    BackHandlerCommon(enabled = true) { onBack() }
    PresentationMode()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        SharedTransitionLayout(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = index,
                label = "slide",
                transitionSpec = {
                    val forward = targetState >= initialState
                    when (deck.slides[targetState].transitionIn) {
                        TransitionIn.PUSH -> {
                            val dir = if (forward) AnimatedContentTransitionScopeDir.Left else AnimatedContentTransitionScopeDir.Right
                            slideIntoContainer(dir, tween(450)) togetherWith slideOutOfContainer(dir, tween(450))
                        }
                        TransitionIn.ZOOM -> (scaleIn(initialScale = 0.85f, animationSpec = tween(450)) + fadeIn(tween(450))) togetherWith (scaleOut(targetScale = 1.1f, animationSpec = tween(450)) + fadeOut(tween(450)))
                        TransitionIn.NONE -> fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                        else -> fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                    }
                }
            ) { i ->
                SlideRenderer(deck.slides[i], this@SharedTransitionLayout, this@AnimatedContent, Modifier.fillMaxSize())
            }
        }

        // tap zones: left third = back, right two-thirds = next
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxHeight().weight(1f).noRippleClick { go(-1) })
            Box(Modifier.fillMaxHeight().weight(2f).noRippleClick { go(1) })
        }

        // top overlay: back + counter
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Exit", color = Color.White) }
            Spacer(Modifier.weight(1f))
            Text("${index + 1} / ${deck.slides.size}", color = Color(0xCCFFFFFF))
        }

        // bottom progress dots
        Row(Modifier.align(Alignment.BottomCenter).padding(20.dp)) {
            deck.slides.indices.forEach { i ->
                Box(Modifier.padding(horizontal = 4.dp).size(if (i == index) 10.dp else 7.dp).clip(CircleShape)
                    .background(if (i == index) Color.White else Color(0x66FFFFFF)))
            }
        }
    }
}

private typealias AnimatedContentTransitionScopeDir = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection

private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)
)
