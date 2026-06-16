# Implementation Plan — HTML Parser & Live Compose Rendering Lab

This plan describes the implementation of a new category, **Parsers**, starting with the **HTML Parser & Live Render Playground** (`Route.HTML_PARSER_PLAYGROUND`).

## User Review Required

> [!IMPORTANT]
> The playground will support live editing of HTML text, real-time parsing into an in-memory Document Object Model (DOM) tree, and rendering natively in Jetpack Compose.
> 
> Key features of the proposed parser & renderer:
> 1. **DOM Parser Engine**: A custom, lightweight parser in `commonMain` that tokenizes tags, attributes, text nodes, and auto-corrects unclosed tags.
> 2. **Compose Renderer**: Recursively maps DOM nodes to Compose Composables:
>    - Layout: `<div>`, `<section>` -> `Column` or `Row` layouts.
>    - Typography: `<h1>`, `<h2>`, `<p>`, `<span>`, `<b>`, `<i>`, `<a>` -> `Text` with corresponding font weights, styles, and links.
>    - Styles: Parses basic inline CSS `style` attributes (e.g. `color`, `background`, `font-size`, `padding`, `border-radius`, `border`) and maps them to Compose `Modifier`s and `TextStyle` properties.
> 3. **UI Layout**: Split editor (HTML editor with template presets) and tabbed outputs (Live Render Preview, Interactive DOM Tree Tree Browser, Parser Log).
> 
> Please review and confirm if this scope aligns with your goals!

---

## Proposed Changes

We will register the new category **Parsers** and its route, then implement the custom parser engine and interactive screen in `commonMain`.

### Navigation & Registration

#### [NEW] [Route.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Route.kt)
Add `HTML_PARSER_PLAYGROUND` to the `Route` enum.

#### [NEW] [Categories.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Categories.kt)
Create a new category `Parsers` and register `HTML_PARSER_PLAYGROUND`:
```kotlin
    LabCategoryDef(
        "Parsers",
        listOf(
        LabItem("HTML Parser & Live Render", Route.HTML_PARSER_PLAYGROUND),
        ),
    ),
```

#### [MODIFY] [ScreenRegistry.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/ScreenRegistry.kt)
- Register `Route.HTML_PARSER_PLAYGROUND` in `CommonScreen()`.
- Add `Route.HTML_PARSER_PLAYGROUND` to `commonRoutes` set.

#### [MODIFY] [MainActivity.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/MainActivity.kt)
- Register `Route.HTML_PARSER_PLAYGROUND -> HtmlParserPlaygroundScreen(onBack = onBack)` in the `when (route)` statement.

---

### Component UI & Logic

#### [NEW] [HtmlParserPlaygroundScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/HtmlParserPlaygroundScreen.kt)
This file will contain:
1. **DOM Tree Data Structures**:
   ```kotlin
   sealed class HtmlNode {
       data class Element(
           val tagName: String,
           val attributes: Map<String, String>,
           val children: List<HtmlNode>
       ) : HtmlNode()
       
       data class Text(val content: String) : HtmlNode()
   }
   ```
2. **Parser Engine**:
   - String Scanner/Tokenizer separating tags (`<...>`, `</...>`, `<... />`) from text.
   - Stack-based parser building the DOM tree. If a tag is unclosed (e.g. `<b>text`), the parser automatically closes it when hitting parents' boundaries to prevent node crashes.
3. **Compose Renderer**:
   - A Composable `RenderHtmlNode(node: HtmlNode)` recursively rendering children.
   - Inline style parser mapping CSS string expressions to Compose styling tokens.
4. **Interactive Sandbox Layout**:
   - Left Pane: Scrollable HTML editor. Includes preset buttons like "Simple Profile Card", "Custom Button", "Nested Columns".
   - Right Pane: Tabs for:
     - **Preview**: Live interactive rendering of the parsed HTML.
     - **DOM Inspector**: Hierarchical list view of the parsed nodes (e.g. `Element (div, style="...")`).
     - **Log**: Output log showing tokenization steps, execution time, and any tag repair warnings.

---

## Verification Plan

### Automated Verification
```sh
./gradlew :androidApp:assembleDebug
```

### Manual Verification
1. Launch app, navigate to "Parsers" -> "HTML Parser & Live Render".
2. Select presets and verify they parse and render correctly.
3. Edit inline CSS (e.g. change color from `red` to `blue` or increase padding) and verify preview updates in real-time.
4. Verify the DOM tree structure displays the correct tag nested hierarchy.
