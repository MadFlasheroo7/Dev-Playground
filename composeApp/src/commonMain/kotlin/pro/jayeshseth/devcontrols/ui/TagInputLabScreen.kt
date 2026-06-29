package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable TagInputField component.
 * Lays out chips inline inside the input area. Handles comma/enter triggers
 * and backspace-to-highlight/remove.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagInputField(
    tags: List<String>,
    onTagAdded: (String) -> Unit,
    onTagRemoved: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Add tags...",
    maxTags: Int = 10
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    // Track if the last tag is currently highlighted for deletion
    var isLastTagHighlighted by remember { mutableStateOf(false) }

    fun commitTag() {
        val rawText = textFieldValue.text
        val trimmed = rawText.trim().trimEnd(',')
        if (trimmed.isNotEmpty() && tags.size < maxTags) {
            onTagAdded(trimmed)
            textFieldValue = TextFieldValue("")
            isLastTagHighlighted = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { focusRequester.requestFocus() }
            .padding(8.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Render active tags as animated chips
            tags.forEachIndexed { index, tag ->
                val isHighlighted = isLastTagHighlighted && index == tags.lastIndex
                
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    InputChip(
                        selected = false,
                        onClick = { onTagRemoved(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✕", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = androidx.compose.material3.InputChipDefaults.inputChipColors(
                            containerColor = if (isHighlighted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .then(
                                if (isHighlighted) Modifier.border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp)) else Modifier
                            )
                    )
                }
            }

            // Input text box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.CenterStart
            ) {
                if (textFieldValue.text.isEmpty() && tags.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { value ->
                        isLastTagHighlighted = false
                        if (value.text.endsWith(",") || value.text.endsWith(" ")) {
                            commitTag()
                        } else {
                            textFieldValue = value
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitTag() }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                        .onKeyEvent { keyEvent ->
                            // Custom backspace interception
                            if (keyEvent.key == Key.Backspace && textFieldValue.text.isEmpty() && tags.isNotEmpty()) {
                                if (isLastTagHighlighted) {
                                    onTagRemoved(tags.last())
                                    isLastTagHighlighted = false
                                } else {
                                    isLastTagHighlighted = true
                                }
                                true
                            } else {
                                false
                            }
                        }
                )
            }
        }
    }
}

/** Tag input screen showcasing the reusable TagInputField with suggestions. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagInputLabScreen(onBack: () -> Unit) {
    val tags = remember { mutableStateListOf("compose", "kotlin", "android") }
    val suggestions = listOf("animation", "canvas", "ui", "reactive", "swiftui", "flutter", "web")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tag Input Field") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                "Reusable Tag Input Field",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Type a tag and press Comma, Space or Enter. Backspace on empty highlights and deletes the last tag.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(24.dp))

            // Reusable Tag Input
            TagInputField(
                tags = tags,
                onTagAdded = { if (it !in tags) tags.add(it) },
                onTagRemoved = { tags.remove(it) },
                placeholder = "Add skills (max 8)...",
                maxTags = 8
            )

            Spacer(Modifier.height(32.dp))

            Text(
                "Popular Suggestions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.filter { it !in tags }.forEach { s ->
                    AssistChip(
                        onClick = { if (tags.size < 8) tags.add(s) },
                        label = { Text("+ $s") },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}
