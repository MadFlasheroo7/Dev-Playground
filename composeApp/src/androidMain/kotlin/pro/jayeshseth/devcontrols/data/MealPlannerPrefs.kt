package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class Dish(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val course: String, // "Breakfast", "Lunch", "Dinner", "Snack"
    val prepTimeMin: Int = 20,
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
    val ingredients: List<String> = emptyList(),
    val calories: Int = 0,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatsGrams: Int = 0
)

@Serializable
data class DailyPlan(
    val breakfastDishId: String? = null,
    val breakfastCustomName: String? = null,
    val lunchDishId: String? = null,
    val lunchCustomName: String? = null,
    val dinnerDishId: String? = null,
    val dinnerCustomName: String? = null
)

@Serializable
data class WeeklyPlan(
    val days: Map<String, DailyPlan> = emptyMap()
)

private val Context.mealPlannerDataStore: DataStore<Preferences> by preferencesDataStore(name = "meal_planner_prefs")

class MealPlannerPrefs private constructor(private val context: Context) {
    private val store get() = context.mealPlannerDataStore
    private val json = Json { ignoreUnknownKeys = true }

    // --- Flows ---

    val dishes: Flow<List<Dish>> = store.data.map { prefs ->
        val raw = prefs[KEY_DISHES] ?: ""
        if (raw.isEmpty()) {
            defaultDishes
        } else {
            runCatching { json.decodeFromString<List<Dish>>(raw) }.getOrElse { defaultDishes }
        }
    }

    val weeklyPlan: Flow<WeeklyPlan> = store.data.map { prefs ->
        val raw = prefs[KEY_WEEKLY_PLAN] ?: ""
        if (raw.isEmpty()) {
            emptyWeeklyPlan
        } else {
            runCatching { json.decodeFromString<WeeklyPlan>(raw) }.getOrElse { emptyWeeklyPlan }
        }
    }

    // --- Dish CRUD Operations ---

    suspend fun addDish(dish: Dish) {
        val current = dishes.first().toMutableList()
        current.add(dish)
        saveDishesList(current)
    }

    suspend fun updateDish(updated: Dish) {
        val current = dishes.first().map {
            if (it.id == updated.id) updated else it
        }
        saveDishesList(current)
    }

    suspend fun deleteDish(dishId: String) {
        val current = dishes.first().filter { it.id != dishId }
        saveDishesList(current)

        // Clean up references in the weekly plan
        val currentPlan = weeklyPlan.first()
        val cleanedDays = currentPlan.days.mapValues { (_, daily) ->
            DailyPlan(
                breakfastDishId = daily.breakfastDishId?.takeIf { it != dishId },
                breakfastCustomName = if (daily.breakfastDishId == dishId) "Deleted Dish" else daily.breakfastCustomName,
                lunchDishId = daily.lunchDishId?.takeIf { it != dishId },
                lunchCustomName = if (daily.lunchDishId == dishId) "Deleted Dish" else daily.lunchCustomName,
                dinnerDishId = daily.dinnerDishId?.takeIf { it != dishId },
                dinnerCustomName = if (daily.dinnerDishId == dishId) "Deleted Dish" else daily.dinnerCustomName
            )
        }
        savePlan(WeeklyPlan(cleanedDays))
    }

    private suspend fun saveDishesList(list: List<Dish>) {
        val raw = json.encodeToString(list)
        store.edit { it[KEY_DISHES] = raw }
    }

    // --- Plan Operations ---

    suspend fun savePlan(plan: WeeklyPlan) {
        val raw = json.encodeToString(plan)
        store.edit { it[KEY_WEEKLY_PLAN] = raw }
    }

    /**
     * Auto-generates a weekly meal plan by randomly selecting dishes matching the course types.
     */
    suspend fun randomizeWeeklyPlan() {
        val allDishes = dishes.first()
        val breakfasts = allDishes.filter { it.course == "Breakfast" }
        val lunches = allDishes.filter { it.course == "Lunch" }
        val dinners = allDishes.filter { it.course == "Dinner" }

        val newDays = DAYS_OF_WEEK.associateWith { _ ->
            DailyPlan(
                breakfastDishId = breakfasts.randomOrNull()?.id,
                lunchDishId = lunches.randomOrNull()?.id,
                dinnerDishId = dinners.randomOrNull()?.id
            )
        }
        savePlan(WeeklyPlan(newDays))
    }

    /**
     * Re-randomizes a single slot (day + meal type).
     */
    suspend fun randomizeSlot(day: String, mealType: String) {
        val allDishes = dishes.first()
        val candidates = allDishes.filter { it.course == mealType }
        val randomDish = candidates.randomOrNull()

        val currentPlan = weeklyPlan.first()
        val currentDayPlan = currentPlan.days[day] ?: DailyPlan()

        val updatedDayPlan = when (mealType) {
            "Breakfast" -> currentDayPlan.copy(breakfastDishId = randomDish?.id, breakfastCustomName = null)
            "Lunch" -> currentDayPlan.copy(lunchDishId = randomDish?.id, lunchCustomName = null)
            "Dinner" -> currentDayPlan.copy(dinnerDishId = randomDish?.id, dinnerCustomName = null)
            else -> currentDayPlan
        }

        val updatedDays = currentPlan.days.toMutableMap()
        updatedDays[day] = updatedDayPlan
        savePlan(WeeklyPlan(updatedDays))
    }

    /**
     * Manually assigns a dish to a specific slot.
     */
    suspend fun assignDishToSlot(day: String, mealType: String, dishId: String?) {
        val currentPlan = weeklyPlan.first()
        val currentDayPlan = currentPlan.days[day] ?: DailyPlan()

        val updatedDayPlan = when (mealType) {
            "Breakfast" -> currentDayPlan.copy(breakfastDishId = dishId, breakfastCustomName = null)
            "Lunch" -> currentDayPlan.copy(lunchDishId = dishId, lunchCustomName = null)
            "Dinner" -> currentDayPlan.copy(dinnerDishId = dishId, dinnerCustomName = null)
            else -> currentDayPlan
        }

        val updatedDays = currentPlan.days.toMutableMap()
        updatedDays[day] = updatedDayPlan
        savePlan(WeeklyPlan(updatedDays))
    }

    companion object {
        private val KEY_DISHES = stringPreferencesKey("planner_dishes")
        private val KEY_WEEKLY_PLAN = stringPreferencesKey("planner_weekly_plan")

        val DAYS_OF_WEEK = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        private val emptyWeeklyPlan = WeeklyPlan(
            DAYS_OF_WEEK.associateWith { DailyPlan() }
        )

        private val defaultDishes = listOf(
            Dish(
                name = "Classic Pancakes", course = "Breakfast", prepTimeMin = 15, difficulty = "Easy",
                ingredients = listOf("Flour", "Milk", "Eggs", "Maple Syrup", "Butter"),
                calories = 380, proteinGrams = 8, carbsGrams = 55, fatsGrams = 14
            ),
            Dish(
                name = "Oatmeal Bowl", course = "Breakfast", prepTimeMin = 10, difficulty = "Easy",
                ingredients = listOf("Oats", "Banana", "Honey", "Berries", "Almond Milk"),
                calories = 310, proteinGrams = 10, carbsGrams = 52, fatsGrams = 6
            ),
            Dish(
                name = "Scrambled Eggs on Toast", course = "Breakfast", prepTimeMin = 10, difficulty = "Easy",
                ingredients = listOf("Eggs", "Bread", "Butter", "Salt", "Pepper"),
                calories = 290, proteinGrams = 16, carbsGrams = 22, fatsGrams = 15
            ),
            Dish(
                name = "Chicken Caesar Salad", course = "Lunch", prepTimeMin = 20, difficulty = "Medium",
                ingredients = listOf("Chicken Breast", "Romaine Lettuce", "Caesar Dressing", "Croutons", "Parmesan"),
                calories = 480, proteinGrams = 38, carbsGrams = 15, fatsGrams = 28
            ),
            Dish(
                name = "Tomato Basil Soup & Toastie", course = "Lunch", prepTimeMin = 25, difficulty = "Easy",
                ingredients = listOf("Tomato Soup", "Bread", "Cheddar Cheese", "Butter", "Basil"),
                calories = 520, proteinGrams = 21, carbsGrams = 50, fatsGrams = 24
            ),
            Dish(
                name = "Grilled Salmon & Rice", course = "Dinner", prepTimeMin = 30, difficulty = "Medium",
                ingredients = listOf("Salmon Fillet", "Jasmine Rice", "Asparagus", "Lemon", "Olive Oil"),
                calories = 620, proteinGrams = 42, carbsGrams = 45, fatsGrams = 26
            ),
            Dish(
                name = "Spaghetti Bolognese", course = "Dinner", prepTimeMin = 40, difficulty = "Medium",
                ingredients = listOf("Spaghetti", "Ground Beef", "Tomato Sauce", "Onion", "Garlic", "Parmesan"),
                calories = 680, proteinGrams = 35, carbsGrams = 80, fatsGrams = 22
            ),
            Dish(
                name = "Vegetable Stir Fry", course = "Dinner", prepTimeMin = 20, difficulty = "Easy",
                ingredients = listOf("Tofu", "Broccoli", "Bell Peppers", "Carrots", "Soy Sauce", "Ginger", "Sesame Oil"),
                calories = 340, proteinGrams = 18, carbsGrams = 30, fatsGrams = 16
            )
        )

        @Volatile private var instance: MealPlannerPrefs? = null

        fun get(context: Context): MealPlannerPrefs =
            instance ?: synchronized(this) {
                instance ?: MealPlannerPrefs(context.applicationContext).also { instance = it }
            }
    }
}
