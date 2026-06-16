package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// DOM Node structures
sealed class HtmlNode {
    data class Element(
        val tagName: String,
        val attributes: Map<String, String>,
        val children: MutableList<HtmlNode>
    ) : HtmlNode()
    
    data class Text(val content: String) : HtmlNode()
}

// Token definition for tokenizer
data class HtmlToken(
    val type: TokenType,
    val text: String,
    val tagName: String = "",
    val attributes: Map<String, String> = emptyMap()
) {
    enum class TokenType { TAG_START, TAG_END, TAG_SELF_CLOSING, TEXT }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlParserPlaygroundScreen(onBack: () -> Unit) {
    val presets = remember {
        listOf(
            Pair("Profile Card", """
<div style="background: #1e1e24; padding: 20px; border-radius: 12px; border: 2px solid #3b82f6; width: 300px;">
  <h2 style="color: #3b82f6; font-size: 20px; font-weight: bold;">Jane Doe</h2>
  <p style="color: #9ca3af; font-size: 13px;">Senior Android Architect</p>
  <hr />
  <p style="color: #eceff1; font-size: 12px;">Building Kotlin Multiplatform rendering engines inside native Jetpack Compose.</p>
  <div style="display: flex; flex-direction: row; padding: 4px;">
    <span style="color: #10b981; font-weight: bold; padding: 4px;">KMP</span>
    <span style="color: #8b5cf6; font-weight: bold; padding: 4px;">Compose</span>
  </div>
</div>
            """.trimIndent()),
            Pair("Flex Row Container", """
<div style="display: flex; flex-direction: row; background: #0f0f12; padding: 16px; border-radius: 8px; border: 1px solid #2d2d35;">
  <div style="background: #ef4444; padding: 8px; border-radius: 4px; margin: 4px;">
    <b style="color: white;">Red</b>
  </div>
  <div style="background: #3b82f6; padding: 8px; border-radius: 4px; margin: 4px;">
    <b style="color: white;">Blue</b>
  </div>
  <div style="background: #10b981; padding: 8px; border-radius: 4px; margin: 4px;">
    <b style="color: white;">Green</b>
  </div>
</div>
            """.trimIndent()),
            Pair("Nested Typography", """
<div style="padding: 16px; background: #18181c; border-radius: 10px; border: 1px solid #2d2d35;">
  <h1 style="color: #00e5ff;">Main Title Header 1</h1>
  <h2 style="color: #ff007f;">Subtitle Header 2</h2>
  <p style="font-size: 13px;">This is a paragraph containing <b>bold text</b>, <i>italicized text</i>, and some <code>monospace code</code> blocks.</p>
  <br />
  <p style="color: #a0aec0; font-size: 11px;">Note: unclosed tags will automatically close at node bounds.</p>
</div>
            """.trimIndent())
        )
    }

    var selectedPresetIndex by remember { mutableStateOf(0) }
    var htmlInput by remember { mutableStateOf(presets[selectedPresetIndex].second) }
    
    // Parser outputs
    var parsedNodes by remember { mutableStateOf<List<HtmlNode>>(emptyList()) }
    val parserLogs = remember { mutableStateListOf<String>() }
    var parseTimeMs by remember { mutableStateOf(0L) }
    
    // Synchronously parse DOM on text input changes
    LaunchedEffect(htmlInput) {
        parserLogs.clear()
        val startTime = currentTimeMillis()
        
        parserLogs.add("Parsing started...")
        val tokens = tokenize(htmlInput, parserLogs)
        parserLogs.add("Tokenized: ${tokens.size} tokens found.")
        
        val nodes = parseDom(tokens, parserLogs)
        parsedNodes = nodes
        
        val endTime = currentTimeMillis()
        parseTimeMs = endTime - startTime
        parserLogs.add("Parsing completed in ${parseTimeMs}ms.")
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Preview, 1: DOM Tree, 2: Logs

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HTML Parser Playground", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        htmlInput = presets[selectedPresetIndex].second
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
                    // Left Column: HTML Input Editor
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
                                        htmlInput = pair.second
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
                                    Text("html-editor.html", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                                
                                BasicTextField(
                                    value = htmlInput,
                                    onValueChange = { htmlInput = it },
                                    textStyle = TextStyle(
                                        color = Color(0xFF00FF66),
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
                    
                    // Right Column: Output Preview and DOM inspection
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
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DOM Inspector", fontSize = 12.sp) })
                            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Parser Logs", fontSize = 12.sp) })
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (selectedTab) {
                                0 -> PreviewTab(parsedNodes = parsedNodes)
                                1 -> DomInspectorTab(parsedNodes = parsedNodes)
                                2 -> LogsTab(logs = parserLogs)
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
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DOM", fontSize = 11.sp) })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Logs", fontSize = 11.sp) })
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        when (selectedTab) {
                            0 -> PreviewTab(parsedNodes = parsedNodes)
                            1 -> DomInspectorTab(parsedNodes = parsedNodes)
                            2 -> LogsTab(logs = parserLogs)
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
                            value = htmlInput,
                            onValueChange = { htmlInput = it },
                            textStyle = TextStyle(
                                color = Color(0xFF00FF66),
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
fun PreviewTab(parsedNodes: List<HtmlNode>) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (parsedNodes.isEmpty()) {
                Text("Empty Document", color = Color.Gray, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            } else {
                parsedNodes.forEach { node ->
                    RenderHtmlNode(node = node)
                }
            }
        }
    }
}

@Composable
fun DomInspectorTab(parsedNodes: List<HtmlNode>) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(parsedNodes) { node ->
                DomNodeRow(node = node, depth = 0)
            }
        }
    }
}

@Composable
fun DomNodeRow(node: HtmlNode, depth: Int) {
    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        when (node) {
            is HtmlNode.Text -> {
                Text(
                    text = "TextNode(\"${node.content.trim().replace("\n", "\\n")}\")",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            is HtmlNode.Element -> {
                val attrsStr = if (node.attributes.isNotEmpty()) {
                    node.attributes.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }
                } else ""
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "<${node.tagName}",
                        color = Color(0xFFE5E7EB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (attrsStr.isNotEmpty()) {
                        Text(
                            text = " $attrsStr",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = ">",
                        color = Color(0xFFE5E7EB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                // Recursively display children
                node.children.forEach { child ->
                    DomNodeRow(node = child, depth = depth + 1)
                }
                
                if (node.children.isNotEmpty()) {
                    Text(
                        text = "</${node.tagName}>",
                        color = Color(0xFFE5E7EB).copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun LogsTab(logs: List<String>) {
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

// Tokenizer implementation
fun tokenize(html: String, logs: MutableList<String>): List<HtmlToken> {
    val tokens = mutableListOf<HtmlToken>()
    var i = 0
    val len = html.length
    
    while (i < len) {
        if (html[i] == '<') {
            val startIdx = i
            var endIdx = -1
            var inQuotes = false
            var quoteChar = ' '
            var j = i + 1
            while (j < len) {
                val c = html[j]
                if (c == '"' || c == '\'') {
                    if (!inQuotes) {
                        inQuotes = true
                        quoteChar = c
                    } else if (c == quoteChar) {
                        inQuotes = false
                    }
                }
                if (!inQuotes && c == '>') {
                    endIdx = j
                    break
                }
                j++
            }
            
            if (endIdx != -1) {
                val tagStr = html.substring(startIdx + 1, endIdx).trim()
                i = endIdx + 1
                
                if (tagStr.startsWith("/")) {
                    val tagName = tagStr.substring(1).trim().lowercase()
                    tokens.add(HtmlToken(HtmlToken.TokenType.TAG_END, "</$tagName>", tagName))
                } else {
                    val isSelfClosing = tagStr.endsWith("/")
                    val cleanedTag = if (isSelfClosing) tagStr.substring(0, tagStr.length - 1).trim() else tagStr
                    
                    val nameAndAttrs = parseTagNameAndAttrs(cleanedTag)
                    val tagName = nameAndAttrs.first.lowercase()
                    val attrs = nameAndAttrs.second
                    
                    if (isSelfClosing) {
                        tokens.add(HtmlToken(HtmlToken.TokenType.TAG_SELF_CLOSING, "<$tagStr>", tagName, attrs))
                    } else {
                        val isImplicitSelfClosing = tagName in setOf("img", "br", "hr", "meta", "link", "input")
                        if (isImplicitSelfClosing) {
                            tokens.add(HtmlToken(HtmlToken.TokenType.TAG_SELF_CLOSING, "<$tagStr>", tagName, attrs))
                        } else {
                            tokens.add(HtmlToken(HtmlToken.TokenType.TAG_START, "<$tagStr>", tagName, attrs))
                        }
                    }
                }
            } else {
                tokens.add(HtmlToken(HtmlToken.TokenType.TEXT, "<"))
                i++
            }
        } else {
            var j = i
            while (j < len && html[j] != '<') {
                j++
            }
            val text = html.substring(i, j)
            if (text.isNotBlank() || text.contains("\n")) {
                tokens.add(HtmlToken(HtmlToken.TokenType.TEXT, text))
            }
            i = j
        }
    }
    return tokens
}

fun parseTagNameAndAttrs(tagContent: String): Pair<String, Map<String, String>> {
    val trimmed = tagContent.trim()
    val spaceIdx = trimmed.indexOfFirst { it.isWhitespace() }
    if (spaceIdx == -1) {
        return Pair(trimmed, emptyMap())
    }
    val tagName = trimmed.substring(0, spaceIdx)
    val attrsContent = trimmed.substring(spaceIdx + 1).trim()
    
    val attrs = mutableMapOf<String, String>()
    var i = 0
    val len = attrsContent.length
    while (i < len) {
        while (i < len && attrsContent[i].isWhitespace()) {
            i++
        }
        if (i >= len) break
        
        val nameStart = i
        while (i < len && attrsContent[i] != '=' && !attrsContent[i].isWhitespace()) {
            i++
        }
        val attrName = attrsContent.substring(nameStart, i).trim().lowercase()
        
        while (i < len && attrsContent[i].isWhitespace()) {
            i++
        }
        
        if (i < len && attrsContent[i] == '=') {
            i++ // skip '='
            while (i < len && attrsContent[i].isWhitespace()) {
                i++
            }
            if (i < len) {
                val quote = attrsContent[i]
                if (quote == '"' || quote == '\'') {
                    i++ // skip quote
                    val valStart = i
                    while (i < len && attrsContent[i] != quote) {
                        i++
                    }
                    val attrValue = attrsContent.substring(valStart, i)
                    attrs[attrName] = attrValue
                    if (i < len) i++ // skip close quote
                } else {
                    val valStart = i
                    while (i < len && !attrsContent[i].isWhitespace()) {
                        i++
                    }
                    val attrValue = attrsContent.substring(valStart, i)
                    attrs[attrName] = attrValue
                }
            }
        } else {
            if (attrName.isNotEmpty()) {
                attrs[attrName] = "true"
            }
        }
    }
    
    return Pair(tagName, attrs)
}

// Stack-based DOM parser building tree
fun parseDom(tokens: List<HtmlToken>, logs: MutableList<String>): List<HtmlNode> {
    val roots = mutableListOf<HtmlNode>()
    val stack = mutableListOf<HtmlNode.Element>()
    
    tokens.forEach { token ->
        when (token.type) {
            HtmlToken.TokenType.TEXT -> {
                val textNode = HtmlNode.Text(token.text)
                if (stack.isEmpty()) {
                    roots.add(textNode)
                } else {
                    stack.last().children.add(textNode)
                }
            }
            HtmlToken.TokenType.TAG_START -> {
                val element = HtmlNode.Element(token.tagName, token.attributes, mutableListOf())
                if (stack.isEmpty()) {
                    roots.add(element)
                } else {
                    stack.last().children.add(element)
                }
                stack.add(element)
            }
            HtmlToken.TokenType.TAG_SELF_CLOSING -> {
                val element = HtmlNode.Element(token.tagName, token.attributes, mutableListOf())
                if (stack.isEmpty()) {
                    roots.add(element)
                } else {
                    stack.last().children.add(element)
                }
            }
            HtmlToken.TokenType.TAG_END -> {
                val idx = stack.indexOfLast { it.tagName == token.tagName }
                if (idx != -1) {
                    if (idx < stack.size - 1) {
                        logs.add("Warning: Auto-closed tags: " + stack.subList(idx + 1, stack.size).map { "<${it.tagName}>" }.joinToString(", "))
                    }
                    while (stack.size > idx) {
                        stack.removeAt(stack.size - 1)
                    }
                } else {
                    logs.add("Warning: Ignored stray closing tag </${token.tagName}>")
                }
            }
        }
    }
    
    if (stack.isNotEmpty()) {
        logs.add("Warning: Auto-closed remaining open tags at document end: " + stack.map { "<${it.tagName}>" }.joinToString(", "))
    }
    
    return roots
}

// Inline CSS styles helper
fun parseCssStyle(styleStr: String?): Map<String, String> {
    if (styleStr == null) return emptyMap()
    val styles = mutableMapOf<String, String>()
    styleStr.split(";").forEach { rule ->
        val parts = rule.split(":")
        if (parts.size >= 2) {
            val key = parts[0].trim().lowercase()
            val value = parts.subList(1, parts.size).joinToString(":").trim()
            styles[key] = value
        }
    }
    return styles
}

fun applyCssStyles(styles: Map<String, String>): Modifier {
    var modifier: Modifier = Modifier
    
    val bg = styles["background"] ?: styles["background-color"]
    if (bg != null) {
        val color = parseColor(bg)
        if (color != null) {
            modifier = modifier.background(color)
        }
    }
    
    val padding = styles["padding"]
    if (padding != null) {
        val px = parseDimension(padding)
        if (px > 0f) {
            modifier = modifier.padding(px.dp)
        }
    }
    
    val radius = styles["border-radius"]
    var shape = RoundedCornerShape(0.dp)
    if (radius != null) {
        val rPx = parseDimension(radius)
        if (rPx > 0f) {
            shape = RoundedCornerShape(rPx.dp)
            modifier = modifier.clip(shape)
        }
    }
    
    val border = styles["border"]
    if (border != null) {
        val borderParts = border.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (borderParts.size >= 3) {
            val widthPx = parseDimension(borderParts[0]).coerceAtLeast(1f)
            val color = parseColor(borderParts.last()) ?: Color.Gray
            modifier = modifier.border(widthPx.dp, color, shape)
        }
    }
    
    val width = styles["width"]
    if (width != null) {
        if (width == "100%") {
            modifier = modifier.fillMaxWidth()
        } else {
            val wPx = parseDimension(width)
            if (wPx > 0f) {
                modifier = modifier.width(wPx.dp)
            }
        }
    }
    val height = styles["height"]
    if (height != null) {
        if (height == "100%") {
            modifier = modifier.fillMaxHeight()
        } else {
            val hPx = parseDimension(height)
            if (hPx > 0f) {
                modifier = modifier.height(hPx.dp)
            }
        }
    }
    
    return modifier
}

fun getTextStyleForStyles(tagName: String, styles: Map<String, String>): TextStyle {
    var color = Color(0xFFECEFF1)
    val textColorStr = styles["color"]
    if (textColorStr != null) {
        val parsed = parseColor(textColorStr)
        if (parsed != null) color = parsed
    }
    
    var fontSize = when (tagName) {
        "h1" -> 24.sp
        "h2" -> 20.sp
        "h3" -> 16.sp
        "small" -> 11.sp
        else -> 14.sp
    }
    val fontSizeStr = styles["font-size"]
    if (fontSizeStr != null) {
        val sizeVal = parseDimension(fontSizeStr)
        if (sizeVal > 0f) {
            fontSize = sizeVal.sp
        }
    }
    
    var fontWeight = when (tagName) {
        "h1", "h2", "h3", "b", "strong" -> FontWeight.Bold
        else -> FontWeight.Normal
    }
    val fontWeightStr = styles["font-weight"]
    if (fontWeightStr != null) {
        if (fontWeightStr == "bold") fontWeight = FontWeight.Bold
    }
    
    val fontStyle = when (tagName) {
        "i", "em" -> androidx.compose.ui.text.font.FontStyle.Italic
        else -> androidx.compose.ui.text.font.FontStyle.Normal
    }
    
    return TextStyle(
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = if (tagName == "code" || tagName == "pre") FontFamily.Monospace else FontFamily.SansSerif
    )
}

fun parseColor(colorStr: String): Color? {
    val cleaned = colorStr.trim().lowercase()
    if (cleaned.startsWith("#")) {
        val hex = cleaned.substring(1)
        return try {
            if (hex.length == 3) {
                val r = hex[0].toString().repeat(2)
                val g = hex[1].toString().repeat(2)
                val b = hex[2].toString().repeat(2)
                Color(("$r$g$b").toLong(16) or 0xFF000000)
            } else if (hex.length == 6) {
                Color(hex.toLong(16) or 0xFF000000)
            } else if (hex.length == 8) {
                Color(hex.toLong(16))
            } else null
        } catch (e: Exception) {
            null
        }
    }
    return when (cleaned) {
        "red" -> Color(0xFFEF4444)
        "blue" -> Color(0xFF3B82F6)
        "green" -> Color(0xFF10B981)
        "white" -> Color.White
        "black" -> Color.Black
        "gray", "grey" -> Color.Gray
        "yellow" -> Color.Yellow
        "cyan" -> Color.Cyan
        "magenta" -> Color.Magenta
        "orange" -> Color(0xFFF59E0B)
        "transparent" -> Color.Transparent
        else -> null
    }
}

fun parseDimension(dimStr: String): Float {
    val cleaned = dimStr.trim().lowercase()
    val digits = cleaned.takeWhile { it.isDigit() || it == '.' }
    return digits.toFloatOrNull() ?: 0f
}

// Recursive HTML element layout renderer
@Composable
fun RenderHtmlNode(node: HtmlNode) {
    when (node) {
        is HtmlNode.Text -> {
            Text(
                text = node.content,
                color = Color.White,
                fontSize = 14.sp
            )
        }
        is HtmlNode.Element -> {
            val styles = remember(node.attributes) { parseCssStyle(node.attributes["style"]) }
            val modifier = remember(styles) { applyCssStyles(styles) }
            val textStyle = remember(node.tagName, styles) { getTextStyleForStyles(node.tagName, styles) }
            
            val display = styles["display"] ?: "block"
            val isFlexRow = display == "flex" && styles["flex-direction"] == "row"
            
            when (node.tagName) {
                "div", "section", "article", "nav", "header", "footer" -> {
                    if (isFlexRow) {
                        Row(
                            modifier = modifier,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            node.children.forEach { child -> RenderHtmlNode(child) }
                        }
                    } else {
                        Column(
                            modifier = modifier,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            node.children.forEach { child -> RenderHtmlNode(child) }
                        }
                    }
                }
                
                "p", "span", "h1", "h2", "h3", "b", "strong", "i", "em", "code", "a" -> {
                    val textContent = buildString {
                        node.children.forEach { child ->
                            if (child is HtmlNode.Text) {
                                append(child.content)
                            } else if (child is HtmlNode.Element && child.tagName == "br") {
                                append("\n")
                            } else if (child is HtmlNode.Element) {
                                val firstText = child.children.firstOrNull() as? HtmlNode.Text
                                if (firstText != null) {
                                    append(firstText.content)
                                }
                            }
                        }
                    }
                    if (textContent.isNotEmpty()) {
                        Text(
                            text = textContent,
                            style = textStyle,
                            modifier = modifier
                        )
                    } else {
                        node.children.forEach { child -> RenderHtmlNode(child) }
                    }
                }
                
                "br" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                "hr" -> {
                    HorizontalDivider(
                        color = Color.DarkGray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                "img" -> {
                    val src = node.attributes["src"] ?: ""
                    Box(
                        modifier = modifier
                            .size(100.dp)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "IMAGE\n($src)",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                else -> {
                    Column(modifier = modifier) {
                        node.children.forEach { child -> RenderHtmlNode(child) }
                    }
                }
            }
        }
    }
}

// System current millis time shim
fun currentTimeMillis(): Long {
    // Cross-platform KMP helper
    return ClockShim.now()
}

object ClockShim {
    // Basic shim utilizing platform capability
    fun now(): Long {
        // We will default to a basic implementation since standard Java System.currentTimeMillis() works inside android/desktop JVM sets
        return System.currentTimeMillis()
    }
}
