# Implementation Plan — SVG Path Compiler & Bezier Curve Editor

This plan describes the implementation of the new category, **Vector & Graphics**, starting with the **SVG Path Compiler & Bezier Curve Editor** (`Route.SVG_COMPILER_PLAYGROUND`).

## Goals

1. Implement an SVG Path parser/lexer in `commonMain` that parses SVG path `d` strings containing commands: `M/m` (Move to), `L/l` (Line to), `H/h` (Horizontal line to), `V/v` (Vertical line to), `C/c` (Cubic Bezier), `Q/q` (Quadratic Bezier), and `Z/z` (Close path).
2. Build a coordinate translator that converts relative/absolute offsets into absolute Cartesian positions for drag-handles.
3. Build a bidirectional synchronization loop:
   - When text changes in the editor, compile it to AST and render path and handles.
   - When handles are dragged, update coordinate values, serialize back to SVG string, and update the editor text.
4. Construct an interactive design canvas showing the compiled path vector alongside draggable control handle circles, connected by dashed control lines.

---

## User Review Required

> [!IMPORTANT]
> The Bezier curve editor will feature interactive drag anchors.
>
> **Interactive Features:**
> 1. **Dashed Control Indicators**: Connecting lines showing the relationship between anchor endpoints and control handle points.
> 2. **Bidirectional Coding**: Code edits compile to UI handles, and dragging handles serializes directly back into SVG text in real time.
> 3. **Grid Snapping**: Optional feature to snap handle points to a custom background grid.
>
> Please confirm if this matches the design expectations!

---

## Proposed Changes

We will register the new category and route, then implement the custom compiler engine and screen in `commonMain`.

### Navigation & Registration

#### [MODIFY] [Route.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Route.kt)
Add `SVG_COMPILER_PLAYGROUND` to the `Route` enum.

#### [MODIFY] [Categories.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Categories.kt)
Create a new category `Vector & Graphics` and register `SVG_COMPILER_PLAYGROUND`:
```kotlin
    LabCategoryDef(
        "Vector & Graphics",
        listOf(
        LabItem("SVG Path Compiler & Editor", Route.SVG_COMPILER_PLAYGROUND),
        ),
    ),
```

#### [MODIFY] [ScreenRegistry.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/ScreenRegistry.kt)
- Register `Route.SVG_COMPILER_PLAYGROUND` in `CommonScreen()`.
- Add `Route.SVG_COMPILER_PLAYGROUND` to the `commonRoutes` set.

#### [MODIFY] [MainActivity.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/MainActivity.kt)
- Register `Route.SVG_COMPILER_PLAYGROUND -> SvgCompilerPlaygroundScreen(onBack = onBack)` in the `PlatformScreenHost`.

---

### Component UI & Logic

#### [NEW] [SvgCompilerPlaygroundScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/SvgCompilerPlaygroundScreen.kt)
This file will contain:
1. **Path Segment AST Structure**:
   ```kotlin
   sealed class PathSegment {
       data class MoveTo(val x: Float, val y: Float, val isRelative: Boolean = false) : PathSegment()
       data class LineTo(val x: Float, val y: Float, val isRelative: Boolean = false) : PathSegment()
       data class HorizontalLineTo(val x: Float, val isRelative: Boolean = false) : PathSegment()
       data class VerticalLineTo(val y: Float, val isRelative: Boolean = false) : PathSegment()
       data class CubicTo(
           val x1: Float, val y1: Float,
           val x2: Float, val y2: Float,
           val x: Float, val y: Float,
           val isRelative: Boolean = false
       ) : PathSegment()
       data class QuadraticTo(
           val x1: Float, val y1: Float,
           val x: Float, val y: Float,
           val isRelative: Boolean = false
       ) : PathSegment()
       object Close : PathSegment()
   }
   ```
2. **SVG Compiler Engine**:
   - `parseSvgPath(d: String)`: Regex/Char scanner reading numbers and formatting tokens.
   - `serializeSvgPath(segments: List<PathSegment>)`: Serializes segments to a standard `d` path string.
3. **Interactive Design Canvas**:
   - Renders a styled vector preview inside a coordinate space box.
   - Computes absolute Cartesian locations for all end and control points.
   - Draws draggable circles for each handle, connecting them with control lines (e.g. dotted lines for cubic handle offsets).
   - Responds to pointer drag gestures to update coordinate properties dynamically.

---

## Verification Plan

### Automated Verification
```sh
./gradlew :androidApp:assembleDebug
```

### Manual Verification
1. Launch app, navigate to "Vector & Graphics" -> "SVG Path Compiler & Editor".
2. Test preset paths (e.g., Simple Heart, Wave Curve, Custom Shape).
3. Drag any handle point on the design canvas and verify the text code editor updates coordinates in real time.
4. Manually type coordinates or change commands (e.g., swapping `L` for `C`) and verify the path shape updates visually.
