package pro.jayeshseth.devcontrols.ui.timedate

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pro.jayeshseth.devcontrols.util.pad
import kotlinx.datetime.Clock

data class FocusTask(val id: String, val title: String, val isCompleted: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(onBack: () -> Unit) {
    // Timer state
    var isWorkingBlock by remember { mutableStateOf(true) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var completedFocusCycles by remember { mutableIntStateOf(0) }

    // Task list state
    var focusTasks by remember {
        mutableStateOf(
            listOf(
                FocusTask("f1", "Setup wireless ADB configuration"),
                FocusTask("f2", "Refactor Budget Donut Chart Skia Canvas"),
                FocusTask("f3", "Audit secure biometric authentication flow")
            )
        )
    }
    var selectedTaskId by remember { mutableStateOf<String?>("f1") }
    var newTaskTitle by remember { mutableStateOf("") }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    val totalDuration = if (isWorkingBlock) 25 * 60 else 5 * 60

    // Sync task from Task Tracker if present
    LaunchedEffect(Unit) {
        val title = pro.jayeshseth.devcontrols.util.SharedFocusState.activeFocusTaskTitle
        if (title != null) {
            val existing = focusTasks.firstOrNull { it.title == title }
            if (existing != null) {
                selectedTaskId = existing.id
            } else {
                val newId = "f_sync_${Clock.System.now().toEpochMilliseconds()}"
                focusTasks = focusTasks + FocusTask(newId, title)
                selectedTaskId = newId
            }
            isTimerRunning = true
            pro.jayeshseth.devcontrols.util.SharedFocusState.activeFocusTaskTitle = null
        }
    }

    // Core Timer Loop
    LaunchedEffect(isTimerRunning, remainingSeconds) {
        if (isTimerRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        } else if (isTimerRunning && remainingSeconds == 0) {
            // Cycle finished
            if (isWorkingBlock) {
                completedFocusCycles++
                isWorkingBlock = false
                remainingSeconds = 5 * 60 // 5-minute break
                // Auto-complete the linked task if selected
                selectedTaskId?.let { tid ->
                    focusTasks = focusTasks.map { if (it.id == tid) it.copy(isCompleted = true) else it }
                }
            } else {
                isWorkingBlock = true
                remainingSeconds = 25 * 60 // 25-minute focus
            }
            isTimerRunning = false
        }
    }

    // Palette Colors
    val dashboardBackground = Color(0xFF0F172A)
    val cardBackground = Color(0xFF1E293B)
    val dividerColor = Color(0xFF334155)
    val accentRed = Color(0xFFEF4444)
    val accentGreen = Color(0xFF10B981)
    val accentBlue = Color(0xFF3B82F6)

    // Pulse animation for active countdown
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pomodoro Tasks Sync", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = dashboardBackground)
            )
        },
        containerColor = dashboardBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Timer dial
            val modeColor = if (isWorkingBlock) accentRed else accentGreen
            val ringRatio = remainingSeconds.toFloat() / totalDuration

            Box(
                modifier = Modifier.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Neon glow ring Canvas
                Canvas(modifier = Modifier.size(200.dp)) {
                    val strokeW = 16f
                    val sizeMinusStroke = Size(size.width - strokeW, size.height - strokeW)
                    val offsetHalfStroke = Offset(strokeW / 2, strokeW / 2)

                    // Track
                    drawCircle(
                        color = dividerColor,
                        radius = (size.minDimension - strokeW) / 2f,
                        style = Stroke(width = strokeW)
                    )
                    // Active countdown progress
                    drawArc(
                        color = if (isTimerRunning) modeColor.copy(alpha = pulseAlpha) else modeColor,
                        startAngle = -90f,
                        sweepAngle = ringRatio * 360f,
                        useCenter = false,
                        topLeft = offsetHalfStroke,
                        size = sizeMinusStroke,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isWorkingBlock) "Focus Session" else "Take a Break",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(remainingSeconds / 60).pad(2)}:${(remainingSeconds % 60).pad(2)}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 38.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Cycles: $completedFocusCycles completed",
                        color = accentBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Controls buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { isTimerRunning = !isTimerRunning },
                    colors = ButtonDefaults.buttonColors(containerColor = modeColor, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isTimerRunning) "Pause Focus" else "Start Focus", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        isTimerRunning = false
                        isWorkingBlock = true
                        remainingSeconds = 25 * 60
                    },
                    border = BorderStroke(1.dp, dividerColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reset timer")
                }
            }

            // Linked Task Selector card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Linked Focus Task", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        TextButton(onClick = { showAddTaskDialog = true }) {
                            Text("+ Add Task", color = accentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    val activeTask = focusTasks.firstOrNull { it.id == selectedTaskId }
                    if (activeTask != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(dashboardBackground)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = true,
                                onClick = {},
                                colors = RadioButtonDefaults.colors(selectedColor = accentBlue)
                            )
                            Text(
                                text = activeTask.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (activeTask.isCompleted) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Done ✅", color = accentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Text("No task linked. Timer runs independently.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            // Checklist of Focus Tasks
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Focus Tasks Pool", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(focusTasks) { task ->
                        val isSelected = selectedTaskId == task.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) cardBackground else cardBackground.copy(alpha = 0.5f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) accentBlue else dividerColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTaskId = task.id }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { checked ->
                                        focusTasks = focusTasks.map { if (it.id == task.id) it.copy(isCompleted = checked) else it }
                                    }
                                )
                                Text(
                                    text = task.title,
                                    color = if (task.isCompleted) Color.Gray else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = {
                                    focusTasks = focusTasks.filterNot { it.id == task.id }
                                    if (selectedTaskId == task.id) selectedTaskId = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("🗑", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Focus Task
    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Create Focus Task", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    label = { Text("Task description") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentGreen,
                        unfocusedBorderColor = dividerColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            val newId = "f_${Clock.System.now().toEpochMilliseconds()}"
                            focusTasks = focusTasks + FocusTask(newId, newTaskTitle)
                            selectedTaskId = newId
                            newTaskTitle = ""
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen, contentColor = Color.Black)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") }
            },
            containerColor = cardBackground
        )
    }
}
