# APK Builder — visual playground → installable APK (no compiler, no aapt2)

## Concept (user's vision)
A **playground/layout editor** screen: add Text + Button widgets to a canvas, drag to
position them, edit text / color / size / action. Hit **Generate APK** → it builds and
**installs a standalone app** that renders exactly that layout.

## Key insight that makes it tractable on an unrooted phone
Separate the **component class name** (baked into a prebuilt dex, never changes) from the
**applicationId** (the manifest `package=`, just a string, can be anything). One generic
**renderer Activity** is reused for every generated app; only a `layout.json` + the
package name vary. No on-device compiler, no resources, no aapt2.

```
[Playground editor] → layout.json → APK = renderer.dex + binary manifest + assets/layout.json → align → apksig sign → install
   widgets[]            {type,x,y,w,h,text,color,fontSize,action}      (renderer.dex prebuilt, identical every time)
```

## Single source of truth
Write the renderer as ONE Composable `RenderLayout(widgets)`. Use it both for the **live
preview** in the editor AND (compiled to a dex) inside the generated app → preview is
pixel-identical to the installed app.

## Can / can't
- ✅ Place & move text + buttons; set text/size/color/position; interpreted button actions
  (toast, open URL, counter, switch designed "screen").
- ❌ Arbitrary custom code in buttons → needs the on-device compiler (ECJ+D8), the big
  risky stretch. Interpreted actions cover most real uses.

## Backend stages (the gotchas)
1. **Binary manifest (AXML):** chunked format (header, string pool, resource-map of attr→
   framework IDs, typed element/attr chunks). Either hand-encode (~300 lines) or ship a
   gradle-compiled template manifest and patch the package string in the string pool.
2. **Signing — version trap:** Android 11+ needs the **v2 APK Signing Block** for targetSdk
   30+. v2 requires a **zip-aligned** archive (4-byte-aligned stored entries). `apksig`
   signs but does NOT align → write our own aligned zip, then v2-sign. Self-signed cert via
   the `sun-security-android` dep already in the project.
3. **Install (both paths):**
   - **PackageInstaller** session API (or ACTION_VIEW + FileProvider) — needs
     `REQUEST_INSTALL_PACKAGES`. Works anytime, shows a confirm dialog.
   - **Silent via self-ADB (UID 2000):** shell can't read app-private files → either write
     the APK to `/sdcard/Download` (MediaStore, shell-readable) then `pm install /sdcard/…`,
     or stream `pm install -S <size> -` piping APK bytes into the shell's **stdin** (small
     `AdbClient` addition to expose the stream's output stream).

## Dependencies / setup
- New dep: `com.android.tools.build:apksig`.
- New perm: `REQUEST_INSTALL_PACKAGES` + a FileProvider (authority + file_paths.xml).
- Gradle step to emit `assets/renderer.dex` (build a tiny renderer module/apk, pull its
  classes.dex). Bundle a template binary manifest asset if going the patch route.

## Build order
**Track A — Playground editor (build FIRST):** Compose canvas; add/drag/edit Text & Button
widgets; properties panel (text, color, size, action); live preview via `RenderLayout`.
Low-risk, fun, defines the JSON schema. Usable before the backend exists.

**Track B — APK backend (after):**
1. **PoC:** gradle emits a real tiny `assets/template.apk`; on-device generate keypair +
   self-signed cert → **v2-sign** it with apksig → install via PackageInstaller AND silent
   `pm install` via self-ADB. Proves apksig-on-ART + both install paths on the Moto.
2. Renderer-dex packaging + `assets/layout.json` injection.
3. Binary manifest generation/patching with the chosen package.
4. Wire **Generate APK** to the editor's widgets → both install paths.

## Status: PLANNED (deferred — build the 3D Tactile Components first per user)
