# Implementation Plan — Git Branching & Rebase Interactive Lab

This plan describes the implementation of the **Git Branching & Rebase Interactive Lab** (`Route.GIT_VISUALIZER`). This is an interactive sandbox and educational tool for learning Git's commit graph, branches, merging, and rebasing.

## User Review Required

> [!IMPORTANT]
> The screen will be a **fully interactive Git Sandbox and Command Console (REPL)**. It will not be a static animation.
> The user will be able to type commands (e.g., `git commit`, `git merge`, `git rebase`) or tap quick action buttons to manipulate the Git commit graph.
>
> We will implement an in-memory Git graph state machine in pure Kotlin, supporting:
> 1. **`git commit`**: Adds a node.
> 2. **`git branch <name>`**: Creates a new pointer.
> 3. **`git checkout <name>`**: Shifts the `HEAD` reference (supporting detached HEAD).
> 4. **`git merge <name>`**: Computes Fast-Forward vs 3-Way Merge (with two parent pointers).
> 5. **`git rebase <name>`**: Resolves LCA (Lowest Common Ancestor), detaches branch commits, and replants them onto the target branch tip.
>
> Please confirm if this interactive design aligns with your expectations!

---

## Proposed Changes

We will modify the navigation infrastructure to add `Route.GIT_VISUALIZER`, and implement the screen in `commonMain` so it's fully cross-platform.

### Navigation

#### [MODIFY] [Route.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Route.kt)
Add `GIT_VISUALIZER` to the `Route` enum.

#### [MODIFY] [Categories.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Categories.kt)
Add `LabItem("Git Branch & Rebase", Route.GIT_VISUALIZER)` under the "Learning Labs" category list.

#### [MODIFY] [ScreenRegistry.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/ScreenRegistry.kt)
- Add `Route.GIT_VISUALIZER` to the `when (route)` branch in `CommonScreen()`.
- Add `Route.GIT_VISUALIZER` to the `commonRoutes` set.

#### [MODIFY] [MainActivity.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/MainActivity.kt)
Add `Route.GIT_VISUALIZER -> GitVisualizerScreen(onBack = onBack)` branch to `MainActivity`'s route matching.

---

### Component UI

#### [NEW] [GitVisualizerScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/GitVisualizerScreen.kt)
We will build the screen with:
1. **Interactive Console/REPL Pane**:
   - A command-line input text field.
   - Command history log.
   - Shortcut buttons: `git commit`, `git checkout main`, `git merge feature`, `git rebase main`, `git branch feature`.
2. **Git Graph Canvas**:
   - Custom drawing of commit circles. Each circle shows its ID (e.g. `c0`, `c1`, `c2`).
   - Branch labels (e.g., `main`, `feature`) positioned next to their target commit nodes.
   - `HEAD` label/pointer indicating current location.
   - Connectors with arrows pointing from child commits back to parent commits.
3. **Tutorial Mode Pane**:
   - Offers guided exercises (e.g., "Fast-Forward Merge", "3-Way Merge conflict resolution", "Linearizing History via Rebase").
   - Shows instructions and checks if the current Git state meets the objective.

---

## Verification Plan

### Automated Verification
Run KMP compilation of the androidApp package:
```sh
./gradlew :androidApp:assembleDebug
```

### Manual Verification
1. Launch the app and select "Learning Labs" -> "Git Branch & Rebase".
2. Type commands in the console (e.g., `git commit`, `git branch dev`, `git checkout dev`, `git commit`, `git checkout main`, `git merge dev`) and verify that the graph updates in real time.
3. Test rebase and verify that nodes are replanted properly with new commit hashes.
4. Try the interactive tutorial exercises.
