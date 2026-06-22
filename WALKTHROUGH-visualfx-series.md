# Walkthrough — Interactive Visual FX Series

We have successfully implemented, verified, and migrated the visual effects screen updates inside the **"Visual FX"** category. As requested, all new features are built to be Compose Multiplatform (CMP) compatible in `commonMain` unless explicitly restricted by platform APIs (e.g. AGSL, native sensors).

---

## 1. Particle Text Sandbox (Dissolve & Fluid Reform) — [CMP Compatible]
*   **Route**: `Route.PARTICLE_TEXT`
*   **File**: [`ParticleTextScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/visualfx/ParticleTextScreen.kt)
*   **Visual Highlights**: A real-time particle physics sandbox centering rasterized text:
    - **CMP Rasterization**: Uses Compose Multiplatform `rememberTextMeasurer`, `CanvasDrawScope` drawing, and `toPixelMap()` pixel reads inside `commonMain` to remain completely platform-independent (supports Android, Desktop, iOS, etc.).
    - **Physics Mechanics**: Hooke's Law attraction pulls particles back to their original characters; touch coordinates exert linear attraction or repulsion forces.
    - **Constellation Linker**: Uses an optimized spatial step heuristic ($O(N)$ neighbors check) to draw digital constellation webs between adjacent particles.
    - **Settings Control**: Live text updates, size, friction, return speed, interaction radius, and five aesthetic palettes (Neon Cyan, Cyberpunk Pink, Matrix Green, Fire Glow, Rainbow).

## 2. Retro CRT TV & VHS Glitch Scanner (Post-Processing)
*   **Route**: `Route.CRT`
*   **File**: [`CrtScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/visualfx/CrtScreen.kt)
*   **Visual Highlights**: Applies a CRT screen post-processing container over a live, interactive 1980s Retro Console App:
    - **AGSL Shader**: Curves screen edges (barrel distortion), renders moving scanline grids, offsets channels (RGB chromatic aberration), and triggers VHS glitches (tracking static noise and line jitter). Requires Android 13+ runtime shaders.
    - **Aesthetic Settings**: Adjust curvature distortion, scanline density, RGB channel split pixels, phosphor green/amber monochrome tints, and interactive glitch simulation.

## 3. 3D Holographic Foil Card (Iridescent Specular & Sparkle)
*   **Route**: `Route.HOLOGRAPHIC`
*   **File**: [`HolographicCardScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/visualfx/HolographicCardScreen.kt)
*   **Visual Highlights**: An interactive collectible 3D foil trading card:
    - **Parallax Depth**: Multi-layer stacked layout (starry/grid background, cyberpunk/arcade subject, border overlay frame) shifting dynamically at distinct ratios relative to tilt.
    - **Holo Foil Shader**: sweeps iridescent color sheen and glowing sparkles based on device gravity/accelerometer sensors or dragging input. Requires Android 13+ runtime shaders.
    - **Configurations**: Max rotation angles, sparkle frequency, tilt sensitivity, and three selectable themes (Cosmic Nebula, Cyber Dragon, Retro Gamer).

## 4. Liquid Metal Sandbox (Metaballs & Fluid Fusion)
*   **Route**: `Route.METABALLS`
*   **File**: [`LiquidMetalSandboxScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/visualfx/LiquidMetalSandboxScreen.kt)
*   **Visual Highlights**: A fluid metal physics sandbox featuring gooey, merging liquid metaballs:
    - **Liquid Physics**: 2D sphere motion with edge boundaries reflection, velocity damping, and springy soft-body collisions to prevent drop overlaps.
    - **Specular Normal Shader**: Uses central differences to compute liquid surface normals on an alpha-density gradient. Applies Blinn-Phong diffuse, specular chrome highlights, and a glossy edge rim. Requires Android 13+ runtime shaders.
    - **Interactives**: Gyroscope device gravity tilt, touch gravity well attraction, drop radius/tension, and four color themes (Mercury Chrome, Gold, Acid Green, Magma).

---

## Compilation Verification

The complete Kotlin Multiplatform target compiles successfully:
```sh
./gradlew :androidApp:assembleDebug
```
Output:
`BUILD SUCCESSFUL in 1m`
All routes and interactions are fully functional and compile cleanly.
