package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Stepper Progress Indicator.
 * Renders a clean progress track with animated checkmarks and step nodes.
 */
@Composable
fun Stepper(
    currentStep: Int,
    steps: List<String>,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, title ->
            val isCompleted = index < currentStep
            val isActive = index == currentStep

            val circleColor by animateColorAsState(
                targetValue = when {
                    isCompleted || isActive -> activeColor
                    else -> inactiveColor
                }
            )

            val borderScale by animateFloatAsState(
                targetValue = if (isActive) 1.15f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )

            val stepModifier = if (index < steps.lastIndex) Modifier.weight(1f) else Modifier
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = stepModifier
            ) {
                // Step Circle Node
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .scale(borderScale)
                        .clip(CircleShape)
                        .background(circleColor)
                        .border(
                            width = if (isActive) 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else {
                        Text(
                            text = "${index + 1}",
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Connecting Track Line
                if (index < steps.lastIndex) {
                    val trackProgress by animateFloatAsState(
                        targetValue = if (index < currentStep) 1f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(inactiveColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(trackProgress)
                                .fillMaxSize()
                                .background(activeColor)
                        )
                    }
                }
            }
        }
    }
}

/** Stepper Wizard screen showcasing the reusable Stepper component with smooth content transitions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepperWizardLabScreen(onBack: () -> Unit) {
    val stepsList = listOf("Account", "Profile", "Payment", "Confirm")
    var currentStep by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stepper Wizard") },
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Reusable Stepper Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Interactive wizard flow displaying step completion and tracking states.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(32.dp))

                // Reusable Stepper
                Stepper(
                    currentStep = currentStep,
                    steps = stepsList
                )

                Spacer(Modifier.height(40.dp))

                // Step Content card with sliding animations
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                        } else {
                            slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                        }
                    },
                    label = "stepContent"
                ) { stepIndex ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stepsList[stepIndex],
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = when (stepIndex) {
                                    0 -> "Enter your username and secure password details."
                                    1 -> "Set up your public avatar, biography and social links."
                                    2 -> "Choose your billing plan and input card details."
                                    else -> "Review your selections and complete registration."
                                },
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentStep > 0) {
                            currentStep--
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    enabled = currentStep > 0
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentStep < stepsList.lastIndex) {
                            currentStep++
                        } else {
                            currentStep = 0
                        }
                    }
                ) {
                    Text(if (currentStep < stepsList.lastIndex) "Next" else "Reset")
                }
            }
        }
    }
}
