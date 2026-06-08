# Dev Controls — Deck file format (`.cdeck`)

## Context
The presentation builder exports/imports decks via SAF (`util/DeckJson.kt`, Open/Save in
`DeckEditorScreen`). We branded the portable format as `.cdeck`: a self-identifying,
versioned JSON file, with a reader that sniffs the container so a future zip bundle drops
in without breaking files written today.

## Phase 1 — Brand `.cdeck` + format-sniffing reader (DONE)
- `util/DeckJson.kt`: `EXTENSION = "cdeck"`, added `FORMAT = "cdeck"` header in `toJson`,
  and `read(bytes)` that routes `{` → JSON (now + legacy `.deck.json`), `PK` → zip (future),
  else → error. `fromJson` stays tolerant (optInt/optLong/enumOr defaults).
- `DeckEditorScreen.kt`: Save filename uses `DeckJson.EXTENSION` (→ `*.cdeck`); import now
  reads bytes → `DeckJson.read(...)`. Open picker MIME includes `*/*` (no registered MIME).
- Transfer stays via SAF Open/Save (robust cross-device). Tap-to-open intentionally skipped
  — Android can't reliably match a custom extension for `content://` URIs (Drive/Gmail).

## Phase 2 — Zip bundle (LATER, when an image/asset element exists)
`my-talk.cdeck` = ZIP { `manifest.json`, `deck.json`, `media/` }. Writer = variant of
`DeckExport.exportPptx`'s `ZipOutputStream` plumbing (copy assets into `media/`, repoint
refs). Reader = the `'P'` branch of `DeckJson.read` (`ZipInputStream`). Old raw-JSON
`.cdeck` files keep working via the `'{'` branch — no migration.

## Verification
1. Build, install, open Presentation → Deck Editor.
2. Save → filename ends `.cdeck`, writes to chosen location.
3. Open it back → deck intact; Present → `sharedKey` morph still works.
4. Open an old `.deck.json` → still imports (`{` sniff path).
5. Open a random non-deck file → "Couldn't read that deck file" toast, no crash.
6. Cross-device: Save → Drive → Open on another device → present.
