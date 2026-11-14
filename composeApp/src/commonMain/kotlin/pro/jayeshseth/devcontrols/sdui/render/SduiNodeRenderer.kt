package pro.jayeshseth.devcontrols.sdui.render

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.devcontrols.sdui.model.SduiEnterTransition
import pro.jayeshseth.devcontrols.sdui.model.SduiExitTransition
import pro.jayeshseth.devcontrols.sdui.model.SduiNode

/**
 * Recursive Composable renderer for SDUI AST nodes.
 */
@Composable
fun SduiNodeRenderer(node: SduiNode) {
    val context = currentSduiContext()
    val store = context.store
    val modifier = SduiModifierRenderer.render(
        baseModifier = Modifier,
        modifiers = node.modifiers,
        context = context
    )

    when (node) {
        is SduiNode.Box -> {
            Box(
                modifier = modifier,
                contentAlignment = parseBoxAlignment(node.contentAlignment)
            ) {
                node.children.forEach { child ->
                    SduiNodeRenderer(child)
                }
            }
        }
        is SduiNode.Column -> {
            val verticalArrangement = if (node.spacingDp > 0f) {
                val align = when (node.verticalArrangement.lowercase()) {
                    "bottom" -> Alignment.Bottom
                    "center" -> Alignment.CenterVertically
                    else -> Alignment.Top
                }
                Arrangement.spacedBy(node.spacingDp.dp, align)
            } else {
                parseVerticalArrangement(node.verticalArrangement)
            }
            Column(
                modifier = modifier,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = parseHorizontalAlignment(node.horizontalAlignment)
            ) {
                node.children.forEach { child ->
                    SduiNodeRenderer(child)
                }
            }
        }
        is SduiNode.Row -> {
            val horizontalArrangement = if (node.spacingDp > 0f) {
                val align = when (node.horizontalArrangement.lowercase()) {
                    "end" -> Alignment.End
                    "center" -> Alignment.CenterHorizontally
                    else -> Alignment.Start
                }
                Arrangement.spacedBy(node.spacingDp.dp, align)
            } else {
                parseHorizontalArrangement(node.horizontalArrangement)
            }
            Row(
                modifier = modifier,
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = parseRowVerticalAlignment(node.verticalAlignment)
            ) {
                node.children.forEach { child ->
                    SduiNodeRenderer(child)
                }
            }
        }
        is SduiNode.LazyColumn -> {
            val arrangement = if (node.spacingDp > 0f) {
                val align = when (node.verticalArrangement.lowercase()) {
                    "bottom" -> Alignment.Bottom
                    "center" -> Alignment.CenterVertically
                    else -> Alignment.Top
                }
                Arrangement.spacedBy(node.spacingDp.dp, align)
            } else {
                parseVerticalArrangement(node.verticalArrangement)
            }
            LazyColumn(
                modifier = modifier,
                verticalArrangement = arrangement,
                contentPadding = PaddingValues(node.contentPaddingDp.dp)
            ) {
                items(node.children, key = { it.id ?: it.hashCode().toString() }) { child ->
                    SduiNodeRenderer(child)
                }
            }
        }
        is SduiNode.LazyRow -> {
            val arrangement = if (node.spacingDp > 0f) {
                val align = when (node.horizontalArrangement.lowercase()) {
                    "end" -> Alignment.End
                    "center" -> Alignment.CenterHorizontally
                    else -> Alignment.Start
                }
                Arrangement.spacedBy(node.spacingDp.dp, align)
            } else {
                parseHorizontalArrangement(node.horizontalArrangement)
            }
            LazyRow(
                modifier = modifier,
                horizontalArrangement = arrangement,
                contentPadding = PaddingValues(node.contentPaddingDp.dp)
            ) {
                items(node.children, key = { it.id ?: it.hashCode().toString() }) { child ->
                    SduiNodeRenderer(child)
                }
            }
        }
        is SduiNode.LazyVerticalGrid -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(node.columns.coerceAtLeast(1)),
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(node.spacingDp.dp),
                horizontalArrangement = Arrangement.spacedBy(node.spacingDp.dp),
                contentPadding = PaddingValues(node.contentPaddingDp.dp)
            ) {
                gridItems(node.children, key = { it.id ?: it.hashCode().toString() }) { child ->
                    SduiNodeRenderer(child)
                }
            }
        }
        is SduiNode.Text -> {
            val resolvedText = store.evalString(node.text)
            val color = node.color?.let { SduiColorParser.parse(store.evalString(it), Color.Unspecified) }
                ?: Color.Unspecified
            Text(
                text = resolvedText,
                modifier = modifier,
                color = color,
                fontSize = node.fontSize.sp,
                fontWeight = parseFontWeight(node.fontWeight),
                fontStyle = parseFontStyle(node.fontStyle),
                textAlign = parseTextAlign(node.textAlign),
                maxLines = node.maxLines ?: Int.MAX_VALUE,
                overflow = parseTextOverflow(node.overflow)
            )
        }
        is SduiNode.Image -> {
            // Render either vector icon or network/canvas fallback
            if (node.vectorIcon != null) {
                RenderVectorIcon(
                    iconName = node.vectorIcon,
                    tint = node.tintColor?.let { SduiColorParser.parse(store.evalString(it), MaterialTheme.colorScheme.primary) }
                        ?: MaterialTheme.colorScheme.primary,
                    modifier = modifier
                )
            } else {
                // Placeholder image representation
                Box(
                    modifier = modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(Color.LightGray.copy(alpha = 0.5f))
                        drawLine(Color.Gray, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = 2f)
                    }
                }
            }
        }
        is SduiNode.Canvas -> {
            Canvas(modifier = modifier) {
                SduiCanvasRenderer.draw(this, node.operations, store)
            }
        }
        is SduiNode.Spacer -> {
            if (node.width > 0f && node.height > 0f) {
                Spacer(modifier = modifier.size(node.width.dp, node.height.dp))
            } else if (node.width > 0f) {
                Spacer(modifier = modifier.width(node.width.dp))
            } else if (node.height > 0f) {
                Spacer(modifier = modifier.height(node.height.dp))
            } else {
                Spacer(modifier = modifier)
            }
        }
        is SduiNode.TextField -> {
            val textValue = store.get(node.valueStateKey)?.toString() ?: ""
            val labelText = node.label?.let { store.evalString(it) }
            val placeholderText = node.placeholder?.let { store.evalString(it) }
            OutlinedTextField(
                value = textValue,
                onValueChange = { store.set(node.valueStateKey, it) },
                modifier = modifier,
                label = labelText?.let { { Text(it) } },
                placeholder = placeholderText?.let { { Text(it) } },
                visualTransformation = if (node.isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = parseKeyboardType(node.keyboardType)),
                maxLines = node.maxLines
            )
        }
        is SduiNode.Button -> {
            val isEnabled = node.enabledExpr?.let { store.evalBoolean(it, true) } ?: true
            val onClick = { context.actionRunner.execute(node.onClick) }
            val resolvedVariant = store.evalString(node.variant, "filled").lowercase()
            when (resolvedVariant) {
                "outlined" -> {
                    OutlinedButton(onClick = onClick, enabled = isEnabled, modifier = modifier) {
                        RenderButtonContent(node)
                    }
                }
                "elevated" -> {
                    ElevatedButton(onClick = onClick, enabled = isEnabled, modifier = modifier) {
                        RenderButtonContent(node)
                    }
                }
                "text" -> {
                    TextButton(onClick = onClick, enabled = isEnabled, modifier = modifier) {
                        RenderButtonContent(node)
                    }
                }
                else -> {
                    Button(onClick = onClick, enabled = isEnabled, modifier = modifier) {
                        RenderButtonContent(node)
                    }
                }
            }
        }
        is SduiNode.Card -> {
            val bg = node.backgroundColor?.let { SduiColorParser.parse(store.evalString(it), MaterialTheme.colorScheme.surface) }
                ?: MaterialTheme.colorScheme.surface
            val borderModifier = if (node.borderWidth != null && node.borderWidth > 0f && node.borderColor != null) {
                Modifier.border(
                    node.borderWidth.dp,
                    SduiColorParser.parse(store.evalString(node.borderColor), Color.Gray),
                    RoundedCornerShape(node.cornerRadius.dp)
                )
            } else {
                Modifier
            }

            Card(
                onClick = { context.actionRunner.execute(node.onClick) },
                enabled = node.onClick.isNotEmpty(),
                modifier = modifier.then(borderModifier),
                shape = RoundedCornerShape(node.cornerRadius.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = node.elevation.dp),
                colors = CardDefaults.cardColors(containerColor = bg)
            ) {
                node.children.forEach { child ->
                    SduiNodeRenderer(child)
                }
            }
        }
        is SduiNode.AnimatedVisibility -> {
            val isVisible = store.evalBoolean(node.visibleExpr, true)
            AnimatedVisibility(
                visible = isVisible,
                modifier = modifier,
                enter = buildEnterTransition(node.enter),
                exit = buildExitTransition(node.exit)
            ) {
                SduiNodeRenderer(node.child)
            }
        }
        is SduiNode.Custom -> {
            context.componentRegistry.renderCustom(node, context)
        }
        is SduiNode.Unknown -> {
            Box(
                modifier = modifier
                    .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                Text(
                    text = "Unknown: ${node.rawType}",
                    fontSize = 10.sp,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
private fun RenderButtonContent(node: SduiNode.Button) {
    if (node.text != null) {
        val label = currentSduiContext().store.evalString(node.text)
        Text(text = label)
    } else {
        node.children.forEach { child ->
            SduiNodeRenderer(child)
        }
    }
}

@Composable
private fun RenderVectorIcon(iconName: String, tint: Color, modifier: Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        when (iconName.lowercase()) {
            "star" -> {
                // Draw 5-pointed star
                val cx = w / 2f
                val cy = h / 2f
                val r = w * 0.45f
                drawLine(tint, Offset(cx, cy - r), Offset(cx + r * 0.6f, cy + r * 0.7f), strokeWidth = 3f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx + r * 0.6f, cy + r * 0.7f), Offset(cx - r * 0.8f, cy - r * 0.2f), strokeWidth = 3f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx - r * 0.8f, cy - r * 0.2f), Offset(cx + r * 0.8f, cy - r * 0.2f), strokeWidth = 3f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx + r * 0.8f, cy - r * 0.2f), Offset(cx - r * 0.6f, cy + r * 0.7f), strokeWidth = 3f, cap = StrokeCap.Round)
                drawLine(tint, Offset(cx - r * 0.6f, cy + r * 0.7f), Offset(cx, cy - r), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            "heart" -> {
                drawCircle(tint, radius = w * 0.22f, center = Offset(w * 0.35f, h * 0.38f))
                drawCircle(tint, radius = w * 0.22f, center = Offset(w * 0.65f, h * 0.38f))
                drawLine(tint, Offset(w * 0.15f, h * 0.45f), Offset(w * 0.5f, h * 0.85f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.85f, h * 0.45f), Offset(w * 0.5f, h * 0.85f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            "check" -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.45f, h * 0.75f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.45f, h * 0.75f), Offset(w * 0.85f, h * 0.25f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            "close" -> {
                drawLine(tint, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.25f, h * 0.75f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            "arrowback" -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), strokeWidth = 3f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.45f, h * 0.25f), strokeWidth = 3f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.45f, h * 0.75f), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            "refresh" -> {
                drawArc(tint, startAngle = 30f, sweepAngle = 290f, useCenter = false, topLeft = Offset(w * 0.2f, h * 0.2f), size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f, cap = StrokeCap.Round))
                drawLine(tint, Offset(w * 0.7f, h * 0.2f), Offset(w * 0.85f, h * 0.35f), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            "search" -> {
                drawCircle(tint, radius = w * 0.25f, center = Offset(w * 0.4f, h * 0.4f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                drawLine(tint, Offset(w * 0.6f, h * 0.6f), Offset(w * 0.85f, h * 0.85f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            else -> {
                drawCircle(tint, radius = w * 0.35f, center = Offset(w / 2f, h / 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
            }
        }
    }
}

private fun buildEnterTransition(t: SduiEnterTransition): EnterTransition {
    var enter: EnterTransition = EnterTransition.None
    if (t.fadeIn) enter += fadeIn(animationSpec = tween(t.fadeDurationMillis))
    if (t.slideInVertically) {
        val dir = if (t.slideVerticalDirection.lowercase() == "down") -1 else 1
        enter += slideInVertically { fullHeight -> dir * fullHeight }
    }
    if (t.slideInHorizontally) {
        val dir = if (t.slideHorizontalDirection.lowercase() == "start") -1 else 1
        enter += slideInHorizontally { fullWidth -> dir * fullWidth }
    }
    if (t.expandVertically) enter += expandVertically()
    if (t.scaleIn) enter += scaleIn(initialScale = t.initialScale)
    return if (enter == EnterTransition.None) fadeIn() else enter
}

private fun buildExitTransition(t: SduiExitTransition): ExitTransition {
    var exit: ExitTransition = ExitTransition.None
    if (t.fadeOut) exit += fadeOut(animationSpec = tween(t.fadeDurationMillis))
    if (t.slideOutVertically) {
        val dir = if (t.slideVerticalDirection.lowercase() == "up") -1 else 1
        exit += slideOutVertically { fullHeight -> dir * fullHeight }
    }
    if (t.slideOutHorizontally) {
        val dir = if (t.slideHorizontalDirection.lowercase() == "start") -1 else 1
        exit += slideOutHorizontally { fullWidth -> dir * fullWidth }
    }
    if (t.shrinkVertically) exit += shrinkVertically()
    if (t.scaleOut) exit += scaleOut(targetScale = t.targetScale)
    return if (exit == ExitTransition.None) fadeOut() else exit
}

private fun parseBoxAlignment(v: String): Alignment = when (v.lowercase()) {
    "topstart" -> Alignment.TopStart
    "topcenter" -> Alignment.TopCenter
    "topend" -> Alignment.TopEnd
    "centerstart" -> Alignment.CenterStart
    "center" -> Alignment.Center
    "centerend" -> Alignment.CenterEnd
    "bottomstart" -> Alignment.BottomStart
    "bottomcenter" -> Alignment.BottomCenter
    "bottomend" -> Alignment.BottomEnd
    else -> Alignment.Center
}

private fun parseVerticalArrangement(v: String): Arrangement.Vertical = when (v.lowercase()) {
    "bottom" -> Arrangement.Bottom
    "center" -> Arrangement.Center
    "spacebetween" -> Arrangement.SpaceBetween
    "spacearound" -> Arrangement.SpaceAround
    "spaceevenly" -> Arrangement.SpaceEvenly
    else -> Arrangement.Top
}

private fun parseHorizontalArrangement(v: String): Arrangement.Horizontal = when (v.lowercase()) {
    "end" -> Arrangement.End
    "center" -> Arrangement.Center
    "spacebetween" -> Arrangement.SpaceBetween
    "spacearound" -> Arrangement.SpaceAround
    "spaceevenly" -> Arrangement.SpaceEvenly
    else -> Arrangement.Start
}

private fun parseHorizontalAlignment(v: String): Alignment.Horizontal = when (v.lowercase()) {
    "centerhorizontally", "center" -> Alignment.CenterHorizontally
    "end" -> Alignment.End
    else -> Alignment.Start
}

private fun parseRowVerticalAlignment(v: String): Alignment.Vertical = when (v.lowercase()) {
    "top" -> Alignment.Top
    "bottom" -> Alignment.Bottom
    else -> Alignment.CenterVertically
}

private fun parseFontWeight(w: String): FontWeight = when (w.lowercase()) {
    "bold" -> FontWeight.Bold
    "semibold" -> FontWeight.SemiBold
    "medium" -> FontWeight.Medium
    "light" -> FontWeight.Light
    else -> FontWeight.Normal
}

private fun parseFontStyle(s: String): FontStyle = when (s.lowercase()) {
    "italic" -> FontStyle.Italic
    else -> FontStyle.Normal
}

private fun parseTextAlign(a: String): TextAlign = when (a.lowercase()) {
    "center" -> TextAlign.Center
    "end" -> TextAlign.End
    "justify" -> TextAlign.Justify
    else -> TextAlign.Start
}

private fun parseTextOverflow(o: String): TextOverflow = when (o.lowercase()) {
    "ellipsis" -> TextOverflow.Ellipsis
    else -> TextOverflow.Clip
}

private fun parseKeyboardType(k: String): KeyboardType = when (k.lowercase()) {
    "number" -> KeyboardType.Number
    "email" -> KeyboardType.Email
    "phone" -> KeyboardType.Phone
    else -> KeyboardType.Text
}
