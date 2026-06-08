# Deck Builder — feature backlog

Parked for later (we have bigger fish to fry). Current state: authoring (drag/resize,
filmstrip add/select/delete, property panel), present mode (SharedTransitionLayout,
landscape lock, tap nav), exports (PDF / PNG / PPTX with transitions + fade entrances),
and a portable `.cdeck` zip-bundle format (`util/DeckJson.kt`, Open/Save via SAF).

Effort: **S** ≈ <1h · **M** ≈ a few hours · **L** ≈ day+.

## Tier 1 — usability (makes it a real tool)
- **Saved-deck library + templates** (M) — editor always boots `sampleDeck()`; add a
  gallery of `.cdeck` files in app storage (reuse `DeckJson`) + starter layouts
  (title / section / bullets / quote). Biggest single upgrade; makes everything else worth more.
- **Reorder + duplicate slides** (S) — filmstrip can add/select/delete but not move/clone.
- **Undo/redo** (S) — deck-history stack in `DeckEditorScreen`.
- **Markdown → deck import** (M) — `#` → title, `##` → section, `-` → bullets. Fastest authoring.

## Tier 2 — visual richness
- **Image element** (M) — Photo Picker → embed. Unlocks (and is unlocked by) the `.cdeck`
  zip `media/` folder; needs a `DeckExport` render branch.
- **Snap-to-grid + alignment guides** (M) — drag guides + snap threshold on the canvas.
- **Custom color picker + element opacity / z-order** (S–M) — colors are presets only today.
- **More shapes** (S) — triangle, line/arrow, rounded-rect stroke (`ShapeKind` + renderer).

## Tier 3 — Compose animation differentiator
- **AGSL blur-in / dissolve entrance** (M) — reuse existing shader scaffolds; a `RenderEffect`
  blur-in is a genuine "PowerPoint can't do this" moment.
- **Richer shared-element morphs** (M) — morph text size/color; allow `sharedKey` on any
  element type, not just shapes.
- **More entrances + build-order control** (M) — directional slide-ins, spring bounce,
  typewriter text; user-ordered build.

## Tier 4 — present experience
- **Presenter mode** (M) — the `notes` field exists but is unused: speaker view with
  next-slide preview + notes + timer.
- **Auto-advance / loop** (S), **draw-on-slide / laser pointer** (M).

## Recommended start order (when we return)
1. Tier 1 bundle: **library + templates + reorder/duplicate + undo**.
2. **Image element** (lights up the zip `media/` we future-proofed).
3. Flashy solo option: **AGSL blur-in entrance**.
