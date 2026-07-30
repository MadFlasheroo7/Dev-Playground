package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.Dish
import pro.jayeshseth.devcontrols.data.DailyPlan
import pro.jayeshseth.devcontrols.data.MealPlannerPrefs
import pro.jayeshseth.devcontrols.data.WeeklyPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { MealPlannerPrefs.get(context) }

    val dishes by prefs.dishes.collectAsState(initial = emptyList())
    val weeklyPlan by prefs.weeklyPlan.collectAsState(initial = WeeklyPlan())

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("📅 Weekly Plan", "🍳 My Dishes", "🛒 Grocery List")

    var showAddDishDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Planner & Randomizer") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddDishDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text("➕", fontSize = 20.sp)
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            // Tab Selector Row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Medium) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> WeeklyPlanTab(
                        plan = weeklyPlan,
                        allDishes = dishes,
                        onReRollAll = { scope.launch { prefs.randomizeWeeklyPlan() } },
                        onReRollSlot = { day, meal -> scope.launch { prefs.randomizeSlot(day, meal) } },
                        onManualSelect = { day, meal, id -> scope.launch { prefs.assignDishToSlot(day, meal, id) } }
                    )
                    1 -> DishesTab(
                        dishes = dishes,
                        onDelete = { id -> scope.launch { prefs.deleteDish(id) } }
                    )
                    2 -> GroceryTab(
                        plan = weeklyPlan,
                        allDishes = dishes
                    )
                }
            }
        }
    }

    if (showAddDishDialog) {
        AddDishDialog(
            onDismiss = { showAddDishDialog = false },
            onAdd = { newDish ->
                scope.launch { prefs.addDish(newDish) }
                showAddDishDialog = false
            }
        )
    }
}

// ── Tab 1: Weekly Plan Calendar Grid ──────────────────────────────────────────

@Composable
private fun WeeklyPlanTab(
    plan: WeeklyPlan,
    allDishes: List<Dish>,
    onReRollAll: () -> Unit,
    onReRollSlot: (day: String, meal: String) -> Unit,
    onManualSelect: (day: String, meal: String, dishId: String?) -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF00796B), Color(0xFF00897B))
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Roll all dashboard header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(gradient)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Need a plan for the week?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Auto-generate matching slots", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Button(
                    onClick = onReRollAll,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF00796B))
                ) {
                    Text("🔄", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Roll Week")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(MealPlannerPrefs.DAYS_OF_WEEK) { day ->
                val dayPlan = plan.days[day] ?: DailyPlan()
                DayCard(
                    day = day,
                    dayPlan = dayPlan,
                    allDishes = allDishes,
                    onReRollSlot = { meal -> onReRollSlot(day, meal) },
                    onManualSelect = { meal, id -> onManualSelect(day, meal, id) }
                )
            }
        }
    }
}

@Composable
private fun DayCard(
    day: String,
    dayPlan: DailyPlan,
    allDishes: List<Dish>,
    onReRollSlot: (String) -> Unit,
    onManualSelect: (String, String?) -> Unit
) {
    val breakfast = allDishes.firstOrNull { it.id == dayPlan.breakfastDishId }
    val lunch = allDishes.firstOrNull { it.id == dayPlan.lunchDishId }
    val dinner = allDishes.firstOrNull { it.id == dayPlan.dinnerDishId }

    val totalCalories = (breakfast?.calories ?: 0) + (lunch?.calories ?: 0) + (dinner?.calories ?: 0)
    val totalProtein = (breakfast?.proteinGrams ?: 0) + (lunch?.proteinGrams ?: 0) + (dinner?.proteinGrams ?: 0)
    val totalCarbs = (breakfast?.carbsGrams ?: 0) + (lunch?.carbsGrams ?: 0) + (dinner?.carbsGrams ?: 0)
    val totalFats = (breakfast?.fatsGrams ?: 0) + (lunch?.fatsGrams ?: 0) + (dinner?.fatsGrams ?: 0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (totalCalories > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "🔥 $totalCalories kcal",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "🍗 ${totalProtein}g P  •  🍞 ${totalCarbs}g C  •  🥑 ${totalFats}g F",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            MealSlotRow("🍳 Breakfast", dayPlan.breakfastDishId, allDishes, "Breakfast", onReRollSlot, onManualSelect)
            Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            MealSlotRow("☀️ Lunch", dayPlan.lunchDishId, allDishes, "Lunch", onReRollSlot, onManualSelect)
            Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            MealSlotRow("🌙 Dinner", dayPlan.dinnerDishId, allDishes, "Dinner", onReRollSlot, onManualSelect)
        }
    }
}

@Composable
private fun MealSlotRow(
    label: String,
    assignedId: String?,
    allDishes: List<Dish>,
    mealType: String,
    onReRollSlot: (String) -> Unit,
    onManualSelect: (String, String?) -> Unit
) {
    val assignedDish = allDishes.firstOrNull { it.id == assignedId }
    val candidates = allDishes.filter { it.course == mealType }

    var expandedMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Box {
                Text(
                    text = assignedDish?.name ?: "Click to select a dish",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (assignedDish != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (assignedDish != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.clickable { expandedMenu = true }
                )
                DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("None (Empty Slot)") },
                        onClick = { onManualSelect(mealType, null); expandedMenu = false }
                    )
                    candidates.forEach { dish ->
                        DropdownMenuItem(
                            text = { Text(dish.name) },
                            onClick = { onManualSelect(mealType, dish.id); expandedMenu = false }
                        )
                    }
                }
            }
        }

        IconButton(onClick = { onReRollSlot(mealType) }) {
            Text("🔄", fontSize = 18.sp)
        }
    }
}

// ── Tab 2: My Dishes Database ────────────────────────────────────────────────

@Composable
private fun DishesTab(
    dishes: List<Dish>,
    onDelete: (String) -> Unit
) {
    if (dishes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No dishes in your database yet.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("Tap + below to add your first dish!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dishes) { dish ->
                DishItemCard(dish = dish, onDelete = { onDelete(dish.id) })
            }
        }
    }
}

@Composable
private fun DishItemCard(dish: Dish, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dish.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    DifficultyTag(dish.difficulty)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${dish.course} • ⏱️ ${dish.prepTimeMin} mins • 🔥 ${dish.calories} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (dish.calories > 0 || dish.proteinGrams > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "🍗 ${dish.proteinGrams}g Protein  •  🍞 ${dish.carbsGrams}g Carbs  •  🥑 ${dish.fatsGrams}g Fats",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (dish.ingredients.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Ingredients: ${dish.ingredients.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Text("🗑️", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun DifficultyTag(difficulty: String) {
    val (color, text) = when (difficulty) {
        "Easy" -> Color(0xFF2E7D32) to "Easy"
        "Medium" -> Color(0xFFEF6C00) to "Medium"
        else -> Color(0xFFC62828) to "Hard"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Tab 3: Grocery List Compiler ─────────────────────────────────────────────

@Composable
private fun GroceryTab(
    plan: WeeklyPlan,
    allDishes: List<Dish>
) {
    // Compile active ingredients list
    val activeDishIds = plan.days.values.flatMap {
        listOfNotNull(it.breakfastDishId, it.lunchDishId, it.dinnerDishId)
    }
    val activeDishes = activeDishIds.mapNotNull { id -> allDishes.firstOrNull { it.id == id } }
    val compiledIngredients = activeDishes.flatMap { it.ingredients }
        .map { it.trim().lowercase().capitalize() }
        .distinct()
        .sorted()

    if (compiledIngredients.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Your weekly plan is empty. Generate a plan to see a grocery list!",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        var checkedItems by remember { mutableStateOf(emptySet<String>()) }

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "${compiledIngredients.size} items needed this week",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(compiledIngredients) { item ->
                    val isChecked = checkedItems.contains(item)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                checkedItems = if (isChecked) {
                                    checkedItems - item
                                } else {
                                    checkedItems + item
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    checkedItems = if (isChecked) {
                                        checkedItems - item
                                    } else {
                                        checkedItems + item
                                    }
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Dialog: Add New Dish ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDishDialog(
    onDismiss: () -> Unit,
    onAdd: (Dish) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("Lunch") }
    val courses = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    var expandedCourse by remember { mutableStateOf(false) }

    var difficulty by remember { mutableStateOf("Medium") }
    val difficulties = listOf("Easy", "Medium", "Hard")
    var expandedDiff by remember { mutableStateOf(false) }

    var prepTime by remember { mutableStateOf("20") }
    var ingredientsStr by remember { mutableStateOf("") }

    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val ingredientsList = ingredientsStr.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        onAdd(
                            Dish(
                                name = name.trim(),
                                course = course,
                                prepTimeMin = prepTime.toIntOrNull() ?: 20,
                                difficulty = difficulty,
                                ingredients = ingredientsList,
                                calories = calories.toIntOrNull() ?: 0,
                                proteinGrams = protein.toIntOrNull() ?: 0,
                                carbsGrams = carbs.toIntOrNull() ?: 0,
                                fatsGrams = fats.toIntOrNull() ?: 0
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add Dish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Add Custom Dish") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Dish Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Course Dropdown Selector
                ExposedDropdownMenuBox(
                    expanded = expandedCourse,
                    onExpandedChange = { expandedCourse = it }
                ) {
                    OutlinedTextField(
                        value = course,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Course Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCourse) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCourse,
                        onDismissRequest = { expandedCourse = false }
                    ) {
                        courses.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { course = c; expandedCourse = false }
                            )
                        }
                    }
                }

                // Difficulty Selector
                ExposedDropdownMenuBox(
                    expanded = expandedDiff,
                    onExpandedChange = { expandedDiff = it }
                ) {
                    OutlinedTextField(
                        value = difficulty,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Difficulty") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedDiff) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDiff,
                        onDismissRequest = { expandedDiff = false }
                    ) {
                        difficulties.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d) },
                                onClick = { difficulty = d; expandedDiff = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = prepTime,
                    onValueChange = { prepTime = it.filter { char -> char.isDigit() } },
                    label = { Text("Prep Time (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ingredientsStr,
                    onValueChange = { ingredientsStr = it },
                    label = { Text("Ingredients (comma-separated)") },
                    placeholder = { Text("e.g. Tomato, Cheese, Bread") },
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()
                Text("Nutrition Information (Optional)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { char -> char.isDigit() } },
                    label = { Text("Calories (kcal)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { char -> char.isDigit() } },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { char -> char.isDigit() } },
                        label = { Text("Carbs (g)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it.filter { char -> char.isDigit() } },
                        label = { Text("Fats (g)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}
