# Dev Controls — Feature Inventory

A single-activity Jetpack Compose "playground" app (`pro.jayeshseth.devcontrols`,
minSdk 30 / targetSdk 36) that explores how far an **unrooted** Android app can be
pushed, doubling as a security-testing surface for the author's own production app.
It has grown into **~258 feature screens across 24 collapsible categories**.

> Generated from the source of truth (`MainActivity.kt`). Every feature screen carries a
> one-line KDoc summary; shared infrastructure is documented at the top of each module.

---

## Architecture

- **Single Activity** (`MainActivity`), no Navigation library. In-app nav is a flat
  `Route` enum rendered one screen at a time via `when(route)`; `MAIN` is the status
  panel + collapsible category menu.
- **One nav callback.** Screens are reached through a single `onNavigate: (Route) -> Unit`
  (refactored from ~150 per-screen callbacks after hitting the JVM 64 KB method limit).
  Adding a screen = enum entry + `when` branch + one `LabBtn`.
- **`LabCategory` / `LabBtn`** — collapsible category headers (expansion state hoisted to
  the Activity via a `CompositionLocal` + `SnapshotStateMap`, so it survives navigation),
  with a hoisted `rememberScrollState` and a `BackHandler` that returns to `MAIN`.

## Shared infrastructure

| Module | Purpose |
| --- | --- |
| `adb/AdbClient` + `ui/adbtools/AdbTool` (`AdbToolScaffold`, `AdbRunner`) | Wireless **self-ADB**: after one pairing, run shell commands as **UID 2000** over loopback. Scaffold gates a screen behind a pair/connect banner and runs commands with snackbar feedback. |
| `ui/adbtools/AppPickerDialog` + `util/InstalledApps` | Searchable app picker (user or all packages). |
| `ui/PermGate` / `PermGateMulti` | Runtime-permission gate wrapper. |
| `ui/shaders/ShaderLabScaffold` (`GenerativeShaderScreen`, `ContentShaderScreen`) | AGSL `RuntimeShader` scaffolds — generative (brush) and live-UI-sampling (`RenderEffect`), API-33 gated, with `iResolution`/`iTime` uniforms. |
| `ui/threed/Tactile` | Haptic (composition primitives: quick-rise/fall, ticks, thud) + streaming click-synth depth-feedback engine. |
| `ui/maps/GeoMath` | Haversine distance, bearing, midpoint, DMS. |
| `util/AudioDsp.fft` | Shared radix-2 FFT for the audio screens. |
| `ui/camera/CameraCommon` (`CameraScreenScaffold`, `MediaSink`) | CAMERA-gated scaffold + MediaStore save/list for captures. |

## Dependencies of note
Compose BOM, CameraX (core/camera2/lifecycle/view/**video**/**extensions**),
MediaPipe `tasks-vision`, ML Kit document-scanner, ZXing, Glance, DataStore,
`libadb-android` + `conscrypt` + `sun-security-android` (self-ADB pairing & certs),
`graphics-shapes`, kotlinx-coroutines.

## Permissions
WRITE_SECURE_SETTINGS, WRITE_SETTINGS, ACCESS_WIFI_STATE, ACCESS_NETWORK_STATE, INTERNET,
READ_PHONE_STATE, ANSWER_PHONE_CALLS, PACKAGE_USAGE_STATS, QUERY_ALL_PACKAGES,
POST_NOTIFICATIONS, FOREGROUND_SERVICE(+SHORT_SERVICE/SPECIAL_USE), MANAGE_OWN_CALLS,
REORDER_TASKS, VIBRATE, RECORD_AUDIO, CAMERA, BLUETOOTH_SCAN/CONNECT/**ADVERTISE**,
ACTIVITY_RECOGNITION, NFC, ACCESS_FINE/COARSE_LOCATION, NEARBY_WIFI_DEVICES,
SYSTEM_ALERT_WINDOW. (Many ADB-gated features additionally rely on the UID-2000 self-ADB.)

---

## Categories & screens

### System & ADB
adb_g (local shell) · adb_c (wireless ADB, UID 2000) · adb_pm (permission manager) ·
Hide Dev · Force Dark · App Info Dump · ADB Detection · Accessibility Detection ·
Accessibility Cloak · Network & Proxy · Settings Editor · Input & Automation ·
Feature Flags (device_config) · Package Explorer.

### Windows & Gestures
Gesture Lab · System Bar Sheet · Window FX (blur/shader) · Display Lab · Window Size ·
Nested Scroll · Touch Internals · Predictive Back · Picture-in-Picture · Immersive Mode ·
Orientation Lab · Cutout & Insets · IME Insets · Gesture Exclusion · Floating Bubble (overlay).

### Shaders & Graphics
Gradient · Shader Editor · RenderEffect Chain · Raymarch SDF · Ripple · Voronoi · Halftone ·
Dissolve · Mandelbrot · Glassmorphism · Dynamic Color · fBm Noise · Metaballs · Truchet ·
Glitch/VHS · Edge Detect · Bayer Dither · Reaction-Diffusion · Bloom.

### Motion & Transitions
3D Transform · Physics Motion · Shape Morph · Path Drawing · Pager Transforms · Swipe Cards ·
Circular Reveal · Animated Borders · Shimmer · Flip Clock · Shared Element · Parallax Header ·
AnimatedContent · Carousel · Staggered Reveal · Pull to Dismiss · Page Curl · Path Player.

### Algorithms
Sorting · Pathfinding (A*) · Maze · Fractal Tree · Spirograph · Convex Hull · Delaunay ·
L-System · N-Queens · TSP (2-opt) · Marching Squares · Quadtree · Perlin Terrain.

### Physics
Double Pendulum · N-Body · Boids · Falling Sand · Soft Body · Spring Chain · Cloth ·
Billiards · Galton Board · Newton's Cradle · Orbit Sandbox · Charged Particles · Marble Run · Fluid.

### Software 3D
Wireframe 3D · Point-Cloud Globe · Starfield · Solids (lit) · Heightmap 3D · Function Plotter ·
Point-Cloud Morph · Raycaster · OBJ Model.

### Canvas & Fun
Particles · Confetti · Scratch to Reveal · Game of Life · Mandala Pad · Flow Field ·
Circle Packing · Lightning · Clock Gallery · Turtle REPL · Doodle Physics.

### Camera
Lens Explorer · Pro Mode (manual ISO/shutter/focus + histogram) · Exposure Tools (zebra/peaking) ·
Video Recorder · RAW/DNG · Extensions (HDR/Night/Bokeh) · High Frame Rate · Timelapse ·
Motion Cam · Document Scanner (ML Kit) · Gallery.

### Visual FX (26)
Holographic Card · Liquid Swipe · Dynamic Island · Touch Ripple Water · Gooey Nav Bar ·
Magnifier Lens · Frosted Glass · Morphing Sheet · Morphing Icons · Particle Text ·
Palette Theming · Speedometer · Aurora · Caustics · Soap Bubble · CRT TV · Pixelate · Heat Haze ·
Black Hole · Neon Sign · Conic Glow Border · Liquid Wave Fill · 2.5D Parallax · Ink Spread ·
Voronoi Shatter · Smoke.

### Security & Privacy
Fingerprint Auditor · Crypto Playground (AES-GCM/PBKDF2) · Hashing · Password Generator ·
Biometric Lock · Clipboard Monitor.

### Networking & Connectivity
Local Server · Network Info · Ping & Port Scan · NSD Discovery · BLE Peripheral.

### Notifications
Notification Styles · Actions & Reply (RemoteInput) · Progress & Ongoing.

### Developer Utilities
JSON Formatter · Encoder (Base64/Hex/URL) · Regex Tester · JWT Decoder · Generators.

### Time & Date
World Clock · Stopwatch · Pomodoro.

### Toys & Novelty
Bubble Wrap · Decision Wheel · Magic 8-Ball · White Noise.

### Audio Lab
Drum Machine · Theremin · Tone Generator · Clap & Whistle Detector.

### 3D Components
Canvas (push button, knob, toggle, slider, liquid-fill, joystick, jelly) ·
Shader (glossy orb, emboss panel, liquid-fill, domed LED). All with haptic+audio depth feedback.

### Maps & Location
Coordinate Tools · Sun & Moon · Compass Navigator · GPS Tracker.

### Machine Learning
Hand Landmarks · Pose Landmarks · Image Classification (MediaPipe, models in assets).

### Components (27)
Mesh Components · Segmented Control · Speed-Dial FAB · Bottom Sheet · Stepper/Wizard · Timeline ·
Rating · Pull to Refresh · Accordion · PIN Pad · Custom Layout · Reorder List · Drawing · Charts ·
Color Picker · Text FX · Spotlight · Drag & Drop · Calendar · Wheel Picker · Tag Input ·
Tree Browser · Data Table · Kanban · Signature Pad · Progress Gallery · Coach-mark.

### Device & Media (29)
Haptics · USB Inspector · Audio Reactive · Synth Keyboard · Oscilloscope · Metronome ·
Sensor Parallax · Compass · Light Meter · Barometer · Step Counter · BLE Scanner · QR Generator ·
NFC Status · Camera Filter · Social Share · Torch & Strobe · Spectrum · Tuner · Decibel Meter ·
Sensors Dashboard · Bubble Level · Metal Detector · Barcode Scanner · GNSS Dashboard ·
Wi-Fi Scanner · TTS/STT · NFC Reader · Media Visualizer.

### Games
2048 (animated: sliding/merge/spawn) · Snake · Breakout · Tetris-lite · Memory Match · Minesweeper.

### Creative
Kaleidoscope · Matrix Rain · Fireworks · Plasma · Weather · Pixel Art.

---

## Known caveats / not yet verified
- A large fraction was built compile-green but **not visually verified on-device** (esp. the
  Camera category, the 26 Visual FX, and device-gated paths). "Builds" ≠ "looks right."
- **Device-gated**: RAW/DNG capture, high-frame-rate, CameraX extensions, AGSL screens (API 33+),
  composition-primitive haptics, BLE advertise — degrade or no-op where unsupported.
- **No persistence** beyond a few DataStore prefs; category expansion is in-session only.
- ~32 MB of ML/vision model assets bundled; no ABI splits → APK is large.
- Detection/cloak screens are **defensive self-tests of the author's own app only**.
