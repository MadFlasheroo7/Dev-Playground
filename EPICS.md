# Dev Controls — ambitious epics (multi-session engineering)

These are not one-screen labs; each is a real subsystem with persistence, lifecycle,
and architecture. Grouped by theme. Complexity ★ (1–5). Shared prerequisite for the
profiler/logcat ones: the deferred `AdbClient.openShellFlow(cmd): Flow<String>` streaming
primitive (incremental reads off the exec mutex).

## Theme 1 — No-root power tools (the "how far can it go" thread)

### E1. On-device VPN traffic inspector / MITM  ★★★★★  ⟵ also serves Moneyview testing
A local `VpnService` (no root) that routes all device traffic through an in-app TUN
interface, parses IP/TCP/UDP, reassembles HTTP, and (with a user-installed CA) does TLS
interception of your own app. UI: live flow list, request/response viewer, filter by
app/host, export PCAP/HAR. The hardest and highest-value epic.
- Key: `VpnService`, TUN fd, userspace TCP/IP (e.g. a lwIP-style reassembler), CA cert,
  per-app allow-list (`addAllowedApplication`).
- Risks: userspace TCP is genuinely hard; TLS MITM needs the target app to trust the CA.

### E2. On-device app profiler  ★★★★
A mini Android-Studio-Profiler for any package (esp. Moneyview): live CPU%, RSS/PSS,
frame jank (`dumpsys gfxinfo framestats`), battery drain, thread count, GC — polled +
streamed via self-ADB, charted over time. Logcat tail filtered to the target PID with
crash/ANR capture. Export a session report.
- Key: `openShellFlow("logcat …")`, periodic `dumpsys meminfo/gfxinfo/batterystats`,
  time-series store, charts (reuse Charts lab).

### E3. No-root automation engine (Appium-lite / Tasker-lite)  ★★★★
A scripting layer over self-ADB + accessibility: record a UI flow (a11y tree snapshots +
`input` events), replay with element matching ("tap the button with text X", "wait until
screen Y"), conditionals/loops, named macros, scheduling. Drive any app hands-free.
- Key: `uiautomator dump` / AccessibilityService node tree, `input`, a small DSL +
  interpreter, DataStore persistence, foreground service for scheduled runs.

### E4. APK / DEX reverse-engineering inspector  ★★★
Point at any installed package → pull its APK (`pm path` + read), parse the manifest,
permissions, components, signing certs, native libs, and dex method/class counts +
string table. A real on-device RE tool.
- Key: read APK zip, parse binary AXML manifest, dex header/method-id counts, `dumpsys
  package` signatures.

## Theme 2 — Security self-test harness (Moneyview defensive validation)

### E5. Attack-surface test suite  ★★★★
A battery of checks that simulate what a hostile environment looks like, then reports how
your own app reacts: debuggable/`ADB connected`/emulator-prop/hooking-framework presence,
overlay tapjacking (draw over your app, check `FILTER_TOUCHES`/`FLAG_SECURE`), screen
recording/`MediaProjection` capture of a secure screen, accessibility scraping, clipboard
sniffing, backup extraction (`bmgr`), and `StrictMode`/exported-component probing.
Consolidates the existing ADB/A11y detection screens into one graded scorecard.

### E6. TLS / cert-pinning probe  ★★★
Hit your app's endpoints: enumerate supported TLS versions/ciphers, test pinning by
presenting a user CA (ties to E1), attempt downgrade, check HSTS, dump cert chains.

## Theme 3 — Deep OS integration (real apps, not demos)

### E7. Custom soft keyboard (IME)  ★★★★
A real `InputMethodService`: layout, popups, gesture/swipe typing, suggestions, a
"developer" mode (insert snippets, hex, emoji). Full IME lifecycle + system registration.

### E8. Live wallpaper engine  ★★★
A `WallpaperService` that renders our AGSL shaders / particle sims as the actual device
wallpaper, reacting to touch + time, with a picker to choose which sim.

### E9. Rich Glance widget suite  ★★★
Real home-screen widgets (the `glance` dep is already in): live battery/sensor/ADB-endpoint
widgets, an interactive toggle widget, a "now playing"-style card — with update workers.

### E10. Quick Settings tile pack + Tasker plugin  ★★
Several `TileService` tiles (proxy toggle, kiosk, torch-strobe) + expose actions to Tasker
via the locale plugin protocol.

## Theme 4 — Graphics / audio / ML heavy

### E11. OpenGL ES / Vulkan 3D engine  ★★★★
Replace the software 3D with a real GL renderer: textured meshes, Phong lighting, a loaded
glTF/OBJ model, orbit camera, skybox, post-processing. Optionally a GPU compute pipeline
(ping-pong FBOs) for a real GPU fluid/SPH.

### E12. Low-latency audio workstation (Oboe)  ★★★★
A polyphonic synth + step sequencer + effects chain (delay/reverb/filter) on the
`AAudio/Oboe` low-latency path, with a real-time-safe audio engine and a pattern editor.

### E13. On-device ML lab  ★★★★
MediaPipe / TFLite tasks running on camera/audio: hand & pose tracking with skeleton
overlay, image classification, and an on-device LLM (Gemini Nano via AICore, or MediaPipe
LLM Inference) chat — all offline.

### E14. Real-time camera CV / AR  ★★★
`ImageAnalysis` pipeline: edge/contour detection, ArUco-style marker tracking, optical
flow, and a simple AR overlay anchored to a detected plane/marker.

## Suggested first targets
- Most on-theme + valuable: **E1 VPN inspector** (security) and **E2 profiler** (power),
  which share the streaming primitive and a time-series/flow store.
- Highest "wow" for least risk: **E8 live wallpaper** or **E11 GL engine** (reuse shaders),
  **E13 on-device LLM**.
- Build order within an epic: vertical slice first (smallest end-to-end working path),
  then iterate. Each epic spans multiple build-green-then-install steps like the batches.
