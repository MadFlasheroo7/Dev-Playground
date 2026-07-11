package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import kotlinx.datetime.Clock
import pro.jayeshseth.devcontrols.nav.Route
import pro.jayeshseth.devcontrols.util.SharedFocusState

// ─────────────────────────────────────────────────────────────────────────────
// Domain models
// ─────────────────────────────────────────────────────────────────────────────

enum class TaskPriority(val label: String, val color: Color) {
    HIGH("High", Color(0xFFEF4444)),
    MEDIUM("Medium", Color(0xFFF59E0B)),
    LOW("Low", Color(0xFF10B981))
}

data class SubTask(
    val id: String,
    val title: String,
    val priority: TaskPriority,
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false
)

data class Milestone(
    val id: String,
    val title: String,
    val tasks: List<SubTask>,
    val isExpanded: Boolean = true
)

data class Goal(
    val id: String,
    val title: String,
    val description: String,
    val targetDate: String,
    val milestones: List<Milestone>,
    val isArchived: Boolean = false
)

data class InboxTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanLabScreen(onBack: () -> Unit, onNavigate: (Route) -> Unit = {}) {

    // ── State ────────────────────────────────────────────────────────────────
    var goals by remember {
        mutableStateOf(
            listOf(
                Goal(
                    id = "g1",
                    title = "Deploy App to Production",
                    description = "Release wireless ADB console and setting editors.",
                    targetDate = "2026-08-01",
                    milestones = listOf(
                        Milestone(
                            id = "m11", title = "Security Audit",
                            tasks = listOf(
                                SubTask("t11", "Implement biometric locking logs", TaskPriority.HIGH, true),
                                SubTask("t12", "Sanitize ADB root shell commands input", TaskPriority.HIGH)
                            ), isExpanded = true
                        ),
                        Milestone(
                            id = "m12", title = "User Interface Polish",
                            tasks = listOf(
                                SubTask("t13", "Rework custom layout canvas dials", TaskPriority.MEDIUM, false, true),
                                SubTask("t14", "Design glowing top navigation headers", TaskPriority.LOW)
                            ), isExpanded = true
                        )
                    )
                ),
                Goal(
                    id = "g2",
                    title = "Complete Kotlin Flow Labs",
                    description = "Build structured concurrency and flow visualizations.",
                    targetDate = "2026-07-20",
                    milestones = listOf(
                        Milestone(
                            id = "m21", title = "Core Pipelines",
                            tasks = listOf(
                                SubTask("t21", "StateFlow vs SharedFlow visualizer", TaskPriority.HIGH, true),
                                SubTask("t22", "Backpressure buffer animation controls", TaskPriority.MEDIUM)
                            ), isExpanded = true
                        )
                    )
                )
            )
        )
    }

    var inboxTasks by remember {
        mutableStateOf(
            listOf(
                InboxTask("ib1", "Check broadband bill due date"),
                InboxTask("ib2", "Audit CRT scanlines performance on low-end devices")
            )
        )
    }

    var activeTab            by remember { mutableStateOf("g1") }
    var showDetailOnMobile   by remember { mutableStateOf(false) }
    var showAddGoalDialog    by remember { mutableStateOf(false) }
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog    by remember { mutableStateOf<String?>(null) }
    var showInboxConvertDialog by remember { mutableStateOf<InboxTask?>(null) }
    var isFocusPanelOpen     by remember { mutableStateOf(false) }
    var isArchivedSectionExpanded by remember { mutableStateOf(false) }

    // ── Accent colours ───────────────────────────────────────────────────────
    val accentRed    = MaterialTheme.colorScheme.error
    val accentGreen  = Color(0xFF00FFCC)   // Neon Mint
    val accentBlue   = Color(0xFF00D2FF)   // Electric Blue
    val accentOrange = Color(0xFFFFAA00)   // Neon Amber
    val dividerColor = Color(0x1AFFFFFF)

    // ── Root layout with liquid glass background ─────────────────────────────
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isMobile = maxWidth < 600.dp

        // ── Full-screen GPU shader background ────────────────────────────────
        LiquidGlassBackground(modifier = Modifier.fillMaxSize()) {}

        // ── Scaffold (transparent so the shader shows through) ───────────────
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val headerText = if (isMobile && showDetailOnMobile) {
                            when (activeTab) {
                                "inbox"    -> "📥 Inbox Capture"
                                "insights" -> "📊 Productivity Insights"
                                else       -> goals.firstOrNull { it.id == activeTab }?.title ?: "Goal Details"
                            }
                        } else "Goal & Task Tracker"
                        Text(
                            headerText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isMobile && showDetailOnMobile) showDetailOnMobile = false
                            else onBack()
                        }) {
                            Text("←", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        val pinnedCount = goals.flatMap { it.milestones.flatMap { m -> m.tasks.filter { t -> t.isPinned && !t.isCompleted } } }.size
                        IconButton(onClick = { isFocusPanelOpen = !isFocusPanelOpen }) {
                            BadgedBox(badge = {
                                if (pinnedCount > 0) Badge(containerColor = accentRed) { Text(pinnedCount.toString()) }
                            }) { Text("🎯", fontSize = 20.sp) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0x33000000))
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->

            // ── Sidebar (reusable in both mobile and desktop) ─────────────────
            @Composable
            fun SidebarContent(modifier: Modifier = Modifier) {
                Column(
                    modifier = modifier
                        .background(Color(0x22000000))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Capture & Analytics",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0x99FFFFFF),
                        fontWeight = FontWeight.Bold
                    )

                    // ── Inbox nav item ────────────────────────────────────────
                    val inboxActive = activeTab == "inbox"
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeTab = "inbox"; if (isMobile) showDetailOnMobile = true },
                        shape = RoundedCornerShape(16.dp),
                        borderColor = if (inboxActive) accentBlue else GlassBorder
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📥 Inbox Capture",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (inboxActive) accentBlue else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (inboxTasks.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(accentBlue)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        inboxTasks.size.toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // ── Insights nav item ─────────────────────────────────────
                    val insightsActive = activeTab == "insights"
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeTab = "insights"; if (isMobile) showDetailOnMobile = true },
                        shape = RoundedCornerShape(16.dp),
                        borderColor = if (insightsActive) accentOrange else GlassBorder
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📊 Insights Panel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (insightsActive) accentOrange else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = dividerColor)
                    Text(
                        "Active Goals",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0x99FFFFFF),
                        fontWeight = FontWeight.Bold
                    )

                    // ── Active goals list ─────────────────────────────────────
                    val activeGoals = goals.filter { !it.isArchived }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(activeGoals) { goal ->
                            val isActive = activeTab == goal.id
                            val totalTasks = goal.milestones.flatMap { it.tasks }.size
                            val completedTasks = goal.milestones.flatMap { it.tasks.filter { t -> t.isCompleted } }.size
                            val progressRatio = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

                            LiquidGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeTab = goal.id; if (isMobile) showDetailOnMobile = true },
                                shape = RoundedCornerShape(16.dp),
                                borderColor = if (isActive) accentGreen else GlassBorder
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Neon progress ring
                                    Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val sw = 4f
                                            drawCircle(color = dividerColor, radius = (size.minDimension - sw) / 2f, style = Stroke(sw))
                                            drawArc(color = accentGreen.copy(alpha = 0.3f), startAngle = -90f, sweepAngle = progressRatio * 360f, useCenter = false, style = Stroke(sw + 4f, cap = StrokeCap.Round))
                                            drawArc(color = accentGreen, startAngle = -90f, sweepAngle = progressRatio * 360f, useCenter = false, style = Stroke(sw, cap = StrokeCap.Round))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(goal.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${(progressRatio * 100).toInt()}% Done", style = MaterialTheme.typography.labelSmall, color = Color(0x99FFFFFF))
                                    }
                                }
                            }
                        }
                    }

                    // ── Archived goals section ────────────────────────────────
                    val archivedGoals = goals.filter { it.isArchived }
                    if (archivedGoals.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { isArchivedSectionExpanded = !isArchivedSectionExpanded }.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📦 Archived (${archivedGoals.size})", style = MaterialTheme.typography.labelMedium, color = Color(0x66FFFFFF), fontWeight = FontWeight.Bold)
                            Text(if (isArchivedSectionExpanded) "▼" else "▶", color = Color(0x66FFFFFF), fontSize = 10.sp)
                        }
                        if (isArchivedSectionExpanded) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.height(100.dp).fillMaxWidth()
                            ) {
                                items(archivedGoals) { goal ->
                                    val isActive = activeTab == goal.id
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isActive) Color(0x22FFFFFF) else Color.Transparent)
                                            .clickable { activeTab = goal.id; if (isMobile) showDetailOnMobile = true }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0x66FFFFFF)))
                                        Text(goal.title, color = Color(0x66FFFFFF), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // ── Add Goal button ───────────────────────────────────────
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth().clickable { showAddGoalDialog = true },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text("+ New Goal", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Layout switch: mobile vs desktop ─────────────────────────────
            if (isMobile) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    if (!showDetailOnMobile) {
                        SidebarContent(modifier = Modifier.fillMaxSize())
                    } else {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            when (activeTab) {
                                "inbox" -> InboxDetailsPane(
                                    inboxTasks = inboxTasks,
                                    onAddInboxTask = { title -> inboxTasks = inboxTasks + InboxTask("ib_${Clock.System.now().toEpochMilliseconds()}", title) },
                                    onDeleteInboxTask = { tid -> inboxTasks = inboxTasks.filterNot { it.id == tid } },
                                    onConvertTrigger = { showInboxConvertDialog = it },
                                    accentBlue = accentBlue, dividerColor = dividerColor
                                )
                                "insights" -> ProductivityInsightsPane(goals = goals, accentGreen = accentGreen, accentBlue = accentBlue, accentOrange = accentOrange, dividerColor = dividerColor)
                                else -> {
                                    val goal = goals.firstOrNull { it.id == activeTab }
                                    if (goal != null) GoalDetailsPane(
                                        goal = goal,
                                        onDeleteGoal = { goals = goals.filterNot { it.id == goal.id }; showDetailOnMobile = false; activeTab = "inbox" },
                                        onArchiveGoal = { goals = goals.map { g -> if (g.id == goal.id) g.copy(isArchived = true) else g }; showDetailOnMobile = false; activeTab = "inbox" },
                                        onAddMilestoneClick = { showAddMilestoneDialog = true },
                                        onDeleteMilestone = { mid -> goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.filterNot { it.id == mid }) else g } },
                                        onAddTaskClick = { showAddTaskDialog = it },
                                        onTaskCheckedChange = { mid, tid, checked ->
                                            goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == mid) m.copy(tasks = m.tasks.map { t -> if (t.id == tid) t.copy(isCompleted = checked) else t }) else m }) else g }
                                        },
                                        onTaskPinnedChange = { mid, tid, pinned ->
                                            goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == mid) m.copy(tasks = m.tasks.map { t -> if (t.id == tid) t.copy(isPinned = pinned) else t }) else m }) else g }
                                        },
                                        onDeleteTask = { mid, tid ->
                                            goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == mid) m.copy(tasks = m.tasks.filterNot { it.id == tid }) else m }) else g }
                                        },
                                        onTasksReordered = { mid, list ->
                                            goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == mid) m.copy(tasks = list) else m }) else g }
                                        },
                                        onFocusTaskTrigger = { taskTitle -> SharedFocusState.activeFocusTaskTitle = taskTitle; onNavigate(Route.POMODORO) },
                                        onMilestoneToggle = { mid ->
                                            goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == mid) m.copy(isExpanded = !m.isExpanded) else m }) else g }
                                        },
                                        accentGreen = accentGreen, accentBlue = accentBlue, accentOrange = accentOrange, dividerColor = dividerColor
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Desktop: side-by-side
                Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    SidebarContent(modifier = Modifier.width(220.dp).fillMaxHeight())

                    Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when (activeTab) {
                            "inbox" -> InboxDetailsPane(
                                inboxTasks = inboxTasks,
                                onAddInboxTask = { title -> inboxTasks = inboxTasks + InboxTask("ib_${Clock.System.now().toEpochMilliseconds()}", title) },
                                onDeleteInboxTask = { tid -> inboxTasks = inboxTasks.filterNot { it.id == tid } },
                                onConvertTrigger = { showInboxConvertDialog = it },
                                accentBlue = accentBlue, dividerColor = dividerColor
                            )
                            "insights" -> ProductivityInsightsPane(goals = goals, accentGreen = accentGreen, accentBlue = accentBlue, accentOrange = accentOrange, dividerColor = dividerColor)
                            else -> {
                                val goal = goals.firstOrNull { it.id == activeTab }
                                if (goal != null) GoalDetailsPane(
                                    goal = goal,
                                    onDeleteGoal = { goals = goals.filterNot { it.id == goal.id }; activeTab = "inbox" },
                                    onArchiveGoal = { goals = goals.map { g -> if (g.id == goal.id) g.copy(isArchived = true) else g }; activeTab = "inbox" },
                                    onAddMilestoneClick = { showAddMilestoneDialog = true },
                                    onDeleteMilestone = { mid -> goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.filterNot { it.id == mid }) else g } },
                                    onAddTaskClick = { showAddTaskDialog = it },
                                    onTaskCheckedChange = { mid, tid, checked ->
                                        goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == mid) m.copy(tasks = m.tasks.map { t -> if (t.id == tid) t.copy(isCompleted = checked) else t }) else m }) else g }
                                    },
                                    onTaskPinnedChange = { mid, tid, pinned ->
                                        goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == mid) m.copy(tasks = m.tasks.map { t -> if (t.id == tid) t.copy(isPinned = pinned) else t }) else m }) else g }
                                    },
                                    onDeleteTask = { mid, tid ->
                                        goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == mid) m.copy(tasks = m.tasks.filterNot { it.id == tid }) else m }) else g }
                                    },
                                    onTasksReordered = { mid, list ->
                                        goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == mid) m.copy(tasks = list) else m }) else g }
                                    },
                                    onFocusTaskTrigger = { taskTitle -> SharedFocusState.activeFocusTaskTitle = taskTitle; onNavigate(Route.POMODORO) },
                                    onMilestoneToggle = { mid ->
                                        goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == mid) m.copy(isExpanded = !m.isExpanded) else m }) else g }
                                    },
                                    accentGreen = accentGreen, accentBlue = accentBlue, accentOrange = accentOrange, dividerColor = dividerColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dialogs
    // ─────────────────────────────────────────────────────────────────────────

    // Focus panel dialog
    if (isFocusPanelOpen) {
        val pinnedFocusTasks = goals.flatMap { goal -> goal.milestones.flatMap { mile -> mile.tasks.filter { it.isPinned }.map { Pair(goal, it) } } }
        AlertDialog(
            onDismissRequest = { isFocusPanelOpen = false },
            title = { Text("🎯 Today's Pinned Focus", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("High priority tasks flagged for today:", style = MaterialTheme.typography.bodyMedium, color = Color(0x99FFFFFF))
                    LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pinnedFocusTasks.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Text("No tasks pinned for today. Click ☆ next to any task to pin it here!", style = MaterialTheme.typography.bodyMedium, color = Color(0x66FFFFFF), textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            items(pinnedFocusTasks) { (goal, task) ->
                                LiquidGlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(task.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text("Goal: ${goal.title}", style = MaterialTheme.typography.labelSmall, color = Color(0x66FFFFFF))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { SharedFocusState.activeFocusTaskTitle = task.title; isFocusPanelOpen = false; onNavigate(Route.POMODORO) }) { Text("⏱", fontSize = 14.sp) }
                                            Checkbox(checked = task.isCompleted, onCheckedChange = { checked ->
                                                goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> m.copy(tasks = m.tasks.map { t -> if (t.id == task.id) t.copy(isCompleted = checked) else t }) }) else g }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { isFocusPanelOpen = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Close") } },
            containerColor = Color(0xCC060913),
            modifier = Modifier.border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Add Goal dialog
    if (showAddGoalDialog) {
        var gTitle  by remember { mutableStateOf("") }
        var gDesc   by remember { mutableStateOf("") }
        var gTarget by remember { mutableStateOf("2026-08-30") }
        val accentGreen = Color(0xFF00FFCC)
        val dividerColor = Color(0x1AFFFFFF)
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Create Long-Term Goal", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = gTitle, onValueChange = { gTitle = it }, label = { Text("Goal Title") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentGreen, unfocusedBorderColor = dividerColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = gDesc,  onValueChange = { gDesc  = it }, label = { Text("Short Description") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentGreen, unfocusedBorderColor = dividerColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = gTarget, onValueChange = { gTarget = it }, label = { Text("Target Date (YYYY-MM-DD)") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentGreen, unfocusedBorderColor = dividerColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (gTitle.isNotBlank()) {
                        goals = goals + Goal(id = "goal_${Clock.System.now().toEpochMilliseconds()}", title = gTitle, description = gDesc, targetDate = gTarget, milestones = emptyList())
                        showAddGoalDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showAddGoalDialog = false }) { Text("Cancel") } },
            containerColor = Color(0xCC060913),
            modifier = Modifier.border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Add Milestone dialog
    if (showAddMilestoneDialog) {
        var mTitle by remember { mutableStateOf("") }
        val accentGreen  = Color(0xFF00FFCC)
        val dividerColor = Color(0x1AFFFFFF)
        AlertDialog(
            onDismissRequest = { showAddMilestoneDialog = false },
            title = { Text("Create Milestone", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = mTitle, onValueChange = { mTitle = it }, label = { Text("Milestone Title") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentGreen, unfocusedBorderColor = dividerColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White)) },
            confirmButton = {
                Button(onClick = {
                    if (mTitle.isNotBlank()) {
                        goals = goals.map { g -> if (g.id == activeTab) g.copy(milestones = g.milestones + Milestone(id = "mile_${Clock.System.now().toEpochMilliseconds()}", title = mTitle, tasks = emptyList())) else g }
                        showAddMilestoneDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddMilestoneDialog = false }) { Text("Cancel") } },
            containerColor = Color(0xCC060913),
            modifier = Modifier.border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Add Task dialog
    showAddTaskDialog?.let { mileId ->
        var tTitle           by remember { mutableStateOf("") }
        var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
        val accentGreen  = Color(0xFF00FFCC)
        val dividerColor = Color(0x1AFFFFFF)
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = null },
            title = { Text("Add Task to Milestone", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = tTitle, onValueChange = { tTitle = it }, label = { Text("Task description") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentGreen, unfocusedBorderColor = dividerColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Text("Priority:", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskPriority.values().forEach { pri ->
                            val selected = selectedPriority == pri
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (selected) pri.color else Color(0x14FFFFFF)).clickable { selectedPriority = pri }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(pri.label, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tTitle.isNotBlank()) {
                        goals = goals.map { g -> if (g.id == activeTab) g.copy(milestones = g.milestones.map { m -> if (m.id == mileId) m.copy(tasks = m.tasks + SubTask(id = "task_${Clock.System.now().toEpochMilliseconds()}", title = tTitle, priority = selectedPriority)) else m }) else g }
                        showAddTaskDialog = null
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddTaskDialog = null }) { Text("Cancel") } },
            containerColor = Color(0xCC060913),
            modifier = Modifier.border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Convert Inbox → Goal/Milestone dialog
    showInboxConvertDialog?.let { inboxTask ->
        val accentGreen  = Color(0xFF00FFCC)
        val accentBlue   = Color(0xFF00D2FF)
        AlertDialog(
            onDismissRequest = { showInboxConvertDialog = null },
            title = { Text("Link Unsorted Task to Goal", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select a Goal and Milestone to assign this task:", style = MaterialTheme.typography.bodyMedium, color = Color(0x99FFFFFF))
                    LazyColumn(modifier = Modifier.height(240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        goals.forEach { goal ->
                            item { Text(goal.title, color = accentGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp)) }
                            items(goal.milestones) { milestone ->
                                LiquidGlassCard(modifier = Modifier.fillMaxWidth().clickable {
                                    goals = goals.map { g -> if (g.id == goal.id) g.copy(milestones = g.milestones.map { m -> if (m.id == milestone.id) m.copy(tasks = m.tasks + SubTask(id = "task_${Clock.System.now().toEpochMilliseconds()}", title = inboxTask.title, priority = TaskPriority.MEDIUM)) else m }) else g }
                                    inboxTasks = inboxTasks.filterNot { it.id == inboxTask.id }
                                    showInboxConvertDialog = null
                                }, shape = RoundedCornerShape(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("  ⤷ ${milestone.title}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                        Text("Link 🔗", color = accentBlue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInboxConvertDialog = null }) { Text("Cancel") } },
            containerColor = Color(0xCC060913),
            modifier = Modifier.border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// InboxDetailsPane
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InboxDetailsPane(
    inboxTasks: List<InboxTask>,
    onAddInboxTask: (String) -> Unit,
    onDeleteInboxTask: (String) -> Unit,
    onConvertTrigger: (InboxTask) -> Unit,
    accentBlue: Color,
    dividerColor: Color
) {
    var quickText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Inbox Capture", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)

        // Quick-add input
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = quickText, onValueChange = { quickText = it },
                placeholder = { Text("Log a raw task or idea...") },
                singleLine = true, modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentBlue, unfocusedBorderColor = dividerColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Button(onClick = { if (quickText.isNotBlank()) { onAddInboxTask(quickText); quickText = "" } }, colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Text("Log")
            }
        }

        // Task list
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (inboxTasks.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("Inbox is empty. Log raw ideas above!", color = Color(0x66FFFFFF), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                items(inboxTasks) { task ->
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(task.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onConvertTrigger(task) }) { Text("Link Goal 🔗", color = Color(0xFF00FFCC), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                                IconButton(onClick = { onDeleteInboxTask(task.id) }) { Text("🗑", color = Color(0x66FFFFFF)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GoalDetailsPane
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GoalDetailsPane(
    goal: Goal,
    onDeleteGoal: () -> Unit,
    onArchiveGoal: () -> Unit,
    onAddMilestoneClick: () -> Unit,
    onDeleteMilestone: (String) -> Unit,
    onAddTaskClick: (String) -> Unit,
    onTaskCheckedChange: (String, String, Boolean) -> Unit,
    onTaskPinnedChange: (String, String, Boolean) -> Unit,
    onDeleteTask: (String, String) -> Unit,
    onTasksReordered: (String, List<SubTask>) -> Unit,
    onFocusTaskTrigger: (String) -> Unit,
    onMilestoneToggle: (String) -> Unit,
    accentGreen: Color,
    accentBlue: Color,
    accentOrange: Color,
    dividerColor: Color
) {
    val totalTasks     = goal.milestones.flatMap { it.tasks }.size
    val completedTasks = goal.milestones.flatMap { it.tasks.filter { t -> t.isCompleted } }.size
    val progressRatio  = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ── Goal header glass card ────────────────────────────────────────────
        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(goal.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(goal.description, style = MaterialTheme.typography.bodyMedium, color = Color(0x99FFFFFF))
                    }
                    Row {
                        if (progressRatio == 1f && !goal.isArchived) {
                            IconButton(onClick = onArchiveGoal) { Text("📦", color = accentGreen, fontSize = 18.sp) }
                        }
                        IconButton(onClick = onDeleteGoal) { Text("🗑", color = Color(0x66FFFFFF)) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯 Target: ${goal.targetDate}", color = accentOrange, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("${(progressRatio * 100).toInt()}% Done ($completedTasks/$totalTasks Tasks)", color = accentGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = accentGreen,
                    trackColor = dividerColor
                )
            }
        }

        // ── Section header ───────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Milestones & Tasks", style = MaterialTheme.typography.labelLarge, color = Color(0x99FFFFFF), fontWeight = FontWeight.Bold)
            if (!goal.isArchived) {
                LiquidGlassCard(modifier = Modifier.clickable { onAddMilestoneClick() }, shape = RoundedCornerShape(16.dp)) {
                    Text("+ Milestone", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }

        // ── Milestones list ───────────────────────────────────────────────────
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(goal.milestones) { milestone ->
                val mileTotal     = milestone.tasks.size
                val mileCompleted = milestone.tasks.count { it.isCompleted }
                val mileProgress  = if (mileTotal > 0) mileCompleted.toFloat() / mileTotal else 0f

                LiquidGlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        // Milestone header
                        Row(modifier = Modifier.fillMaxWidth().clickable { onMilestoneToggle(milestone.id) }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (milestone.isExpanded) "▼" else "▶", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    Text(milestone.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Text("$mileCompleted/$mileTotal Tasks Done", style = MaterialTheme.typography.labelSmall, color = Color(0x66FFFFFF), modifier = Modifier.padding(start = 24.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⇩ Sort", color = accentBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onTasksReordered(milestone.id, milestone.tasks.sortedBy { it.title }) }.padding(4.dp))
                                Text("⚡ Prior", color = accentOrange, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onTasksReordered(milestone.id, milestone.tasks.sortedBy { it.priority.ordinal }) }.padding(4.dp))
                                if (!goal.isArchived) {
                                    TextButton(onClick = { onAddTaskClick(milestone.id) }) { Text("+ Task", color = accentGreen, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                                    IconButton(onClick = { onDeleteMilestone(milestone.id) }, modifier = Modifier.size(24.dp)) { Text("🗑", color = Color(0x66FFFFFF), fontSize = 12.sp) }
                                }
                            }
                        }

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { mileProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = accentBlue,
                            trackColor = dividerColor.copy(alpha = 0.5f)
                        )

                        // Bouncy spring collapse/expand of task rows
                        AnimatedVisibility(
                            visible = milestone.isExpanded,
                            enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
                            exit  = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                milestone.tasks.forEachIndexed { index, task ->
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = task.isCompleted, onCheckedChange = { onTaskCheckedChange(milestone.id, task.id, it) })
                                                Column {
                                                    Text(task.title, color = if (task.isCompleted) Color(0x66FFFFFF) else Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(task.priority.color.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                        Text(task.priority.label, color = task.priority.color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                if (index > 0 && !goal.isArchived) Text("▲", color = Color(0x66FFFFFF), fontSize = 11.sp, modifier = Modifier.clickable { val l = milestone.tasks.toMutableList(); val t = l[index]; l[index] = l[index-1]; l[index-1] = t; onTasksReordered(milestone.id, l) }.padding(3.dp))
                                                if (index < milestone.tasks.size - 1 && !goal.isArchived) Text("▼", color = Color(0x66FFFFFF), fontSize = 11.sp, modifier = Modifier.clickable { val l = milestone.tasks.toMutableList(); val t = l[index]; l[index] = l[index+1]; l[index+1] = t; onTasksReordered(milestone.id, l) }.padding(3.dp))
                                                if (!task.isCompleted && !goal.isArchived) IconButton(onClick = { onFocusTaskTrigger(task.title) }) { Text("⏱", fontSize = 13.sp) }
                                                if (!goal.isArchived) {
                                                    IconButton(onClick = { onTaskPinnedChange(milestone.id, task.id, !task.isPinned) }) { Text(if (task.isPinned) "🎯" else "☆", color = Color.White, fontSize = 13.sp) }
                                                    IconButton(onClick = { onDeleteTask(milestone.id, task.id) }) { Text("🗑", color = Color(0x66FFFFFF), fontSize = 12.sp) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProductivityInsightsPane
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProductivityInsightsPane(
    goals: List<Goal>,
    accentGreen: Color,
    accentBlue: Color,
    accentOrange: Color,
    dividerColor: Color
) {
    val allTasks       = goals.flatMap { it.milestones.flatMap { m -> m.tasks } }
    val completedCount = allTasks.count { it.isCompleted }
    val highCompleted  = allTasks.count { it.isCompleted && it.priority == TaskPriority.HIGH }
    val medCompleted   = allTasks.count { it.isCompleted && it.priority == TaskPriority.MEDIUM }
    val lowCompleted   = allTasks.count { it.isCompleted && it.priority == TaskPriority.LOW }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Productivity Insights", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)

        // ── Summary stat cards ────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LiquidGlassCard(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Total Completed", color = Color(0x99FFFFFF), style = MaterialTheme.typography.labelSmall)
                    Text("$completedCount Tasks", color = accentGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            LiquidGlassCard(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("High Priority Done", color = Color(0x99FFFFFF), style = MaterialTheme.typography.labelSmall)
                    Text("$highCompleted Tasks", color = Color(0xFFEF4444), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Velocity line chart ───────────────────────────────────────────────
        LiquidGlassCard(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Velocity Line Chart (Completed Tasks past 7 days)", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Canvas(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0x1A000000))) {
                    val pts = listOf(
                        Offset(50f, size.height - 40f),
                        Offset(size.width * 0.15f, size.height - 70f),
                        Offset(size.width * 0.32f, size.height - 50f),
                        Offset(size.width * 0.48f, size.height - 120f),
                        Offset(size.width * 0.65f, size.height - 180f),
                        Offset(size.width * 0.82f, size.height - 160f),
                        Offset(size.width - 50f,  size.height - 230f)
                    )
                    for (i in 1..3) {
                        val y = size.height - 40f - i * (size.height - 80f) / 3f
                        drawLine(color = dividerColor.copy(alpha = 0.3f), start = Offset(40f, y), end = Offset(size.width - 40f, y), strokeWidth = 2f)
                    }
                    for (i in 0 until pts.size - 1) {
                        drawLine(color = accentBlue.copy(alpha = 0.3f), start = pts[i], end = pts[i + 1], strokeWidth = 10f, cap = StrokeCap.Round)
                        drawLine(color = accentBlue, start = pts[i], end = pts[i + 1], strokeWidth = 4f, cap = StrokeCap.Round)
                        drawCircle(color = Color.White, radius = 5f, center = pts[i])
                    }
                    drawCircle(color = Color.White, radius = 5f, center = pts.last())
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { Text(it, color = Color(0x66FFFFFF), fontSize = 10.sp) }
                }
            }
        }

        // ── Priority donut chart ──────────────────────────────────────────────
        LiquidGlassCard(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(24.dp)) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(110.dp)) {
                    val strokeW = 20f
                    val innerSize   = Size(size.width - strokeW, size.height - strokeW)
                    val innerOffset = Offset(strokeW / 2, strokeW / 2)
                    val total = (highCompleted + medCompleted + lowCompleted).toFloat()
                    if (total == 0f) {
                        drawCircle(color = dividerColor, radius = (size.minDimension - strokeW) / 2f, style = Stroke(strokeW))
                    } else {
                        val aHigh = highCompleted / total * 360f
                        val aMed  = medCompleted  / total * 360f
                        val aLow  = lowCompleted  / total * 360f
                        var start = -90f
                        if (aHigh > 0) {
                            drawArc(color = Color(0xFFEF4444).copy(alpha = 0.25f), startAngle = start, sweepAngle = aHigh, useCenter = false, topLeft = innerOffset, size = innerSize, style = Stroke(strokeW + 6f, cap = StrokeCap.Round))
                            drawArc(color = Color(0xFFEF4444), startAngle = start, sweepAngle = aHigh, useCenter = false, topLeft = innerOffset, size = innerSize, style = Stroke(strokeW, cap = StrokeCap.Round))
                            start += aHigh
                        }
                        if (aMed > 0) {
                            drawArc(color = Color(0xFFF59E0B).copy(alpha = 0.25f), startAngle = start, sweepAngle = aMed, useCenter = false, topLeft = innerOffset, size = innerSize, style = Stroke(strokeW + 6f, cap = StrokeCap.Round))
                            drawArc(color = Color(0xFFF59E0B), startAngle = start, sweepAngle = aMed, useCenter = false, topLeft = innerOffset, size = innerSize, style = Stroke(strokeW, cap = StrokeCap.Round))
                            start += aMed
                        }
                        if (aLow > 0) {
                            drawArc(color = accentGreen.copy(alpha = 0.25f), startAngle = start, sweepAngle = aLow, useCenter = false, topLeft = innerOffset, size = innerSize, style = Stroke(strokeW + 6f, cap = StrokeCap.Round))
                            drawArc(color = accentGreen, startAngle = start, sweepAngle = aLow, useCenter = false, topLeft = innerOffset, size = innerSize, style = Stroke(strokeW, cap = StrokeCap.Round))
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Completion Priority Distribution", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444))); Text("High: $highCompleted tasks", color = Color(0x99FFFFFF), style = MaterialTheme.typography.bodySmall) }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B))); Text("Medium: $medCompleted tasks", color = Color(0x99FFFFFF), style = MaterialTheme.typography.bodySmall) }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accentGreen)); Text("Low: $lowCompleted tasks", color = Color(0x99FFFFFF), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
