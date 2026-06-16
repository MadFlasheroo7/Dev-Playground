# Implementation Plan — Jetpack Compose Internals Visualizer Labs

This plan proposes new educational playgrounds in the **Learning Labs** category focusing on Jetpack Compose internals and performance concepts.

## User Review Required

> [!IMPORTANT]
> Please review the three proposed Compose-specific visualizers and let us know which one(s) you would like to implement:
>
> 1. **Compose Phase Pipeline (`Route.COMPOSE_PHASES`)**:
>    - **Concept**: Visualizes the three phases of Compose (**Composition** -> **Layout** -> **Drawing**).
>    - **Interactive Lab**: Users toggle state reads inside standard parameters vs. lambda modifiers (`Modifier.offset { ... }` or `Modifier.graphicsLayer { ... }`).
>    - **Visualization**: Shows particle paths flowing through the phases. Toggling lambdas will show state updates bypassing Composition and Layout, going straight to Draw, illustrating performance optimization.
>
> 2. **Modifier Constraints Flow (`Route.CONSTRAINT_FLOW`)**:
>    - **Concept**: Demystifies how constraints travel down the layout tree and sizes travel up ("Constraints go down, sizes go up, parent decides position").
>    - **Interactive Lab**: Users chain modifiers (e.g., `fillMaxSize()`, `padding()`, `wrapContentSize()`, `size()`) on a layout.
>    - **Visualization**: Animates constraints bounding boxes (Min/Max Width & Height) passing down node-by-node, showing how each modifier alters the constraint bounds before measuring the child, and showing the resulting size moving back up.
>
> 3. **SubcomposeLayout & Lazy Viewport (`Route.LAZY_LIFECYCLE`)**:
>    - **Concept**: Visualizes lazy pre-fetching, layout composition deferral, and layout recycling.
>    - **Interactive Lab**: A scrollable viewport representing a virtual `LazyColumn`.
>    - **Visualization**: Shows a grid of items moving through states: **Idle (not composed)** -> **Pre-fetched (composed but not placed)** -> **Active (placed & visible)** -> **Disposed (recycled)** as the user scrolls.
>
> Please let us know which topic you'd like to implement or if you have specific preferences!

---

## Proposed Changes

We will register the chosen route and build the selected screen in the KMP `commonMain` directory:

- Modify `Route.kt`
- Modify `Categories.kt`
- Modify `ScreenRegistry.kt`
- Modify `MainActivity.kt`
- Create the selected Screen in `composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/`

---

## Verification Plan

### Automated Verification
```sh
./gradlew :androidApp:assembleDebug
```

### Manual Verification
Deploy and verify that interactions correctly simulate the underlying Compose internal algorithms and state changes.
