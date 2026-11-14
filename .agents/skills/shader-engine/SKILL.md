---
name: shader-engine
description: >-
  Comprehensive guide for developing, authoring, and verifying shaders, real-time UI lenses,
  and dual-input transitions in the :shader-engine: Kotlin Multiplatform library across Android
  (AGSL, API 33+), Pure Canvas (API 21+), and Skia Compose Multiplatform.
---

# Shader Engine Development Guide (`:shader-engine:`)

This skill provides technical patterns, invariant rules, and authoring workflows for extending the `:shader-engine:` library.

---

## 1. Zero-Recomposition Invariants

When writing or modifying shaders and modifiers:
1. **Never read animated state in Composable body**:
   Always pass state via deferred lambda providers (e.g. `{ controller.timeSeconds }`) or directly inside `drawWithContent` / `graphicsLayer`.
2. **Maintain FP32 precision**:
   GLSL variables for coordinates (`fragCoord`, `resolution`, `pointer`, `time`) must remain `float`. Never cast coordinates to `half`.
3. **Clamp premultiplied alpha**:
   Always clamp color output:
   ```glsl
   return half4(min(col.rgb, half3(col.a)), col.a);
   ```

---

## 2. Recipe: Adding a New Real-Time UI Lens

1. Create preset in `shader-engine/.../presets/<Name>Preset.kt`:
   ```kotlin
   object MyLensPreset {
       @Language("AGSL")
       const val SHADER = """
           uniform shader content;
           uniform float2 resolution;
           uniform float time;
           uniform float2 pointer;
           uniform float p1;

           half4 main(float2 fragCoord) {
               // ... compute optics / deformation ...
               return content.eval(fragCoord);
           }
       """
   }
   ```
2. Register in `ShaderEngineStudioScreen.kt`:
   - Add entry to `StudioPreset` enum.
   - Bind default values and uniform names in `shaderRenderEffect`.
   - Add `StudioSlider` controls.
3. Add compile assertion to `ShaderEngineTests.kt` (`testAllPresetsCompileOnDesktopSkia`).

---

## 3. Recipe: Adding a Dual-Input Transition

### For AGSL / SkSL (Shader pass):
1. Declare transition shader in `TransitionPresets.kt`:
   ```glsl
   uniform shader imageA;
   uniform shader imageB;
   uniform float2 resolution;
   uniform float progress;
   uniform float time;
   ```
2. Remember: GLSL two-argument arctangent is `atan(y, x)` (never `atan2`).
3. Add enum entry to `TransitionPreset`.

### For Pure Canvas (API 21+ fallback):
1. Add enum value to `CanvasTransitionType`.
2. Add branch to `CanvasTransitionLayout.kt`.
3. **Strict Invariant**: Ensure Card B is completely invisible at $t \le 0.001$ via `clipPath`, `clipRect`, or quadratic alpha (`alphaB = (progress * progress)`).
4. Update catalog count assertion in `ShaderEngineTests.kt`.
