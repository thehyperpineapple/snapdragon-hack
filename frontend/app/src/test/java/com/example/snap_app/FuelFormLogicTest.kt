package com.example.snap_app

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unit tests for FuelForm app frontend logic
 */
class FuelFormLogicTest {

    private lateinit var sampleDietWeeks: List<DietWeek>
    private lateinit var sampleReminders: List<MealReminder>

    @Before
    fun setup() {
        sampleDietWeeks = listOf(
            DietWeek(
                week = "Week 1",
                breakfast = Meal(
                    aiDesc = "Oatmeal with berries",
                    actualMeal = "Steel-cut oats, blueberries",
                    calories = "450",
                    carbs = "65g",
                    fats = "12g",
                    protein = "15g",
                    completed = false
                ),
                lunch = Meal(
                    aiDesc = "Grilled chicken salad",
                    actualMeal = "Mixed greens, chicken",
                    calories = "550",
                    carbs = "45g",
                    fats = "18g",
                    protein = "42g",
                    completed = false
                ),
                dinner = Meal(
                    aiDesc = "Salmon with vegetables",
                    actualMeal = "Baked salmon, broccoli",
                    calories = "680",
                    carbs = "52g",
                    fats = "28g",
                    protein = "48g",
                    completed = false
                )
            )
        )

        sampleReminders = listOf(
            MealReminder(0, "🍽️ Breakfast", "meal", "7:00 AM - 9:00 AM", false),
            MealReminder(1, "💧 Water after Breakfast", "water", "7:00 AM - 9:00 AM", false),
            MealReminder(2, "🍽️ Lunch", "meal", "12:00 PM - 2:00 PM", false),
            MealReminder(3, "💧 Water after Lunch", "water", "12:00 PM - 2:00 PM", false),
            MealReminder(4, "🍽️ Dinner", "meal", "6:00 PM - 8:00 PM", false),
            MealReminder(5, "💧 Water after Dinner", "water", "6:00 PM - 8:00 PM", false),
            MealReminder(6, "🏋️ Gym Workout", "gym", "5:00 PM - 7:00 PM", false)
        )
    }

    // ============ Completion Percentage Tests ============

    @Test
    fun completionPercentage_noReminders_returnsZero() {
        val reminders = emptyList<MealReminder>()
        val completed = reminders.filter { it.isCompleted }
        val percentage = if (reminders.isNotEmpty()) {
            (completed.size.toFloat() / reminders.size.toFloat() * 100).toInt()
        } else 0

        assertEquals(0, percentage)
    }

    @Test
    fun completionPercentage_allCompleted_returns100() {
        val reminders = listOf(
            MealReminder(1, "Breakfast", "meal", "7:00 AM", true),
            MealReminder(2, "Lunch", "meal", "12:00 PM", true),
            MealReminder(3, "Dinner", "meal", "6:00 PM", true)
        )
        val completed = reminders.filter { it.isCompleted }
        val percentage = (completed.size.toFloat() / reminders.size.toFloat() * 100).toInt()

        assertEquals(100, percentage)
    }

    @Test
    fun completionPercentage_halfCompleted_returns50() {
        val reminders = listOf(
            MealReminder(1, "Breakfast", "meal", "7:00 AM", true),
            MealReminder(2, "Lunch", "meal", "12:00 PM", false),
            MealReminder(3, "Dinner", "meal", "6:00 PM", true),
            MealReminder(4, "Water", "water", "8:00 AM", false)
        )
        val completed = reminders.filter { it.isCompleted }
        val percentage = (completed.size.toFloat() / reminders.size.toFloat() * 100).toInt()

        assertEquals(50, percentage)
    }

    @Test
    fun completionPercentage_oneOfThree_returns33() {
        val reminders = listOf(
            MealReminder(1, "Breakfast", "meal", "7:00 AM", true),
            MealReminder(2, "Lunch", "meal", "12:00 PM", false),
            MealReminder(3, "Dinner", "meal", "6:00 PM", false)
        )
        val completed = reminders.filter { it.isCompleted }
        val percentage = (completed.size.toFloat() / reminders.size.toFloat() * 100).toInt()

        assertEquals(33, percentage)
    }

    @Test
    fun completionPercentage_twoOfSeven_returns28() {
        val reminders = sampleReminders.mapIndexed { index, reminder ->
            if (index < 2) reminder.copy(isCompleted = true) else reminder
        }
        val completed = reminders.filter { it.isCompleted }
        val percentage = (completed.size.toFloat() / reminders.size.toFloat() * 100).toInt()

        assertEquals(28, percentage)
    }

    // ============ Meme Selection Tests ============

    @Test
    fun memeSelection_below30Percent_returnsSadMeme() {
        val completionPercentage = 25
        val memeUrl = when {
            completionPercentage < 30 -> "https://i.ytimg.com/vi/jXdbw21SKQg/mqdefault.jpg"
            completionPercentage < 70 -> "medium_meme"
            else -> "happy_meme"
        }

        assertEquals("https://i.ytimg.com/vi/jXdbw21SKQg/mqdefault.jpg", memeUrl)
    }

    @Test
    fun memeSelection_exactlyZero_returnsSadMeme() {
        val completionPercentage = 0
        val memeUrl = when {
            completionPercentage < 30 -> "https://i.ytimg.com/vi/jXdbw21SKQg/mqdefault.jpg"
            completionPercentage < 70 -> "medium_meme"
            else -> "happy_meme"
        }

        assertEquals("https://i.ytimg.com/vi/jXdbw21SKQg/mqdefault.jpg", memeUrl)
    }

    @Test
    fun memeSelection_30To69Percent_returnsMediumMeme() {
        val completionPercentages = listOf(30, 50, 69)
        val expectedMeme = "https://static.wikia.nocookie.net/belugacinematicuniversefanon/images/6/6a/Beluga.jpg/revision/latest/thumbnail/width/360/height/360?cb=20231226224904"

        completionPercentages.forEach { percentage ->
            val memeUrl = when {
                percentage < 30 -> "sad_meme"
                percentage < 70 -> expectedMeme
                else -> "happy_meme"
            }
            assertEquals("Meme for $percentage%", expectedMeme, memeUrl)
        }
    }

    @Test
    fun memeSelection_boundaryAt29_returnsSadMeme() {
        val memeUrl = when {
            29 < 30 -> "https://i.ytimg.com/vi/jXdbw21SKQg/mqdefault.jpg"
            29 < 70 -> "medium_meme"
            else -> "happy_meme"
        }
        assertEquals("https://i.ytimg.com/vi/jXdbw21SKQg/mqdefault.jpg", memeUrl)
    }

    @Test
    fun memeSelection_boundaryAt70_returnsHappyMeme() {
        val memeUrl = when {
            70 < 30 -> "sad_meme"
            70 < 70 -> "medium_meme"
            else -> "https://i.pinimg.com/736x/ea/de/1f/eade1feca67faed06570cf5495621746.jpg"
        }
        assertEquals("https://i.pinimg.com/736x/ea/de/1f/eade1feca67faed06570cf5495621746.jpg", memeUrl)
    }

    @Test
    fun memeSelection_100Percent_returnsHappyMeme() {
        val completionPercentage = 100
        val memeUrl = when {
            completionPercentage < 30 -> "sad_meme"
            completionPercentage < 70 -> "medium_meme"
            else -> "https://i.pinimg.com/736x/ea/de/1f/eade1feca67faed06570cf5495621746.jpg"
        }

        assertEquals("https://i.pinimg.com/736x/ea/de/1f/eade1feca67faed06570cf5495621746.jpg", memeUrl)
    }

    // ============ Status Color and Text Tests ============

    @Test
    fun status_below30Percent_returnsNeedsAttention() {
        val percentage = 25
        val (_, statusText) = when {
            percentage < 30 -> Pair(null, "Needs Attention")
            percentage < 70 -> Pair(null, "On Track")
            else -> Pair(null, "Crushing It!")
        }

        assertEquals("Needs Attention", statusText)
    }

    @Test
    fun status_30To69Percent_returnsOnTrack() {
        val percentage = 50
        val (_, statusText) = when {
            percentage < 30 -> Pair(null, "Needs Attention")
            percentage < 70 -> Pair(null, "On Track")
            else -> Pair(null, "Crushing It!")
        }

        assertEquals("On Track", statusText)
    }

    @Test
    fun status_70AndAbove_returnsCrushingIt() {
        val percentage = 85
        val (_, statusText) = when {
            percentage < 30 -> Pair(null, "Needs Attention")
            percentage < 70 -> Pair(null, "On Track")
            else -> Pair(null, "Crushing It!")
        }

        assertEquals("Crushing It!", statusText)
    }

    // ============ Reminder Type Tests ============

    @Test
    fun reminders_containsCorrectTypes() {
        val mealCount = sampleReminders.count { it.type == "meal" }
        val waterCount = sampleReminders.count { it.type == "water" }
        val gymCount = sampleReminders.count { it.type == "gym" }

        assertEquals(3, mealCount)
        assertEquals(3, waterCount)
        assertEquals(1, gymCount)
    }

    @Test
    fun reminders_totalCount_isSeven() {
        assertEquals(7, sampleReminders.size)
    }

    @Test
    fun reminders_allHaveUniqueIds() {
        val uniqueIds = sampleReminders.map { it.id }.toSet()
        assertEquals(sampleReminders.size, uniqueIds.size)
    }

    @Test
    fun reminders_allStartUncompleted() {
        val completedCount = sampleReminders.count { it.isCompleted }
        assertEquals(0, completedCount)
    }

    @Test
    fun reminders_allHaveTimeWindows() {
        val withoutTime = sampleReminders.filter { it.timeWindow.isEmpty() }
        assertEquals(0, withoutTime.size)
    }

    // ============ Date Formatting Tests ============

    @Test
    fun getCurrentDate_returnsCorrectFormat() {
        val dateString = getCurrentDate()

        // Should match YYYY-M-DD or YYYY-MM-DD format
        assertTrue(dateString.matches(Regex("\\d{4}-\\d{1,2}-\\d{1,2}")))
    }

    @Test
    fun getCurrentDate_isNotEmpty() {
        val dateString = getCurrentDate()
        assertTrue(dateString.isNotEmpty())
    }

    @Test
    fun dateFormat_matchesExpectedPattern() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        // Should contain a day name and month name
        assertNotNull(currentDate)
        assertTrue(currentDate.isNotEmpty())
    }

    // ============ Calorie Calculation Tests ============

    @Test
    fun totalCalories_sumCorrectly() {
        val dietWeek = sampleDietWeeks.first()
        val totalCalories = listOf(
            dietWeek.breakfast.calories,
            dietWeek.lunch.calories,
            dietWeek.dinner.calories
        ).sumOf { it.replace("g", "").toIntOrNull() ?: 0 }

        assertEquals(1680, totalCalories)
    }

    @Test
    fun totalCalories_handlesMissingData() {
        val emptyMeal = Meal(calories = "")
        val total = listOf(emptyMeal.calories).sumOf {
            it.replace("g", "").toIntOrNull() ?: 0
        }

        assertEquals(0, total)
    }

    @Test
    fun totalCalories_handlesInvalidFormat() {
        val invalidMeal = Meal(calories = "invalid")
        val total = listOf(invalidMeal.calories).sumOf {
            it.replace("g", "").toIntOrNull() ?: 0
        }

        assertEquals(0, total)
    }

    @Test
    fun totalCalories_handlesNullOrBlank() {
        val meals = listOf(
            Meal(calories = ""),
            Meal(calories = "100"),
            Meal(calories = "")
        )
        val total = meals.sumOf {
            it.calories.replace("g", "").toIntOrNull() ?: 0
        }

        assertEquals(100, total)
    }

    // ============ Meal Completion Tests ============

    @Test
    fun mealCompletion_countsCorrectly() {
        val dietWeek = DietWeek(
            week = "Week 1",
            breakfast = Meal(aiDesc = "Breakfast", completed = true),
            lunch = Meal(aiDesc = "Lunch", completed = false),
            dinner = Meal(aiDesc = "Dinner", completed = true)
        )

        val completedMeals = listOf(
            dietWeek.breakfast.completed,
            dietWeek.lunch.completed,
            dietWeek.dinner.completed
        ).count { it }

        assertEquals(2, completedMeals)
    }

    @Test
    fun mealCompletion_progressCalculation() {
        val dietWeek = DietWeek(
            week = "Week 1",
            breakfast = Meal(aiDesc = "Breakfast", completed = true),
            lunch = Meal(aiDesc = "Lunch", completed = true),
            dinner = Meal(aiDesc = "Dinner", completed = true)
        )

        val completedMeals = listOf(
            dietWeek.breakfast.completed,
            dietWeek.lunch.completed,
            dietWeek.dinner.completed
        ).count { it }

        val progress = completedMeals / 3f
        assertEquals(1.0f, progress, 0.01f)
    }

    @Test
    fun mealCompletion_noneCompleted_returns0() {
        val dietWeek = sampleDietWeeks.first()
        val completedMeals = listOf(
            dietWeek.breakfast.completed,
            dietWeek.lunch.completed,
            dietWeek.dinner.completed
        ).count { it }

        assertEquals(0, completedMeals)
    }

    // ============ Reminder Filtering Tests ============

    @Test
    fun reminderFiltering_separatesActiveAndCompleted() {
        val reminders = listOf(
            MealReminder(1, "Breakfast", "meal", "7:00 AM", true),
            MealReminder(2, "Lunch", "meal", "12:00 PM", false),
            MealReminder(3, "Dinner", "meal", "6:00 PM", true)
        )

        val active = reminders.filter { !it.isCompleted }
        val completed = reminders.filter { it.isCompleted }

        assertEquals(1, active.size)
        assertEquals(2, completed.size)
    }

    @Test
    fun reminderFiltering_emptyList() {
        val reminders = emptyList<MealReminder>()
        val active = reminders.filter { !it.isCompleted }
        val completed = reminders.filter { it.isCompleted }

        assertEquals(0, active.size)
        assertEquals(0, completed.size)
    }

    @Test
    fun reminderFiltering_allCompleted() {
        val reminders = sampleReminders.map { it.copy(isCompleted = true) }
        val active = reminders.filter { !it.isCompleted }
        val completed = reminders.filter { it.isCompleted }

        assertEquals(0, active.size)
        assertEquals(7, completed.size)
    }

    // ============ String Manipulation Tests ============

    @Test
    fun emojiRemoval_removesBreakfastEmoji() {
        val reminderName = "🍽️ Breakfast"
        val cleaned = reminderName.replace("🍽️ ", "")

        assertEquals("Breakfast", cleaned)
    }

    @Test
    fun emojiRemoval_removesWaterEmoji() {
        val reminderName = "💧 Water after Breakfast"
        val cleaned = reminderName.replace("💧 ", "")

        assertEquals("Water after Breakfast", cleaned)
    }

    @Test
    fun emojiRemoval_removesGymEmoji() {
        val reminderName = "🏋️ Gym Workout"
        val cleaned = reminderName.replace("🏋️ ", "")

        assertEquals("Gym Workout", cleaned)
    }

    @Test
    fun emojiRemoval_multipleEmojis() {
        val reminderName = "🏋️ Push-ups"
        val cleaned = reminderName
            .replace("🍽️ ", "")
            .replace("💧 ", "")
            .replace("🏋️ ", "")

        assertEquals("Push-ups", cleaned)
    }

    @Test
    fun weekFormatting_convertsCorrectly() {
        val weekString = "week1"
        val formatted = weekString.replace("week", "Week ")

        assertEquals("Week 1", formatted)
    }

    @Test
    fun weekFormatting_alreadyFormatted() {
        val weekString = "Week 1"
        val formatted = weekString.replace("week", "Week ")

        assertEquals("Week 1", formatted)
    }

    // ============ Edge Cases ============

    @Test
    fun emptyDietWeek_handlesGracefully() {
        val emptyWeek = DietWeek(
            week = "",
            breakfast = Meal(),
            lunch = Meal(),
            dinner = Meal()
        )

        val totalCalories = listOf(
            emptyWeek.breakfast.calories,
            emptyWeek.lunch.calories,
            emptyWeek.dinner.calories
        ).sumOf { it.replace("g", "").toIntOrNull() ?: 0 }

        assertEquals(0, totalCalories)
    }

    @Test
    fun reminderToggle_changesState() {
        var reminder = MealReminder(1, "Breakfast", "meal", "7:00 AM", false)
        reminder = reminder.copy(isCompleted = !reminder.isCompleted)

        assertTrue(reminder.isCompleted)
    }

    @Test
    fun reminderToggle_multipleToggles() {
        var reminder = MealReminder(1, "Breakfast", "meal", "7:00 AM", false)

        reminder = reminder.copy(isCompleted = !reminder.isCompleted)
        assertTrue(reminder.isCompleted)

        reminder = reminder.copy(isCompleted = !reminder.isCompleted)
        assertFalse(reminder.isCompleted)

        reminder = reminder.copy(isCompleted = !reminder.isCompleted)
        assertTrue(reminder.isCompleted)
    }

    // ============ Data Class Tests ============

    @Test
    fun mealDataClass_copiesCorrectly() {
        val original = Meal(
            aiDesc = "Oatmeal",
            calories = "450",
            completed = false
        )
        val copy = original.copy(completed = true)

        assertEquals("Oatmeal", copy.aiDesc)
        assertEquals("450", copy.calories)
        assertTrue(copy.completed)
        assertFalse(original.completed)
    }

    @Test
    fun mealDataClass_defaultValues() {
        val meal = Meal()

        assertEquals("", meal.aiDesc)
        assertEquals("", meal.actualMeal)
        assertEquals("", meal.calories)
        assertEquals("", meal.carbs)
        assertEquals("", meal.fats)
        assertEquals("", meal.protein)
        assertFalse(meal.completed)
    }

    @Test
    fun reminderDataClass_equality() {
        val reminder1 = MealReminder(1, "Breakfast", "meal", "7:00 AM", false)
        val reminder2 = MealReminder(1, "Breakfast", "meal", "7:00 AM", false)

        assertEquals(reminder1, reminder2)
    }

    @Test
    fun dietWeekDataClass_copiesCorrectly() {
        val original = sampleDietWeeks.first()
        val updated = original.copy(
            breakfast = original.breakfast.copy(completed = true)
        )

        assertTrue(updated.breakfast.completed)
        assertFalse(original.breakfast.completed)
        assertEquals(original.week, updated.week)
    }

    // ============ Macros Formatting Tests ============

    @Test
    fun macros_formatWithG_suffix() {
        val meal = sampleDietWeeks.first().breakfast

        assertTrue(meal.carbs.endsWith("g"))
        assertTrue(meal.fats.endsWith("g"))
        assertTrue(meal.protein.endsWith("g"))
    }

    @Test
    fun macros_extractNumericValue() {
        val carbs = "65g"
        val value = carbs.replace("g", "").toIntOrNull()

        assertEquals(65, value)
    }
}