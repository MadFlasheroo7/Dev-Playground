package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class UIElement {
    object AvatarElement : UIElement()
    data class TextElement(val text: String, val isTitle: Boolean) : UIElement()
    data class InputElement(val label: String, val isPassword: Boolean) : UIElement()
    data class SwitchElement(val label: String, val defaultValue: Boolean = false) : UIElement()
    data class SliderElement(val label: String, val defaultValue: Float = 0.5f) : UIElement()
    data class ButtonElement(val label: String) : UIElement()
    data class RatingElement(val stars: Int) : UIElement()
    object DividerElement : UIElement()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptToUiScreen(onBack: () -> Unit) {
    var promptInput by remember { mutableStateOf(
        "card\navatar\ntitle: Sarah Jenkins\ntext: Principal Mobile Developer\ndivider\nswitch: Enable Status\nbutton: Follow Profile"
    ) }
    var parsedElements by remember { mutableStateOf(emptyList<UIElement>()) }
    var generatedCode by remember { mutableStateOf("") }
    
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Dynamic states for interactive rendering
    val inputStates = remember { mutableStateMapOf<Int, String>() }
    val switchStates = remember { mutableStateMapOf<Int, Boolean>() }
    val sliderStates = remember { mutableStateMapOf<Int, Float>() }
    val ratingStates = remember { mutableStateMapOf<Int, Int>() }

    // Synchronize compilation of components
    LaunchedEffect(promptInput) {
        val parsed = parsePromptToUi(promptInput)
        parsedElements = parsed
        generatedCode = generateComposeCode(parsed)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local AI Component Canvas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isWideLayout = maxWidth >= 850.dp

            if (isWideLayout) {
                // Large Desktop/Tablet screen side-by-side layout
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT COLUMN: Prompt Editor
                    Card(
                        modifier = Modifier.width(320.dp).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        PromptEditorSection(
                            prompt = promptInput,
                            onPromptChange = { promptInput = it },
                            onSelectPreset = { promptInput = it }
                        )
                    }

                    // MIDDLE COLUMN: Visual Rendering Canvas
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        VisualCanvasSection(
                            elements = parsedElements,
                            inputStates = inputStates,
                            switchStates = switchStates,
                            sliderStates = sliderStates,
                            ratingStates = ratingStates
                        )
                    }

                    // RIGHT COLUMN: Generated Code View
                    Card(
                        modifier = Modifier.width(350.dp).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        CodeViewerSection(
                            code = generatedCode,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(generatedCode))
                            }
                        )
                    }
                }
            } else {
                // Mobile vertical scroll layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Prompt Editor Section
                    Card(
                        modifier = Modifier.fillMaxWidth().height(360.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        PromptEditorSection(
                            prompt = promptInput,
                            onPromptChange = { promptInput = it },
                            onSelectPreset = { promptInput = it }
                        )
                    }

                    // Visual Rendering Canvas
                    Card(
                        modifier = Modifier.fillMaxWidth().height(420.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        VisualCanvasSection(
                            elements = parsedElements,
                            inputStates = inputStates,
                            switchStates = switchStates,
                            sliderStates = sliderStates,
                            ratingStates = ratingStates
                        )
                    }

                    // Generated Code View
                    Card(
                        modifier = Modifier.fillMaxWidth().height(350.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        CodeViewerSection(
                            code = generatedCode,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(generatedCode))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptEditorSection(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSelectPreset: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AI UI Prompt Input", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Describe a component or write layout keys (e.g. card, avatar, title: John, switch: Active, button: OK)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        )

        Text("Examples Presets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { onSelectPreset("card\navatar\ntitle: Sarah Jenkins\ntext: Principal Mobile Developer\ndivider\nswitch: Enable Status\nbutton: Follow Profile") },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("👤 Profile", fontSize = 10.sp)
            }
            Button(
                onClick = { onSelectPreset("card\ntitle: Welcome Back\ntext: Login secure sandbox\ninput: Email Address\npassword: Password\nbutton: Log In") },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("🔐 Login", fontSize = 10.sp)
            }
            Button(
                onClick = { onSelectPreset("card\ntitle: Preferences\nswitch: Enable Sync\nslider: Audio Volume\nbutton: Done") },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("⚙️ Settings", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun VisualCanvasSection(
    elements: List<UIElement>,
    inputStates: MutableMap<Int, String>,
    switchStates: MutableMap<Int, Boolean>,
    sliderStates: MutableMap<Int, Float>,
    ratingStates: MutableMap<Int, Int>
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Visual Canvas Frame", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E2E))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (elements.isEmpty()) {
                Text("Type layout descriptions on the left to see interactive components live.", color = Color.Gray, textAlign = TextAlign.Center)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        elements.forEachIndexed { idx, elem ->
                            when (elem) {
                                is UIElement.AvatarElement -> {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👤", fontSize = 24.sp)
                                    }
                                }
                                is UIElement.TextElement -> {
                                    Text(
                                        text = elem.text,
                                        style = if (elem.isTitle) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (elem.isTitle) FontWeight.Bold else FontWeight.Normal,
                                        color = if (elem.isTitle) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                is UIElement.InputElement -> {
                                    val textVal = inputStates.getOrPut(idx) { "" }
                                    OutlinedTextField(
                                        value = textVal,
                                        onValueChange = { inputStates[idx] = it },
                                        label = { Text(elem.label) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                                is UIElement.SwitchElement -> {
                                    val checkVal = switchStates.getOrPut(idx) { elem.defaultValue }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(elem.label, style = MaterialTheme.typography.bodyMedium)
                                        Switch(
                                            checked = checkVal,
                                            onCheckedChange = { switchStates[idx] = it }
                                        )
                                    }
                                }
                                is UIElement.SliderElement -> {
                                    val sliderVal = sliderStates.getOrPut(idx) { elem.defaultValue }
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(elem.label, style = MaterialTheme.typography.bodySmall)
                                            Text("${(sliderVal * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = sliderVal,
                                            onValueChange = { sliderStates[idx] = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                is UIElement.ButtonElement -> {
                                    Button(
                                        onClick = {},
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(elem.label)
                                    }
                                }
                                is UIElement.RatingElement -> {
                                    val ratingVal = ratingStates.getOrPut(idx) { 3 }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (i in 1..elem.stars) {
                                            Text(
                                                text = if (i <= ratingVal) "★" else "☆",
                                                fontSize = 28.sp,
                                                color = if (i <= ratingVal) Color(0xFFFFD700) else Color.LightGray,
                                                modifier = Modifier.clickable { ratingStates[idx] = i }
                                            )
                                        }
                                    }
                                }
                                is UIElement.DividerElement -> {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeViewerSection(
    code: String,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Generated Compose Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onCopy) {
                Text("Copy Code")
            }
        }
        
        OutlinedTextField(
            value = code,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        )
    }
}

private fun parsePromptToUi(prompt: String): List<UIElement> {
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isBlank()) return emptyList()

    val elements = mutableListOf<UIElement>()
    val lines = cleanPrompt.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    if (lines.size == 1 && !cleanPrompt.contains(":")) {
        val words = cleanPrompt.lowercase()
        
        if (words.contains("avatar") || words.contains("profile") || words.contains("user")) {
            elements.add(UIElement.AvatarElement)
        }
        
        val titleMatch = Regex("(?:title|name|header|user)\\s+[\"']?([^\"']+)[\"']?").find(words)
        val titleText = titleMatch?.groupValues?.get(1) ?: "Jane Doe"
        elements.add(UIElement.TextElement(titleText, isTitle = true))
        
        if (words.contains("bio") || words.contains("description") || words.contains("text")) {
            elements.add(UIElement.TextElement("Software Engineer", isTitle = false))
        }
        
        if (words.contains("divider") || words.contains("line")) {
            elements.add(UIElement.DividerElement)
        }
        
        if (words.contains("email") || words.contains("login") || words.contains("input") || words.contains("field")) {
            elements.add(UIElement.InputElement("Email Address", isPassword = false))
            if (words.contains("password")) {
                elements.add(UIElement.InputElement("Password", isPassword = true))
            }
        }
        
        if (words.contains("switch") || words.contains("toggle") || words.contains("notification")) {
            elements.add(UIElement.SwitchElement("Enable Notifications"))
        }
        
        if (words.contains("slider") || words.contains("volume") || words.contains("brightness")) {
            elements.add(UIElement.SliderElement("Volume Level"))
        }
        
        if (words.contains("rating") || words.contains("stars")) {
            elements.add(UIElement.RatingElement(5))
        }
        
        if (words.contains("button") || words.contains("submit") || words.contains("login") || words.contains("follow")) {
            val buttonText = when {
                words.contains("login") -> "Log In"
                words.contains("follow") -> "Follow"
                words.contains("submit") -> "Submit"
                else -> "Action"
            }
            elements.add(UIElement.ButtonElement(buttonText))
        }
    } else {
        lines.forEach { line ->
            val lowerLine = line.lowercase()
            val colonIndex = line.indexOf(":")
            val key = if (colonIndex != -1) lowerLine.substring(0, colonIndex).trim() else lowerLine.trim()
            val value = if (colonIndex != -1) line.substring(colonIndex + 1).trim() else ""

            when {
                key.contains("avatar") || key.contains("image") || key.contains("pic") -> {
                    elements.add(UIElement.AvatarElement)
                }
                key.contains("title") || key.contains("header") || key.contains("name") -> {
                    elements.add(UIElement.TextElement(value.ifBlank { "Title" }, isTitle = true))
                }
                key.contains("text") || key.contains("desc") || key.contains("bio") || key.contains("subtitle") -> {
                    elements.add(UIElement.TextElement(value.ifBlank { "Sample text content." }, isTitle = false))
                }
                key.contains("input") || key.contains("field") || key.contains("email") || key.contains("password") -> {
                    val isPass = key.contains("password")
                    val label = value.ifBlank { if (isPass) "Password" else "Input Field" }
                    elements.add(UIElement.InputElement(label, isPassword = isPass))
                }
                key.contains("switch") || key.contains("toggle") -> {
                    elements.add(UIElement.SwitchElement(value.ifBlank { "Enable setting" }))
                }
                key.contains("slider") || key.contains("range") -> {
                    elements.add(UIElement.SliderElement(value.ifBlank { "Value" }))
                }
                key.contains("rating") || key.contains("stars") -> {
                    val starsVal = value.toIntOrNull() ?: 5
                    elements.add(UIElement.RatingElement(starsVal))
                }
                key.contains("button") || key.contains("btn") || key.contains("submit") || key.contains("action") -> {
                    elements.add(UIElement.ButtonElement(value.ifBlank { "Click Me" }))
                }
                key.contains("divider") || key.contains("line") || key.contains("hr") -> {
                    elements.add(UIElement.DividerElement)
                }
                else -> {
                    if (line.startsWith("- ") || line.startsWith("* ")) {
                        elements.add(UIElement.TextElement(line.substring(2), isTitle = false))
                    } else {
                        elements.add(UIElement.TextElement(line, isTitle = false))
                    }
                }
            }
        }
    }
    return elements
}

private fun generateComposeCode(elements: List<UIElement>): String {
    val sb = StringBuilder()
    sb.append("// Generated by Compose Local AI Canvas\n")
    sb.append("import androidx.compose.foundation.background\n")
    sb.append("import androidx.compose.foundation.clickable\n")
    sb.append("import androidx.compose.foundation.layout.*\n")
    sb.append("import androidx.compose.foundation.shape.CircleShape\n")
    sb.append("import androidx.compose.foundation.shape.RoundedCornerShape\n")
    sb.append("import androidx.compose.material3.*\n")
    sb.append("import androidx.compose.runtime.*\n")
    sb.append("import androidx.compose.ui.Alignment\n")
    sb.append("import androidx.compose.ui.Modifier\n")
    sb.append("import androidx.compose.ui.draw.clip\n")
    sb.append("import androidx.compose.ui.graphics.Color\n")
    sb.append("import androidx.compose.ui.text.font.FontWeight\n")
    sb.append("import androidx.compose.ui.text.style.TextAlign\n")
    sb.append("import androidx.compose.ui.unit.dp\n")
    sb.append("import androidx.compose.ui.unit.sp\n\n")

    sb.append("@Composable\n")
    sb.append("fun GeneratedComponent(modifier: Modifier = Modifier) {\n")
    sb.append("    Card(\n")
    sb.append("        modifier = modifier.fillMaxWidth().padding(16.dp),\n")
    sb.append("        shape = RoundedCornerShape(16.dp),\n")
    sb.append("        colors = CardDefaults.cardColors(\n")
    sb.append("            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)\n")
    sb.append("        )\n")
    sb.append("    ) {\n")
    sb.append("        Column(\n")
    sb.append("            modifier = Modifier.padding(16.dp),\n")
    sb.append("            verticalArrangement = Arrangement.spacedBy(12.dp),\n")
    sb.append("            horizontalAlignment = Alignment.CenterHorizontally\n")
    sb.append("        ) {\n")

    elements.forEachIndexed { i, elem ->
        when (elem) {
            is UIElement.AvatarElement -> {
                sb.append("            // Avatar\n")
                sb.append("            Box(\n")
                sb.append("                modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),\n")
                sb.append("                contentAlignment = Alignment.Center\n")
                sb.append("            ) {\n")
                sb.append("                Text(\"👤\", fontSize = 24.sp)\n")
                sb.append("            }\n")
            }
            is UIElement.TextElement -> {
                if (elem.isTitle) {
                    sb.append("            Text(\n")
                    sb.append("                text = \"${elem.text}\",\n")
                    sb.append("                style = MaterialTheme.typography.titleLarge,\n")
                    sb.append("                fontWeight = FontWeight.Bold,\n")
                    sb.append("                color = MaterialTheme.colorScheme.onSurface\n")
                    sb.append("            )\n")
                } else {
                    sb.append("            Text(\n")
                    sb.append("                text = \"${elem.text}\",\n")
                    sb.append("                style = MaterialTheme.typography.bodyMedium,\n")
                    sb.append("                color = MaterialTheme.colorScheme.onSurfaceVariant,\n")
                    sb.append("                textAlign = TextAlign.Center\n")
                    sb.append("            )\n")
                }
            }
            is UIElement.InputElement -> {
                sb.append("            var textState$i by remember { mutableStateOf(\"\") }\n")
                sb.append("            OutlinedTextField(\n")
                sb.append("                value = textState$i,\n")
                sb.append("                onValueChange = { textState$i = it },\n")
                sb.append("                label = { Text(\"${elem.label}\") },\n")
                sb.append("                modifier = Modifier.fillMaxWidth(),\n")
                sb.append("                singleLine = true\n")
                sb.append("            )\n")
            }
            is UIElement.SwitchElement -> {
                sb.append("            var checkedState$i by remember { mutableStateOf(${elem.defaultValue}) }\n")
                sb.append("            Row(\n")
                sb.append("                modifier = Modifier.fillMaxWidth(),\n")
                sb.append("                horizontalArrangement = Arrangement.SpaceBetween,\n")
                sb.append("                verticalAlignment = Alignment.CenterVertically\n")
                sb.append("            ) {\n")
                sb.append("                Text(\"${elem.label}\", style = MaterialTheme.typography.bodyMedium)\n")
                sb.append("                Switch(checked = checkedState$i, onCheckedChange = { checkedState$i = it })\n")
                sb.append("            }\n")
            }
            is UIElement.SliderElement -> {
                sb.append("            var sliderValue$i by remember { mutableStateOf(${elem.defaultValue}f) }\n")
                sb.append("            Column(modifier = Modifier.fillMaxWidth()) {\n")
                sb.append("                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {\n")
                sb.append("                    Text(\"${elem.label}\", style = MaterialTheme.typography.bodySmall)\n")
                sb.append("                    Text(\"\${(sliderValue$i * 100).toInt()}%\", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)\n")
                sb.append("                }\n")
                sb.append("                Slider(value = sliderValue$i, onValueChange = { sliderValue$i = it }, modifier = Modifier.fillMaxWidth())\n")
                sb.append("            }\n")
            }
            is UIElement.ButtonElement -> {
                sb.append("            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {\n")
                sb.append("                Text(\"${elem.label}\")\n")
                sb.append("            }\n")
            }
            is UIElement.RatingElement -> {
                sb.append("            var rating$i by remember { mutableStateOf(3) }\n")
                sb.append("            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {\n")
                sb.append("                for (k in 1..${elem.stars}) {\n")
                sb.append("                    Text(\n")
                sb.append("                        text = if (k <= rating$i) \"★\" else \"☆\",\n")
                sb.append("                        fontSize = 28.sp,\n")
                sb.append("                        color = if (k <= rating$i) Color(0xFFFFD700) else Color.LightGray,\n")
                sb.append("                        modifier = Modifier.clickable { rating$i = k }\n")
                sb.append("                    )\n")
                sb.append("                }\n")
                sb.append("            }\n")
            }
            is UIElement.DividerElement -> {
                sb.append("            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)\n")
            }
        }
    }

    sb.append("        }\n")
    sb.append("    }\n")
    sb.append("}\n")
    return sb.toString()
}
