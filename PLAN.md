# Dev Controls — feature roadmap (all categories)

## STATUS: all category batches executed (2026-06-04)
Built green + installed. 74 new screens across DM(13), SG(8), WG(7), CO(9), MT(8),
AL(8), PH(8), S3(6), CF(7). MainActivity navigation refactored from ~150 per-screen
callbacks to a single `onNavigate: (Route) -> Unit` (fixed a "method too large" 64KB
limit). New perms: ACCESS_FINE/COARSE_LOCATION, NEARBY_WIFI_DEVICES, SYSTEM_ALERT_WINDOW;
MainActivity now `supportsPictureInPicture`. Still open from the System & ADB plan:
W/X/Z/AB/AD/AF + Intent Forge / Content Provider Probe / AppOps / Screen Capture /
Activity Stack Inspector.

---


The app is a single-activity `Route`-enum playground; each feature is one screen wired
through `MainActivity` (import → callback param → call-site → `when` branch → `Route`
entry → `LabBtn` under a `LabCategory`). Shaders use the `GenerativeShaderScreen` /
`ContentShaderScreen` scaffolds; ADB tools use `AdbToolScaffold` + `AdbRunner`.

Build cadence per batch: write screens → wire → `:app:assembleDebug` green → install →
report remaining batches.

---

## System & ADB  (in progress)
DONE this session: shared infra (`AdbToolScaffold`/`AdbRunner`/`AppPickerDialog`),
**Y** Network & Proxy, **AA** Settings Editor, **AC** Input & Automation,
**AE** Feature Flags, **AG** Package Explorer.
REMAINING from the prior plan: **W** ADB Toolbox · **X** Battery & Power ·
**Z** Live Logcat (needs `openShellFlow` streaming primitive) · **AB** dumpsys Explorer ·
**AD** Process Monitor · **AF** Display/UI tweaks.
NEW ideas: **Intent Forge** (craft `am start`/`broadcast` with extras), **Content
Provider Probe** (`content query/insert`), **AppOps Console** (full `appops` per pkg),
**Screen Capture/Record** (`screencap`/`screenrecord` → preview), **Activity Stack
Inspector** (`dumpsys activity activities`).

---

## Windows & Gestures  (Batch WG)
Has: Gesture Lab, System Bar Sheet, Window FX, Display Lab, Window Size, Nested Scroll,
Touch Internals, Predictive Back.
1. **Picture-in-Picture** — `enterPictureInPictureMode` with `PictureInPictureParams`
   (aspect ratio, source-rect hint, remote actions); auto-PiP on leave.
2. **Floating bubble overlay** — `TYPE_APPLICATION_OVERLAY` draggable chat-head via
   `WindowManager.addView` (needs SYSTEM_ALERT_WINDOW; reuse the WindowManager work
   from System Bar Sheet). Snap-to-edge, expand/collapse.
3. **IME insets animation** — keyboard-follow content using `WindowInsetsAnimation` /
   `Modifier.imeNestedScroll` + `imePadding`; show inset values live.
4. **Display-cutout visualizer** — draw `DisplayCutout` bounds + safe areas, toggle
   `LAYOUT_IN_DISPLAY_CUTOUT_MODE` (default / shortEdges / never).
5. **Immersive mode lab** — hide/show bars, sticky-immersive, `systemBarsBehavior`,
   keep-screen-on, `FLAG_SECURE` (blocks screenshots — visible demo).
6. **Gesture exclusion zones** — `setSystemGestureExclusionRects` so edge-swipes in a
   region don't trigger back; visualize the rects.
7. **Orientation/rotation lab** — per-screen `requestedOrientation`, sensor lock,
   reverse-portrait, lock-to-current.

## Shaders & Graphics  (Batch SG)
Has: Gradient, Shader Editor, RenderEffect Chain, Raymarch, Ripple, Voronoi, Halftone,
Dissolve, Mandelbrot, Glassmorphism, Dynamic Color.
1. **fBm / domain-warp noise** (AGSL) — layered value/simplex noise, animated warp.
2. **Metaballs** (AGSL) — N moving charges, threshold iso-surface, palette ramp.
3. **Glitch / VHS / chromatic aberration** — content-sampling `ContentShaderScreen`
   (RGB channel offset, scanlines, jitter) over live UI.
4. **Sobel edge detect** — content/camera shader; knob for threshold.
5. **Bayer dithering / ASCII** — ordered-dither matrix shader (extends Halftone).
6. **Reaction-diffusion** (Gray-Scott) — ping-pong textures via repeated draw passes.
7. **Truchet / hex tiling** (AGSL) — animated tile rotation, moiré.
8. **Bloom / light-bleed** — bright-pass + blur + additive composite (RenderEffect chain).

## Motion & Transitions  (Batch MT)
Has: 3D Transform, Physics Motion, Shape Morph, Path Drawing, Pager Transforms, Swipe
Cards, Circular Reveal, Animated Borders, Shimmer, Flip Clock.
1. **Shared-element transition** — `SharedTransitionLayout` list→detail hero animation.
2. **Collapsing parallax header** — custom nested-scroll toolbar with image parallax.
3. **AnimatedContent gallery** — slide/fade/scale/size-transform transitions side-by-side.
4. **Carousel & wheel picker** — M3 carousel + an infinite snapping wheel (date/number).
5. **Staggered reveal grid** — items animate in with index-based delay on first compose.
6. **Pull-to-dismiss sheet** — spring-backed draggable sheet with velocity fling.
7. **Page-curl / book flip** — graphicsLayer rotationY fold with shadow gradient.
8. **Keyframe path player** — animate an object along a user-drawn `Path` with easing.

## Algorithms  (Batch AL)
Has: Sorting, Pathfinding (A*), Maze, Fractal Tree, Spirograph.
1. **Convex hull** — Graham scan; tap to add points, animate the wrap.
2. **Delaunay / Voronoi (compute)** — Bowyer-Watson; tap to add sites.
3. **L-systems** — rewrite rules → turtle graphics (Koch, dragon, plant presets).
4. **Sudoku / N-Queens backtracking** — visualize the search + backtrack.
5. **TSP** — nearest-neighbour + 2-opt, animated tour improvement.
6. **Marching squares** — iso-contours over a scalar field, animated threshold.
7. **Quadtree / k-d tree** — spatial partition that subdivides around moving points.
8. **Perlin terrain** — noise heightmap with color ramp + octave sliders.

## Physics  (Batch PH)
Has: Double Pendulum, N-Body, Boids, Falling Sand, Soft Body, Spring Chain.
1. **Cloth** — grid of Verlet constraints; drag/pin corners, wind.
2. **SPH fluid** — smoothed-particle fluid in a box; tilt with accelerometer.
3. **Billiards / elastic collisions** — circle-circle impulse resolution, drag to shoot.
4. **Galton board** — pegs + bins → normal distribution histogram.
5. **Newton's cradle** — constrained pendulums with momentum transfer.
6. **Orbit sandbox** — tap to place bodies, trails, adjustable G.
7. **Charged particles** — Coulomb attraction/repulsion, field lines.
8. **Marble run / pegboard** — draw pegs, drop marbles with collision.

## Software 3D  (Batch S3)
Has: Wireframe, Point-Cloud Globe, Starfield.
1. **OBJ model loader** — parse `.obj` from assets/picker, painter's-algo filled render.
2. **Flat-shaded solids** — cube/torus/sphere with back-face cull + Lambert lighting.
3. **Raycaster maze** — Wolfenstein-style first-person walk via DDA.
4. **Heightmap terrain** — perlin mesh, rotating, wireframe/filled toggle.
5. **3D function plotter** — z = f(x,y) surface mesh with rotation.
6. **Point-cloud morph** — interpolate particles between two 3D shapes.

## Canvas & Fun  (Batch CF)
Has: Particles, Confetti, Scratch to Reveal, Game of Life.
1. **Mandala / symmetry pad** — radial-mirrored drawing (k-fold), export.
2. **Flow field** — Perlin-driven particle streams (generative art).
3. **Circle packing** — grow non-overlapping circles to fill an image/region.
4. **Lightning generator** — recursive midpoint-displacement bolts on tap.
5. **Clock gallery** — analog, binary, word-clock, dotmatrix faces.
6. **Turtle graphics REPL** — tiny command language drawing live.
7. **Doodle physics** — draw shapes that drop and collide (ties to Physics engine).

## Components  (Batch CO)
Has: Mesh, Segmented, Speed-Dial, Bottom Sheet, Stepper, Timeline, Rating, Pull-Refresh,
Accordion, PIN Pad, Custom Layout, Reorder List, Drawing, Charts, Color Picker, Text FX,
Spotlight, Drag & Drop.
1. **Calendar / date-range picker** — from-scratch month grid + range selection.
2. **Wheel time picker** — snapping hour/minute spinners.
3. **Tag/chip input** — type-to-add chips with backspace-delete + suggestions.
4. **Tree / file browser** — expandable nested rows with indentation guides.
5. **Sortable data table** — sticky header, column sort, zebra rows.
6. **Kanban board** — drag cards between columns (extends Drag & Drop).
7. **Signature / cropper** — capture-area + bitmap export; image crop with handles.
8. **Progress gallery** — determinate, segmented, M3 wavy, ring countdown.
9. **Coach-mark / tooltip overlay** — spotlight a target with a callout balloon.

## Device & Media  (Batch DM)
Has: Haptics, USB Inspector, Audio Reactive, Synth, Oscilloscope, Metronome, Sensor
Parallax, Compass, Light Meter, Barometer, Step Counter, BLE, QR gen, NFC status,
Camera Filter, Social Share.
1. **Torch / strobe** — `CameraManager.setTorchMode`, adjustable strobe Hz + screen-light.
2. **FFT spectrum / spectrogram** — `AudioRecord` → FFT bars + scrolling spectrogram.
3. **Guitar tuner** — autocorrelation/YIN pitch detect → nearest note + cents.
4. **Decibel meter** — RMS → dB SPL gauge with peak hold.
5. **Sensors dashboard** — every `SensorManager` sensor live (accel/gyro/mag/…).
6. **Bubble level** — accelerometer 2-axis level + inclinometer.
7. **Metal detector** — magnetometer magnitude vs baseline.
8. **QR/barcode scanner** — CameraX + zxing decode (pairs with QR generator).
9. **GNSS dashboard** — lat/long/speed/altitude + per-satellite `GnssStatus` C/N0 bars.
10. **Wi-Fi scanner** — `WifiManager.scanResults` list with RSSI bars/channels.
11. **TTS / STT playground** — TextToSpeech + SpeechRecognizer round-trip.
12. **NFC NDEF reader** — read/parse tag records (extends NFC status).
13. **Media Visualizer** — pick audio, `Visualizer` API waveform/FFT bars.

---

## Recommended ordering (suggested)
Quick visual wins first, then heavier engines:
1. **DM** Device & Media (torch, FFT, tuner, sensors dashboard, QR scanner) — high utility
2. **SG** Shaders (fBm, metaballs, glitch, edge-detect) — reuse shader scaffolds
3. **WG** Windows (PiP, floating bubble, immersive, cutout) — reuse WindowManager work
4. **CO** Components (calendar, wheel, tree, table, kanban)
5. **MT** Motion (shared-element, parallax header, carousel)
6. **AL/PH/S3/CF** the simulation/generative batches as desired

Pick any subset / order — same per-batch build-green-then-install cadence as U/V and the
System & ADB tools. New permissions to expect: SYSTEM_ALERT_WINDOW (bubble),
ACCESS_FINE_LOCATION (GNSS/Wi-Fi scan), RECORD_AUDIO already present.
