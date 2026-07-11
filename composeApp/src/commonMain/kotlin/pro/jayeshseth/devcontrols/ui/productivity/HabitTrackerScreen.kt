package pro.jayeshseth.devcontrols.ui.productivity

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*
import pro.jayeshseth.devcontrols.ui.finance.FinanceBadgeChip
import kotlin.random.Random

data class Habit(val id: String, val title: String, val category: String)
data class HabitLog(val date: LocalDate, val completedHabitIds: Set<String>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTrackerScreen(onBack: () -> Unit) {
    // Current LocalDate
    val tz = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(tz).date

    // Static Habits List
    var habits by remember {
        mutableStateOf(
            listOf(
                Habit("h1", "LeetCode Daily Coding", "Code"),
                Habit("h2", "Drink 3L Water", "Health"),
                Habit("h3", "Read Tech Articles / Documentation", "Study"),
                Habit("h4", "Deep Work Focus Block (Pomodoro)", "Productivity")
            )
        )
    }

    // Pre-populate historical logs to make the GitHub contribution grid look beautiful immediately!
    var habitLogs by remember {
        val list = mutableListOf<HabitLog>()
        // Generate logs for past 140 days (20 weeks)
        for (i in 1..140) {
            val date = today.minus(i, DateTimeUnit.DAY)
            // Randomly check off some habits (70% chance of completion)
            if (Random.nextDouble() < 0.75) {
                val checkedIds = habits.filter { Random.nextDouble() < 0.6 }.map { it.id }.toSet()
                list.add(HabitLog(date, checkedIds))
            } else {
                list.add(HabitLog(date, emptySet()))
            }
        }
        // Logs for today initially starts empty
        mutableStateOf(list.toList())
    }

    // Today's checked list
    var todayCheckedIds by remember { mutableStateOf(setOf<String>()) }

    // Dialog state
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var newHabitTitle by remember { mutableStateOf("") }
    var newHabitCategory by remember { mutableStateOf("General") }

    // Palette Colors
    val dashboardBackground = Color(0xFF0F172A)
    val cardBackground = Color(0xFF1E293B)
    val dividerColor = Color(0xFF334155)
    val accentGreen = Color(0xFF10B981)
    val accentBlue = Color(0xFF3B82F6)
    val accentOrange = Color(0xFFF59E0B)

    // Calculate streaks
    val allLogsMap = (habitLogs + HabitLog(today, todayCheckedIds)).associateBy { it.date }
    var currentStreak = 0
    var checkDate = today
    while (true) {
        val log = allLogsMap[checkDate]
        if (log != null && log.completedHabitIds.isNotEmpty()) {
            currentStreak++
            checkDate = checkDate.minus(1, DateTimeUnit.DAY)
        } else {
            break
        }
    }

    // Longest streak calculation
    var longestStreak = 0
    var runningStreak = 0
    // Sort dates ascending
    val sortedDates = allLogsMap.keys.sorted()
    for (d in sortedDates) {
        val log = allLogsMap[d]
        if (log != null && log.completedHabitIds.isNotEmpty()) {
            runningStreak++
            if (runningStreak > longestStreak) longestStreak = runningStreak
        } else {
            runningStreak = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habit Grid Tracker", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    Button(
                        onClick = { showAddHabitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Habit", fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Streaks metrics card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Active Habit Streaks", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FinanceBadgeChip(
                                icon = "🔥",
                                label = "Current: $currentStreak Days",
                                isActive = currentStreak > 0,
                                activeColor = accentOrange
                            )
                            FinanceBadgeChip(
                                icon = "👑",
                                label = "Best: $longestStreak Days",
                                isActive = longestStreak > 0,
                                activeColor = accentBlue
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(accentGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (habits.isNotEmpty()) "${(todayCheckedIds.size * 100 / habits.size)}%" else "0%",
                            color = accentGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // GitHub style Green Contribution Grid (representing 20 weeks)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Contribution Grid (Past 20 Weeks)", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    // Draw the contribution grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // We show 20 weeks, each week is a column of 7 days (Sunday to Saturday)
                        val totalWeeks = 20
                        val gridData = (0 until totalWeeks).map { weekIndex ->
                            val daysOfWeek = (0..6).map { dayOffset ->
                                val daysAgo = (totalWeeks - 1 - weekIndex) * 7 + (6 - dayOffset)
                                val date = today.minus(daysAgo, DateTimeUnit.DAY)
                                val log = allLogsMap[date]
                                val totalHabits = habits.size
                                val completedCount = log?.completedHabitIds?.size ?: 0
                                val ratio = if (totalHabits > 0) completedCount.toFloat() / totalHabits else 0f
                                Pair(date, ratio)
                            }
                            daysOfWeek
                        }

                        gridData.forEach { week ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                week.forEach { (date, ratio) ->
                                    val cellColor = when {
                                        ratio == 0f -> dividerColor.copy(alpha = 0.5f)
                                        ratio <= 0.33f -> Color(0xFF1E4620)
                                        ratio <= 0.66f -> Color(0xFF2E7D32)
                                        else -> Color(0xFF4CAF50)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(cellColor)
                                            .clickable {
                                                // Quick toast/alert of details could go here
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Grid Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Less  ", color = Color.Gray, fontSize = 10.sp)
                        val legendColors = listOf(
                            dividerColor.copy(alpha = 0.5f),
                            Color(0xFF1E4620),
                            Color(0xFF2E7D32),
                            Color(0xFF4CAF50)
                        )
                        legendColors.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(col)
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        Text("  More", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }

            // Today's habits checklist
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Today's Tasks Checklist", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(habits) { habit ->
                        val isChecked = todayCheckedIds.contains(habit.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isChecked) cardBackground else cardBackground.copy(alpha = 0.5f))
                                .border(
                                    width = 1.dp,
                                    color = if (isChecked) accentGreen else dividerColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    todayCheckedIds = if (isChecked) {
                                        todayCheckedIds - habit.id
                                    } else {
                                        todayCheckedIds + habit.id
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = habit.title,
                                    color = if (isChecked) Color.Gray else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(habit.category, color = Color.Gray, fontSize = 11.sp)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        todayCheckedIds = if (checked) {
                                            todayCheckedIds + habit.id
                                        } else {
                                            todayCheckedIds - habit.id
                                        }
                                    }
                                )

                                IconButton(
                                    onClick = { habits = habits.filterNot { it.id == habit.id } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("🗑", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Habit
    if (showAddHabitDialog) {
        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            title = { Text("Create New Habit Task", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newHabitTitle,
                        onValueChange = { newHabitTitle = it },
                        label = { Text("Habit Description") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGreen,
                            unfocusedBorderColor = dividerColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = newHabitCategory,
                        onValueChange = { newHabitCategory = it },
                        label = { Text("Category Tag") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentGreen,
                            unfocusedBorderColor = dividerColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newHabitTitle.isNotBlank()) {
                            habits = habits + Habit(
                                id = "h_${Clock.System.now().toEpochMilliseconds()}",
                                title = newHabitTitle,
                                category = newHabitCategory
                            )
                            newHabitTitle = ""
                            showAddHabitDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen, contentColor = Color.Black)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabitDialog = false }) { Text("Cancel") }
            },
            containerColor = cardBackground
        )
    }
}
