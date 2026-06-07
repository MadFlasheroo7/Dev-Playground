# Presentation Builder — Compose-native decks ("Keynote in Compose")

## Goal
Author slide decks **in Compose**, present them full-screen with Compose-only transitions
(**shared-element morphs**, spring physics, staggered builds), and optionally **export**
to PDF / PNG (reliable, on-device) — with `.pptx` as a lossy stretch. The whole point is
that slides are live Compose scenes, not static rectangles, so a title/image can *morph*
from one slide to the next.

## Single source of truth
One `@Composable SlideRenderer(slide, animatedVisibilityScope, sharedScope)` used in THREE
places: the **editor preview**, **present mode**, and the **export rasterizer**. Build it
once; everything else drives it.

## Data model (`presentation/Model.kt`, serializable to JSON)
```
Deck(id, title, theme, slides: List<Slide>)
Theme(bg: Brush/colors, accent, fontFamily, titleSize…)
Slide(id, layout, background?, elements: List<Element>, transitionIn, notes, autoAdvanceMs?)
Element(id, type, x, y, w, h, rotation, z,
        text?, style(font/size/weight/color/align), imageUri?, shapeKind?,
        entrance: Entrance, sharedKey: String?)   // sharedKey ties morphs across slides
enum Layout { TITLE, TITLE_CONTENT, TWO_COLUMN, FULL_IMAGE, SECTION, BLANK }
enum ElementType { TITLE, SUBTITLE, BULLETS, BODY, IMAGE, SHAPE, CODE, ICON }
enum TransitionIn { NONE, FADE, SLIDE_LEFT, PUSH, ZOOM, FLIP }
enum Entrance { NONE, FADE_UP, SCALE_IN, TYPEWRITER, STAGGER }  // STAGGER = bullets in sequence
```
Positions are normalized (0..1) on a 16:9 canvas so it scales to any screen/export size.

## The Compose goodies (why this beats PPTX)
- **Shared-element morph** — `SharedTransitionLayout` + `Modifier.sharedElement(
  rememberSharedContentState(element.sharedKey), avScope)`. Tag an image/title on slide N
  and N+1 with the same `sharedKey` → it smoothly **moves + resizes** between slides.
  (Already proven in the Visual FX "Shared Element" screen.)
- **Per-slide transitions** via `AnimatedContent(targetState = index, transitionSpec = …)`
  (fade / slide / push / zoom / flip with `graphicsLayer rotationY`).
- **Per-element entrances** — `FADE_UP`/`SCALE_IN` via `Animatable`, `TYPEWRITER` (substring
  reveal), `STAGGER` (bullets animate in with index delay).
- Spring physics, AGSL backgrounds (reuse our shader scaffolds as slide backgrounds), live
  widgets on a slide (a chart, a counter) — all things a static deck can't hold.

## Present mode (`presentation/PresentScreen.kt`)
Full-screen immersive (reuse Immersive lab approach). Tap right / swipe-left = next, etc.
Slide counter + progress dots, **speaker-notes** overlay, **auto-advance** timer, "B" to
black, a **laser pointer** (touch → glowing dot) and freehand **annotation** layer.
`BackHandler` exits to the editor.

## Editor (`presentation/EditorScreen.kt`) — Track A core
- Slide filmstrip (reorderable) + an editing canvas (the live `SlideRenderer`).
- Add elements (title/bullets/body/image/shape/code/icon); **drag to position**, pinch/
  handles to resize; tap to edit text + a properties panel (font, size, color, align,
  entrance, **sharedKey** picker that lists keys used on adjacent slides).
- Per-slide: layout preset, background (color/gradient/AGSL/image), transition, notes.
- Theme editor for the deck. (Mirrors the planned APK-builder playground pattern.)

## Export (`presentation/Export.kt`)
- **PDF (reliable):** render each slide into a fixed 1920×1080 `GraphicsLayer`
  (`rememberGraphicsLayer()` → `graphicsLayer.toImageBitmap()`, Compose 1.7+), draw each
  bitmap onto an Android `PdfDocument` page, save to MediaStore/Downloads, share. (Static
  snapshot — transitions are lost, that's expected for PDF.)
- **PNG sequence** — same capture, one file per slide to the gallery.
- **`.pptx` (stretch, lossy):** Apache POI `XSLF` (`org.apache.poi:poi-ooxml`) emits real
  PowerPoint. ⚠ POI is heavy on Android and parts touch `java.awt`; text+shape+image export
  is feasible but adds size/risk. Decide only if a real .pptx is needed; otherwise PDF
  covers "share it."
- Export caveat: off-screen capture of *every* slide needs each mounted briefly; do it in a
  sequential capture pass (mount slide → capture → next), not all at once.

## Persistence
Decks as JSON (org.json — already a dep — or kotlinx-serialization). Save/load to app
storage; "recent decks" list. Image elements reference `content://` URIs (persist read perms).

## Dependencies
Core needs **nothing new** (Compose + Android `PdfDocument` are built in; `PickVisualMedia`
for images). `.pptx` only: `org.apache.poi:poi-ooxml` (+ likely a slimmed/Android-compatible
build). Persistence: existing `org.json` or add kotlinx-serialization.

## Build order
**Track A — engine (build first):** Model + `SlideRenderer` + present mode with
**AnimatedContent transitions and shared-element morphs**, driven by a hardcoded sample
deck. This proves the "wow" (the morphs) with no editor yet.
**Track B — editor:** filmstrip + drag/resize/edit + properties + theme; JSON save/load.
**Track C — export:** PDF (then PNG); `.pptx` only if required.

## Risks
- `SharedTransitionLayout` is `@Experimental` (works; API may shift).
- Off-screen rasterization for full-deck export is the fiddliest part — `graphicsLayer
  .toImageBitmap()` needs the slide actually composed; sequential capture pass mitigates.
- Drag/resize editor with normalized coords + rotation handles is real work (Track B).

## Status: PLANNED
Recommended start: **Track A** — a sample deck + present mode showing slide transitions and
a shared-element morph between two slides. Smallest path to the magic.
