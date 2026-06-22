# Implementation Plan — Markdown Compiler & Rich Native Render Playground

This plan describes the implementation of the **Markdown Compiler & Rich Native Render Playground** (`Route.MARKDOWN_PARSER_PLAYGROUND`) under the **Parsers** category.

## Goals

1. Build a custom, lightweight Markdown Lexer & Parser in `commonMain` that splits text into structural blocks (`MarkdownBlock`) and parses inline formatting recursively.
2. Build a rich native Jetpack Compose rendering engine mapping blocks to native components (`Text`, `Column`, `Row`, `Card`) and styling properties (bold, italic, strikethrough, links, code blocks) using Compose `AnnotatedString` spans.
3. Construct an interactive split-screen playground with preset Markdown templates, real-time render preview, an AST visualizer tree, and compiler execution logs.

---

## User Review Required

> [!IMPORTANT]
> The markdown parser will compile standard Markdown markers directly to native Compose UI elements without relying on HTML WebViews.
>
> **Supported Grammar Rules:**
> 1. **Block Elements**:
>    - Headings: `#` through `######`
>    - Paragraphs: Standard lines separated by double newlines
>    - Blockquotes: Lines prefixed with `> `
>    - Code Blocks: Blocks wrapped in triple backticks ` ``` ` (supporting language labels)
>    - Unordered Lists: Bullet items starting with `- `, `* `, or `+ `
>    - Ordered Lists: Numbered items starting with `1. `, `2. `, etc.
>    - Horizontal Rules: Divider lines containing `---` or `***`
> 2. **Inline Elements** (rendered via Compose `AnnotatedString` with SpanStyles):
>    - Bold: `**text**` or `__text__`
>    - Italic: `*text*` or `_text_`
>    - Strikethrough: `~~text~~`
>    - Inline Code: `` `code` ``
>    - Links: `[text](url)`
>
> Please let me know if there are any specific Markdown syntaxes you would like to add!

---

## Proposed Changes

We will register the new route, update the screen registries, and build the custom tokenizer and UI in `commonMain`.

### Navigation & Registration

#### [MODIFY] [Route.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Route.kt)
Add `MARKDOWN_PARSER_PLAYGROUND` to the `Route` enum.

#### [MODIFY] [Categories.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/Categories.kt)
Register the new route under the **Parsers** category:
```kotlin
    LabCategoryDef(
        "Parsers",
        listOf(
        LabItem("HTML Parser & Live Render", Route.HTML_PARSER_PLAYGROUND),
        LabItem("Markdown Compiler & Native Render", Route.MARKDOWN_PARSER_PLAYGROUND),
        ),
    ),
```

#### [MODIFY] [ScreenRegistry.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/nav/ScreenRegistry.kt)
- Register `Route.MARKDOWN_PARSER_PLAYGROUND` to map to `pro.jayeshseth.devcontrols.ui.MarkdownParserPlaygroundScreen(onBack)`.
- Include it in the `commonRoutes` set.

#### [MODIFY] [MainActivity.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/androidMain/kotlin/pro/jayeshseth/devcontrols/MainActivity.kt)
- Add route mapping under `PlatformScreenHost`:
  ```kotlin
  Route.MARKDOWN_PARSER_PLAYGROUND -> MarkdownParserPlaygroundScreen(onBack = onBack)
  ```

---

### Component UI & Logic

#### [NEW] [MarkdownParserPlaygroundScreen.kt](file:///home/mad_flasher/AndroidStudioProjects/Testdel/composeApp/src/commonMain/kotlin/pro/jayeshseth/devcontrols/ui/MarkdownParserPlaygroundScreen.kt)
This file will contain:
1. **Markdown AST Nodes**:
   ```kotlin
   sealed class MarkdownBlock {
       data class Heading(val level: Int, val content: String) : MarkdownBlock()
       data class Paragraph(val content: String) : MarkdownBlock()
       data class Blockquote(val content: String) : MarkdownBlock()
       data class CodeBlock(val language: String, val content: String) : MarkdownBlock()
       data class UnorderedList(val items: List<String>) : MarkdownBlock()
       data class OrderedList(val items: List<String>) : MarkdownBlock()
       object HorizontalRule : MarkdownBlock()
   }
   ```
2. **Parser Functions**:
   - `fun parseMarkdownBlocks(text: String, logs: MutableList<String>): List<MarkdownBlock>`: Stateful line scanner mapping text paragraphs, lists, rules, and code fences.
   - `fun parseInlineMarkdown(text: String, primaryColor: Color): AnnotatedString`: Recursive span builder applying styling rules for nested bold, italic, code, and link elements.
3. **Compose Widgets Mapping**:
   - `RenderMarkdownBlock(block: MarkdownBlock)`: Renders specific components with premium styling (e.g. styled blockquotes with blue borders, highlighted syntax code cards, dynamic header sizes).
4. **Playground Sandbox UI**:
   - High-fidelity dark mode split screen (stacked on mobile screens).
   - Editable editor with syntax template selectors ("Simple Article", "Format Guide", "Nested Lists").
   - Tab outputs:
     - **Render Preview**: Live interactive rendering of the parsed Markdown layout.
     - **AST Tree Browser**: Visual list structure showing parsed compile blocks.
     - **Compiler Logs**: Log metrics showing tokenizer passes, line analysis, and compile time duration.

---

## Verification Plan

### Automated Verification
Run compile check:
```sh
./gradlew :androidApp:assembleDebug
```

### Manual Verification
1. Open the "Parsers" category on a device/emulator.
2. Launch "Markdown Compiler & Native Render".
3. Toggle presets and inspect rendering correctness.
4. Modify formatting in the editor (e.g. nested lists, bold formatting, links) and verify updates.
5. Inspect the AST Tree view to confirm proper block nesting and logs to check compile execution times.
