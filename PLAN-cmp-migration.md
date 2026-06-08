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
5. Capability layer (expect/actual): kotlinx-serialization (DeckJson), DeckArchive zip, FileIo
   save/open, clipboard, share, showToast — android+desktop actuals.
6. Web (Wasm) target; gate off SharedTransitionLayout/`java.*` screens.
7. iOS source sets (declared only; unbuildable on Linux — CI/Mac verifies).

## Standing risks
iOS unverifiable here; SharedTransitionLayout (ui/present) off-Android gated; `java.*` crypto
screens stay android/desktop; DataStore + DeckExport(PdfDocument/MediaStore) stay androidMain;
desktop/web deck export deferred.
