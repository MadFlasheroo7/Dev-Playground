# E5 — Attack-surface test suite  ★★★★

## Goal
A graded scorecard of how the user's own app behaves under hostile conditions. Defensive
validation only (own app). Consolidates the existing ADB-detection / A11y-detection /
cloak screens into one framework.

## Framework
- `security/Check.kt`:
  ```
  interface Check { val id: String; val title: String; val category: Cat; val severity: Sev
      suspend fun run(ctx, target: String): Finding /* Pass | Fail | Info + detail + remediation */ }
  ```
  Registry of checks; runner executes all (or by category) → `Scorecard` with per-category
  grade (A–F from pass ratio weighted by severity) + overall.
- `ui/AttackSurfaceScreen.kt`: pick target app → "Run all" → expandable category sections,
  each finding with status chip, detail, and remediation text; export Markdown report.

## Checks (by category)
- **Manifest/static** (read via self-ADB `dumpsys package` / `pm`):
  - `debuggable` flag set? · `allowBackup` true? · `usesCleartextTraffic`/network-security
    config · exported components (activities/services/receivers/providers) enumerated &
    flagged · `taskAffinity`/launchMode task-hijack surface.
- **Environment** (does the device look hostile, and does the app care):
  - emulator props (`getprop ro.kernel.qemu`, ro.product.model/brand) · root artifacts
    (su, Magisk paths, `which su`) · Frida (open port 27042, frida-server proc) · Xposed/
    LSPosed packages · ADB/USB-debug enabled · developer options on.
- **Overlay / tapjacking**: draw a `TYPE_APPLICATION_OVERLAY` over the target (reuse the
  Floating Bubble overlay), guide the user to tap through; report whether target uses
  `setFilterTouchesWhenObscured` / `FLAG_SECURE` (touches blocked = good).
- **Screen capture**: `MediaProjection` capture a frame while target is foreground; if the
  target marks sensitive screens `FLAG_SECURE` the frame is black → pass; else fail.
- **Accessibility scraping**: our a11y service dumps the target's node tree; report what
  text/PII is readable (demonstrates scraping exposure).
- **Clipboard**: monitor `ClipboardManager` while user operates target; flag secrets copied
  in cleartext.
- **Backup**: `bmgr` enabled + `allowBackup` → attempt a backup and report extractability.

## Build order (vertical slices)
1. Check framework + Scorecard UI + 4 static checks (debuggable, allowBackup, cleartext,
   exported components) read over self-ADB.
2. Environment probes (emulator/root/frida/adb).
3. Overlay tapjacking probe (reuse Floating Bubble).
4. MediaProjection secure-screen capture probe.
5. A11y scraping + clipboard monitor probes.
6. Markdown report export.

## Risks / notes
- Several probes need the user to actively use the target app → design as guided steps with
  "start probe → switch to app → return".
- MediaProjection needs a consent dialog per session; a11y needs the service enabled
  (already wired). Strictly own-app, defensive.
- Reuses: AdbToolScaffold/AdbRunner, AppPickerDialog, FloatingBubble overlay, A11y service,
  existing AdbDetection/A11yDetection logic folded in as checks.
