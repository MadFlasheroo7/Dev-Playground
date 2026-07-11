package pro.jayeshseth.devcontrols.ui.finance

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ─────────────────────────────────────────────────────────────────────────────
// Navigation Screen States
// ─────────────────────────────────────────────────────────────────────────────
sealed interface FinanceScreen {
    object Overview : FinanceScreen
    data class MonthDetails(val year: Int, val monthIndex: Int) : FinanceScreen
}

// ─────────────────────────────────────────────────────────────────────────────
// Domain Models
// ─────────────────────────────────────────────────────────────────────────────
data class RecurringTemplate(val id: String, val label: String, val defaultAmount: Double, val category: ExpenseCategory)
data class SplitBill(val partnerName: String, val totalAmount: Double, val sharePercent: Double, val paidByMe: Boolean)
data class ExpenseItem(
    val id: String, val label: String, val amount: Double, val category: ExpenseCategory,
    val isFromTemplate: Boolean = false, val foreignAmount: Double? = null,
    val foreignCurrency: String? = null, val exchangeRate: Double? = null,
    val partnerSplit: SplitBill? = null
)
data class IncomeItem(val id: String, val label: String, val amount: Double)
data class SavingsGoal(val id: String, val label: String, val target: Double, var saved: Double)
data class BillReminder(val id: String, val label: String, val dueDay: Int, val isPaid: Boolean)

enum class ExpenseCategory(val label: String, val color: Color, val icon: String) {
    HOUSING("Housing",   Color(0xFF7C6AFF), "🏠"),
    UTILITIES("Utilities", Color(0xFF00D2FF), "⚡"),
    TRANSPORT("Transport", Color(0xFF00FFCC), "🚗"),
    FOOD("Food",        Color(0xFF10B981), "🍔"),
    FUN("Leisure",      Color(0xFFFF5370), "🎉"),
    OTHER("Other",      Color(0xFFFFAA00), "📦")
}

data class MonthBudget(
    val year: Int, val monthIndex: Int, val monthName: String, val limit: Double,
    val expenses: List<ExpenseItem>, val incomes: List<IncomeItem>,
    val billReminders: List<BillReminder>, val categoryLimits: Map<ExpenseCategory, Double> = emptyMap()
)
data class PresetExpense(val label: String, val amount: Double, val category: ExpenseCategory)
data class AutocompleteItem(val query: String, val label: String, val category: ExpenseCategory, val defaultAmount: Double)

// ─────────────────────────────────────────────────────────────────────────────
// M3 Expressive Design Tokens
// ─────────────────────────────────────────────────────────────────────────────
private val BgDeep         = Color(0xFF060D1F)
private val SurfaceGlass   = Color(0x14FFFFFF)
private val BorderGlass    = Color(0x26FFFFFF)
private val DividerGlass   = Color(0x1AFFFFFF)
private val AccentEmerald  = Color(0xFF00FFCC)
private val AccentRed      = Color(0xFFFF5370)
private val AccentAmber    = Color(0xFFFFAA00)
private val AccentElectric = Color(0xFF00D2FF)
private val TextPrimary    = Color.White
private val TextSecondary  = Color(0x99FFFFFF)
private val TextMuted      = Color(0x55FFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// Glass Card helper (inline, avoids cross-package import)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    borderColor: Color = BorderGlass,
    shimmer: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val shimmerAnim by if (shimmer) {
        rememberInfiniteTransition(label = "shimmer").animateFloat(
            initialValue = -0.5f, targetValue = 1.5f,
            animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
            label = "sh"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier
            .border(BorderStroke(1.dp, borderColor), shape)
            .clip(shape)
            .drawBehind {
                drawRect(SurfaceGlass)
                drawRect(Brush.radialGradient(listOf(Color(0x10FFFFFF), Color.Transparent), Offset.Zero, maxOf(size.width, size.height) * 0.7f))
                if (shimmer) {
                    val cx = size.width * shimmerAnim
                    val hb = size.width * 0.30f
                    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color(0x0AFFFFFF), Color(0x14FFFFFF), Color(0x0AFFFFFF), Color.Transparent), cx - hb, cx + hb))
                }
            },
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagerScreen(onBack: () -> Unit) {
    val now          = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val currentYear  = now.year
    val currentMonth = now.monthNumber
    val currentDay   = now.dayOfMonth

    var currentScreen by remember { mutableStateOf<FinanceScreen>(FinanceScreen.Overview) }
    var selectedYear  by remember { mutableStateOf(currentYear) }

    val autocompleteDb = remember {
        listOf(
            AutocompleteItem("zomato",      "Zomato Food",            ExpenseCategory.FOOD,      450.0),
            AutocompleteItem("swiggy",      "Swiggy Food Order",      ExpenseCategory.FOOD,      380.0),
            AutocompleteItem("uber",        "Uber Ride",              ExpenseCategory.TRANSPORT, 350.0),
            AutocompleteItem("ola",         "Ola Auto",               ExpenseCategory.TRANSPORT, 150.0),
            AutocompleteItem("starbucks",   "Starbucks Coffee",       ExpenseCategory.FOOD,      400.0),
            AutocompleteItem("netflix",     "Netflix Subscription",   ExpenseCategory.FUN,       199.0),
            AutocompleteItem("spotify",     "Spotify Premium",        ExpenseCategory.FUN,       119.0),
            AutocompleteItem("electricity", "Electricity Bill",       ExpenseCategory.UTILITIES, 2800.0),
            AutocompleteItem("aws",         "AWS Hosting",            ExpenseCategory.UTILITIES, 1500.0),
            AutocompleteItem("amazon",      "Amazon Shopping",        ExpenseCategory.OTHER,     2500.0)
        )
    }

    var savingsGoals by remember {
        mutableStateOf(listOf(
            SavingsGoal("g1", "New GPU Upgrade",     45000.0, 15000.0),
            SavingsGoal("g2", "Vacation Travel Fund", 80000.0, 25000.0)
        ))
    }

    var recurringTemplates by remember {
        mutableStateOf(listOf(
            RecurringTemplate("t1", "Rent / Mortgage",       18000.0, ExpenseCategory.HOUSING),
            RecurringTemplate("t2", "Broadband Internet",     1200.0, ExpenseCategory.UTILITIES),
            RecurringTemplate("t3", "Gym Membership",         1500.0, ExpenseCategory.FUN),
            RecurringTemplate("t4", "Streaming Apps Bundle",   699.0, ExpenseCategory.FUN)
        ))
    }

    fun generateTemplateExpenses(): List<ExpenseItem> =
        recurringTemplates.map { ExpenseItem("tmpl_${it.id}", it.label, it.defaultAmount, it.category, isFromTemplate = true) }

    fun generateDefaultBillReminders() = listOf(
        BillReminder("br1", "Rent / Mortgage Payment", 5,  false),
        BillReminder("br2", "Broadband Bill Due",       10, false),
        BillReminder("br3", "Gym Renewal",              15, false)
    )

    fun createYearBudgets(year: Int): List<MonthBudget> {
        val names = listOf("January","February","March","April","May","June","July","August","September","October","November","December")
        return names.mapIndexed { idx, name ->
            MonthBudget(year = year, monthIndex = idx + 1, monthName = name, limit = 35000.0,
                expenses = generateTemplateExpenses(),
                incomes = listOf(IncomeItem("inc_${year}_${idx}", "Monthly Salary", 60000.0)),
                billReminders = generateDefaultBillReminders(),
                categoryLimits = mapOf(ExpenseCategory.FOOD to 8000.0)
            )
        }
    }

    var monthlyBudgets by remember {
        val base2026 = createYearBudgets(2026).map { mb ->
            if (mb.monthIndex == 7) mb.copy(
                limit = 40000.0,
                expenses = mb.expenses + listOf(
                    ExpenseItem("v71", "Weekly Veggies",   2800.0, ExpenseCategory.FOOD),
                    ExpenseItem("v72", "Ride Hailing",     1200.0, ExpenseCategory.TRANSPORT),
                    ExpenseItem("v73", "New Headphones",   8999.0, ExpenseCategory.FUN)
                ),
                incomes = mb.incomes + listOf(IncomeItem("inc72", "Freelance Gig", 15000.0)),
                categoryLimits = mapOf(ExpenseCategory.FOOD to 10000.0, ExpenseCategory.FUN to 12000.0)
            ) else mb
        }
        mutableStateOf(createYearBudgets(2025) + base2026 + createYearBudgets(2027))
    }

    // Dialog / panel states
    var showFixedSettings      by remember { mutableStateOf(false) }
    var showAddFixedDialog     by remember { mutableStateOf(false) }
    var showAddExpenseDialog   by remember { mutableStateOf(false) }
    var showEditExpenseDialog  by remember { mutableStateOf<ExpenseItem?>(null) }
    var showEditLimitDialog    by remember { mutableStateOf(false) }
    var showAllocationDialog   by remember { mutableStateOf<MonthBudget?>(null) }
    var showIncomeSettings     by remember { mutableStateOf<MonthBudget?>(null) }
    var showAddIncomeDialog    by remember { mutableStateOf(false) }
    var showRemindersDialog    by remember { mutableStateOf<MonthBudget?>(null) }
    var showCategoryLimitDialog by remember { mutableStateOf<MonthBudget?>(null) }
    var showSandboxDialog      by remember { mutableStateOf(false) }
    var showCsvExportResult    by remember { mutableStateOf<String?>(null) }

    // Sandbox state
    var simulatedSalaryIncrease  by remember { mutableStateOf("") }
    var simulatedLimitReduction  by remember { mutableStateOf("") }
    var simulatedSubscriptionCut by remember { mutableStateOf(false) }

    var expenseLabel   by remember { mutableStateOf("") }
    var expenseAmount  by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var newLimitAmount by remember { mutableStateOf("") }

    // Quick-add presets
    val presetExpenses = listOf(
        PresetExpense("Chai & Snacks",    120.0,  ExpenseCategory.FOOD),
        PresetExpense("Auto / Cab Ride",  220.0,  ExpenseCategory.TRANSPORT),
        PresetExpense("Movie Ticket",     350.0,  ExpenseCategory.FUN),
        PresetExpense("Groceries Run",   1500.0,  ExpenseCategory.FOOD),
        PresetExpense("Electric Bill",   2400.0,  ExpenseCategory.UTILITIES),
        PresetExpense("Online Shopping", 3200.0,  ExpenseCategory.OTHER)
    )

    fun getMonthSpent(b: MonthBudget)  = b.expenses.sumOf { it.amount }
    fun getMonthIncome(b: MonthBudget) = b.incomes.sumOf { it.amount }

    // ── Main router ──────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(BgDeep).drawBehind {
        // Subtle ambient radial glow — deep indigo + teal
        drawRect(Brush.radialGradient(listOf(Color(0x220D2060), Color.Transparent), Offset(size.width * 0.25f, size.height * 0.1f), size.width * 1.1f))
        drawRect(Brush.radialGradient(listOf(Color(0x1A003333), Color.Transparent), Offset(size.width * 0.8f, size.height * 0.7f), size.width * 0.8f))
    }) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState is FinanceScreen.MonthDetails) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            }
        ) { screen ->
            when (screen) {

                // ── Overview screen ─────────────────────────────────────────
                is FinanceScreen.Overview -> {
                    val budgetsForYear      = monthlyBudgets.filter { it.year == selectedYear }
                    val yearlyTotalSpent    = budgetsForYear.sumOf { getMonthSpent(it) }
                    val yearlyTotalLimit    = budgetsForYear.sumOf { it.limit }
                    val yearlyTotalIncome   = budgetsForYear.sumOf { getMonthIncome(it) }
                    val budgetStreaks        = budgetsForYear.filter { getMonthSpent(it) <= it.limit }.size
                    val isFrugalHero        = budgetsForYear.any { mb -> mb.expenses.filter { !it.isFromTemplate }.sumOf { it.amount } < 5000.0 }
                    val isNetSaver          = yearlyTotalIncome > yearlyTotalSpent

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text("Personal Finance", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                                        Text("Budget Manager", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = onBack) { Text("←", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                                },
                                actions = {
                                    IconButton(onClick = { showSandboxDialog = true }) { Text("🧪", fontSize = 20.sp) }
                                    IconButton(onClick = { showFixedSettings = true }) { Text("⚙", fontSize = 22.sp) }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0x44000000))
                            )
                        },
                        containerColor = Color.Transparent
                    ) { pv ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(pv).padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Year Switcher pill
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), shimmer = false) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = {
                                            val ty = selectedYear - 1
                                            if (monthlyBudgets.none { it.year == ty }) monthlyBudgets = monthlyBudgets + createYearBudgets(ty)
                                            selectedYear = ty
                                        }) { Text("◀", color = AccentEmerald, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                                        Spacer(Modifier.width(28.dp))
                                        Text(selectedYear.toString(), color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = 2.sp)
                                        Spacer(Modifier.width(28.dp))
                                        TextButton(onClick = {
                                            val ty = selectedYear + 1
                                            if (monthlyBudgets.none { it.year == ty }) monthlyBudgets = monthlyBudgets + createYearBudgets(ty)
                                            selectedYear = ty
                                        }) { Text("▶", color = AccentEmerald, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }

                            // Yearly hero card
                            item {
                                YearlyProgressCard(yearlyTotalSpent, yearlyTotalLimit, yearlyTotalIncome)
                            }

                            // Achievements badges
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Financial Milestones & Streaks", color = TextSecondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            FinanceBadgeChip("🏆", "$budgetStreaks Months Safe", budgetStreaks > 0, AccentEmerald, Modifier.weight(1f))
                                            FinanceBadgeChip("🍔", "Frugal Hero",   isFrugalHero, AccentElectric, Modifier.weight(1f))
                                            FinanceBadgeChip("💰", "Net Surplus",   isNetSaver,   AccentAmber,    Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            // Savings Goals
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Savings Goals 🐷", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            TextButton(onClick = {
                                                val cur = budgetsForYear.firstOrNull { it.monthIndex == currentMonth } ?: budgetsForYear.firstOrNull()
                                                if (cur != null) showAllocationDialog = cur
                                            }) { Text("Allocate Surplus", color = AccentEmerald, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                                        }
                                        savingsGoals.forEach { goal ->
                                            SavingsGoalRow(goal)
                                        }
                                    }
                                }
                            }

                            // Section label
                            item {
                                Text(
                                    "Monthly Summaries ($selectedYear)",
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Month cards
                            items(budgetsForYear.sortedBy { if (it.monthIndex == currentMonth && selectedYear == currentYear) 0 else 1 }) { mb ->
                                MonthOverviewCard(
                                    monthBudget = mb,
                                    isCurrent   = mb.monthIndex == currentMonth && selectedYear == currentYear,
                                    spent       = getMonthSpent(mb),
                                    income      = getMonthIncome(mb),
                                    onClick        = { currentScreen = FinanceScreen.MonthDetails(mb.year, mb.monthIndex) },
                                    onIncomeClick  = { showIncomeSettings = mb },
                                    onReminderClick = { showRemindersDialog = mb },
                                    onSubLimitClick = { showCategoryLimitDialog = mb }
                                )
                            }

                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }

                // ── Month Detail screen ──────────────────────────────────────
                is FinanceScreen.MonthDetails -> {
                    val activeYear  = screen.year
                    val monthIndex  = screen.monthIndex
                    val monthBudget = monthlyBudgets.first { it.year == activeYear && it.monthIndex == monthIndex }
                    val monthTotal  = getMonthSpent(monthBudget)
                    val isOverLimit = monthTotal > monthBudget.limit

                    // MoM delta
                    val prevMonthIndex = if (monthIndex == 1) 12 else monthIndex - 1
                    val prevYear       = if (monthIndex == 1) activeYear - 1 else activeYear
                    val prevTotal      = monthlyBudgets.firstOrNull { it.year == prevYear && it.monthIndex == prevMonthIndex }?.let { getMonthSpent(it) } ?: 0.0
                    val momDelta       = monthTotal - prevTotal
                    val momPct         = if (prevTotal > 0.0) (momDelta / prevTotal * 100).toInt() else 0

                    // Burn rate
                    val elapsedDays        = if (monthIndex == currentMonth && activeYear == currentYear) currentDay.coerceAtLeast(1) else 30
                    val avgDailySpend      = monthTotal / elapsedDays
                    val projectedMonthEnd  = avgDailySpend * 30
                    val isVelocityWarning  = projectedMonthEnd > monthBudget.limit

                    // Search & filter
                    var searchQuery           by remember { mutableStateOf("") }
                    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
                    val filteredExpenses = monthBudget.expenses.filter { exp ->
                        exp.label.contains(searchQuery, ignoreCase = true) &&
                            (selectedCategoryFilter == null || exp.category == selectedCategoryFilter)
                    }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text("${monthBudget.monthName} $activeYear", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                                        Text("Expense Details", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { currentScreen = FinanceScreen.Overview }) { Text("←", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                                },
                                actions = {
                                    IconButton(onClick = {
                                        val csv = StringBuilder("Label,Amount,Category,Type\n")
                                        monthBudget.expenses.forEach { csv.append("\"${it.label}\",${it.amount},\"${it.category.label}\",\"${if (it.isFromTemplate) "Recurring" else "Variable"}\"\n") }
                                        showCsvExportResult = csv.toString()
                                    }) { Text("📤", fontSize = 18.sp) }
                                    IconButton(onClick = { newLimitAmount = monthBudget.limit.toString(); showEditLimitDialog = true }) { Text("✎", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0x44000000))
                            )
                        },
                        containerColor = Color.Transparent,
                        floatingActionButton = {
                            ExtendedFloatingActionButton(
                                onClick = { showAddExpenseDialog = true },
                                containerColor = AccentEmerald,
                                contentColor = Color.Black,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("+ Add Expense", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    ) { pv ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(pv).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Summary card with donut
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                borderColor = if (isOverLimit) AccentRed else BorderGlass
                            ) {
                                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                                        val catValues = mutableMapOf<ExpenseCategory, Double>()
                                        monthBudget.expenses.forEach { catValues[it.category] = (catValues[it.category] ?: 0.0) + it.amount }
                                        if (catValues.isEmpty()) Text("None", color = TextMuted, fontSize = 12.sp)
                                        else DonutChart(catValues, monthTotal, Modifier.fillMaxSize())
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("₹${monthTotal.toInt()}", color = if (isOverLimit) AccentRed else TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                                        Text("of ₹${monthBudget.limit.toInt()} limit", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                        val momColor = if (momDelta <= 0) AccentEmerald else AccentRed
                                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(momColor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                            Text(
                                                "${if (momDelta <= 0) "↓" else "↑"} ₹${Math.abs(momDelta).toInt()} ($momPct%) vs last month",
                                                color = momColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Burn rate banner
                            BurnRateBanner(isVelocityWarning, avgDailySpend, projectedMonthEnd, monthBudget.limit)

                            // Quick preset chips
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Quick Add Presets", color = TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(presetExpenses) { preset ->
                                        GlassCard(
                                            modifier = Modifier.clickable {
                                                val newItem = ExpenseItem("var_${Clock.System.now().toEpochMilliseconds()}", preset.label, preset.amount, preset.category)
                                                monthlyBudgets = monthlyBudgets.map { mb -> if (mb.year == activeYear && mb.monthIndex == monthIndex) mb.copy(expenses = mb.expenses + newItem) else mb }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            shimmer = false
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(preset.category.color))
                                                Text("${preset.label} ₹${preset.amount.toInt()}", color = TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Search bar
                            OutlinedTextField(
                                value = searchQuery, onValueChange = { searchQuery = it },
                                placeholder = { Text("Search expenses…", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentEmerald, unfocusedBorderColor = BorderGlass,
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceGlass, unfocusedContainerColor = SurfaceGlass
                                )
                            )

                            // Category filter chips
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    FilterChip(selected = selectedCategoryFilter == null, onClick = { selectedCategoryFilter = null }, label = { Text("All") },
                                        colors = FilterChipDefaults.filterChipColors(labelColor = TextSecondary, selectedLabelColor = Color.Black, selectedContainerColor = AccentEmerald))
                                }
                                items(ExpenseCategory.values()) { cat ->
                                    val limit   = monthBudget.categoryLimits[cat]
                                    val catTot  = monthBudget.expenses.filter { it.category == cat }.sumOf { it.amount }
                                    val overCat = limit != null && catTot > limit
                                    FilterChip(
                                        selected = selectedCategoryFilter == cat,
                                        onClick  = { selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat },
                                        label    = {
                                            Text(if (limit != null) "${cat.icon} ${cat.label} ₹${catTot.toInt()}/₹${limit.toInt()}"
                                                 else "${cat.icon} ${cat.label}",
                                                 color = if (overCat) AccentRed else Color.Unspecified)
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            labelColor = TextSecondary, selectedLabelColor = Color.White,
                                            selectedContainerColor = if (overCat) AccentRed else cat.color)
                                    )
                                }
                            }

                            // Expense list
                            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (filteredExpenses.isEmpty()) {
                                    item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No expenses match filters", color = TextMuted, style = MaterialTheme.typography.bodyMedium) } }
                                } else {
                                    items(filteredExpenses) { item ->
                                        val catLimit  = monthBudget.categoryLimits[item.category]
                                        val catTotal  = monthBudget.expenses.filter { it.category == item.category }.sumOf { it.amount }
                                        val overCat   = catLimit != null && catTotal > catLimit
                                        ExpenseRowItem(
                                            item      = item,
                                            onEditClick   = { showEditExpenseDialog = item },
                                            onDeleteClick = {
                                                monthlyBudgets = monthlyBudgets.map { mb ->
                                                    if (mb.year == activeYear && mb.monthIndex == monthIndex) mb.copy(expenses = mb.expenses.filterNot { it.id == item.id }) else mb
                                                }
                                            },
                                            isWarning = overCat
                                        )
                                    }
                                }
                            }

                            // Surplus / deficit summary
                            val targetSavings = monthBudget.limit - monthTotal
                            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), shimmer = false) {
                                Text(
                                    text = if (targetSavings >= 0) "💡 Projected Savings: ₹${targetSavings.toInt()} this month — keep it up!" else "💡 Deficit: ₹${(-targetSavings).toInt()} — consider pausing Leisure/Other expenses.",
                                    color = if (targetSavings >= 0) AccentEmerald else AccentAmber,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(14.dp)
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

    // ── Fixed Settings / Recurring Templates ─────────────────────────────────
    if (showFixedSettings) {
        AlertDialog(
            onDismissRequest = { showFixedSettings = false },
            title = { Text("Recurring Templates ⚙", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(modifier = Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Templates auto-populate each month's baseline expenses.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    LazyColumn(modifier = Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recurringTemplates) { tmpl ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), shimmer = false) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(tmpl.category.color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Text(tmpl.category.icon, fontSize = 16.sp) }
                                        Column {
                                            Text(tmpl.label, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                            Text("₹${tmpl.defaultAmount.toInt()} / month · ${tmpl.category.label}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    IconButton(onClick = { recurringTemplates = recurringTemplates.filterNot { it.id == tmpl.id } }, modifier = Modifier.size(28.dp)) { Text("🗑", color = AccentRed.copy(alpha = 0.8f), fontSize = 14.sp) }
                                }
                            }
                        }
                    }
                    TextButton(onClick = { showAddFixedDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("+ Add New Template", color = AccentEmerald, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            confirmButton = { Button(onClick = { showFixedSettings = false }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Done") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // Add Template sub-dialog
    if (showAddFixedDialog) {
        var tLabel    by remember { mutableStateOf("") }
        var tAmount   by remember { mutableStateOf("") }
        var tCategory by remember { mutableStateOf(ExpenseCategory.HOUSING) }
        AlertDialog(
            onDismissRequest = { showAddFixedDialog = false },
            title = { Text("New Recurring Template", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = tLabel, onValueChange = { tLabel = it }, label = { Text("Template Label") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                    OutlinedTextField(value = tAmount, onValueChange = { tAmount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Default Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                    Text("Category:", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExpenseCategory.values()) { cat ->
                            val selected = tCategory == cat
                            GlassCard(modifier = Modifier.clickable { tCategory = cat }, shape = RoundedCornerShape(10.dp), borderColor = if (selected) cat.color else BorderGlass, shimmer = false) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat.icon, fontSize = 14.sp); Text(cat.label, color = if (selected) cat.color else TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = tAmount.toDoubleOrNull() ?: 0.0
                    if (tLabel.isNotBlank() && amt > 0.0) {
                        recurringTemplates = recurringTemplates + RecurringTemplate("tmpl_${Clock.System.now().toEpochMilliseconds()}", tLabel, amt, tCategory)
                        showAddFixedDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddFixedDialog = false }) { Text("Cancel") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Add Expense dialog ────────────────────────────────────────────────────
    if (showAddExpenseDialog) {
        val detailsState = currentScreen as? FinanceScreen.MonthDetails
        val activeYear   = detailsState?.year ?: currentYear
        val monthIndex   = detailsState?.monthIndex ?: currentMonth
        var aLabel       by remember { mutableStateOf("") }
        var aAmount      by remember { mutableStateOf("") }
        var aCategory    by remember { mutableStateOf(ExpenseCategory.FOOD) }
        var splitEnabled by remember { mutableStateOf(false) }
        var splitPartner by remember { mutableStateOf("") }
        var splitPercent by remember { mutableStateOf("50") }
        var splitPaidByMe by remember { mutableStateOf(true) }
        // Autocomplete suggestions
        val suggestions = if (aLabel.length >= 2) autocompleteDb.filter { it.query.startsWith(aLabel.lowercase()) } else emptyList()

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Add Expense", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.width(380.dp)) {
                    // Label field
                    OutlinedTextField(value = aLabel, onValueChange = { aLabel = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                    // Autocomplete chips
                    if (suggestions.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(suggestions) { sug ->
                                GlassCard(modifier = Modifier.clickable { aLabel = sug.label; aAmount = sug.defaultAmount.toString(); aCategory = sug.category }, shape = RoundedCornerShape(10.dp), shimmer = false) {
                                    Text("${sug.category.icon} ${sug.label}", color = TextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                    // Amount
                    OutlinedTextField(value = aAmount, onValueChange = { aAmount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                    // Category row
                    Text("Category", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExpenseCategory.values()) { cat ->
                            val sel = aCategory == cat
                            GlassCard(modifier = Modifier.clickable { aCategory = cat }, shape = RoundedCornerShape(12.dp), borderColor = if (sel) cat.color else BorderGlass, shimmer = false) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(cat.icon, fontSize = 18.sp); Text(cat.label, color = if (sel) cat.color else TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    // Split bill toggle
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Split Bill?", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = splitEnabled, onCheckedChange = { splitEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = AccentEmerald, checkedTrackColor = AccentEmerald.copy(alpha = 0.4f)))
                    }
                    AnimatedVisibility(visible = splitEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = splitPartner, onValueChange = { splitPartner = it }, label = { Text("Partner's Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                            OutlinedTextField(value = splitPercent, onValueChange = { splitPercent = it.filter { c -> c.isDigit() } }, label = { Text("My Share (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Paid by me?", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Switch(checked = splitPaidByMe, onCheckedChange = { splitPaidByMe = it }, colors = SwitchDefaults.colors(checkedThumbColor = AccentEmerald, checkedTrackColor = AccentEmerald.copy(alpha = 0.4f)))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = aAmount.toDoubleOrNull() ?: 0.0
                    if (aLabel.isNotBlank() && amt > 0.0) {
                        val split = if (splitEnabled && splitPartner.isNotBlank()) SplitBill(splitPartner, amt, splitPercent.toDoubleOrNull() ?: 50.0, splitPaidByMe) else null
                        val newItem = ExpenseItem("var_${Clock.System.now().toEpochMilliseconds()}", aLabel, amt, aCategory, partnerSplit = split)
                        monthlyBudgets = monthlyBudgets.map { mb -> if (mb.year == activeYear && mb.monthIndex == monthIndex) mb.copy(expenses = mb.expenses + newItem) else mb }
                        showAddExpenseDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Add Expense") }
            },
            dismissButton = { TextButton(onClick = { showAddExpenseDialog = false }) { Text("Cancel") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Edit Expense dialog ───────────────────────────────────────────────────
    showEditExpenseDialog?.let { origItem ->
        val detailsState = currentScreen as? FinanceScreen.MonthDetails
        val activeYear   = detailsState?.year ?: currentYear
        val monthIndex   = detailsState?.monthIndex ?: currentMonth
        var eLabel    by remember { mutableStateOf(origItem.label) }
        var eAmount   by remember { mutableStateOf(origItem.amount.toString()) }
        var eCategory by remember { mutableStateOf(origItem.category) }
        AlertDialog(
            onDismissRequest = { showEditExpenseDialog = null },
            title = { Text("Edit Expense", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = eLabel, onValueChange = { eLabel = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                    OutlinedTextField(value = eAmount, onValueChange = { eAmount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                    Text("Category", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExpenseCategory.values()) { cat ->
                            val sel = eCategory == cat
                            GlassCard(modifier = Modifier.clickable { eCategory = cat }, shape = RoundedCornerShape(12.dp), borderColor = if (sel) cat.color else BorderGlass, shimmer = false) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(cat.icon, fontSize = 18.sp); Text(cat.label, color = if (sel) cat.color else TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = eAmount.toDoubleOrNull() ?: origItem.amount
                    if (eLabel.isNotBlank()) {
                        val updated = origItem.copy(label = eLabel, amount = amt, category = eCategory)
                        monthlyBudgets = monthlyBudgets.map { mb ->
                            if (mb.year == activeYear && mb.monthIndex == monthIndex) mb.copy(expenses = mb.expenses.map { if (it.id == origItem.id) updated else it }) else mb
                        }
                        showEditExpenseDialog = null
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditExpenseDialog = null }) { Text("Cancel") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Sandbox Simulator ─────────────────────────────────────────────────────
    if (showSandboxDialog) {
        val cb           = monthlyBudgets.firstOrNull { it.year == selectedYear && it.monthIndex == currentMonth } ?: monthlyBudgets.first { it.year == selectedYear }
        val curSpent     = getMonthSpent(cb)
        val curIncome    = getMonthIncome(cb)
        val simSalInc    = simulatedSalaryIncrease.toDoubleOrNull() ?: 0.0
        val simLimRed    = simulatedLimitReduction.toDoubleOrNull() ?: 0.0
        val subSavings   = if (simulatedSubscriptionCut) recurringTemplates.sumOf { it.defaultAmount } else 0.0
        val simIncome    = curIncome + simSalInc
        val simLimit     = cb.limit - simLimRed
        val simSpend     = curSpent - subSavings
        val simSurplus   = simLimit - simSpend
        val simSavRate   = if (simIncome > 0.0) ((simIncome - simSpend) / simIncome * 100).toInt() else 0

        AlertDialog(
            onDismissRequest = { showSandboxDialog = false },
            title = { Text("What-If Sandbox 🧪", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Simulate financial tweaks to see instant projections.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = simulatedSalaryIncrease, onValueChange = { simulatedSalaryIncrease = it.filter { c -> c.isDigit() } }, label = { Text("Salary Increase (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                    OutlinedTextField(value = simulatedLimitReduction, onValueChange = { simulatedLimitReduction = it.filter { c -> c.isDigit() } }, label = { Text("Reduce Monthly Limit by (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cut all recurring templates?", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = simulatedSubscriptionCut, onCheckedChange = { simulatedSubscriptionCut = it }, colors = SwitchDefaults.colors(checkedThumbColor = AccentEmerald, checkedTrackColor = AccentEmerald.copy(alpha = 0.4f)))
                    }
                    HorizontalDivider(color = DividerGlass)
                    Text("Simulated Projections:", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassCard(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), shimmer = false) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Limit",  color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Text("₹${simLimit.toInt()}", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Spent", color = TextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                                Text("₹${simSpend.toInt()}", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        GlassCard(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), borderColor = if (simSurplus >= 0) AccentEmerald else AccentRed, shimmer = false) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Surplus", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Text("₹${simSurplus.toInt()}", color = if (simSurplus >= 0) AccentEmerald else AccentRed, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                Text("Savings Rate", color = TextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                                Text("$simSavRate%", color = AccentElectric, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showSandboxDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Close") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Allocate Surplus dialog ───────────────────────────────────────────────
    showAllocationDialog?.let { mb ->
        val surplus = mb.limit - getMonthSpent(mb)
        AlertDialog(
            onDismissRequest = { showAllocationDialog = null },
            title = { Text("Allocate Surplus 💰", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.width(360.dp)) {
                    Text("₹${surplus.toInt()} surplus from ${mb.monthName} ${mb.year}. Choose a savings goal:", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    if (surplus <= 0.0) {
                        Text("No surplus available to allocate.", color = AccentRed, fontWeight = FontWeight.Bold)
                    } else {
                        savingsGoals.forEach { goal ->
                            GlassCard(modifier = Modifier.fillMaxWidth().clickable {
                                savingsGoals = savingsGoals.map { sg -> if (sg.id == goal.id) sg.copy(saved = (sg.saved + surplus).coerceAtMost(sg.target)) else sg }
                                monthlyBudgets = monthlyBudgets.map { m -> if (m.year == mb.year && m.monthIndex == mb.monthIndex) m.copy(limit = getMonthSpent(mb)) else m }
                                showAllocationDialog = null
                            }, shape = RoundedCornerShape(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(goal.label, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("+₹${surplus.toInt()}", color = AccentEmerald, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAllocationDialog = null }) { Text("Cancel") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Income Manager dialog ─────────────────────────────────────────────────
    showIncomeSettings?.let { mb ->
        AlertDialog(
            onDismissRequest = { showIncomeSettings = null },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Income Manager · ${mb.monthName}", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { showAddIncomeDialog = true }) { Text("+", color = AccentEmerald, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                }
            },
            text = {
                Column(modifier = Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total Income: ₹${getMonthIncome(mb).toInt()}", color = AccentEmerald, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    LazyColumn(modifier = Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(mb.incomes) { inc ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), shimmer = false) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(inc.label, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("₹${inc.amount.toInt()}", color = AccentEmerald, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(onClick = {
                                        monthlyBudgets = monthlyBudgets.map { m -> if (m.year == mb.year && m.monthIndex == mb.monthIndex) m.copy(incomes = m.incomes.filterNot { it.id == inc.id }) else m }
                                        showIncomeSettings = monthlyBudgets.first { it.year == mb.year && it.monthIndex == mb.monthIndex }
                                    }) { Text("🗑", color = AccentRed, fontSize = 16.sp) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showIncomeSettings = null }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Close") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // Add Income
    if (showAddIncomeDialog) {
        val activeMB  = showIncomeSettings
        var incLabel  by remember { mutableStateOf("") }
        var incAmount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddIncomeDialog = false },
            title = { Text("Add Income Source", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = incLabel, onValueChange = { incLabel = it }, label = { Text("Income description") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                    OutlinedTextField(value = incAmount, onValueChange = { incAmount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = incAmount.toDoubleOrNull() ?: 0.0
                    if (incLabel.isNotBlank() && amt > 0.0 && activeMB != null) {
                        monthlyBudgets = monthlyBudgets.map { mb -> if (mb.year == activeMB.year && mb.monthIndex == activeMB.monthIndex) mb.copy(incomes = mb.incomes + IncomeItem("inc_${Clock.System.now().toEpochMilliseconds()}", incLabel, amt)) else mb }
                        showIncomeSettings = monthlyBudgets.first { it.year == activeMB.year && it.monthIndex == activeMB.monthIndex }
                        showAddIncomeDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddIncomeDialog = false }) { Text("Cancel") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Category Sub-limits ───────────────────────────────────────────────────
    showCategoryLimitDialog?.let { mb ->
        var catLimitVal by remember { mutableStateOf("") }
        var selectedCat by remember { mutableStateOf(ExpenseCategory.FOOD) }
        AlertDialog(
            onDismissRequest = { showCategoryLimitDialog = null },
            title = { Text("Category Sub-Limits ⚡", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Set a monthly warning threshold per category.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    // Category selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExpenseCategory.values()) { cat ->
                            val sel = selectedCat == cat
                            GlassCard(modifier = Modifier.clickable { selectedCat = cat }, shape = RoundedCornerShape(12.dp), borderColor = if (sel) cat.color else BorderGlass, shimmer = false) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(cat.icon, fontSize = 18.sp)
                                    Text(cat.label, color = if (sel) cat.color else TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    val existing = mb.categoryLimits[cat]
                                    if (existing != null) Text("₹${existing.toInt()}", color = AccentAmber, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    OutlinedTextField(value = catLimitVal, onValueChange = { catLimitVal = it.filter { c -> c.isDigit() } }, label = { Text("Limit (₹) — blank to remove") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = catLimitVal.toDoubleOrNull()
                    monthlyBudgets = monthlyBudgets.map { m ->
                        if (m.year == mb.year && m.monthIndex == mb.monthIndex) {
                            val updated = m.categoryLimits.toMutableMap()
                            if (amt != null) updated[selectedCat] = amt else updated.remove(selectedCat)
                            m.copy(categoryLimits = updated)
                        } else m
                    }
                    showCategoryLimitDialog = null
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showCategoryLimitDialog = null }) { Text("Cancel") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Bill Reminders ────────────────────────────────────────────────────────
    showRemindersDialog?.let { mb ->
        AlertDialog(
            onDismissRequest = { showRemindersDialog = null },
            title = { Text("Bill Reminders 🔔", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.width(360.dp)) {
                    Text("Toggle payment status of recurring monthly bills.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    LazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(mb.billReminders) { rem ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), borderColor = if (rem.isPaid) AccentEmerald else BorderGlass, shimmer = false) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(rem.label, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("Due: Day ${rem.dueDay} · ${if (rem.isPaid) "✅ Paid" else "⏳ Pending"}", color = if (rem.isPaid) AccentEmerald else AccentAmber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Checkbox(checked = rem.isPaid, onCheckedChange = { checked ->
                                        monthlyBudgets = monthlyBudgets.map { m ->
                                            if (m.year == mb.year && m.monthIndex == mb.monthIndex) m.copy(billReminders = m.billReminders.map { r -> if (r.id == rem.id) r.copy(isPaid = checked) else r }) else m
                                        }
                                        showRemindersDialog = monthlyBudgets.first { it.year == mb.year && it.monthIndex == mb.monthIndex }
                                    })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showRemindersDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Done") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // ── CSV Export ────────────────────────────────────────────────────────────
    showCsvExportResult?.let { csvText ->
        AlertDialog(
            onDismissRequest = { showCsvExportResult = null },
            title = { Text("Exported CSV Data 📤", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = { OutlinedTextField(value = csvText, onValueChange = {}, readOnly = true, modifier = Modifier.height(180.dp).fillMaxWidth(), colors = glassTextFieldColors()) },
            confirmButton = { Button(onClick = { showCsvExportResult = null }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Dismiss") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Edit Month Limit ──────────────────────────────────────────────────────
    if (showEditLimitDialog) {
        val detailsState = currentScreen as? FinanceScreen.MonthDetails
        val monthIdx   = detailsState?.monthIndex ?: currentMonth
        val activeYear = detailsState?.year ?: selectedYear
        val mb         = monthlyBudgets.first { it.year == activeYear && it.monthIndex == monthIdx }
        AlertDialog(
            onDismissRequest = { showEditLimitDialog = false },
            title = { Text("Edit Limit · ${mb.monthName}", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = { OutlinedTextField(value = newLimitAmount, onValueChange = { newLimitAmount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Monthly Budget Limit (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = glassTextFieldColors()) },
            confirmButton = {
                Button(onClick = {
                    val lim = newLimitAmount.toDoubleOrNull() ?: mb.limit
                    monthlyBudgets = monthlyBudgets.map { m -> if (m.year == activeYear && m.monthIndex == monthIdx) m.copy(limit = lim) else m }
                    showEditLimitDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditLimitDialog = false }) { Text("Cancel") } },
            containerColor = Color(0xCC060D1F), modifier = Modifier.border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Glass TextField Colors helper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun glassTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AccentEmerald,
    unfocusedBorderColor = BorderGlass,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    focusedLabelColor    = AccentEmerald,
    unfocusedLabelColor  = TextSecondary,
    cursorColor          = AccentEmerald,
    focusedContainerColor   = SurfaceGlass,
    unfocusedContainerColor = SurfaceGlass
)

// ─────────────────────────────────────────────────────────────────────────────
// YearlyProgressCard — animated hero ring
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun YearlyProgressCard(yearlyTotal: Double, yearlyLimit: Double, yearlyIncome: Double) {
    val ratio      = (yearlyTotal / yearlyLimit).toFloat().coerceIn(0f, 1f)
    val isOver     = yearlyTotal > yearlyLimit
    val progressColor = when {
        isOver    -> AccentRed
        ratio > 0.8f -> AccentAmber
        else      -> AccentEmerald
    }

    // Animated ring entrance
    val animatedRatio by animateFloatAsState(
        targetValue  = ratio,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "yearly-ring"
    )

    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                // Animated ring
                Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sw = 18f
                        drawCircle(color = DividerGlass, radius = (size.minDimension - sw) / 2f, style = Stroke(sw))
                        drawArc(color = progressColor.copy(alpha = 0.25f), startAngle = -90f, sweepAngle = animatedRatio * 360f, useCenter = false, style = Stroke(sw + 8f, cap = StrokeCap.Round))
                        drawArc(color = progressColor, startAngle = -90f, sweepAngle = animatedRatio * 360f, useCenter = false, style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(ratio * 100).toInt()}%", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Text("spent", color = TextSecondary, fontSize = 10.sp)
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Annual Budget", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Text("₹${yearlyTotal.toInt()}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                    Text("of ₹${yearlyLimit.toInt()}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(color = DividerGlass)

            // Net Cash Flow
            val net  = yearlyIncome - yearlyTotal
            val rate = if (yearlyIncome > 0.0) ((net / yearlyIncome) * 100).toInt() else 0
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Net Cash Flow", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(if (net >= 0) "Surplus +₹${net.toInt()}" else "Deficit -₹${(-net).toInt()}", color = if (net >= 0) AccentEmerald else AccentRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (net >= 0) AccentEmerald.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("$rate% Savings", color = if (net >= 0) AccentEmerald else AccentRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SavingsGoalRow — animated spring progress bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SavingsGoalRow(goal: SavingsGoal) {
    val progress = (goal.saved / goal.target).toFloat().coerceIn(0f, 1f)
    val animProg by animateFloatAsState(progress, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "goal-prog")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(3.dp).height(32.dp).clip(CircleShape).background(AccentEmerald))
                Column {
                    Text(goal.label, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text("${(progress * 100).toInt()}% funded", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("₹${goal.saved.toInt()} / ₹${goal.target.toInt()}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(DividerGlass)) {
            Box(modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(AccentEmerald.copy(alpha = 0.8f), AccentElectric))))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FinanceBadgeChip — M3 Expressive badge pill
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FinanceBadgeChip(icon: String, label: String, isActive: Boolean, activeColor: Color, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (isActive) 1.04f else 1f, animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "chip-scale")

    GlassCard(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        borderColor = if (isActive) activeColor else BorderGlass,
        shimmer = false
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 18.sp)
            Text(label, color = if (isActive) activeColor else TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MonthOverviewCard — glass card with animated progress
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MonthOverviewCard(
    monthBudget: MonthBudget, isCurrent: Boolean, spent: Double, income: Double,
    onClick: () -> Unit, onIncomeClick: () -> Unit, onReminderClick: () -> Unit, onSubLimitClick: () -> Unit
) {
    val isOverLimit   = spent > monthBudget.limit
    val ratio         = (spent / monthBudget.limit).toFloat().coerceIn(0f, 1f)
    val progressColor = when { isOverLimit -> AccentRed; ratio > 0.8f -> AccentAmber; else -> AccentEmerald }
    val animProg      by animateFloatAsState(ratio, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "month-prog")

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        borderColor = when { isCurrent -> AccentEmerald; isOverLimit -> AccentRed; else -> BorderGlass }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(monthBudget.monthName, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (isCurrent) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentEmerald.copy(alpha = 0.2f)).border(BorderStroke(1.dp, AccentEmerald), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Current", color = AccentEmerald, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("₹${spent.toInt()} / ₹${monthBudget.limit.toInt()}", color = if (isOverLimit) AccentRed else TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            // Animated progress bar
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(DividerGlass)) {
                Box(modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(progressColor.copy(alpha = 0.7f), progressColor))))
            }

            // Action chips row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Income chip
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AccentEmerald.copy(alpha = 0.12f)).border(BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.4f)), RoundedCornerShape(8.dp)).clickable(onClick = onIncomeClick).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("₹${income.toInt()} income", color = AccentEmerald, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    // Bills chip
                    val pendingBills = monthBudget.billReminders.count { !it.isPaid }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (pendingBills > 0) AccentAmber.copy(alpha = 0.12f) else SurfaceGlass).border(BorderStroke(1.dp, if (pendingBills > 0) AccentAmber.copy(alpha = 0.5f) else BorderGlass), RoundedCornerShape(8.dp)).clickable(onClick = onReminderClick).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("$pendingBills bills due", color = if (pendingBills > 0) AccentAmber else TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                // Limits chip
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceGlass).border(BorderStroke(1.dp, BorderGlass), RoundedCornerShape(8.dp)).clickable(onClick = onSubLimitClick).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Limits ⚙", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BurnRateBanner — expressive warning / safe banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BurnRateBanner(isWarning: Boolean, avgDaily: Double, projected: Double, limit: Double) {
    val pulseAnim by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.85f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse-scale"
    )
    val scale = if (isWarning) pulseAnim else 1f
    val color = if (isWarning) AccentRed else AccentEmerald

    GlassCard(modifier = Modifier.fillMaxWidth().scale(scale), shape = RoundedCornerShape(16.dp), borderColor = color.copy(alpha = 0.6f), shimmer = false) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (isWarning) "🚨" else "✅", fontSize = 22.sp)
            Column {
                Text(if (isWarning) "Velocity Warning — High burn rate!" else "Daily velocity is safe", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("₹${avgDaily.toInt()}/day avg · Projected ₹${projected.toInt()} (Limit ₹${limit.toInt()})", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ExpenseRowItem — glass row with category accent square
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ExpenseRowItem(item: ExpenseItem, onEditClick: () -> Unit, onDeleteClick: () -> Unit, isWarning: Boolean) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEditClick),
        shape = RoundedCornerShape(16.dp),
        borderColor = if (isWarning) AccentRed.copy(alpha = 0.5f) else BorderGlass,
        shimmer = false
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Category icon square
                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(item.category.color.copy(alpha = 0.2f)).border(BorderStroke(1.dp, item.category.color.copy(alpha = 0.4f)), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Text(item.category.icon, fontSize = 18.sp)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.label, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (item.isFromTemplate) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AccentAmber.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                Text("Recurring", color = AccentAmber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    val subtitle = when {
                        item.partnerSplit != null -> "Split: ${item.partnerSplit.sharePercent.toInt()}% · ${if (item.partnerSplit.paidByMe) "Paid by me" else "Paid by ${item.partnerSplit.partnerName}"}"
                        item.foreignCurrency != null -> "Converted: ${item.foreignCurrency} ${item.foreignAmount?.toInt()} @ ₹${item.exchangeRate}"
                        else -> if (item.isFromTemplate) "Template baseline" else "Logged manually"
                    }
                    Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("₹${item.amount.toInt()}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) { Text("🗑", color = AccentRed.copy(alpha = 0.7f), fontSize = 15.sp) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DonutChart
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DonutChart(categoryValues: Map<ExpenseCategory, Double>, totalAmount: Double, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw       = 22f
        val chartSz  = size.minDimension - sw
        val topLeft  = Offset((size.width - chartSz) / 2f, (size.height - chartSz) / 2f)
        val rectSize = Size(chartSz, chartSz)
        var start    = -90f
        categoryValues.forEach { (cat, amt) ->
            val sweep = ((amt / totalAmount) * 360f).toFloat()
            drawArc(color = cat.color.copy(alpha = 0.3f), startAngle = start, sweepAngle = sweep, useCenter = false, topLeft = topLeft, size = rectSize, style = Stroke(sw + 6f, cap = StrokeCap.Round))
            drawArc(color = cat.color, startAngle = start, sweepAngle = sweep, useCenter = false, topLeft = topLeft, size = rectSize, style = Stroke(sw, cap = StrokeCap.Round))
            start += sweep
        }
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Spent", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("₹${totalAmount.toInt()}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        }
    }
}
