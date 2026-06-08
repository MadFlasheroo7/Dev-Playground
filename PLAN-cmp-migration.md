# CMP Migration — DevControls → Android + Desktop + Web + iOS

Big-bang restructure of the single Android `:app` into a Compose Multiplatform `:composeApp`
module. Portable screens (~65%) move to `commonMain`; Android-only features (camera, AGSL
shaders, sensors, audio I/O, ML, ADB, widgets/tiles/services, DataStore) stay in `androidMain`
and are registered into nav only on Android. Full design: `~/.claude/plans/` (approved).

## Toolchain (verified working)
Kotlin 2.2.10, AGP 9.2.1, Compose Multiplatform **1.11.1**, kotlinx-serialization 1.9.0.
**Critical gotcha:** AGP 9.0+ rejects `com.android.application` + the KMP plugin unless
`android.builtInKotlin=false` and `android.newDsl=false` are set in `gradle.properties`
(disables AGP's built-in Kotlin / new DSL). Because new DSL is off, `android{}` uses the
classic `compileSdk = 36` (not the `compileSdk { release(36){...} }` block).

## Source layout
`composeApp/src/{commonMain,androidMain,desktopMain,wasmJsMain,iosMain}/kotlin/...`, package
unchanged (`pro.jayeshseth.devcontrols`) so moving a file between source sets changes no imports.
`androidMain/{AndroidManifest.xml,res,assets}` wired via `android.sourceSets["main"]`.

## Steps & checkpoints
1. **DONE** (e090f35) — scaffold: `:app`→`:composeApp`, MP build (androidTarget only), all
   code in androidMain. ✅ `:composeApp:assembleDebug` builds the unchanged app.
2. **DONE** (364b9ad) — Desktop(JVM) target + hello window. ✅ `:composeApp:compileKotlinDesktop`.
3. **DONE** (ebb61cf) — nav shell + theme → commonMain (Route enum, AppRoot, expect
   BackHandlerCommon/PlatformHome/PlatformScreenHost, expect dynamicColorScheme,
   DefaultHome/MissingScreen). Android keeps the 261-branch when + home as actuals.
4. **IN PROGRESS** (dc5b937, batch 1) — 113 portable screens → commonMain; generated
   nav/ScreenRegistry.kt `CommonScreen(route,onBack):Boolean` + `commonRoutes`. 32 candidates
   reverted (transitive Android deps). ✅ desktop + android green. REMAINING: ~57 screens
   flagged android-only by the conservative heuristic (mostly `java.*`/`String.format`,
   shader-scaffold users, and partial screens) — revisit with the capability layer / for
   desktop-only where `java.*` is fine but wasm is not.
5. **IN PROGRESS** (e24b6e4) — capability layer + **deck builder cross-platform**: PresentScreen,
   DeckEditorScreen, DeckJson → commonMain; DeckJson on kotlinx-serialization; platform/Capabilities
   (DeckArchive zip, rememberDeckFileIo SAF/AWT, rememberDeckExporter android-real/desktop-null,
   rememberToaster, LockLandscape). 115 common screens. REMAINING: migrate the rest + categorized home.
   NEXT per user: (a) migrate "everything else" still in androidMain that is actually portable;
   (b) make the home **categories** common so desktop/web show the same categorized menu (today
   they show a flat DefaultHome list; Android shows the rich DevControlsScreen).
6. **DEFERRED/BLOCKED** — Web (Wasm). wasmJsMain sources + actuals written; target block commented
   out in build.gradle.kts. Blocker: CMP 1.11.1 wasm klibs don't resolve against Kotlin 2.2.10
   (wasm-stdlib skew → unresolved compose symbols); Kotlin 2.2.20 cleared the stdlib warning but
   compose symbols still unresolved (klib↔compiler mismatch). Also note: `@Serializable` crashes the
   wasm FIR frontend here — DeckJson was switched to the kotlinx-serialization **runtime JsonElement
   DSL** (no @Serializable, no plugin codegen) which is the right multiplatform approach regardless.
   Revisit with a newer CMP (1.12.x) or a Kotlin that matches CMP's wasm klibs.
7. iOS source sets (declared only; unbuildable on Linux — needs a Mac/CI). NOT yet started.

## Caveat on java.* screens
~57 screens stayed in androidMain because they use `java.*`/`String.format` — those break BOTH wasm
AND iOS (Kotlin/Native), so to share them on desktop only they'd need a `jvmShared` intermediate
source set (android+desktop). Deferred until iOS/wasm are live.

## Standing risks
iOS unverifiable here; SharedTransitionLayout (ui/present) off-Android gated; `java.*` crypto
screens stay android/desktop; DataStore + DeckExport(PdfDocument/MediaStore) stay androidMain;
desktop/web deck export deferred.
