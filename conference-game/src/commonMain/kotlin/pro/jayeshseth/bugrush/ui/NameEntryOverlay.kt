package pro.jayeshseth.bugrush.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NameEntryOverlay(
    playerHandle: String,
    onUpdatePlayerHandle: (String) -> Unit,
    onTextFieldFocusChanged: (Boolean) -> Unit,
    onConfirmName: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var currentName by remember { mutableStateOf(playerHandle.ifBlank { "Dev_Kotlin_42" }) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        onTextFieldFocusChanged(true)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "name_entry_glow")
    val buttonGlow by infiniteTransition.animateColor(
        initialValue = Color(0xFF00E5FF),
        targetValue = Color(0xFF00E676),
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "btn_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60D1117))
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    val finalName = currentName.trim().ifBlank { "Dev_Kotlin_42" }
                    onConfirmName(finalName)
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161F2E))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(24.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Title Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "⚡", fontSize = 26.sp)
                Text(
                    text = "OPERATOR IDENTIFICATION",
                    color = Color(0xFF00E5FF),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = ArcadeTheme.pixelHeader,
                    letterSpacing = 1.2.sp
                )
            }

            Text(
                text = "ENTER YOUR DEVELOPER HANDLE FOR THE CONFERENCE LEADERBOARD",
                color = Color(0xFF90A4AE),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ArcadeTheme.terminalBody,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Text Input Field
            OutlinedTextField(
                value = currentName,
                onValueChange = { input ->
                    if (input.length <= 18) {
                        currentName = input
                        onUpdatePlayerHandle(input)
                    }
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = ArcadeTheme.pixelHeader,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { onTextFieldFocusChanged(it.isFocused) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF00E5FF),
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF37474F),
                    focusedContainerColor = Color(0xFF0F1722),
                    unfocusedContainerColor = Color(0xFF0F1722)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Confirm & Deploy Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(buttonGlow)
                    .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                    .clickable {
                        val finalName = currentName.trim().ifBlank { "Dev_Kotlin_42" }
                        onConfirmName(finalName)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚡", fontSize = 18.sp)
                    Text(
                        text = "COMMENCE DROP (ENTER)",
                        color = Color(0xFF0A0E17),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = ArcadeTheme.pixelHeader,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
