package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reusable OtpInputField component.
 * Supports custom length, error states with shake animation, and hidden input focus.
 */
@Composable
fun OtpInputField(
    otpText: String,
    onOtpTextChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    otpLength: Int = 4,
    isError: Boolean = false,
    obscureText: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Perform horizontal shake animation
            repeat(3) {
                shakeOffset.animateTo(12f, spring(stiffness = Spring.StiffnessHigh))
                shakeOffset.animateTo(-12f, spring(stiffness = Spring.StiffnessHigh))
            }
            shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(shakeOffset.value.toInt(), 0) }
            .clickable { focusRequester.requestFocus() },
        contentAlignment = Alignment.Center
    ) {
        // Hidden BasicTextField to intercept keyboard input
        BasicTextField(
            value = otpText,
            onValueChange = { value ->
                if (value.length <= otpLength) {
                    val isComplete = value.length == otpLength
                    onOtpTextChange(value, isComplete)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(otpLength) { index ->
                val char = otpText.getOrNull(index)
                val isFocused = otpText.length == index
                val borderBrush = when {
                    isError -> Color(0xFFFF5F6D)
                    isFocused -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            width = if (isFocused || isError) 2.dp else 1.dp,
                            color = borderBrush,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val textVal = if (char != null) {
                        if (obscureText) "•" else char.toString()
                    } else ""

                    Text(
                        text = textVal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) Color(0xFFFF5F6D) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** PIN Pad Lab Screen - showcases the reusable OtpInputField with a custom keyboard and simulated error states. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinPadLabScreen(onBack: () -> Unit) {
    var pinText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var obscureMode by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OTP / PIN Input") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Reusable OTP Input Field",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Click to type using system keyboard, or use the PIN pad below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))

                // Reusable OTP component
                OtpInputField(
                    otpText = pinText,
                    onOtpTextChange = { text, complete ->
                        pinText = text
                        isError = false
                        if (complete) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    otpLength = 4,
                    isError = isError,
                    obscureText = obscureMode
                )

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { isError = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5F6D))
                    ) {
                        Text("Trigger Error (Shake)")
                    }
                    Button(
                        onClick = { obscureMode = !obscureMode },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(if (obscureMode) "Reveal Text" else "Obscure Text")
                    }
                }
            }

            // Custom PIN Pad Keyboard
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isError = false
                                        when (key) {
                                            "⌫" -> {
                                                if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                                            }
                                            "C" -> {
                                                pinText = ""
                                            }
                                            else -> {
                                                if (pinText.length < 4) pinText += key
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
