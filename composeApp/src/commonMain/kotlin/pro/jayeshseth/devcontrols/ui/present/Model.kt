package pro.jayeshseth.devcontrols.ui.present

import kotlinx.serialization.Serializable

/** Data model for Compose-native decks. Positions are normalized (0..1) on a 16:9 canvas. */

@Serializable enum class TransitionIn { NONE, FADE, PUSH, ZOOM }
@Serializable enum class Entrance { NONE, FADE_UP, SCALE_IN, STAGGER }
@Serializable enum class ElementType { TITLE, SUBTITLE, BODY, BULLETS, SHAPE }
@Serializable enum class ShapeKind { CIRCLE, RECT }

@Serializable
data class SlideElement(
    val id: String,
    val type: ElementType,
    val x: Float, val y: Float, val w: Float, val h: Float,
    val text: String? = null,
    val bullets: List<String> = emptyList(),
    val colorArgb: Long = 0xFFFFFFFF,
    val fontSize: Int = 24,
    val centerAlign: Boolean = false,
    val weight: Int = 400,
    val entrance: Entrance = Entrance.NONE,
    val sharedKey: String? = null,
    val shape: ShapeKind = ShapeKind.RECT,
)

@Serializable
data class Slide(
    val id: String,
    val bgColors: List<Long>,
    val transitionIn: TransitionIn = TransitionIn.FADE,
    val elements: List<SlideElement>,
    val notes: String = "",
)

@Serializable
data class Deck(val title: String, val slides: List<Slide>)

/** A sample deck that shows off transitions + a shared-element morph (the "logo" circle). */
fun sampleDeck(): Deck {
    val accent = 0xFF4C6FFF
    val white = 0xFFFFFFFF
    val sub = 0xFFB9C0D8
    return Deck(
        "Compose Decks",
        listOf(
            Slide(
                "s1", listOf(0xFF101225, 0xFF1B1E3A), TransitionIn.FADE,
                listOf(
                    SlideElement("logo", ElementType.SHAPE, 0.36f, 0.16f, 0.28f, 0.28f, colorArgb = accent, shape = ShapeKind.CIRCLE, sharedKey = "logo"),
                    SlideElement("t", ElementType.TITLE, 0.08f, 0.55f, 0.84f, 0.14f, text = "Compose Decks", colorArgb = white, fontSize = 46, weight = 800, centerAlign = true, entrance = Entrance.FADE_UP),
                    SlideElement("s", ElementType.SUBTITLE, 0.08f, 0.70f, 0.84f, 0.1f, text = "presentations that actually morph", colorArgb = sub, fontSize = 20, centerAlign = true, entrance = Entrance.FADE_UP),
                )
            ),
            Slide(
                "s2", listOf(0xFF0E1A14, 0xFF143026), TransitionIn.PUSH,
                listOf(
                    SlideElement("logo", ElementType.SHAPE, 0.82f, 0.06f, 0.12f, 0.12f, colorArgb = accent, shape = ShapeKind.CIRCLE, sharedKey = "logo"),
                    SlideElement("t", ElementType.TITLE, 0.08f, 0.10f, 0.7f, 0.12f, text = "What you get", colorArgb = white, fontSize = 36, weight = 800),
                    SlideElement("b", ElementType.BULLETS, 0.10f, 0.30f, 0.82f, 0.6f, colorArgb = 0xFFD7F5E6, fontSize = 24, entrance = Entrance.STAGGER,
                        bullets = listOf("Shared-element morphs between slides", "Spring physics & staggered builds", "AGSL shader backgrounds", "Live widgets on a slide")),
                )
            ),
            Slide(
                "s3", listOf(0xFF1A0F22, 0xFF2E1640), TransitionIn.ZOOM,
                listOf(
                    SlideElement("logo", ElementType.SHAPE, 0.10f, 0.40f, 0.22f, 0.22f, colorArgb = accent, shape = ShapeKind.CIRCLE, sharedKey = "logo"),
                    SlideElement("t", ElementType.TITLE, 0.40f, 0.40f, 0.55f, 0.2f, text = "The circle morphed here", colorArgb = white, fontSize = 30, weight = 700, entrance = Entrance.SCALE_IN),
                )
            ),
            Slide(
                "s4", listOf(0xFF101225, 0xFF2A1E3A), TransitionIn.FADE,
                listOf(
                    SlideElement("t", ElementType.TITLE, 0.08f, 0.42f, 0.84f, 0.16f, text = "Build it in Compose", colorArgb = white, fontSize = 44, weight = 800, centerAlign = true, entrance = Entrance.SCALE_IN),
                )
            ),
        )
    )
}
