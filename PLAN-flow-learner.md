# Implementation Plan — Visual Kotlin Flow Learner

Create a new interactive "Kotlin Flow Visualizer & Learner" screen within the Dev Controls developer playground app to help visual learners master Kotlin coroutine Flows, Channels, and Operators.

## User Review Required

> [!IMPORTANT]
> The Flow Visualizer uses real-time coroutines and Compose custom Canvas animations to draw emissions moving along a timeline pipeline. To make this extremely premium and educational, we will visualize the exact timing behaviors of cold flows, StateFlow, SharedFlow, and core flow operators (Buffer, CollectLatest, Debounce, Zip, and Combine).

Please review the proposed design of the learning sections:
1. **Cold Flows**: Demonstrates independent execution per collector. Starting Collector B later starts a new timeline.
2. **StateFlow**: Demonstrates state-holding behavior. Shows conflation visually when the collector processing is slower than the emission rate.
3. **SharedFlow**: Demonstrates event broadcasting, replay caches, and drops when no subscribers exist.
4. **Operators (Timing Labs)**:
    - **Buffer**: Shows how buffering decouples emitter speed from collector speed.
    - **CollectLatest**: Animates the cancellation of in-progress collections when new items arrive.
    - **Debounce**: Visualizes search query typing and timing-out emissions.
    - **Combine vs Zip**: Animates matching pairs (Zip) vs pairing latest values (Combine) across dual streams.

## Open Questions

> [!NOTE]
> 1. Should we include a custom "Code Snippet View" side-by-side with the animations, showing the exact Kotlin code executing (e.g. `flow { ... }.buffer().collect { ... }`)?
> 2. Would you prefer a preset step-by-step tutorial format, or a free sandbox environment where you can freely adjust sliders for emission rate and collection delay? (We recommend a hybrid: interactive preset scenarios with custom sandbox controls).

---

## Proposed Changes

### Navigation and Routing

We will add a new route to the navigation model to support the Flow Learner screen.

#### [MODIFY] [Route.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Route.kt)
Add a new enum constant `FLOW_LEARNER` to the `Route` enum.

#### [MODIFY] [MainActivity.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/MainActivity.kt)
- Add a navigation button in the appropriate category list (under a new learning or developer category, or as a standalone highlight).
- Add a route handler in the main `setContent` navigation `when` branch to instantiate `FlowLearnerScreen(onBack = { route = Route.MAIN })`.

---

### Core UI & Animation Components

#### [NEW] [FlowLearnerScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/ui/FlowLearnerScreen.kt)
Create the interactive Composable screen. It will include:
- **Header**: Tab selectors for learning modules (Cold Flow, StateFlow, SharedFlow, Operators).
- **Control Bar**:
  - Sliders for Emitter Speed (ms) and Collector Speed (ms).
  - Toggles/Buttons: "Emit", "Add Subscriber", "Clear", "Play/Pause".
- **Visualizer Canvas**:
  - Draws a pipeline track where color-coded emission particles move from the producer (left) to the subscriber(s) (right).
  - Shows active/inactive states of streams.
  - Draws real-time marble diagrams plotting emissions on a scrolling timeline (X-axis = time).
- **Interactive Scenarios**:
  - **Cold Flow Simulator**: Shows two subscriber lines. When Subscriber A connects, bubbles flow down to it. When Subscriber B connects 3 seconds later, it starts its own independent stream from the beginning.
  - **StateFlow Simulator**: Shows a central "State Card" displaying the current value. Shows Subscriber A getting the value immediately, and shows incoming values merging/disappearing (conflation) if Subscriber A's processing is slow.
  - **SharedFlow Simulator**: Shows broadcast lines. Shows items lost if no subscribers are active. Displays replay cache box holding the last $N$ items.
  - **Timing Operators**: Visualizes `buffer()`, `collectLatest()`, `debounce()`, and `zip()`/`combine()`.

---

## Verification Plan

### Automated Tests
- Build verification:
  ```sh
  ./gradlew :composeApp:assembleDebug
  ```

### Manual Verification
1. Run the application on your Android device:
   ```sh
   ./gradlew installDebug
   ```
2. Navigate to the new **Flow Learner** screen.
3. Verify that tapping on different tabs changes the visualization.
4. Interact with the sandbox: add multiple subscribers, adjust emitter and collector speed sliders, and verify that the bubble animations speed up or slow down accordingly.
5. Verify that `conflation` and `collectLatest` cancel/drop animations behave correctly when the emitter is faster than the collector.
