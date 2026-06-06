# Dev Controls — Roadmap (what's left to explore)

Companion to `FEATURES.md` (what's built). This is the backlog: unbuilt categories, the
deep multi-screen "epics," per-category extensions, and the cross-cutting work that would
make the whole thing more than a pile of demos.

---

## 1. Unbuilt categories (proposed, not yet built)
- **❤️ Health & Fitness** — PPG heart-rate (camera+flash → BPM from red channel), breathing
  pacer (animation + haptics), reaction-time tester, BMI/body calcs.
- **🎨 Generative Art** — strange attractors (Lorenz/Clifford/de Jong), Julia & Newton
  fractals (AGSL, pinch/pan), flow-field paintings, circle packing → save as wallpaper.
- **🗄️ Data & Storage** — Room-backed notes app, SQLite explorer (over self-ADB), prefs/
  DataStore viewer, hex editor. (Would also add the persistence the app lacks.)
- **🔤 Text & Typography** — variable-font axis animator, font explorer, Markdown renderer,
  RTL/bidi + emoji, ASCII-art generator.
- **🧮 Math & Science** — graphing calculator (plot f(x)), unit/constant converter, matrix
  ops, statistics, wave-interference sim.
- **♿ Accessibility** — color-blindness simulator + daltonizer, high-contrast/large-text
  preview, screen-reader demo.

## 2. Deep epics (multi-screen subsystems — plans written, not built)
- **APK Builder** — visual playground → real installable APK (renderer dex + generated
  binary manifest + apksig sign + install via PackageInstaller **and** silent self-ADB
  `pm install`). See **`PLAN-apk-builder.md`**. Track A (editor) is the recommended start.
- **E1 — Traffic inspector / MITM** (`PLAN-E1-vpn-mitm.md`): local HTTP(S) MITM proxy
  (Phase 1) → VpnService raw capture (Phase 2). Most on-theme for Moneyview testing.
- **E2 — On-device profiler** (`PLAN-E2-profiler.md`): live CPU/mem/jank/battery + PID
  logcat over self-ADB. Needs the deferred `AdbClient.openShellFlow` streaming primitive.
- **E5 — Attack-surface suite** (`PLAN-E5-attack-surface.md`): graded scorecard
  (debug/emulator/hook/overlay/screen-capture/clipboard/backup); folds in existing detection.
- **E13 — On-device ML, slice 4** (`PLAN-E13-ml-lab.md`): local LLM chat (MediaPipe Gemma)
  → and the **moonshot**: a tool-calling agent that drives this toolbox by natural language.

## 3. Per-category backlogs (extend what exists)
- **Developer Utilities**: HTTP client (mini-Postman), Base & bitwise, Text toolkit, Diff
  viewer, Color converter + WCAG contrast, Timestamp converter, JSON→Kotlin data class, Cron
  explainer, String escape/unescape, CSV viewer, Markdown preview, HTTP/MIME reference.
- **Audio Lab**: Looper/overdub, Voice changer, Hearing test (audiogram), BPM detector + tap
  tempo, Pitch game, DTMF gen/decode (Goertzel), Echo/delay live, Binaural beats, Filter
  playground, Morse audio.
- **3D Components**: flip card, safe-dial/combination lock, pressure (3D-Touch-style) button,
  piano-key bank, tilt marble-in-bowl.
- **Sensors Pro** (Device & Media): AHRS 3D orientation / artificial horizon, G-force &
  free-fall meter, seismograph/knock detect, air-gesture recognizer (DTW), dead-reckoning
  path tracer, vario/altimeter.
- **Games**: Flappy, Pong, Space Invaders, Asteroids, Dino runner, Tower stack, Doodle Jump,
  Pinball, Bubble Shooter, 15-puzzle, Lights Out, Sudoku (playable), Wordle, Simon,
  Tic-Tac-Toe (minimax), Connect Four.
- **Maps & Location**: star/sky view (rotation-vector planetarium), golden-hour times,
  what3words-style grid, geofence alerts.
- **Networking**: Wi-Fi Direct / Nearby P2P chat, NFC card emulation (HCE), WebSocket client.
- **System & ADB** (from the original plan): Intent Forge, Content-Provider Probe, AppOps
  console, Screen capture/record, Activity-stack inspector, Logcat, Battery/Power, dumpsys
  explorer, Process monitor, Display tweaks.

## 4. Cross-cutting (quality, not features) — highest leverage
- **Verification pass** — actually run each category on-device and fix what's broken/ugly;
  most of the recent breadth is compile-green only.
- **Favorites + search** — ~258 screens in a flat list is now a usability problem. Add a
  search bar over all `LabBtn`s and a favorites row; persist with DataStore.
- **Persistence + settings** — DataStore-backed settings screen; remember category expansion
  across restarts (`rememberSaveable`/DataStore); theme/dark-mode toggle.
- **Tests & CI** — there are none. At least smoke-launch each Route in an instrumented test.
- **APK size** — ABI splits / app bundle; move large ML models to on-demand download.
- **Git hygiene** — repo now initialized; keep committing per feature (done since baseline).

## 5. Suggested priorities
1. **Favorites + search shell** (makes everything else usable).
2. **APK Builder — Track A editor** (the standout user-requested feature).
3. **E1 MITM** or **E5 attack-surface** (re-aim the self-ADB power at the original purpose).
4. Then breadth: Health & Fitness / Generative Art categories.
