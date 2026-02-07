package com.example.snap_app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _completionPercentage = MutableStateFlow(0)
    val completionPercentage: StateFlow<Int> = _completionPercentage

    // User state - persisted across screens
    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    // Plan data fetched from API
    private val _nutritionPlan = MutableStateFlow<List<DietWeek>>(emptyList())
    val nutritionPlan: StateFlow<List<DietWeek>> = _nutritionPlan

    private val _workoutPlan = MutableStateFlow<List<WorkoutData>>(emptyList())
    val workoutPlan: StateFlow<List<WorkoutData>> = _workoutPlan

    private val _planLoading = MutableStateFlow(false)
    val planLoading: StateFlow<Boolean> = _planLoading

    private val _planError = MutableStateFlow<String?>(null)
    val planError: StateFlow<String?> = _planError

    // Food log and calorie tracking
    private val _todayFoodLog = MutableStateFlow<List<FoodLogEntry>>(emptyList())
    val todayFoodLog: StateFlow<List<FoodLogEntry>> = _todayFoodLog

    private val _calorieSummary = MutableStateFlow<CalorieSummary?>(null)
    val calorieSummary: StateFlow<CalorieSummary?> = _calorieSummary

    // Reminders state - persisted across navigation
    private val _reminders = MutableStateFlow<List<MealReminder>>(emptyList())
    val reminders: StateFlow<List<MealReminder>> = _reminders

    private var lastResetDate: String = ""

    fun initReminders() {
        val currentDate = getCurrentDate()
        if (lastResetDate != currentDate) {
            _reminders.value = generateRemindersFromPlan(_nutritionPlan.value, _workoutPlan.value)
            lastResetDate = currentDate
        } else if (_reminders.value.isEmpty()) {
            _reminders.value = generateRemindersFromPlan(_nutritionPlan.value, _workoutPlan.value)
        }
    }

    fun toggleReminderCompletion(id: Int) {
        _reminders.value = _reminders.value.map {
            if (it.id == id) it.copy(isCompleted = true) else it
        }
        recalcCompletionPercentage()
    }

    fun uncompleteReminder(id: Int) {
        _reminders.value = _reminders.value.map {
            if (it.id == id) it.copy(isCompleted = false) else it
        }
        recalcCompletionPercentage()
    }

    private fun recalcCompletionPercentage() {
        val all = _reminders.value
        _completionPercentage.value = if (all.isNotEmpty()) {
            (all.count { it.isCompleted }.toFloat() / all.size.toFloat() * 100).toInt()
        } else 0
    }

    // Nutrition plan meal completion toggle
    fun toggleMealCompletion(weekIndex: Int, mealType: String) {
        val weeks = _nutritionPlan.value.toMutableList()
        if (weekIndex !in weeks.indices) return
        val week = weeks[weekIndex]
        val updatedWeek = when (mealType) {
            "breakfast" -> {
                if (!week.breakfast.completed) logMealCompletion("breakfast", week.breakfast)
                week.copy(breakfast = week.breakfast.copy(completed = !week.breakfast.completed))
            }
            "lunch" -> {
                if (!week.lunch.completed) logMealCompletion("lunch", week.lunch)
                week.copy(lunch = week.lunch.copy(completed = !week.lunch.completed))
            }
            "dinner" -> {
                if (!week.dinner.completed) logMealCompletion("dinner", week.dinner)
                week.copy(dinner = week.dinner.copy(completed = !week.dinner.completed))
            }
            else -> week
        }
        weeks[weekIndex] = updatedWeek
        _nutritionPlan.value = weeks
    }

    // Workout exercise completion toggle
    fun toggleExerciseCompletion(workoutIndex: Int, exerciseIndex: Int) {
        val list = _workoutPlan.value.toMutableList()
        if (workoutIndex !in list.indices) return
        val workout = list[workoutIndex]
        if (exerciseIndex !in workout.exercises.indices) return
        val updatedExercises = workout.exercises.toMutableList()
        updatedExercises[exerciseIndex] = updatedExercises[exerciseIndex].copy(
            completed = !updatedExercises[exerciseIndex].completed
        )
        val allCompleted = updatedExercises.all { it.completed }
        list[workoutIndex] = workout.copy(exercises = updatedExercises, completed = allCompleted)
        _workoutPlan.value = list
    }

    // Toggle entire workout completion
    fun toggleWorkoutCompletion(workoutIndex: Int) {
        val list = _workoutPlan.value.toMutableList()
        if (workoutIndex !in list.indices) return
        val workout = list[workoutIndex]
        val newStatus = !workout.completed
        val updatedExercises = workout.exercises.map { it.copy(completed = newStatus) }
        list[workoutIndex] = workout.copy(exercises = updatedExercises, completed = newStatus)
        if (newStatus) logWorkoutCompletion(updatedExercises)
        _workoutPlan.value = list
    }

    fun setUser(userId: String, email: String, username: String) {
        _userId.value = userId
        _userEmail.value = email
        _username.value = username
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val uid = _userId.value
            if (uid.isNotBlank()) {
                try {
                    ApiService.logoutUser(uid)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Clear all local state regardless of API result
            _userId.value = ""
            _userEmail.value = ""
            _username.value = ""
            _nutritionPlan.value = emptyList()
            _workoutPlan.value = emptyList()
            _planLoading.value = false
            _planError.value = null
            _completionPercentage.value = 0
            _reminders.value = emptyList()
            lastResetDate = ""
            onComplete()
        }
    }

    fun updateCompletionPercentage(percentage: Int) {
        _completionPercentage.value = percentage
    }

    fun createHealthProfile(
        weight: String,
        height: String,
        age: Int,
        gender: String,
        activityLevel: String,
        fitnessGoal: String,
        workoutsPerDay: Int = 0,
        workoutsDuration: Int = 0
    ) {
        viewModelScope.launch {
            val uid = _userId.value
            if (uid.isBlank()) return@launch
            try {
                ApiService.createHealthProfile(
                    userId = uid,
                    weight = weight.toDoubleOrNull() ?: 70.0,
                    height = height.toIntOrNull() ?: 170,
                    age = age,
                    gender = gender.lowercase(),
                    activityLevel = activityLevel,
                    fitnessGoal = fitnessGoal,
                    workoutsPerDay = workoutsPerDay,
                    workoutsDuration = workoutsDuration
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createNutritionProfile(
        dietType: String,
        allergies: List<String> = emptyList(),
        cuisinePreferences: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val uid = _userId.value
            if (uid.isBlank()) return@launch
            try {
                ApiService.createNutritionProfile(
                    userId = uid,
                    dietType = dietType,
                    allergies = allergies,
                    mealsPerDay = 3,
                    cuisinePreferences = cuisinePreferences
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generatePlan(intensity: String = "moderate", goals: List<String> = listOf("weight_loss")) {
        viewModelScope.launch {
            val uid = _userId.value
            if (uid.isBlank()) return@launch
            _planLoading.value = true
            _planError.value = null
            try {
                val response = ApiService.createPlan(
                    userId = uid,
                    planType = "combined",
                    durationWeeks = 4,
                    intensity = intensity,
                    specificGoals = goals,
                    useAi = true
                )
                if (response != null) {
                    // Parse diet from planResponse
                    val dietWeeks = response.plan?.diet?.mapIndexed { index, week ->
                        parsePlanDietWeek(week, index)
                    } ?: emptyList()
                    _nutritionPlan.value = dietWeeks

                    // Parse workouts from planResponse
                    val workoutList = response.plan?.workouts?.flatMap { week ->
                        parsePlanWorkoutWeek(week)
                    } ?: emptyList()
                    _workoutPlan.value = workoutList
                } else {
                    _planError.value = "Failed to generate plan"
                }
            } catch (e: Exception) {
                _planError.value = "Network error: ${e.message}"
                e.printStackTrace()
            } finally {
                _planLoading.value = false
            }
        }
    }

    fun fetchTodayFoodLog() {
        viewModelScope.launch {
            val uid = _userId.value
            if (uid.isBlank()) return@launch
            try {
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
                val response = ApiService.getFoodLog(uid, dateStr)
                if (response?.food_log != null) {
                    _todayFoodLog.value = response.food_log
                }
                // Also fetch calorie summary
                fetchCalorieSummary()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchCalorieSummary() {
        viewModelScope.launch {
            val uid = _userId.value
            if (uid.isBlank()) return@launch
            try {
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
                val summary = ApiService.getCalorieSummary(uid, dateStr)
                _calorieSummary.value = summary
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Calculate optimal daily calorie intake using on-device LLM and update nutrition profile.
     * This should be called when user completes registration or when they click "Recalculate".
     */
    fun calculateAndUpdateCalories() {
        viewModelScope.launch {
            val uid = _userId.value
            if (uid.isBlank()) return@launch
            
            _planLoading.value = true
            
            try {
                // Fetch user profile data
                val healthProfile = ApiService.getHealthProfile(uid)
                
                val age = healthProfile?.profile?.age
                val gender = healthProfile?.profile?.gender
                val weight = healthProfile?.profile?.weight
                val height = healthProfile?.profile?.height
                val activityLevel = healthProfile?.profile?.activity_level ?: "moderately_active"
                val fitnessGoal = healthProfile?.profile?.fitness_goal ?: "general_health"
                
                if (age == null || gender == null || weight == null || height == null) {
                    Log.e("ViewModel", "Incomplete profile data for calorie calculation")
                    _planLoading.value = false
                    return@launch
                }
                
                // Initialize and use on-device LLM to calculate calories
                Log.i("ViewModel", "Starting On-Device Calorie Calculation")
                Log.i("ViewModel", "Profile: $age years, $gender, ${weight}kg, ${height}cm")
                Log.i("ViewModel", "Activity: $activityLevel, Goal: $fitnessGoal")
                
                val context = getApplication<Application>().applicationContext
                val genieService = GenieService(context)
                
                val initialized = genieService.initialize()
                
                if (!initialized) {
                    Log.e("ViewModel", "Failed to initialize GenieService")
                    _planLoading.value = false
                    return@launch
                }
                
                val calculatedCalories = genieService.calculateDailyCalories(
                    age = age,
                    gender = gender,
                    weight = weight,
                    height = height,
                    activityLevel = activityLevel,
                    fitnessGoal = fitnessGoal
                )
                
                if (calculatedCalories != null) {
                    Log.i("ViewModel", "✓ Calculated daily calories: $calculatedCalories")
                    
                    // Update nutrition profile with calculated calories
                    val success = ApiService.updateCalorieGoal(uid, calculatedCalories)
                    
                    if (success) {
                        Log.i("ViewModel", "✓ Updated calorie goal in database")
                        // Refresh calorie summary to show new goal
                        fetchCalorieSummary()
                    } else {
                        Log.e("ViewModel", "Failed to update calorie goal in database")
                    }
                } else {
                    Log.e("ViewModel", "Failed to calculate calories with LLM")
                }
                
                genieService.release()
            } catch (e: Exception) {
                Log.e("ViewModel", "Error during calorie calculation: ${e.message}", e)
                e.printStackTrace()
            } finally {
                _planLoading.value = false
            }
        }
    }

    fun fetchPlan() {
        viewModelScope.launch {
            val uid = _userId.value
            if (uid.isBlank()) return@launch
            _planLoading.value = true
            _planError.value = null
            try {
                val response = ApiService.getPlan(uid)
                if (response != null) {
                    val dietWeeks = response.plan?.diet?.mapIndexed { index, week ->
                        parsePlanDietWeek(week, index)
                    } ?: emptyList()
                    _nutritionPlan.value = dietWeeks

                    val workoutList = response.plan?.workouts?.flatMap { week ->
                        parsePlanWorkoutWeek(week)
                    } ?: emptyList()
                    _workoutPlan.value = workoutList
                }
            } catch (e: Exception) {
                _planError.value = "Error fetching plan: ${e.message}"
                e.printStackTrace()
            } finally {
                _planLoading.value = false
            }
        }
    }

    fun logMealCompletion(mealType: String, meal: Meal) {
        viewModelScope.launch {
            val uid = _userId.value
            if (uid.isBlank()) return@launch
            try {
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
                ApiService.logMeal(uid, dateStr, mealType, meal)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logWorkoutCompletion(exercises: List<Exercise>, durationMinutes: Int = 45) {
        viewModelScope.launch {
            val uid = _userId.value
            if (uid.isBlank()) return@launch
            try {
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
                ApiService.logWorkout(
                    userId = uid,
                    date = dateStr,
                    completed = true,
                    exercises = exercises,
                    durationMinutes = durationMinutes
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parsePlanDietWeek(week: Map<String, Any?>, index: Int): DietWeek {
        val weekName = week["weekName"] as? String ?: "Week ${index + 1}"
        val meals = week["meals"] as? Map<String, Map<String, Any>> ?: emptyMap()

        fun parseMealFromApi(mealMap: Map<String, Any>?): Meal {
            if (mealMap == null) return Meal()
            return Meal(
                aiDesc = mealMap["name"] as? String ?: "",
                actualMeal = mealMap["name"] as? String ?: "",
                calories = (mealMap["calories"] as? Number)?.toString() ?: "0",
                carbs = (mealMap["carbs"] as? String) ?: "${(mealMap["carbs"] as? Number)?.toInt() ?: 0}g",
                fats = (mealMap["fats"] as? String) ?: "${(mealMap["fats"] as? Number)?.toInt() ?: 0}g",
                protein = (mealMap["protein"] as? String) ?: "${(mealMap["protein"] as? Number)?.toInt() ?: 0}g"
            )
        }

        return DietWeek(
            week = weekName.replace("Week ", "week"),
            breakfast = parseMealFromApi(meals["breakfast"]),
            lunch = parseMealFromApi(meals["lunch"]),
            dinner = parseMealFromApi(meals["dinner"])
        )
    }

    private fun parsePlanWorkoutWeek(week: Map<String, Any?>): List<WorkoutData> {
        val weekName = week["weekName"] as? String ?: "Week 1"
        val exercises = week["exercises"] as? List<Map<String, Any>> ?: emptyList()

        if (exercises.isEmpty()) return emptyList()

        // Group into workouts of ~3-4 exercises each
        val chunked = exercises.chunked(3)
        return chunked.mapIndexed { i, group ->
            WorkoutData(
                week = weekName.replace("Week ", "week"),
                workoutName = "workout${i + 1}",
                completed = false,
                exercises = group.map { ex ->
                    Exercise(
                        name = ex["name"] as? String ?: "",
                        sets = (ex["sets"] as? Number)?.toInt()?.toString() ?: "3",
                        reps = (ex["reps"] as? Number)?.toInt()?.toString() ?: "12"
                    )
                }
            )
        }
    }
}