# Implementation Plan — Interactive Visual FX Series

This plan describes the implementation of a sequence of four premium, highly interactive visual effects screen updates inside the **Visual FX** category:
1. **Particle Text Dissolve & Fluid Reform** (`Route.PARTICLE_TEXT`)
2. **Retro CRT Screen & VHS Glitch Scanner** (`Route.CRT`)
3. **3D Holographic Foil Card** (`Route.HOLOGRAPHIC`)
4. **Mercury Liquid Metal Sandbox** (`Route.METABALLS`)

---

## User Review Required

> [!IMPORTANT]
> All four effects run inside Android-specific source sets (`androidMain`) to take advantage of advanced graphics, native sensor integrations (Accelerometer/Gyroscope), and AGSL (Android Graphics Shading Language) runtime shaders (requires API 33+ / Android 13+).
>
> We will implement full sandbox control panels for each screen, exposing sliders for physics coefficients, color palettes, rendering parameters, and custom interactive inputs.

---

## Proposed Changes

We will modify/overwrite the screen implementation files in `androidMain`. No route registration changes are needed since all routes are already registered in `Route.kt`, `Categories.kt`, and `MainActivity.kt`.

### 1. Particle Text Sandbox
#### [MODIFY] [ParticleTextScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/visualfx/ParticleTextScreen.kt)
- **Physics System**:
  - `Particle`: Holds current `(x, y)`, velocity `(vx, vy)`, home target `(tx, ty)`, and color.
  - Return Force (Hooke's Law): `ax = (tx - x) * returnSpeed`, `ay = (ty - y) * returnSpeed`.
  - Cursor Interaction: Dragging applies a repulsion/attraction force: `f = strength / (d^2 + epsilon)`.
  - Friction: `vx *= (1 - friction)`, `vy *= (1 - friction)`.
  - Explosion: Adds high-magnitude random radial velocity vectors.
- **Rasterizer**:
  - Draws custom user text on a temporary memory `Bitmap`.
  - Iterates through pixel grids, reading alpha channel values to spawn particles.
- **UI & Panel**:
  - Sliders: Particle Size, Explosion Force, Interaction Radius, Return Speed, Friction.
  - Toggles: Repulsion vs Attraction, Mesh Constellation Lines (faint lines between particles within proximity).
  - Palettes: Neon Cyan, Cyberpunk Pink, Matrix Green, Fire Glow, Rainbow.
  - Live text input field.

### 2. Retro CRT Screen & VHS Glitch Scanner
#### [MODIFY] [CrtScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/visualfx/CrtScreen.kt)
- **UI Container**:
  - Wrap an interactive mock retro control panel in a Compose layout that is rendered into a layer, allowing the CRT shader to post-process live UI interactions.
- **AGSL Shader Features**:
  - **Curvature (Barrel Distortion)**: `uv = 0.5 + (uv - 0.5) * (1.0 + curvature * r^2)`.
  - **Scanlines**: Animated horizontal line darkening using a time-dependent sine wave.
  - **RGB Chromatic Aberration**: Offsetting channel samples: Red `(uv - offset)`, Green `(uv)`, Blue `(uv + offset)`.
  - **VHS Glitch Scanner**: Intermittent horizontal scanline jitter and tracking noise based on time-based steps.
  - **Phosphor Tints**: Monochromatic green, amber, or full color filter modes.
- **Settings Panel**:
  - Sliders: Curvature, Glitch Frequency, Scanline Density, RGB Offset, Vignette.
  - Dropdown: Full Color, Phosphor Green, Phosphor Amber.

### 3. 3D Holographic Foil Card
#### [MODIFY] [HolographicCardScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/visualfx/HolographicCardScreen.kt)
- **3D Parallax Layout**:
  - A card with three overlapping layers:
    1. Background (deep galactic/stars pattern, shifts slowest).
    2. Character/Subject (cyberpunk character or futuristic crest, shifts at medium rate).
    3. Frame & Text (foreground overlay, shifts fastest).
  - Rotation mapping: `rotationX = -tiltY * maxRotation`, `rotationY = tiltX * maxRotation`.
- **Holo Foil Shader (AGSL)**:
  - Iridescent sheen that sweeps across the card based on tilt vectors.
  - Sparkling noise overlays that flicker with rotation.
- **Settings Panel**:
  - Sliders: Max Rotation Angle, Shine Intensity, Sparkle Frequency, Tilt Sensitivity.
  - Selector: Touch Drag Mode vs Gyroscope Sensor Mode.
  - Theme Selector: Cosmic Nebula, Cyber Dragon, Retro Gamer.

### 4. Mercury Liquid Metal Sandbox
#### [MODIFY] [MetaballsLabScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/MetaballsLabScreen.kt)
- **Metaball Shader & Physics**:
  - Simulates 2D circular particles in a physics loop (gravity, drag, collisions, boundary bounces).
  - Metaball rendering: Circles are drawn with blur, and the AGSL shader filters them via alpha thresholding: `sum(radius_i / distance_i^2) > threshold`.
  - Specular reflection: Computes surface normal `n = normalize(grad(field))` to apply dynamic light reflection, metallic specular highlights, and a chrome/liquid-mercury reflection look.
- **Settings Panel**:
  - Sliders: Surface Tension (threshold), Viscosity (damping), Gravity Strength, Specular Intensity.
  - Dropdown: Liquid themes (Mercury Chrome, Liquid Gold, Neon Acid, Magma).
  - Spawn Button: Adds drops at touch/cursor point.
  - Gyroscope Toggle: Controls gravity direction via device tilt.

---

## Verification Plan

### Automated Build Verification
Compile the debug application to ensure there are no compilation errors:
```sh
./gradlew :androidApp:assembleDebug
```

### Manual Verification
1. **Particle Text**:
   - Type "KOTLIN" and tap "Generate".
   - Drag through particles and verify attraction vs repulsion physics.
   - Toggle "Mesh Linking" and change color themes.
2. **Retro CRT Screen**:
   - Verify that clicking buttons inside the CRT container registers input clicks while being distorted, scanlined, and glitched live.
   - Adjust screen curvature and glitch frequency.
3. **Holographic Card**:
   - Drag across the card (or tilt the phone) and observe 3D perspective rotation and rainbow sheen sweeps.
   - Change card themes (Cosmic Nebula, Cyber Dragon, etc.).
4. **Mercury Liquid Metal**:
   - Add new drops. Verify that they fuse together when colliding and separate when pulled apart.
   - Toggle Gyroscope gravity and observe tilt movement.
