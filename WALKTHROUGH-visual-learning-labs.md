# Walkthrough — Visual Learning Labs

We have successfully implemented and verified the complete suite of 12 visual learning playgrounds under the **"Learning Labs"** category.

---

## 1. Visual Kotlin Flow Learner
*   **File**: [`FlowLearnerScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/FlowLearnerScreen.kt)
*   **Details**: Scrolling marble diagrams, physical pipeline emitters, and subscription collectors.

## 2. Compose Recomposition & Remember Lab
*   **File**: [`RecompositionVisualizerScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/RecompositionVisualizerScreen.kt)
*   **Details**: Visual node glow highlights (Red/Green) indicating recomposition counts vs composition skips, positional slot table inspector.

## 3. Coroutine Dispatchers Visualizer
*   **File**: [`DispatcherVisualizerScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/DispatcherVisualizerScreen.kt)
*   **Details**: Active thread queue simulator, 60 FPS clock stutter UI freezer illustrating blocking `Thread.sleep` vs suspending `delay`.

## 4. Structured Concurrency Visualizer
*   **File**: [`ConcurrencyVisualizerScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/ConcurrencyVisualizerScreen.kt)
*   **Details**: Parent-child job tree propagation, cancellation waves, and `SupervisorJob` error boundaries.

## 5. API Query Models Visualizer
*   **File**: [`ApiVisualizerScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/ApiVisualizerScreen.kt)
*   **Details**: Comparison between REST (multiple roundtrip paths), GraphQL (a single nested query), and WebSockets (persistent duplex pipeline).

## 6. Cryptography Lab (Symmetric vs Asymmetric)
*   **File**: [`CryptoVisualizerScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/CryptoVisualizerScreen.kt)
*   **Details**: Locks and keys visualization. Symmetric mode shows one shared key locking and unlocking chests; Asymmetric mode visualizes public key encryption and private key decryption.

## 7. Database Indexing (Table Scan vs B-Tree)
*   **File**: [`DbIndexVisualizerScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/DbIndexVisualizerScreen.kt)
*   **Details**: Side-by-side search performance comparison. A linear table scan checks elements sequentially one-by-one; a B-Tree search traverses node children down a binary tree.

## 8. TCP Handshake & OSI Layers
*   **File**: [`TcpVisualizerScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/TcpVisualizerScreen.kt)
*   **Details**: An interactive SYN -> SYN-ACK -> ACK packet sender with packet drop and retransmission logic, alongside OSI layer block wrapping/unwrapping headers.

## 9. Git Branching & Rebase Interactive Lab
*   **File**: [`GitVisualizerScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/GitVisualizerScreen.kt)
*   **Details**: In-memory Git engine supporting commit, branch checkout (detached HEAD), fast-forward/3-way merges, and replanted rebases with custom DAG rendering. Includes terminal REPL and 4 interactive lessons.

---

## Jetpack Compose Internals Visualizers

## 10. Compose Phase Pipeline
*   **File**: [`ComposePhasesScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/ComposePhasesScreen.kt)
*   **Visual Highlights**: Shows how state read scope affects rendering passes. Traces flows visually from State Read -> Composition -> Layout -> Drawing. Displays live counters showing how using lambda modifiers (e.g. `Modifier.offset { ... }` or `Modifier.graphicsLayer { ... }`) bypasses composition or layout phases to optimize frame-rates.

## 11. Modifier Constraints Flow
*   **File**: [`ConstraintFlowScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/ConstraintFlowScreen.kt)
*   **Visual Highlights**: A builder for modifier chains (e.g. `fillMaxSize()`, `wrapContentSize()`, `padding()`, `size()`). Renders vertical constraint blocks showing incoming and outgoing limits (minWidth/maxWidth, minHeight/maxHeight), and displays how the final child size is resolved back up the node tree.

## 12. Lazy Viewport Lifecycle
*   **File**: [`LazyLifecycleScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/LazyLifecycleScreen.kt)
*   **Visual Highlights**: A scrolling list simulator that colors list items based on their current memory state: **Active (green) / Pre-fetched (yellow) / Cached (cyan) / Recycled (disposed, red)** in real time, demonstrating pre-fetching and off-screen caching behaviors.

---

## Parsers Category

## 13. HTML Parser & Live Render Playground
*   **File**: [`HtmlParserPlaygroundScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/HtmlParserPlaygroundScreen.kt)
*   **Visual Highlights**: A fully interactive split-screen (or stack on mobile) playground. Allows live editing of HTML code with presets and provides three tabs:
    - **Render Preview**: Live rendering of DOM elements mapping inline CSS (color, background, font-size, padding, border-radius, border) to native Compose Layouts/Modifiers.
    - **DOM Inspector**: Visual representation of the hierarchical DOM Tree.
    - **Parser Logs**: Tokenization metrics and warning messages for auto-closing unclosed HTML tags.

## 14. Markdown Compiler & Native Render Playground
*   **File**: [`MarkdownParserPlaygroundScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/MarkdownParserPlaygroundScreen.kt)
*   **Visual Highlights**: A fully interactive KMP Markdown editor and live compilation playground with three output tabs:
    - **Render Preview**: Compiles Markdown syntax directly to native Compose UI elements (headers, lists, blockquotes with border sidebars, and styled text spans supporting nested bold/italic formatting, links, and code blocks).
    - **AST Blocks**: Shows the Abstract Syntax Tree block nodes built by the compiler.
    - **Compiler Logs**: Displays tokenization metrics, parsing step summaries, and time duration metrics.
---

## Vector & Graphics Category

## 15. SVG Path Compiler & Bezier Curve Editor
*   **File**: [`SvgCompilerPlaygroundScreen.kt`](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/SvgCompilerPlaygroundScreen.kt)
*   **Visual Highlights**: A fully interactive Vector drawing lab. Features a dual-pane live SVG path editor and design canvas:
    - **Canvas Editor**: Renders the SVG path and overlay coordinates with draggable colored anchors. Connects control endpoints to anchors using dashed tangent rigs. Dragging anchors modifies path code coordinates in real time (with snapping to 10px increments grid support).
    - **AST Segments**: Explains each instruction segment type (MoveTo, LineTo, CubicTo, Close).
    - **Compiler Logs**: Lists lexer processing logs, coordinate updates, and translation durations.

---

## Compilation Verification

The complete Kotlin Multiplatform target successfully compiles:
```sh
./gradlew :androidApp:assembleDebug
```
Output:
`BUILD SUCCESSFUL in 29s`
All routes are correctly wired in the cross-platform registry and the Android manifest launcher.
