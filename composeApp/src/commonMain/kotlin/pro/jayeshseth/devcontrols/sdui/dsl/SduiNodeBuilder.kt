package pro.jayeshseth.devcontrols.sdui.dsl

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import pro.jayeshseth.devcontrols.sdui.model.SduiAction
import pro.jayeshseth.devcontrols.sdui.model.SduiEnterTransition
import pro.jayeshseth.devcontrols.sdui.model.SduiExitTransition
import pro.jayeshseth.devcontrols.sdui.model.SduiModifier
import pro.jayeshseth.devcontrols.sdui.model.SduiNode
import pro.jayeshseth.devcontrols.sdui.model.SduiNodeSerializer
import pro.jayeshseth.devcontrols.sdui.model.defaultSduiJson

/**
 * Resolves modifier lists combining explicit list and modifier builder block.
 */
internal fun resolveModifiers(
    modifiers: List<SduiModifier>,
    modifier: (SduiModifierBuilder.() -> Unit)?
): List<SduiModifier> {
    return if (modifier != null) {
        val b = SduiModifierBuilder()
        b.addAll(modifiers)
        b.modifier()
        b.build()
    } else {
        modifiers
    }
}

/**
 * Scope for declaring children nodes in container components.
 */
@SduiDsl
open class SduiChildrenBuilder {
    private val children = mutableListOf<SduiNode>()

    open fun add(node: SduiNode): SduiNode {
        children.add(node)
        return node
    }

    operator fun SduiNode.unaryPlus(): SduiNode = add(this)

    fun sduiColumn(
        id: String? = null,
        verticalArrangement: String = "top",
        spacingDp: Float = 0f,
        horizontalAlignment: String = "start",
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Column {
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.Column(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            verticalArrangement = verticalArrangement,
            spacingDp = spacingDp,
            horizontalAlignment = horizontalAlignment,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiColumn(
        id: String? = null,
        verticalArrangement: String = "top",
        spacingDp: Float = 0f,
        horizontalAlignment: String = "start",
        modifier: List<SduiModifier>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Column = sduiColumn(
        id = id,
        verticalArrangement = verticalArrangement,
        spacingDp = spacingDp,
        horizontalAlignment = horizontalAlignment,
        modifiers = modifier,
        content = content
    )

    fun sduiRow(
        id: String? = null,
        horizontalArrangement: String = "start",
        spacingDp: Float = 0f,
        verticalAlignment: String = "centerVertically",
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Row {
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.Row(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            horizontalArrangement = horizontalArrangement,
            spacingDp = spacingDp,
            verticalAlignment = verticalAlignment,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiRow(
        id: String? = null,
        horizontalArrangement: String = "start",
        spacingDp: Float = 0f,
        verticalAlignment: String = "centerVertically",
        modifier: List<SduiModifier>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Row = sduiRow(
        id = id,
        horizontalArrangement = horizontalArrangement,
        spacingDp = spacingDp,
        verticalAlignment = verticalAlignment,
        modifiers = modifier,
        content = content
    )

    fun sduiBox(
        id: String? = null,
        contentAlignment: String = "center",
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Box {
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.Box(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            contentAlignment = contentAlignment,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiBox(
        id: String? = null,
        contentAlignment: String = "center",
        modifier: List<SduiModifier>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Box = sduiBox(
        id = id,
        contentAlignment = contentAlignment,
        modifiers = modifier,
        content = content
    )

    fun sduiLazyColumn(
        id: String? = null,
        verticalArrangement: String = "top",
        spacingDp: Float = 0f,
        contentPaddingDp: Float = 0f,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.LazyColumn {
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.LazyColumn(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            verticalArrangement = verticalArrangement,
            spacingDp = spacingDp,
            contentPaddingDp = contentPaddingDp,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiLazyColumn(
        id: String? = null,
        verticalArrangement: String = "top",
        spacingDp: Float = 0f,
        contentPaddingDp: Float = 0f,
        modifier: List<SduiModifier>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.LazyColumn = sduiLazyColumn(
        id = id,
        verticalArrangement = verticalArrangement,
        spacingDp = spacingDp,
        contentPaddingDp = contentPaddingDp,
        modifiers = modifier,
        content = content
    )

    fun sduiLazyRow(
        id: String? = null,
        horizontalArrangement: String = "start",
        spacingDp: Float = 0f,
        contentPaddingDp: Float = 0f,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.LazyRow {
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.LazyRow(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            horizontalArrangement = horizontalArrangement,
            spacingDp = spacingDp,
            contentPaddingDp = contentPaddingDp,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiLazyRow(
        id: String? = null,
        horizontalArrangement: String = "start",
        spacingDp: Float = 0f,
        contentPaddingDp: Float = 0f,
        modifier: List<SduiModifier>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.LazyRow = sduiLazyRow(
        id = id,
        horizontalArrangement = horizontalArrangement,
        spacingDp = spacingDp,
        contentPaddingDp = contentPaddingDp,
        modifiers = modifier,
        content = content
    )

    fun sduiLazyGrid(
        id: String? = null,
        columns: Int = 2,
        spacingDp: Float = 8f,
        contentPaddingDp: Float = 0f,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.LazyVerticalGrid {
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.LazyVerticalGrid(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            columns = columns,
            spacingDp = spacingDp,
            contentPaddingDp = contentPaddingDp,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiLazyGrid(
        id: String? = null,
        columns: Int = 2,
        spacingDp: Float = 8f,
        contentPaddingDp: Float = 0f,
        modifier: List<SduiModifier>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.LazyVerticalGrid = sduiLazyGrid(
        id = id,
        columns = columns,
        spacingDp = spacingDp,
        contentPaddingDp = contentPaddingDp,
        modifiers = modifier,
        content = content
    )

    fun sduiCard(
        id: String? = null,
        elevation: Float = 2f,
        cornerRadius: Float = 12f,
        backgroundColor: String? = null,
        borderWidth: Float? = null,
        borderColor: String? = null,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        onClick: (SduiActionBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Card {
        val actions = onClick?.let { SduiActionBuilder().apply(it).build() } ?: emptyList()
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.Card(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            elevation = elevation,
            cornerRadius = cornerRadius,
            backgroundColor = backgroundColor,
            borderWidth = borderWidth,
            borderColor = borderColor,
            onClick = actions,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiCard(
        id: String? = null,
        elevation: Float = 2f,
        cornerRadius: Float = 12f,
        backgroundColor: String? = null,
        borderWidth: Float? = null,
        borderColor: String? = null,
        modifier: List<SduiModifier>,
        onClick: (SduiActionBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Card = sduiCard(
        id = id,
        elevation = elevation,
        cornerRadius = cornerRadius,
        backgroundColor = backgroundColor,
        borderWidth = borderWidth,
        borderColor = borderColor,
        modifiers = modifier,
        onClick = onClick,
        content = content
    )

    fun sduiCard(
        id: String? = null,
        elevation: Float = 2f,
        cornerRadius: Float = 12f,
        backgroundColor: String? = null,
        borderWidth: Float? = null,
        borderColor: String? = null,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        onClick: List<SduiAction>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Card {
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.Card(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            elevation = elevation,
            cornerRadius = cornerRadius,
            backgroundColor = backgroundColor,
            borderWidth = borderWidth,
            borderColor = borderColor,
            onClick = onClick,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiCard(
        id: String? = null,
        elevation: Float = 2f,
        cornerRadius: Float = 12f,
        backgroundColor: String? = null,
        borderWidth: Float? = null,
        borderColor: String? = null,
        modifier: List<SduiModifier>,
        onClick: List<SduiAction>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Card = sduiCard(
        id = id,
        elevation = elevation,
        cornerRadius = cornerRadius,
        backgroundColor = backgroundColor,
        borderWidth = borderWidth,
        borderColor = borderColor,
        modifiers = modifier,
        onClick = onClick,
        content = content
    )

    fun sduiButton(
        text: String? = null,
        id: String? = null,
        variant: String = "filled",
        enabledExpr: String? = null,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        onClick: (SduiActionBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Button {
        val actions = onClick?.let { SduiActionBuilder().apply(it).build() } ?: emptyList()
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.Button(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            text = text,
            onClick = actions,
            variant = variant,
            enabledExpr = enabledExpr,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiButton(
        text: String? = null,
        id: String? = null,
        variant: String = "filled",
        enabledExpr: String? = null,
        modifier: List<SduiModifier>,
        onClick: (SduiActionBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Button = sduiButton(
        text = text,
        id = id,
        variant = variant,
        enabledExpr = enabledExpr,
        modifiers = modifier,
        onClick = onClick,
        content = content
    )

    fun sduiButton(
        text: String? = null,
        id: String? = null,
        variant: String = "filled",
        enabledExpr: String? = null,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        onClick: List<SduiAction>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Button {
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.Button(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            text = text,
            onClick = onClick,
            variant = variant,
            enabledExpr = enabledExpr,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiButton(
        text: String? = null,
        id: String? = null,
        variant: String = "filled",
        enabledExpr: String? = null,
        modifier: List<SduiModifier>,
        onClick: List<SduiAction>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Button = sduiButton(
        text = text,
        id = id,
        variant = variant,
        enabledExpr = enabledExpr,
        modifiers = modifier,
        onClick = onClick,
        content = content
    )

    fun sduiText(
        text: String,
        id: String? = null,
        fontSize: Float = 14f,
        fontWeight: String = "normal",
        fontStyle: String = "normal",
        color: String? = null,
        textAlign: String = "start",
        maxLines: Int? = null,
        overflow: String = "clip",
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null
    ): SduiNode.Text {
        val node = SduiNode.Text(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            color = color,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow
        )
        add(node)
        return node
    }

    fun sduiText(
        text: String,
        id: String? = null,
        fontSize: Float = 14f,
        fontWeight: String = "normal",
        fontStyle: String = "normal",
        color: String? = null,
        textAlign: String = "start",
        maxLines: Int? = null,
        overflow: String = "clip",
        modifier: List<SduiModifier>
    ): SduiNode.Text = sduiText(
        text = text,
        id = id,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        modifiers = modifier
    )

    fun sduiImage(
        url: String = "",
        id: String? = null,
        contentDescription: String? = null,
        contentScale: String = "fit",
        vectorIcon: String? = null,
        tintColor: String? = null,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null
    ): SduiNode.Image {
        val node = SduiNode.Image(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            url = url,
            contentDescription = contentDescription,
            contentScale = contentScale,
            vectorIcon = vectorIcon,
            tintColor = tintColor
        )
        add(node)
        return node
    }

    fun sduiImage(
        url: String = "",
        id: String? = null,
        contentDescription: String? = null,
        contentScale: String = "fit",
        vectorIcon: String? = null,
        tintColor: String? = null,
        modifier: List<SduiModifier>
    ): SduiNode.Image = sduiImage(
        url = url,
        id = id,
        contentDescription = contentDescription,
        contentScale = contentScale,
        vectorIcon = vectorIcon,
        tintColor = tintColor,
        modifiers = modifier
    )

    fun sduiTextField(
        valueStateKey: String,
        id: String? = null,
        label: String? = null,
        placeholder: String? = null,
        isPassword: Boolean = false,
        keyboardType: String = "text",
        maxLines: Int = 1,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null
    ): SduiNode.TextField {
        val node = SduiNode.TextField(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            valueStateKey = valueStateKey,
            label = label,
            placeholder = placeholder,
            isPassword = isPassword,
            keyboardType = keyboardType,
            maxLines = maxLines
        )
        add(node)
        return node
    }

    fun sduiTextField(
        valueStateKey: String,
        id: String? = null,
        label: String? = null,
        placeholder: String? = null,
        isPassword: Boolean = false,
        keyboardType: String = "text",
        maxLines: Int = 1,
        modifier: List<SduiModifier>
    ): SduiNode.TextField = sduiTextField(
        valueStateKey = valueStateKey,
        id = id,
        label = label,
        placeholder = placeholder,
        isPassword = isPassword,
        keyboardType = keyboardType,
        maxLines = maxLines,
        modifiers = modifier
    )

    fun sduiSpacer(
        width: Float = 0f,
        height: Float = 0f,
        id: String? = null,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null
    ): SduiNode.Spacer {
        val node = SduiNode.Spacer(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            width = width,
            height = height
        )
        add(node)
        return node
    }

    fun sduiSpacer(
        width: Float = 0f,
        height: Float = 0f,
        id: String? = null,
        modifier: List<SduiModifier>
    ): SduiNode.Spacer = sduiSpacer(
        width = width,
        height = height,
        id = id,
        modifiers = modifier
    )

    fun sduiCanvas(
        id: String? = null,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        operations: SduiCanvasBuilder.() -> Unit
    ): SduiNode.Canvas {
        val ops = SduiCanvasBuilder().apply(operations).build()
        val node = SduiNode.Canvas(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            operations = ops
        )
        add(node)
        return node
    }

    fun sduiCanvas(
        id: String? = null,
        modifier: List<SduiModifier>,
        operations: SduiCanvasBuilder.() -> Unit
    ): SduiNode.Canvas = sduiCanvas(
        id = id,
        modifiers = modifier,
        operations = operations
    )

    fun sduiAnimatedVisibility(
        visibleExpr: String = "true",
        id: String? = null,
        enter: SduiEnterTransition = enterTransition(fadeIn = true),
        exit: SduiExitTransition = exitTransition(fadeOut = true),
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit
    ): SduiNode.AnimatedVisibility {
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val singleChild = when {
            childNodes.isEmpty() -> SduiNode.Spacer()
            childNodes.size == 1 -> childNodes.first()
            else -> SduiNode.Column(children = childNodes)
        }
        val node = SduiNode.AnimatedVisibility(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            visibleExpr = visibleExpr,
            enter = enter,
            exit = exit,
            child = singleChild
        )
        add(node)
        return node
    }

    fun sduiAnimatedVisibility(
        visibleExpr: String = "true",
        id: String? = null,
        enter: SduiEnterTransition = enterTransition(fadeIn = true),
        exit: SduiExitTransition = exitTransition(fadeOut = true),
        modifier: List<SduiModifier>,
        content: SduiChildrenBuilder.() -> Unit
    ): SduiNode.AnimatedVisibility = sduiAnimatedVisibility(
        visibleExpr = visibleExpr,
        id = id,
        enter = enter,
        exit = exit,
        modifiers = modifier,
        content = content
    )

    fun sduiAnimatedVisibility(
        child: SduiNode,
        visibleExpr: String = "true",
        id: String? = null,
        enter: SduiEnterTransition = enterTransition(fadeIn = true),
        exit: SduiExitTransition = exitTransition(fadeOut = true),
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null
    ): SduiNode.AnimatedVisibility {
        val node = SduiNode.AnimatedVisibility(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            visibleExpr = visibleExpr,
            enter = enter,
            exit = exit,
            child = child
        )
        add(node)
        return node
    }

    fun sduiAnimatedVisibility(
        child: SduiNode,
        visibleExpr: String = "true",
        id: String? = null,
        enter: SduiEnterTransition = enterTransition(fadeIn = true),
        exit: SduiExitTransition = exitTransition(fadeOut = true),
        modifier: List<SduiModifier>
    ): SduiNode.AnimatedVisibility = sduiAnimatedVisibility(
        child = child,
        visibleExpr = visibleExpr,
        id = id,
        enter = enter,
        exit = exit,
        modifiers = modifier
    )

    fun sduiCustom(
        componentName: String,
        id: String? = null,
        props: Map<String, String> = emptyMap(),
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Custom {
        val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
        val node = SduiNode.Custom(
            id = id,
            modifiers = resolveModifiers(modifiers, modifier),
            componentName = componentName,
            props = props,
            children = childNodes
        )
        add(node)
        return node
    }

    fun sduiCustom(
        componentName: String,
        id: String? = null,
        props: Map<String, String> = emptyMap(),
        modifier: List<SduiModifier>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Custom = sduiCustom(
        componentName = componentName,
        id = id,
        props = props,
        modifiers = modifier,
        content = content
    )

    fun sduiCustom(
        componentName: String,
        vararg props: Pair<String, String>,
        id: String? = null,
        modifiers: List<SduiModifier> = emptyList(),
        modifier: (SduiModifierBuilder.() -> Unit)? = null,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Custom = sduiCustom(
        componentName = componentName,
        id = id,
        props = props.toMap(),
        modifiers = modifiers,
        modifier = modifier,
        content = content
    )

    fun sduiCustom(
        componentName: String,
        vararg props: Pair<String, String>,
        id: String? = null,
        modifier: List<SduiModifier>,
        content: SduiChildrenBuilder.() -> Unit = {}
    ): SduiNode.Custom = sduiCustom(
        componentName = componentName,
        id = id,
        props = props.toMap(),
        modifiers = modifier,
        content = content
    )

    open fun buildNodes(): List<SduiNode> = children.toList()
}

// --- Standalone top-level builders for modular subtrees ---

fun sduiColumn(
    id: String? = null,
    verticalArrangement: String = "top",
    spacingDp: Float = 0f,
    horizontalAlignment: String = "start",
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Column {
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.Column(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        verticalArrangement = verticalArrangement,
        spacingDp = spacingDp,
        horizontalAlignment = horizontalAlignment,
        children = childNodes
    )
}

fun sduiColumn(
    id: String? = null,
    verticalArrangement: String = "top",
    spacingDp: Float = 0f,
    horizontalAlignment: String = "start",
    modifier: List<SduiModifier>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Column = sduiColumn(
    id = id,
    verticalArrangement = verticalArrangement,
    spacingDp = spacingDp,
    horizontalAlignment = horizontalAlignment,
    modifiers = modifier,
    content = content
)

fun sduiRow(
    id: String? = null,
    horizontalArrangement: String = "start",
    spacingDp: Float = 0f,
    verticalAlignment: String = "centerVertically",
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Row {
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.Row(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        horizontalArrangement = horizontalArrangement,
        spacingDp = spacingDp,
        verticalAlignment = verticalAlignment,
        children = childNodes
    )
}

fun sduiRow(
    id: String? = null,
    horizontalArrangement: String = "start",
    spacingDp: Float = 0f,
    verticalAlignment: String = "centerVertically",
    modifier: List<SduiModifier>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Row = sduiRow(
    id = id,
    horizontalArrangement = horizontalArrangement,
    spacingDp = spacingDp,
    verticalAlignment = verticalAlignment,
    modifiers = modifier,
    content = content
)

fun sduiBox(
    id: String? = null,
    contentAlignment: String = "center",
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Box {
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.Box(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        contentAlignment = contentAlignment,
        children = childNodes
    )
}

fun sduiBox(
    id: String? = null,
    contentAlignment: String = "center",
    modifier: List<SduiModifier>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Box = sduiBox(
    id = id,
    contentAlignment = contentAlignment,
    modifiers = modifier,
    content = content
)

fun sduiLazyColumn(
    id: String? = null,
    verticalArrangement: String = "top",
    spacingDp: Float = 0f,
    contentPaddingDp: Float = 0f,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.LazyColumn {
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.LazyColumn(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        verticalArrangement = verticalArrangement,
        spacingDp = spacingDp,
        contentPaddingDp = contentPaddingDp,
        children = childNodes
    )
}

fun sduiLazyColumn(
    id: String? = null,
    verticalArrangement: String = "top",
    spacingDp: Float = 0f,
    contentPaddingDp: Float = 0f,
    modifier: List<SduiModifier>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.LazyColumn = sduiLazyColumn(
    id = id,
    verticalArrangement = verticalArrangement,
    spacingDp = spacingDp,
    contentPaddingDp = contentPaddingDp,
    modifiers = modifier,
    content = content
)

fun sduiLazyRow(
    id: String? = null,
    horizontalArrangement: String = "start",
    spacingDp: Float = 0f,
    contentPaddingDp: Float = 0f,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.LazyRow {
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.LazyRow(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        horizontalArrangement = horizontalArrangement,
        spacingDp = spacingDp,
        contentPaddingDp = contentPaddingDp,
        children = childNodes
    )
}

fun sduiLazyRow(
    id: String? = null,
    horizontalArrangement: String = "start",
    spacingDp: Float = 0f,
    contentPaddingDp: Float = 0f,
    modifier: List<SduiModifier>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.LazyRow = sduiLazyRow(
    id = id,
    horizontalArrangement = horizontalArrangement,
    spacingDp = spacingDp,
    contentPaddingDp = contentPaddingDp,
    modifiers = modifier,
    content = content
)

fun sduiLazyGrid(
    id: String? = null,
    columns: Int = 2,
    spacingDp: Float = 8f,
    contentPaddingDp: Float = 0f,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.LazyVerticalGrid {
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.LazyVerticalGrid(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        columns = columns,
        spacingDp = spacingDp,
        contentPaddingDp = contentPaddingDp,
        children = childNodes
    )
}

fun sduiLazyGrid(
    id: String? = null,
    columns: Int = 2,
    spacingDp: Float = 8f,
    contentPaddingDp: Float = 0f,
    modifier: List<SduiModifier>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.LazyVerticalGrid = sduiLazyGrid(
    id = id,
    columns = columns,
    spacingDp = spacingDp,
    contentPaddingDp = contentPaddingDp,
    modifiers = modifier,
    content = content
)

fun sduiCard(
    id: String? = null,
    elevation: Float = 2f,
    cornerRadius: Float = 12f,
    backgroundColor: String? = null,
    borderWidth: Float? = null,
    borderColor: String? = null,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    onClick: (SduiActionBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Card {
    val actions = onClick?.let { SduiActionBuilder().apply(it).build() } ?: emptyList()
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.Card(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        elevation = elevation,
        cornerRadius = cornerRadius,
        backgroundColor = backgroundColor,
        borderWidth = borderWidth,
        borderColor = borderColor,
        onClick = actions,
        children = childNodes
    )
}

fun sduiCard(
    id: String? = null,
    elevation: Float = 2f,
    cornerRadius: Float = 12f,
    backgroundColor: String? = null,
    borderWidth: Float? = null,
    borderColor: String? = null,
    modifier: List<SduiModifier>,
    onClick: (SduiActionBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Card = sduiCard(
    id = id,
    elevation = elevation,
    cornerRadius = cornerRadius,
    backgroundColor = backgroundColor,
    borderWidth = borderWidth,
    borderColor = borderColor,
    modifiers = modifier,
    onClick = onClick,
    content = content
)

fun sduiCard(
    id: String? = null,
    elevation: Float = 2f,
    cornerRadius: Float = 12f,
    backgroundColor: String? = null,
    borderWidth: Float? = null,
    borderColor: String? = null,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    onClick: List<SduiAction>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Card {
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.Card(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        elevation = elevation,
        cornerRadius = cornerRadius,
        backgroundColor = backgroundColor,
        borderWidth = borderWidth,
        borderColor = borderColor,
        onClick = onClick,
        children = childNodes
    )
}

fun sduiCard(
    id: String? = null,
    elevation: Float = 2f,
    cornerRadius: Float = 12f,
    backgroundColor: String? = null,
    borderWidth: Float? = null,
    borderColor: String? = null,
    modifier: List<SduiModifier>,
    onClick: List<SduiAction>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Card = sduiCard(
    id = id,
    elevation = elevation,
    cornerRadius = cornerRadius,
    backgroundColor = backgroundColor,
    borderWidth = borderWidth,
    borderColor = borderColor,
    modifiers = modifier,
    onClick = onClick,
    content = content
)

fun sduiButton(
    text: String? = null,
    id: String? = null,
    variant: String = "filled",
    enabledExpr: String? = null,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    onClick: (SduiActionBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Button {
    val actions = onClick?.let { SduiActionBuilder().apply(it).build() } ?: emptyList()
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.Button(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        text = text,
        onClick = actions,
        variant = variant,
        enabledExpr = enabledExpr,
        children = childNodes
    )
}

fun sduiButton(
    text: String? = null,
    id: String? = null,
    variant: String = "filled",
    enabledExpr: String? = null,
    modifier: List<SduiModifier>,
    onClick: (SduiActionBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Button = sduiButton(
    text = text,
    id = id,
    variant = variant,
    enabledExpr = enabledExpr,
    modifiers = modifier,
    onClick = onClick,
    content = content
)

fun sduiButton(
    text: String? = null,
    id: String? = null,
    variant: String = "filled",
    enabledExpr: String? = null,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    onClick: List<SduiAction>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Button {
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.Button(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        text = text,
        onClick = onClick,
        variant = variant,
        enabledExpr = enabledExpr,
        children = childNodes
    )
}

fun sduiButton(
    text: String? = null,
    id: String? = null,
    variant: String = "filled",
    enabledExpr: String? = null,
    modifier: List<SduiModifier>,
    onClick: List<SduiAction>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Button = sduiButton(
    text = text,
    id = id,
    variant = variant,
    enabledExpr = enabledExpr,
    modifiers = modifier,
    onClick = onClick,
    content = content
)

fun sduiText(
    text: String,
    id: String? = null,
    fontSize: Float = 14f,
    fontWeight: String = "normal",
    fontStyle: String = "normal",
    color: String? = null,
    textAlign: String = "start",
    maxLines: Int? = null,
    overflow: String = "clip",
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null
): SduiNode.Text = SduiNode.Text(
    id = id,
    modifiers = resolveModifiers(modifiers, modifier),
    text = text,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    color = color,
    textAlign = textAlign,
    maxLines = maxLines,
    overflow = overflow
)

fun sduiText(
    text: String,
    id: String? = null,
    fontSize: Float = 14f,
    fontWeight: String = "normal",
    fontStyle: String = "normal",
    color: String? = null,
    textAlign: String = "start",
    maxLines: Int? = null,
    overflow: String = "clip",
    modifier: List<SduiModifier>
): SduiNode.Text = sduiText(
    text = text,
    id = id,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    color = color,
    textAlign = textAlign,
    maxLines = maxLines,
    overflow = overflow,
    modifiers = modifier
)

fun sduiImage(
    url: String = "",
    id: String? = null,
    contentDescription: String? = null,
    contentScale: String = "fit",
    vectorIcon: String? = null,
    tintColor: String? = null,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null
): SduiNode.Image = SduiNode.Image(
    id = id,
    modifiers = resolveModifiers(modifiers, modifier),
    url = url,
    contentDescription = contentDescription,
    contentScale = contentScale,
    vectorIcon = vectorIcon,
    tintColor = tintColor
)

fun sduiImage(
    url: String = "",
    id: String? = null,
    contentDescription: String? = null,
    contentScale: String = "fit",
    vectorIcon: String? = null,
    tintColor: String? = null,
    modifier: List<SduiModifier>
): SduiNode.Image = sduiImage(
    url = url,
    id = id,
    contentDescription = contentDescription,
    contentScale = contentScale,
    vectorIcon = vectorIcon,
    tintColor = tintColor,
    modifiers = modifier
)

fun sduiTextField(
    valueStateKey: String,
    id: String? = null,
    label: String? = null,
    placeholder: String? = null,
    isPassword: Boolean = false,
    keyboardType: String = "text",
    maxLines: Int = 1,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null
): SduiNode.TextField = SduiNode.TextField(
    id = id,
    modifiers = resolveModifiers(modifiers, modifier),
    valueStateKey = valueStateKey,
    label = label,
    placeholder = placeholder,
    isPassword = isPassword,
    keyboardType = keyboardType,
    maxLines = maxLines
)

fun sduiTextField(
    valueStateKey: String,
    id: String? = null,
    label: String? = null,
    placeholder: String? = null,
    isPassword: Boolean = false,
    keyboardType: String = "text",
    maxLines: Int = 1,
    modifier: List<SduiModifier>
): SduiNode.TextField = sduiTextField(
    valueStateKey = valueStateKey,
    id = id,
    label = label,
    placeholder = placeholder,
    isPassword = isPassword,
    keyboardType = keyboardType,
    maxLines = maxLines,
    modifiers = modifier
)

fun sduiSpacer(
    width: Float = 0f,
    height: Float = 0f,
    id: String? = null,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null
): SduiNode.Spacer = SduiNode.Spacer(
    id = id,
    modifiers = resolveModifiers(modifiers, modifier),
    width = width,
    height = height
)

fun sduiSpacer(
    width: Float = 0f,
    height: Float = 0f,
    id: String? = null,
    modifier: List<SduiModifier>
): SduiNode.Spacer = sduiSpacer(
    width = width,
    height = height,
    id = id,
    modifiers = modifier
)

fun sduiCanvas(
    id: String? = null,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    operations: SduiCanvasBuilder.() -> Unit
): SduiNode.Canvas = SduiNode.Canvas(
    id = id,
    modifiers = resolveModifiers(modifiers, modifier),
    operations = SduiCanvasBuilder().apply(operations).build()
)

fun sduiCanvas(
    id: String? = null,
    modifier: List<SduiModifier>,
    operations: SduiCanvasBuilder.() -> Unit
): SduiNode.Canvas = sduiCanvas(
    id = id,
    modifiers = modifier,
    operations = operations
)

fun sduiAnimatedVisibility(
    visibleExpr: String = "true",
    id: String? = null,
    enter: SduiEnterTransition = enterTransition(fadeIn = true),
    exit: SduiExitTransition = exitTransition(fadeOut = true),
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit
): SduiNode.AnimatedVisibility {
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    val singleChild = when {
        childNodes.isEmpty() -> SduiNode.Spacer()
        childNodes.size == 1 -> childNodes.first()
        else -> SduiNode.Column(children = childNodes)
    }
    return SduiNode.AnimatedVisibility(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        visibleExpr = visibleExpr,
        enter = enter,
        exit = exit,
        child = singleChild
    )
}

fun sduiAnimatedVisibility(
    visibleExpr: String = "true",
    id: String? = null,
    enter: SduiEnterTransition = enterTransition(fadeIn = true),
    exit: SduiExitTransition = exitTransition(fadeOut = true),
    modifier: List<SduiModifier>,
    content: SduiChildrenBuilder.() -> Unit
): SduiNode.AnimatedVisibility = sduiAnimatedVisibility(
    visibleExpr = visibleExpr,
    id = id,
    enter = enter,
    exit = exit,
    modifiers = modifier,
    content = content
)

fun sduiAnimatedVisibility(
    child: SduiNode,
    visibleExpr: String = "true",
    id: String? = null,
    enter: SduiEnterTransition = enterTransition(fadeIn = true),
    exit: SduiExitTransition = exitTransition(fadeOut = true),
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null
): SduiNode.AnimatedVisibility = SduiNode.AnimatedVisibility(
    id = id,
    modifiers = resolveModifiers(modifiers, modifier),
    visibleExpr = visibleExpr,
    enter = enter,
    exit = exit,
    child = child
)

fun sduiAnimatedVisibility(
    child: SduiNode,
    visibleExpr: String = "true",
    id: String? = null,
    enter: SduiEnterTransition = enterTransition(fadeIn = true),
    exit: SduiExitTransition = exitTransition(fadeOut = true),
    modifier: List<SduiModifier>
): SduiNode.AnimatedVisibility = sduiAnimatedVisibility(
    child = child,
    visibleExpr = visibleExpr,
    id = id,
    enter = enter,
    exit = exit,
    modifiers = modifier
)

fun sduiCustom(
    componentName: String,
    id: String? = null,
    props: Map<String, String> = emptyMap(),
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Custom {
    val childNodes = SduiChildrenBuilder().apply(content).buildNodes()
    return SduiNode.Custom(
        id = id,
        modifiers = resolveModifiers(modifiers, modifier),
        componentName = componentName,
        props = props,
        children = childNodes
    )
}

fun sduiCustom(
    componentName: String,
    id: String? = null,
    props: Map<String, String> = emptyMap(),
    modifier: List<SduiModifier>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Custom = sduiCustom(
    componentName = componentName,
    id = id,
    props = props,
    modifiers = modifier,
    content = content
)

fun sduiCustom(
    componentName: String,
    vararg props: Pair<String, String>,
    id: String? = null,
    modifiers: List<SduiModifier> = emptyList(),
    modifier: (SduiModifierBuilder.() -> Unit)? = null,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Custom = sduiCustom(
    componentName = componentName,
    id = id,
    props = props.toMap(),
    modifiers = modifiers,
    modifier = modifier,
    content = content
)

fun sduiCustom(
    componentName: String,
    vararg props: Pair<String, String>,
    id: String? = null,
    modifier: List<SduiModifier>,
    content: SduiChildrenBuilder.() -> Unit = {}
): SduiNode.Custom = sduiCustom(
    componentName = componentName,
    id = id,
    props = props.toMap(),
    modifiers = modifier,
    content = content
)

/**
 * Serializes a list of [SduiNode] into a JSON string.
 */
fun List<SduiNode>.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(ListSerializer(SduiNodeSerializer), this)

/**
 * Serializes a list of [SduiNode] into a [JsonElement].
 */
fun List<SduiNode>.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    json.encodeToJsonElement(ListSerializer(SduiNodeSerializer), this)

/**
 * Serializes a single [SduiNode] into a JSON string.
 */
fun SduiNode.toJson(json: Json = defaultSduiJson): String =
    json.encodeToString(SduiNodeSerializer, this)

/**
 * Serializes a single [SduiNode] into a [JsonElement].
 */
fun SduiNode.toJsonElement(json: Json = defaultSduiJson): JsonElement =
    json.encodeToJsonElement(SduiNodeSerializer, this)
