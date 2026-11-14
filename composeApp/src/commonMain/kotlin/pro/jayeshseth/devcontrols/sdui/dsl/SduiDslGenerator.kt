package pro.jayeshseth.devcontrols.sdui.dsl

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import pro.jayeshseth.devcontrols.sdui.model.SduiAction
import pro.jayeshseth.devcontrols.sdui.model.SduiAnimationSpec
import pro.jayeshseth.devcontrols.sdui.model.SduiBottomSheetSpec
import pro.jayeshseth.devcontrols.sdui.model.SduiCanvasOp
import pro.jayeshseth.devcontrols.sdui.model.SduiDocument
import pro.jayeshseth.devcontrols.sdui.model.SduiEnterTransition
import pro.jayeshseth.devcontrols.sdui.model.SduiExitTransition
import pro.jayeshseth.devcontrols.sdui.model.SduiModifier
import pro.jayeshseth.devcontrols.sdui.model.SduiNode

/**
 * Generates human-readable, idiomatic Kotlin SDUI DSL code from AST structures.
 */
object SduiDslGenerator {

    fun generate(doc: SduiDocument): String {
        val sb = StringBuilder()
        sb.appendLine("sduiDocument(")
        sb.appendLine("    id = \"${escapeString(doc.id)}\",")
        sb.appendLine("    version = ${doc.version}")
        sb.appendLine(") {")

        if (doc.metadata.isNotEmpty()) {
            sb.appendLine("    metadata(")
            val metaList = doc.metadata.entries.toList()
            metaList.forEachIndexed { index, (key, value) ->
                val comma = if (index < metaList.size - 1) "," else ""
                sb.appendLine("        \"${escapeString(key)}\" to \"${escapeString(value)}\"$comma")
            }
            sb.appendLine("    )")
            sb.appendLine()
        }

        if (doc.initialState.isNotEmpty()) {
            sb.appendLine("    initialState(")
            val stateList = doc.initialState.entries.toList()
            stateList.forEachIndexed { index, (key, value) ->
                val comma = if (index < stateList.size - 1) "," else ""
                sb.appendLine("        \"${escapeString(key)}\" to ${formatJsonElement(value, 2)}$comma")
            }
            sb.appendLine("    )")
            sb.appendLine()
        }

        // Render root node
        sb.append(generateNode(doc.root, indentLevel = 1))

        // Render bottomSheet if present
        if (doc.bottomSheet != null) {
            sb.appendLine()
            sb.append(generateBottomSheet(doc.bottomSheet, indentLevel = 1))
        }

        sb.appendLine("}")
        return sb.toString().trimEnd() + "\n"
    }

    fun generateBottomSheet(sheet: SduiBottomSheetSpec, indentLevel: Int): String {
        val sb = StringBuilder()
        val indent = "    ".repeat(indentLevel)
        sb.appendLine("${indent}sduiBottomSheet(")
        sb.appendLine("${indent}    id = \"${escapeString(sheet.id)}\",")
        sb.appendLine("${indent}    isOpenExpr = \"${escapeString(sheet.isOpenExpr)}\",")
        if (sheet.skipPartiallyExpanded) {
            sb.appendLine("${indent}    skipPartiallyExpanded = true,")
        }
        if (!sheet.dragHandleVisible) {
            sb.appendLine("${indent}    dragHandleVisible = false,")
        }
        if (sheet.peekHeightDp != 0f) {
            sb.appendLine("${indent}    peekHeightDp = ${formatFloat(sheet.peekHeightDp)},")
        }
        if (sheet.cornerRadiusDp != 24f) {
            sb.appendLine("${indent}    cornerRadiusDp = ${formatFloat(sheet.cornerRadiusDp)},")
        }
        if (sheet.containerColor != null) {
            sb.appendLine("${indent}    containerColor = \"${escapeString(sheet.containerColor)}\",")
        }
        if (sheet.scrimColor != null) {
            sb.appendLine("${indent}    scrimColor = \"${escapeString(sheet.scrimColor)}\",")
        }
        if (sheet.onDismissActions.isNotEmpty()) {
            sb.appendLine("${indent}    onDismiss = {")
            sheet.onDismissActions.forEach { action ->
                sb.append(generateAction(action, indentLevel + 2))
            }
            sb.appendLine("${indent}    }")
        }
        // Remove trailing comma from last arg if any
        val lines = sb.toString().trimEnd().lines()
        val fixedLines = lines.mapIndexed { idx, line ->
            if (idx == lines.size - 1 && line.endsWith(",")) line.dropLast(1) else line
        }
        sb.clear()
        fixedLines.forEach { sb.appendLine(it) }

        sb.appendLine("${indent}) {")
        sb.append(generateNode(sheet.content, indentLevel + 1))
        sb.appendLine("${indent}}")
        return sb.toString()
    }

    fun generateNode(node: SduiNode, indentLevel: Int): String {
        val indent = "    ".repeat(indentLevel)
        val sb = StringBuilder()

        when (node) {
            is SduiNode.Column -> {
                val args = mutableListOf<String>()
                if (node.spacingDp != 0f) args.add("spacingDp = ${formatFloat(node.spacingDp)}")
                if (node.verticalArrangement != "top") args.add("verticalArrangement = \"${escapeString(node.verticalArrangement)}\"")
                if (node.horizontalAlignment != "start") args.add("horizontalAlignment = \"${escapeString(node.horizontalAlignment)}\"")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                appendCallWithChildren(sb, indent, "sduiColumn", args, node.children, indentLevel)
            }
            is SduiNode.Row -> {
                val args = mutableListOf<String>()
                if (node.spacingDp != 0f) args.add("spacingDp = ${formatFloat(node.spacingDp)}")
                if (node.horizontalArrangement != "start") args.add("horizontalArrangement = \"${escapeString(node.horizontalArrangement)}\"")
                if (node.verticalAlignment != "top") args.add("verticalAlignment = \"${escapeString(node.verticalAlignment)}\"")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                appendCallWithChildren(sb, indent, "sduiRow", args, node.children, indentLevel)
            }
            is SduiNode.Box -> {
                val args = mutableListOf<String>()
                if (node.contentAlignment != "topLeft") args.add("contentAlignment = \"${escapeString(node.contentAlignment)}\"")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                appendCallWithChildren(sb, indent, "sduiBox", args, node.children, indentLevel)
            }
            is SduiNode.LazyColumn -> {
                val args = mutableListOf<String>()
                if (node.spacingDp != 0f) args.add("spacingDp = ${formatFloat(node.spacingDp)}")
                if (node.contentPaddingDp != 0f) args.add("contentPaddingDp = ${formatFloat(node.contentPaddingDp)}")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                appendCallWithChildren(sb, indent, "sduiLazyColumn", args, node.children, indentLevel)
            }
            is SduiNode.LazyRow -> {
                val args = mutableListOf<String>()
                if (node.spacingDp != 0f) args.add("spacingDp = ${formatFloat(node.spacingDp)}")
                if (node.contentPaddingDp != 0f) args.add("contentPaddingDp = ${formatFloat(node.contentPaddingDp)}")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                appendCallWithChildren(sb, indent, "sduiLazyRow", args, node.children, indentLevel)
            }
            is SduiNode.LazyVerticalGrid -> {
                val args = mutableListOf<String>()
                args.add("columns = ${node.columns}")
                if (node.spacingDp != 0f) args.add("spacingDp = ${formatFloat(node.spacingDp)}")
                if (node.contentPaddingDp != 0f) args.add("contentPaddingDp = ${formatFloat(node.contentPaddingDp)}")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                appendCallWithChildren(sb, indent, "sduiLazyGrid", args, node.children, indentLevel)
            }
            is SduiNode.Card -> {
                val args = mutableListOf<String>()
                if (node.elevation != 1f) args.add("elevation = ${formatFloat(node.elevation)}")
                if (node.cornerRadius != 12f) args.add("cornerRadius = ${formatFloat(node.cornerRadius)}")
                if (node.backgroundColor != null) args.add("backgroundColor = \"${escapeString(node.backgroundColor)}\"")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")
                if (node.onClick.isNotEmpty()) args.add("onClick = ${generateActionCall(node.onClick, indentLevel)}")

                appendCallWithChildren(sb, indent, "sduiCard", args, node.children, indentLevel)
            }
            is SduiNode.Text -> {
                val args = mutableListOf<String>()
                args.add("text = \"${escapeString(node.text)}\"")
                if (node.fontSize != 16f) args.add("fontSize = ${formatFloat(node.fontSize)}")
                if (node.fontWeight != "normal") args.add("fontWeight = \"${escapeString(node.fontWeight)}\"")
                if (node.fontStyle != "normal") args.add("fontStyle = \"${escapeString(node.fontStyle)}\"")
                if (node.color != null) args.add("color = \"${escapeString(node.color)}\"")
                if (node.textAlign != "start") args.add("textAlign = \"${escapeString(node.textAlign)}\"")
                if (node.maxLines != null) args.add("maxLines = ${node.maxLines}")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                appendSingleCall(sb, indent, "sduiText", args)
            }
            is SduiNode.Image -> {
                val args = mutableListOf<String>()
                if (node.url != null) args.add("url = \"${escapeString(node.url)}\"")
                if (node.vectorIcon != null) args.add("vectorIcon = \"${escapeString(node.vectorIcon)}\"")
                if (node.contentDescription != null) args.add("contentDescription = \"${escapeString(node.contentDescription)}\"")
                if (node.contentScale != "fit") args.add("contentScale = \"${escapeString(node.contentScale)}\"")
                if (node.tintColor != null) args.add("tintColor = \"${escapeString(node.tintColor)}\"")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                appendSingleCall(sb, indent, "sduiImage", args)
            }
            is SduiNode.Spacer -> {
                val args = mutableListOf<String>()
                if (node.width != null) args.add("width = ${formatFloat(node.width)}")
                if (node.height != null) args.add("height = ${formatFloat(node.height)}")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                appendSingleCall(sb, indent, "sduiSpacer", args)
            }
            is SduiNode.TextField -> {
                val args = mutableListOf<String>()
                args.add("valueStateKey = \"${escapeString(node.valueStateKey)}\"")
                if (node.label != null) args.add("label = \"${escapeString(node.label)}\"")
                if (node.placeholder != null) args.add("placeholder = \"${escapeString(node.placeholder)}\"")
                if (node.isPassword) args.add("isPassword = true")
                if (node.keyboardType != "text") args.add("keyboardType = \"${escapeString(node.keyboardType)}\"")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                appendSingleCall(sb, indent, "sduiTextField", args)
            }
            is SduiNode.Button -> {
                val args = mutableListOf<String>()
                if (node.text != null) args.add("text = \"${escapeString(node.text)}\"")
                if (node.variant != "filled") args.add("variant = \"${escapeString(node.variant)}\"")
                if (node.enabledExpr != null && node.enabledExpr != "true") args.add("enabledExpr = \"${escapeString(node.enabledExpr)}\"")
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")
                if (node.onClick.isNotEmpty()) args.add("onClick = ${generateActionCall(node.onClick, indentLevel)}")

                appendSingleCall(sb, indent, "sduiButton", args)
            }
            is SduiNode.Canvas -> {
                val args = mutableListOf<String>()
                if (node.modifiers.isNotEmpty()) args.add("modifier = ${generateModifierCall(node.modifiers, indentLevel)}")

                sb.append("${indent}sduiCanvas(")
                if (args.isNotEmpty()) {
                    sb.append(args.joinToString(", "))
                }
                sb.appendLine(") {")
                node.operations.forEach { op ->
                    sb.append(generateCanvasOp(op, indentLevel + 1))
                }
                sb.appendLine("${indent}}")
            }
            is SduiNode.AnimatedVisibility -> {
                val args = mutableListOf<String>()
                args.add("visibleExpr = \"${escapeString(node.visibleExpr)}\"")
                if (node.enter != null) args.add("enter = ${generateEnterTransition(node.enter)}")
                if (node.exit != null) args.add("exit = ${generateExitTransition(node.exit)}")

                sb.append("${indent}sduiAnimatedVisibility(")
                if (args.isNotEmpty()) {
                    sb.append(args.joinToString(", "))
                }
                sb.appendLine(") {")
                sb.append(generateNode(node.child, indentLevel + 1))
                sb.appendLine("${indent}}")
            }
            is SduiNode.Custom -> {
                val args = mutableListOf<String>()
                args.add("componentName = \"${escapeString(node.componentName)}\"")
                node.props.forEach { (k, v) ->
                    args.add("\"${escapeString(k)}\" to \"${escapeString(v)}\"")
                }
                appendSingleCall(sb, indent, "sduiCustom", args)
            }
            is SduiNode.Unknown -> {
                sb.appendLine("${indent}// Unknown node: ${node.rawType}")
            }
        }

        return sb.toString()
    }

    private fun appendCallWithChildren(
        sb: StringBuilder,
        indent: String,
        name: String,
        args: List<String>,
        children: List<SduiNode>,
        indentLevel: Int
    ) {
        if (args.isEmpty() && children.isEmpty()) {
            sb.appendLine("${indent}$name()")
            return
        }
        if (args.isEmpty()) {
            sb.appendLine("${indent}$name {")
            children.forEach { child -> sb.append(generateNode(child, indentLevel + 1)) }
            sb.appendLine("${indent}}")
            return
        }
        if (children.isEmpty()) {
            sb.appendLine("${indent}$name(${args.joinToString(", ")})")
            return
        }
        sb.appendLine("${indent}$name(")
        args.forEachIndexed { index, arg ->
            val comma = if (index < args.size - 1) "," else ""
            sb.appendLine("${indent}    $arg$comma")
        }
        sb.appendLine("${indent}) {")
        children.forEach { child -> sb.append(generateNode(child, indentLevel + 1)) }
        sb.appendLine("${indent}}")
    }

    private fun appendSingleCall(
        sb: StringBuilder,
        indent: String,
        name: String,
        args: List<String>
    ) {
        if (args.isEmpty()) {
            sb.appendLine("${indent}$name()")
            return
        }
        if (args.size == 1 && !args[0].contains("\n")) {
            sb.appendLine("${indent}$name(${args[0]})")
            return
        }
        sb.appendLine("${indent}$name(")
        args.forEachIndexed { index, arg ->
            val comma = if (index < args.size - 1) "," else ""
            sb.appendLine("${indent}    $arg$comma")
        }
        sb.appendLine("${indent})")
    }

    fun generateModifierCall(modifiers: List<SduiModifier>, indentLevel: Int): String {
        if (modifiers.isEmpty()) return "sduiModifier { }"
        val indent = "    ".repeat(indentLevel + 1)
        val sb = StringBuilder()
        sb.appendLine("modifier {")
        modifiers.forEach { mod ->
            sb.append(generateModifier(mod, indentLevel + 2))
        }
        sb.append("${indent}}")
        return sb.toString()
    }

    fun generateModifier(mod: SduiModifier, indentLevel: Int): String {
        val indent = "    ".repeat(indentLevel)
        return when (mod) {
            is SduiModifier.Padding -> {
                when {
                    mod.all != null -> "${indent}padding(all = ${formatExprOrNumber(mod.all)})\n"
                    mod.horizontal != null || mod.vertical != null -> {
                        val h = mod.horizontal?.let { "horizontal = ${formatExprOrNumber(it)}" }
                        val v = mod.vertical?.let { "vertical = ${formatExprOrNumber(it)}" }
                        val args = listOfNotNull(h, v).joinToString(", ")
                        "${indent}padding($args)\n"
                    }
                    else -> {
                        val s = mod.start?.let { "start = ${formatExprOrNumber(it)}" }
                        val t = mod.top?.let { "top = ${formatExprOrNumber(it)}" }
                        val e = mod.end?.let { "end = ${formatExprOrNumber(it)}" }
                        val b = mod.bottom?.let { "bottom = ${formatExprOrNumber(it)}" }
                        val args = listOfNotNull(s, t, e, b).joinToString(", ")
                        "${indent}padding($args)\n"
                    }
                }
            }
            is SduiModifier.Size -> {
                when {
                    mod.fillMaxSize != null -> "${indent}fillMaxSize(${formatExprOrNumber(mod.fillMaxSize)})\n"
                    mod.fillMaxWidth != null && mod.fillMaxHeight != null ->
                        "${indent}fillMaxWidth(${formatExprOrNumber(mod.fillMaxWidth)})\n${indent}fillMaxHeight(${formatExprOrNumber(mod.fillMaxHeight)})\n"
                    mod.fillMaxWidth != null -> "${indent}fillMaxWidth(${formatExprOrNumber(mod.fillMaxWidth)})\n"
                    mod.fillMaxHeight != null -> "${indent}fillMaxHeight(${formatExprOrNumber(mod.fillMaxHeight)})\n"
                    mod.width != null && mod.height != null ->
                        "${indent}size(width = ${formatExprOrNumber(mod.width)}, height = ${formatExprOrNumber(mod.height)})\n"
                    mod.width != null -> "${indent}size(width = ${formatExprOrNumber(mod.width)})\n"
                    mod.height != null -> "${indent}size(height = ${formatExprOrNumber(mod.height)})\n"
                    mod.aspectRatio != null -> "${indent}aspectRatio(${formatExprOrNumber(mod.aspectRatio)})\n"
                    mod.weight != null -> "${indent}weight(${formatExprOrNumber(mod.weight)})\n"
                    else -> ""
                }
            }
            is SduiModifier.Background -> {
                val args = mutableListOf<String>()
                if (mod.color != null) args.add("color = \"${escapeString(mod.color)}\"")
                if (mod.cornerRadius != null && mod.cornerRadius != 0f) args.add("cornerRadius = ${formatFloat(mod.cornerRadius)}")
                if (mod.gradientColors != null && mod.gradientColors.isNotEmpty()) {
                    val colors = mod.gradientColors.joinToString(", ") { "\"${escapeString(it)}\"" }
                    args.add("gradientColors = listOf($colors)")
                    if (mod.gradientAngle != 0f) args.add("gradientAngle = ${formatFloat(mod.gradientAngle)}")
                }
                "${indent}background(${args.joinToString(", ")})\n"
            }
            is SduiModifier.Border -> {
                val args = mutableListOf<String>()
                args.add("width = ${formatExprOrNumber(mod.width)}")
                args.add("color = \"${escapeString(mod.color)}\"")
                if (mod.cornerRadius != null) args.add("cornerRadius = ${formatExprOrNumber(mod.cornerRadius)}")
                if (mod.shape != "rectangle") args.add("shape = \"${escapeString(mod.shape)}\"")
                "${indent}border(${args.joinToString(", ")})\n"
            }
            is SduiModifier.Clip -> {
                if (mod.shape == "circle") "${indent}clipCircle()\n"
                else "${indent}clip(shape = \"${escapeString(mod.shape)}\", cornerRadius = ${formatExprOrNumber(mod.cornerRadius)})\n"
            }
            is SduiModifier.Shadow -> {
                "${indent}shadow(elevation = ${formatExprOrNumber(mod.elevation)}, shape = \"${escapeString(mod.shape)}\")\n"
            }
            is SduiModifier.Blur -> {
                "${indent}blur(radius = ${formatExprOrNumber(mod.radius)})\n"
            }
            is SduiModifier.GraphicsLayer -> {
                val args = mutableListOf<String>()
                if (mod.scaleX != null) args.add("scaleX = ${formatExprOrNumber(mod.scaleX)}")
                if (mod.scaleY != null) args.add("scaleY = ${formatExprOrNumber(mod.scaleY)}")
                if (mod.rotationZ != null) args.add("rotationZ = ${formatExprOrNumber(mod.rotationZ)}")
                if (mod.alpha != null) args.add("alpha = ${formatExprOrNumber(mod.alpha)}")
                if (mod.translationX != null) args.add("translationX = ${formatExprOrNumber(mod.translationX)}")
                if (mod.translationY != null) args.add("translationY = ${formatExprOrNumber(mod.translationY)}")
                "${indent}graphicsLayer(${args.joinToString(", ")})\n"
            }
            is SduiModifier.Clickable -> {
                val sb = StringBuilder()
                val rippleArg = if (!mod.rippleEnabled) "rippleEnabled = false" else ""
                sb.append("${indent}clickable($rippleArg) {\n")
                mod.actions.forEach { act -> sb.append(generateAction(act, indentLevel + 1)) }
                sb.append("${indent}}\n")
                sb.toString()
            }
            is SduiModifier.GestureDrag -> {
                val args = mutableListOf<String>()
                args.add("orientation = \"${escapeString(mod.orientation)}\"")
                args.add("minOffset = ${formatFloat(mod.minOffset)}")
                args.add("maxOffset = ${formatFloat(mod.maxOffset)}")
                args.add("targetStateKey = \"${escapeString(mod.targetStateKey)}\"")
                if (mod.snapBack) args.add("snapBack = true")
                "${indent}gestureDrag(${args.joinToString(", ")})\n"
            }
            is SduiModifier.AnimateContentSize -> {
                "${indent}animateContentSize()\n"
            }
            is SduiModifier.Unknown -> "${indent}// Unknown modifier: ${mod.rawType}\n"
        }
    }

    fun generateActionCall(actions: List<SduiAction>, indentLevel: Int): String {
        val indent = "    ".repeat(indentLevel + 1)
        val sb = StringBuilder()
        sb.appendLine("onClick {")
        actions.forEach { act ->
            sb.append(generateAction(act, indentLevel + 2))
        }
        sb.append("${indent}}")
        return sb.toString()
    }

    fun generateAction(action: SduiAction, indentLevel: Int): String {
        val indent = "    ".repeat(indentLevel)
        return when (action) {
            is SduiAction.SetState -> {
                "${indent}setState(\"${escapeString(action.key)}\", \"${escapeString(action.valueExpr)}\")\n"
            }
            is SduiAction.ToggleState -> {
                "${indent}toggleState(\"${escapeString(action.key)}\")\n"
            }
            is SduiAction.AnimateTo -> {
                "${indent}animateTo(\"${escapeString(action.key)}\", \"${escapeString(action.targetValueExpr)}\", animation = ${generateAnimationSpec(action.animation)})\n"
            }
            is SduiAction.Navigate -> {
                val ext = if (action.isExternal) ", isExternal = true" else ""
                "${indent}navigate(\"${escapeString(action.destination)}\"$ext)\n"
            }
            is SduiAction.ApiRequest -> {
                val args = mutableListOf<String>()
                args.add("url = \"${escapeString(action.url)}\"")
                if (action.method != "GET") args.add("method = \"${escapeString(action.method)}\"")
                if (action.responseStateKey != null) args.add("responseStateKey = \"${escapeString(action.responseStateKey)}\"")
                "${indent}apiRequest(${args.joinToString(", ")})\n"
            }
            is SduiAction.EmitNativeEvent -> {
                val pairs = action.payload.entries.joinToString(", ") { "\"${escapeString(it.key)}\" to \"${escapeString(it.value)}\"" }
                if (pairs.isEmpty()) {
                    "${indent}emitNativeEvent(\"${escapeString(action.eventName)}\")\n"
                } else {
                    "${indent}emitNativeEvent(\"${escapeString(action.eventName)}\", $pairs)\n"
                }
            }
            is SduiAction.Conditional -> {
                val sb = StringBuilder()
                sb.appendLine("${indent}conditional(")
                sb.appendLine("${indent}    conditionExpr = \"${escapeString(action.conditionExpr)}\",")
                sb.appendLine("${indent}    thenActions = {")
                action.thenActions.forEach { sb.append(generateAction(it, indentLevel + 2)) }
                sb.append("${indent}    }")
                if (action.elseActions.isNotEmpty()) {
                    sb.appendLine(",")
                    sb.appendLine("${indent}    elseActions = {")
                    action.elseActions.forEach { sb.append(generateAction(it, indentLevel + 2)) }
                    sb.appendLine("${indent}    }")
                } else {
                    sb.appendLine()
                }
                sb.appendLine("${indent})")
                sb.toString()
            }
            is SduiAction.Chain -> {
                val sb = StringBuilder()
                sb.appendLine("${indent}chain {")
                action.actions.forEach { sb.append(generateAction(it, indentLevel + 1)) }
                sb.appendLine("${indent}}")
                sb.toString()
            }
            is SduiAction.OpenBottomSheet -> {
                "${indent}openBottomSheet(\"${escapeString(action.sheetId)}\")\n"
            }
            is SduiAction.CloseBottomSheet -> {
                "${indent}closeBottomSheet(\"${escapeString(action.sheetId)}\")\n"
            }
            is SduiAction.Unknown -> "${indent}// Unknown action: ${action.rawType}\n"
        }
    }

    fun generateCanvasOp(op: SduiCanvasOp, indentLevel: Int): String {
        val indent = "    ".repeat(indentLevel)
        return when (op) {
            is SduiCanvasOp.Rect -> {
                val args = mutableListOf<String>()
                args.add("color = \"${escapeString(op.color)}\"")
                args.add("x = ${formatExprOrNumber(op.x)}")
                args.add("y = ${formatExprOrNumber(op.y)}")
                args.add("width = ${formatExprOrNumber(op.width)}")
                args.add("height = ${formatExprOrNumber(op.height)}")
                if (op.cornerRadius != 0f) args.add("cornerRadius = ${formatFloat(op.cornerRadius)}")
                if (!op.isFill) args.add("isFill = false")
                if (op.strokeWidth != 1f) args.add("strokeWidth = ${formatFloat(op.strokeWidth)}")
                "${indent}rect(${args.joinToString(", ")})\n"
            }
            is SduiCanvasOp.Circle -> {
                val args = mutableListOf<String>()
                args.add("color = \"${escapeString(op.color)}\"")
                args.add("centerX = ${formatExprOrNumber(op.centerX)}")
                args.add("centerY = ${formatExprOrNumber(op.centerY)}")
                args.add("radius = ${formatExprOrNumber(op.radius)}")
                if (!op.isFill) args.add("isFill = false")
                if (op.strokeWidth != 1f) args.add("strokeWidth = ${formatFloat(op.strokeWidth)}")
                "${indent}circle(${args.joinToString(", ")})\n"
            }
            is SduiCanvasOp.Line -> {
                val args = mutableListOf<String>()
                args.add("color = \"${escapeString(op.color)}\"")
                args.add("startX = ${formatExprOrNumber(op.startX)}")
                args.add("startY = ${formatExprOrNumber(op.startY)}")
                args.add("endX = ${formatExprOrNumber(op.endX)}")
                args.add("endY = ${formatExprOrNumber(op.endY)}")
                if (op.strokeWidth != 1f) args.add("strokeWidth = ${formatFloat(op.strokeWidth)}")
                "${indent}line(${args.joinToString(", ")})\n"
            }
            is SduiCanvasOp.Arc -> {
                val args = mutableListOf<String>()
                args.add("color = \"${escapeString(op.color)}\"")
                args.add("x = ${formatExprOrNumber(op.x)}")
                args.add("y = ${formatExprOrNumber(op.y)}")
                args.add("width = ${formatExprOrNumber(op.width)}")
                args.add("height = ${formatExprOrNumber(op.height)}")
                args.add("startAngle = ${formatExprOrNumber(op.startAngle)}")
                args.add("sweepAngle = ${formatExprOrNumber(op.sweepAngle)}")
                if (op.useCenter) args.add("useCenter = true")
                if (op.strokeWidth != 1f) args.add("strokeWidth = ${formatFloat(op.strokeWidth)}")
                "${indent}arc(${args.joinToString(", ")})\n"
            }
            is SduiCanvasOp.Rotate -> {
                val sb = StringBuilder()
                val args = mutableListOf<String>()
                args.add("degrees = ${formatExprOrNumber(op.degrees)}")
                if (op.pivotX != null) args.add("pivotX = ${formatExprOrNumber(op.pivotX)}")
                if (op.pivotY != null) args.add("pivotY = ${formatExprOrNumber(op.pivotY)}")
                sb.appendLine("${indent}rotate(${args.joinToString(", ")}) {")
                op.ops.forEach { sb.append(generateCanvasOp(it, indentLevel + 1)) }
                sb.appendLine("${indent}}")
                sb.toString()
            }
            is SduiCanvasOp.Translate -> {
                val sb = StringBuilder()
                val args = mutableListOf<String>()
                args.add("x = ${formatExprOrNumber(op.x)}")
                args.add("y = ${formatExprOrNumber(op.y)}")
                sb.appendLine("${indent}translate(${args.joinToString(", ")}) {")
                op.ops.forEach { sb.append(generateCanvasOp(it, indentLevel + 1)) }
                sb.appendLine("${indent}}")
                sb.toString()
            }
            is SduiCanvasOp.Unknown -> "${indent}// Unknown canvas op: ${op.rawType}\n"
        }
    }

    fun generateAnimationSpec(spec: SduiAnimationSpec): String {
        return when (spec) {
            is SduiAnimationSpec.Spring -> {
                val args = mutableListOf<String>()
                if (spec.dampingRatio != "mediumBouncy") args.add("dampingRatio = \"${escapeString(spec.dampingRatio)}\"")
                if (spec.stiffness != "medium") args.add("stiffness = \"${escapeString(spec.stiffness)}\"")
                "spring(${args.joinToString(", ")})"
            }
            is SduiAnimationSpec.Tween -> {
                val args = mutableListOf<String>()
                if (spec.durationMillis != 300) args.add("durationMillis = ${spec.durationMillis}")
                if (spec.delayMillis != 0) args.add("delayMillis = ${spec.delayMillis}")
                if (spec.easing != "fastOutSlowIn") args.add("easing = \"${escapeString(spec.easing)}\"")
                "tween(${args.joinToString(", ")})"
            }
            is SduiAnimationSpec.Keyframes -> "keyframes(durationMillis = ${spec.durationMillis})"
            is SduiAnimationSpec.Repeatable -> "repeatable(iterations = ${spec.iterations}, repeatMode = \"${escapeString(spec.repeatMode)}\", animation = ${generateAnimationSpec(spec.animation)})"
            is SduiAnimationSpec.Snap -> "snap(delayMillis = ${spec.delayMillis})"
        }
    }

    fun generateEnterTransition(t: SduiEnterTransition): String {
        val args = mutableListOf<String>()
        if (t.fadeIn) args.add("fadeIn = true")
        if (t.slideInVertically) args.add("slideInVertically = true")
        if (t.slideVerticalDirection != "down") args.add("slideVerticalDirection = \"${escapeString(t.slideVerticalDirection)}\"")
        if (t.slideInHorizontally) args.add("slideInHorizontally = true")
        if (t.expandVertically) args.add("expandVertically = true")
        if (t.scaleIn) args.add("scaleIn = true")
        return "enterTransition(${args.joinToString(", ")})"
    }

    fun generateExitTransition(t: SduiExitTransition): String {
        val args = mutableListOf<String>()
        if (t.fadeOut) args.add("fadeOut = true")
        if (t.slideOutVertically) args.add("slideOutVertically = true")
        if (t.slideOutHorizontally) args.add("slideOutHorizontally = true")
        if (t.shrinkVertically) args.add("shrinkVertically = true")
        if (t.scaleOut) args.add("scaleOut = true")
        return "exitTransition(${args.joinToString(", ")})"
    }

    private fun formatJsonElement(element: JsonElement, indentLevel: Int): String {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> "\"${escapeString(element.content)}\""
                    element.booleanOrNull != null -> element.content
                    element.longOrNull != null -> element.content
                    element.doubleOrNull != null -> element.content
                    else -> "\"${escapeString(element.content)}\""
                }
            }
            is JsonArray -> {
                val items = element.map { formatJsonElement(it, indentLevel) }
                "listOf(${items.joinToString(", ")})"
            }
            is JsonObject -> {
                val pairs = element.entries.map { (k, v) -> "\"${escapeString(k)}\" to ${formatJsonElement(v, indentLevel)}" }
                "mapOf(${pairs.joinToString(", ")})"
            }
            JsonNull -> "null"
        }
    }

    private fun formatFloat(f: Float): String {
        return if (f % 1.0f == 0.0f) "${f.toInt()}f" else "${f}f"
    }

    private fun formatExprOrNumber(expr: String): String {
        val num = expr.toFloatOrNull()
        return if (num != null) formatFloat(num) else "\"${escapeString(expr)}\""
    }

    private fun escapeString(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\$", "\\\$")
    }
}
