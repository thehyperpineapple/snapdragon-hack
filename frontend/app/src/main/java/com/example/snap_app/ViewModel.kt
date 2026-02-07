package com.example.snap_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
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
            onComplete()
        }
    }

    fun updateCompletionPercentage(percentage: Int) {
        viewModelScope.launch {
            _completionPercentage.value = percentage
        }
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