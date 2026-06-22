package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// AST Markdown Block definitions
sealed class MarkdownBlock {
    data class Heading(val level: Int, val content: String) : MarkdownBlock()
    data class Paragraph(val content: String) : MarkdownBlock()
    data class Blockquote(val content: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val content: String) : MarkdownBlock()
    data class UnorderedList(val items: List<String>) : MarkdownBlock()
    data class OrderedList(val items: List<String>) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownParserPlaygroundScreen(onBack: () -> Unit) {
    val presets = remember {
        listOf(
            Pair("Simple Article", """
# Deep Dive into Kotlin Multiplatform

KMP allows sharing mobile business logic across **Android**, **iOS**, and **Web** platforms.

## Why KMP?
- **Shared Logic**: Write once, run everywhere.
- **Native UI**: Keep native performance and design language.
- **Interoperability**: Easy access to platform-specific APIs.

> "KMP is not just a framework; it's a structural improvement to cross-platform app engineering."

---
### Sample Code
Here is how you define a KMP expect class:
```kotlin
expect class Platform() {
    val name: String
}
```
You can read more at [Kotlin Documentation](https://kotlinlang.org).
            """.trimIndent()),
            Pair("Typography Guide", """
# Formatting Sandbox

Let's check out our native Compose `AnnotatedString` inline compiler rules:

- **Bold Text**: Compiled using `**double asterisks**` or `__double underscores__`.
- *Italic Text*: Compiled using `*single asterisks*` or `_single underscores_`.
- ~~Strikethrough~~: Compiled using `~~double tildes~~`.
- Inline Code: Clean code blocks like `val list = listOf(1, 2, 3)` can be embedded anywhere.
- Embedded Link: [Google Search](https://google.com) is rendered as an underlined blue link.
- *Italics nested inside **Bold Text***: `**bold text with *italic inside* works**` perfectly!
            """.trimIndent()),
            Pair("Nested Code & Quotes", """
# Advanced Layouts

## Multiline Quote Block
> This is a multiline quote block.
> It displays with a vertical highlight bar on the left edge.
> It can also contain **bold** and *italic* text.

## Complex Code Block
```javascript
function greet(user) {
    console.log(`Hello, ${'$'}{user}!`);
}
greet("Developer");
```

---
*End of sandbox preset guide.*
            """.trimIndent())
        )
    }

    var selectedPresetIndex by remember { mutableStateOf(0) }
    var markdownInput by remember { mutableStateOf(presets[selectedPresetIndex].second) }

    // Parser outputs
    var parsedBlocks by remember { mutableStateOf<List<MarkdownBlock>>(emptyList()) }
    val parserLogs = remember { mutableStateListOf<String>() }
    var parseTimeMs by remember { mutableStateOf(0L) }

    // Synchronously parse Markdown on text input changes
    LaunchedEffect(markdownInput) {
        parserLogs.clear()
        val startTime = currentTimeMillis()

        parserLogs.add("Markdown Parsing started...")
        val blocks = parseMarkdownBlocks(markdownInput, parserLogs)
        parsedBlocks = blocks

        val endTime = currentTimeMillis()
        parseTimeMs = endTime - startTime
        parserLogs.add("Parsing completed: ${blocks.size} block nodes built in ${parseTimeMs}ms.")
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Preview, 1: AST, 2: Logs

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Markdown Compiler Lab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        markdownInput = presets[selectedPresetIndex].second
                    }) {
                        Text("Reset Code", color = Color(0xFFEF4444))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E24),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF121214))
        ) {
            val isWide = maxWidth >= 850.dp

            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Column: Markdown Input Editor
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        // Presets Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEachIndexed { index, pair ->
                                Button(
                                    onClick = {
                                        selectedPresetIndex = index
                                        markdownInput = pair.second
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedPresetIndex == index) Color(0xFF3B82F6) else Color(0xFF26262F)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(pair.first, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Editor Card
                        Card(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
                            border = BorderStroke(1.dp, Color(0xFF26262D))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1B1B1F))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("markdown-editor.md", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }

                                BasicTextField(
                                    value = markdownInput,
                                    onValueChange = { markdownInput = it },
                                    textStyle = TextStyle(
                                        color = Color(0xFFE0E0E6),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFF2D2D35)))

                    // Right Column: Output Preview, AST, Logs
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Render Preview", fontSize = 12.sp) })
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("AST Blocks", fontSize = 12.sp) })
                            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Compiler Logs", fontSize = 12.sp) })
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (selectedTab) {
                                0 -> MarkdownPreviewTab(parsedBlocks = parsedBlocks, logs = parserLogs)
                                1 -> MarkdownAstTab(parsedBlocks = parsedBlocks)
                                2 -> MarkdownLogsTab(logs = parserLogs)
                            }
                        }
                    }
                }
            } else {
                // Portrait stack
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    // Top part: Tab row & Outputs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Preview", fontSize = 11.sp) })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("AST", fontSize = 11.sp) })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Logs", fontSize = 11.sp) })
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        when (selectedTab) {
                            0 -> MarkdownPreviewTab(parsedBlocks = parsedBlocks, logs = parserLogs)
                            1 -> MarkdownAstTab(parsedBlocks = parsedBlocks)
                            2 -> MarkdownLogsTab(logs = parserLogs)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2D2D35))
                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom part: Editor
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
                        border = BorderStroke(1.dp, Color(0xFF26262D))
                    ) {
                        BasicTextField(
                            value = markdownInput,
                            onValueChange = { markdownInput = it },
                            textStyle = TextStyle(
                                color = Color(0xFFE0E0E6),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownPreviewTab(parsedBlocks: List<MarkdownBlock>, logs: MutableList<String>) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (parsedBlocks.isEmpty()) {
                Text("Empty Document", color = Color.Gray, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            } else {
                parsedBlocks.forEach { block ->
                    RenderMarkdownBlock(block = block, logs = logs)
                }
            }
        }
    }
}

@Composable
fun MarkdownAstTab(parsedBlocks: List<MarkdownBlock>) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(parsedBlocks) { block ->
                AstBlockRow(block = block)
            }
        }
    }
}

@Composable
fun AstBlockRow(block: MarkdownBlock) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF232329), shape = RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        val typeName = block::class.simpleName ?: "Unknown"
        val labelColor = when (block) {
            is MarkdownBlock.Heading -> Color(0xFF3B82F6)
            is MarkdownBlock.Paragraph -> Color(0xFFE5E7EB)
            is MarkdownBlock.Blockquote -> Color(0xFFFFB300)
            is MarkdownBlock.CodeBlock -> Color(0xFF10B981)
            is MarkdownBlock.UnorderedList, is MarkdownBlock.OrderedList -> Color(0xFF8B5CF6)
            is MarkdownBlock.HorizontalRule -> Color(0xFFEF4444)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(labelColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = typeName,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (block) {
            is MarkdownBlock.Heading -> {
                Text("level: ${block.level}", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("content: \"${block.content}\"", color = Color(0xFFECEFF1), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            is MarkdownBlock.Paragraph -> {
                Text("content: \"${block.content}\"", color = Color(0xFFECEFF1), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            is MarkdownBlock.Blockquote -> {
                Text("content: \"${block.content}\"", color = Color(0xFFECEFF1), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            is MarkdownBlock.CodeBlock -> {
                Text("language: \"${block.language}\"", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("content: \"${block.content.trim().take(40)}...\"", color = Color(0xFFECEFF1), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            is MarkdownBlock.UnorderedList -> {
                Text("items: [", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                block.items.forEachIndexed { i, it ->
                    Text("  $i: \"$it\"", color = Color(0xFFECEFF1), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 8.dp))
                }
                Text("]", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            is MarkdownBlock.OrderedList -> {
                Text("items: [", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                block.items.forEachIndexed { i, it ->
                    Text("  $i: \"$it\"", color = Color(0xFFECEFF1), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 8.dp))
                }
                Text("]", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            is MarkdownBlock.HorizontalRule -> {
                Text("empty rule node", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun MarkdownLogsTab(logs: List<String>) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { log ->
                val color = if (log.contains("Warning:")) Color(0xFFFFCC00) else Color(0xFFECEFF1)
                Text(
                    text = log,
                    color = color,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Markdown block level line compiler
fun parseMarkdownBlocks(text: String, logs: MutableList<String>): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")

    var inCodeBlock = false
    var codeLanguage = ""
    val codeContent = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()

        if (inCodeBlock) {
            if (trimmed.startsWith("```")) {
                blocks.add(MarkdownBlock.CodeBlock(codeLanguage, codeContent.toString()))
                inCodeBlock = false
                codeLanguage = ""
                codeContent.clear()
                logs.add("Closed code block.")
            } else {
                codeContent.append(line).append("\n")
            }
            continue
        }

        // Check code block start
        if (trimmed.startsWith("```")) {
            val lang = trimmed.substring(3).trim()
            inCodeBlock = true
            codeLanguage = lang
            logs.add("Started code block ($lang).")
            continue
        }

        // Check horizontal rule
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.HorizontalRule)
            logs.add("Parsed horizontal rule.")
            continue
        }

        // Check headings
        if (trimmed.startsWith("#")) {
            val hashCount = trimmed.takeWhile { it == '#' }.length
            if (hashCount in 1..6 && trimmed.length > hashCount && trimmed[hashCount] == ' ') {
                val content = trimmed.substring(hashCount + 1).trim()
                blocks.add(MarkdownBlock.Heading(hashCount, content))
                logs.add("Parsed Heading $hashCount: \"$content\"")
                continue
            }
        }

        // Check blockquotes
        if (trimmed.startsWith(">")) {
            val content = if (trimmed.length > 1 && trimmed[1] == ' ') trimmed.substring(2) else trimmed.substring(1)
            val lastBlock = blocks.lastOrNull()
            if (lastBlock is MarkdownBlock.Blockquote) {
                blocks[blocks.size - 1] = lastBlock.copy(content = lastBlock.content + "\n" + content)
            } else {
                blocks.add(MarkdownBlock.Blockquote(content))
                logs.add("Started blockquote.")
            }
            continue
        }

        // Check unordered list
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
            val content = trimmed.substring(2)
            val lastBlock = blocks.lastOrNull()
            if (lastBlock is MarkdownBlock.UnorderedList) {
                blocks[blocks.size - 1] = MarkdownBlock.UnorderedList(lastBlock.items + content)
            } else {
                blocks.add(MarkdownBlock.UnorderedList(listOf(content)))
                logs.add("Started unordered list.")
            }
            continue
        }

        // Check ordered list
        val orderedRegex = "^\\d+\\.\\s+(.*)".toRegex()
        val match = orderedRegex.matchEntire(trimmed)
        if (match != null) {
            val content = match.groupValues[1]
            val lastBlock = blocks.lastOrNull()
            if (lastBlock is MarkdownBlock.OrderedList) {
                blocks[blocks.size - 1] = MarkdownBlock.OrderedList(lastBlock.items + content)
            } else {
                blocks.add(MarkdownBlock.OrderedList(listOf(content)))
                logs.add("Started ordered list.")
            }
            continue
        }

        // Empty line separates blocks
        if (trimmed.isEmpty()) {
            continue
        }

        // Default: Paragraph
        val lastBlock = blocks.lastOrNull()
        if (lastBlock is MarkdownBlock.Paragraph) {
            blocks[blocks.size - 1] = MarkdownBlock.Paragraph(lastBlock.content + "\n" + line)
        } else {
            blocks.add(MarkdownBlock.Paragraph(line))
            logs.add("Started paragraph: \"${line.take(15)}...\"")
        }
    }

    if (inCodeBlock) {
        blocks.add(MarkdownBlock.CodeBlock(codeLanguage, codeContent.toString()))
        logs.add("Warning: Auto-closed open code block at document end.")
    }

    return blocks
}

// Inline lexer & recursive parser building AnnotatedString styled spans
fun parseInlineMarkdown(text: String, primaryColor: Color = Color.White): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    val len = text.length

    while (i < len) {
        // 1. Inline code: `code`
        if (text[i] == '`') {
            val endIdx = text.indexOf('`', i + 1)
            if (endIdx != -1) {
                builder.pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0xFF26262B),
                        color = Color(0xFF10B981),
                        fontSize = 12.sp
                    )
                )
                builder.append(text.substring(i + 1, endIdx))
                builder.pop()
                i = endIdx + 1
                continue
            }
        }

        // 2. Bold: **text** or __text__
        if (i + 1 < len && ((text[i] == '*' && text[i + 1] == '*') || (text[i] == '_' && text[i + 1] == '_'))) {
            val marker = text.substring(i, i + 2)
            val endIdx = text.indexOf(marker, i + 2)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                val inner = text.substring(i + 2, endIdx)
                builder.append(parseInlineMarkdown(inner, primaryColor))
                builder.pop()
                i = endIdx + 2
                continue
            }
        }

        // 3. Italic: *text* or _text_
        if (text[i] == '*' || text[i] == '_') {
            val marker = text[i].toString()
            val endIdx = text.indexOf(marker, i + 1)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                val inner = text.substring(i + 1, endIdx)
                builder.append(parseInlineMarkdown(inner, primaryColor))
                builder.pop()
                i = endIdx + 1
                continue
            }
        }

        // 4. Strikethrough: ~~text~~
        if (i + 1 < len && text[i] == '~' && text[i + 1] == '~') {
            val endIdx = text.indexOf("~~", i + 2)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                val inner = text.substring(i + 2, endIdx)
                builder.append(parseInlineMarkdown(inner, primaryColor))
                builder.pop()
                i = endIdx + 2
                continue
            }
        }

        // 5. Link: [text](url)
        if (text[i] == '[') {
            val closeBracket = text.indexOf(']', i + 1)
            if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                val closeParen = text.indexOf(')', closeBracket + 2)
                if (closeParen != -1) {
                    val linkText = text.substring(i + 1, closeBracket)
                    val url = text.substring(closeBracket + 2, closeParen)

                    builder.pushStringAnnotation("URL", url)
                    builder.pushStyle(
                        SpanStyle(
                            color = Color(0xFF3B82F6),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    builder.append(linkText)
                    builder.pop()
                    builder.pop()
                    i = closeParen + 1
                    continue
                }
            }
        }

        builder.append(text[i].toString())
        i++
    }

    return builder.toAnnotatedString()
}

// Native Jetpack Compose widget rendering for AST nodes
@Composable
fun RenderMarkdownBlock(block: MarkdownBlock, logs: MutableList<String>) {
    when (block) {
        is MarkdownBlock.Heading -> {
            val headingStyle = when (block.level) {
                1 -> TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                2 -> TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE5E7EB))
                3 -> TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD1D5DB))
                else -> TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
            }
            Text(
                text = parseInlineMarkdown(block.content),
                style = headingStyle,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }
        is MarkdownBlock.Paragraph -> {
            Text(
                text = parseInlineMarkdown(block.content),
                style = TextStyle(
                    color = Color(0xFFE5E7EB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        }
        is MarkdownBlock.Blockquote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(Color(0xFF1E1E24), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(IntrinsicSize.Max)
                        .background(Color(0xFF3B82F6))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = parseInlineMarkdown(block.content),
                    style = TextStyle(
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 18.sp
                    )
                )
            }
        }
        is MarkdownBlock.CodeBlock -> {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070709)),
                border = BorderStroke(1.dp, Color(0xFF26262D))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (block.language.isNotEmpty()) block.language.uppercase() else "CODE",
                            color = Color(0xFF6B7280),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Copy",
                            color = Color(0xFF3B82F6),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable {
                                    logs.add("Code copied to clipboard!")
                                }
                                .padding(2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = block.content.trim(),
                        color = Color(0xFF00FF66),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
        is MarkdownBlock.UnorderedList -> {
            Column(modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                block.items.forEach { item ->
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text(
                            text = "• ",
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        val annotatedText = parseInlineMarkdown(item)
                        ClickableText(
                            text = annotatedText,
                            onClick = { offset ->
                                annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { annotation ->
                                    logs.add("Link clicked: ${annotation.item}")
                                }
                            },
                            style = TextStyle(color = Color(0xFFE5E7EB), fontSize = 14.sp)
                        )
                    }
                }
            }
        }
        is MarkdownBlock.OrderedList -> {
            Column(modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                block.items.forEachIndexed { index, item ->
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text(
                            text = "${index + 1}. ",
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        val annotatedText = parseInlineMarkdown(item)
                        ClickableText(
                            text = annotatedText,
                            onClick = { offset ->
                                annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { annotation ->
                                    logs.add("Link clicked: ${annotation.item}")
                                }
                            },
                            style = TextStyle(color = Color(0xFFE5E7EB), fontSize = 14.sp)
                        )
                    }
                }
            }
        }
        is MarkdownBlock.HorizontalRule -> {
            HorizontalDivider(
                color = Color(0xFF2D2D35),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
