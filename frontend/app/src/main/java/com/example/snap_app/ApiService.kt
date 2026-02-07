package com.example.snap_app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import org.json.JSONArray

object ApiService {
    private const val BASE_URL = "http://192.168.1.232:5000"
    private val gson = Gson()

    // ==================== Auth ====================

    // POST /register
    suspend fun registerUser(email: String, password: String, username: String): RegisterResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("username", username)
                }.toString()
                val response = postRequest("/register", body)
                if (response != null) gson.fromJson(response, RegisterResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // POST /login (custom — sends email+password, expects user_id back)
    suspend fun loginUser(email: String, password: String): LoginResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }.toString()
                val response = postRequest("/login", body)
                if (response != null) gson.fromJson(response, LoginResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // GET /users/<user_id>
    suspend fun getUser(userId: String): UserResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = getRequest("/users/$userId")
                if (response != null) gson.fromJson(response, UserResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // DELETE /users/<user_id>
    suspend fun deleteUser(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                deleteRequest("/users/$userId")
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // ==================== Health Profile ====================

    // POST /users/<user_id>/health
    suspend fun createHealthProfile(
        userId: String,
        weight: Double,
        height: Int,
        age: Int,
        gender: String,
        activityLevel: String,
        fitnessGoal: String,
        workoutsPerDay: Int = 0,
        workoutsDuration: Int = 0
    ): HealthProfileResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("weight", weight)
                    put("height", height)
                    put("age", age)
                    put("gender", gender)
                    put("activity_level", activityLevel)
                    put("fitness_goal", fitnessGoal)
                    put("workouts_per_day", workoutsPerDay)
                    put("workouts_duration", workoutsDuration)
                }.toString()
                val response = postRequest("/users/$userId/health", body)
                if (response != null) gson.fromJson(response, HealthProfileResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // GET /users/<user_id>/health
    suspend fun getHealthProfile(userId: String): HealthProfileResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = getRequest("/users/$userId/health")
                if (response != null) gson.fromJson(response, HealthProfileResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // PUT /users/<user_id>/health
    suspend fun updateHealthProfile(userId: String, updates: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject(updates).toString()
                putRequest("/users/$userId/health", body) != null
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // ==================== Nutrition Profile ====================

    // POST /users/<user_id>/nutrition
    suspend fun createNutritionProfile(
        userId: String,
        dietType: String,
        allergies: List<String> = emptyList(),
        mealsPerDay: Int = 3,
        cuisinePreferences: List<String> = emptyList()
    ): NutritionProfileResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("diet_type", dietType)
                    put("allergies", JSONArray(allergies))
                    put("meals_per_day", mealsPerDay)
                    put("cuisine_preferences", JSONArray(cuisinePreferences))
                }.toString()
                val response = postRequest("/users/$userId/nutrition", body)
                if (response != null) gson.fromJson(response, NutritionProfileResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // GET /users/<user_id>/nutrition
    suspend fun getNutritionProfile(userId: String): NutritionProfileResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = getRequest("/users/$userId/nutrition")
                if (response != null) gson.fromJson(response, NutritionProfileResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // ==================== Plans ====================

    // POST /users/<user_id>/plan
    suspend fun createPlan(
        userId: String,
        planType: String,
        durationWeeks: Int,
        intensity: String,
        specificGoals: List<String>,
        useAi: Boolean
    ): PlanResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("plan_type", planType)
                    put("duration_weeks", durationWeeks)
                    put("intensity", intensity)
                    put("specific_goals", JSONArray(specificGoals))
                    put("use_ai", useAi)
                }.toString()
                val response = postRequest("/users/$userId/plan", body)
                if (response != null) gson.fromJson(response, PlanResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // GET /users/<user_id>/plan
    suspend fun getPlan(userId: String): PlanResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = getRequest("/users/$userId/plan")
                if (response != null) gson.fromJson(response, PlanResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // DELETE /users/<user_id>/plan
    suspend fun deletePlan(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                deleteRequest("/users/$userId/plan")
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // PUT /users/<user_id>/plan/adjust
    suspend fun adjustPlan(userId: String, adjustmentRequest: String, userFeedback: String): PlanResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("adjustment_request", adjustmentRequest)
                    put("user_feedback", userFeedback)
                }.toString()
                val response = putRequest("/users/$userId/plan/adjust", body)
                if (response != null) gson.fromJson(response, PlanResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // PUT /users/<user_id>/plan/nutrition/adjust
    suspend fun adjustNutritionPlan(
        userId: String,
        weekName: String,
        extraCalories: Int,
        dayOfWeek: Int,
        notes: String
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("week_name", weekName)
                    put("extra_calories", extraCalories)
                    put("day_of_week", dayOfWeek)
                    put("notes", notes)
                }.toString()
                putRequest("/users/$userId/plan/nutrition/adjust", body)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // PUT /users/<user_id>/plan/workout/adjust
    suspend fun adjustWorkoutPlan(
        userId: String,
        weekName: String,
        skippedWorkouts: List<String>,
        reason: String
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("week_name", weekName)
                    put("skipped_workouts", JSONArray(skippedWorkouts))
                    put("reason", reason)
                }.toString()
                putRequest("/users/$userId/plan/workout/adjust", body)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // POST /users/<user_id>/plan/validate
    suspend fun validatePlan(userId: String): ValidationResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = postRequest("/users/$userId/plan/validate", "{}")
                if (response != null) gson.fromJson(response, ValidationResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // ==================== Tracking ====================

    // GET /users/<user_id>/tracking/daily
    suspend fun getDailyLog(userId: String, date: String? = null): DailyLogResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val query = if (date != null) "?date=$date" else ""
                val response = getRequest("/users/$userId/tracking/daily$query")
                if (response != null) gson.fromJson(response, DailyLogResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // GET /users/<user_id>/tracking/history
    suspend fun getTrackingHistory(userId: String): TrackingHistoryResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = getRequest("/users/$userId/tracking/history")
                if (response != null) gson.fromJson(response, TrackingHistoryResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // POST /users/<user_id>/tracking/meals
    suspend fun logMeal(userId: String, date: String, mealType: String, meal: Meal): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("date", date)
                    put("meal_type", mealType)
                    put("items", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", meal.aiDesc)
                            put("calories", meal.calories.replace("g", "").toIntOrNull() ?: 0)
                        })
                    })
                }.toString()
                postRequest("/users/$userId/tracking/meals", body) != null
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // POST /users/<user_id>/tracking/workout
    suspend fun logWorkout(
        userId: String,
        date: String,
        completed: Boolean,
        exercises: List<Exercise>,
        durationMinutes: Int
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val exercisesArray = JSONArray()
                exercises.forEach { ex ->
                    exercisesArray.put(JSONObject().apply {
                        put("name", ex.name)
                        put("sets", ex.sets.toIntOrNull() ?: 3)
                        put("reps", ex.reps.toIntOrNull() ?: 12)
                    })
                }
                val body = JSONObject().apply {
                    put("date", date)
                    put("completed", completed)
                    put("exercises", exercisesArray)
                    put("duration_minutes", durationMinutes)
                }.toString()
                postRequest("/users/$userId/tracking/workout", body) != null
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // POST /users/<user_id>/tracking/water
    suspend fun logWater(userId: String, amountMl: Int): WaterResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("amount_ml", amountMl)
                }.toString()
                val response = postRequest("/users/$userId/tracking/water", body)
                if (response != null) gson.fromJson(response, WaterResponse::class.java) else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // POST /users/<user_id>/tracking/wellness
    suspend fun logWellness(
        userId: String,
        sleepHours: Double,
        mood: String,
        energyLevel: Int
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("sleep_hours", sleepHours)
                    put("mood", mood)
                    put("energy_level", energyLevel)
                }.toString()
                postRequest("/users/$userId/tracking/wellness", body) != null
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // POST /users/logout
    suspend fun logoutUser(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("userId", userId)
                }.toString()
                postRequest("/users/logout", body) != null
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // ==================== HTTP Helpers ====================

    private fun getRequest(path: String): String? {
        val url = URL("$BASE_URL$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        return if (conn.responseCode in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            println("GET $path failed: ${conn.responseCode} - ${conn.errorStream?.bufferedReader()?.readText()}")
            null
        }
    }

    private fun postRequest(path: String, body: String): String? {
        val url = URL("$BASE_URL$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 60000 // longer for AI plan generation
        conn.outputStream.write(body.toByteArray())
        return if (conn.responseCode in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            println("POST $path failed: ${conn.responseCode} - ${conn.errorStream?.bufferedReader()?.readText()}")
            null
        }
    }

    private fun putRequest(path: String, body: String): String? {
        val url = URL("$BASE_URL$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.outputStream.write(body.toByteArray())
        return if (conn.responseCode in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            println("PUT $path failed: ${conn.responseCode} - ${conn.errorStream?.bufferedReader()?.readText()}")
            null
        }
    }

    private fun deleteRequest(path: String): Boolean {
        val url = URL("$BASE_URL$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        return conn.responseCode in 200..299
    }
}

// ==================== Response Models ====================

data class RegisterResponse(
    @SerializedName("userId") val user_id: String?,
    val email: String?,
    val username: String?,
    val message: String?
)

data class LoginResponse(
    @SerializedName("userId") val user_id: String?,
    val email: String?,
    val username: String?,
    val message: String?
)

data class UserResponse(
    val user_id: String,
    val email: String,
    val username: String,
    val created_at: String?
)

data class HealthProfileResponse(
    val message: String?,
    val profile: HealthProfile?
)

data class HealthProfile(
    val weight: Double?,
    val height: Int?,
    val age: Int?,
    val gender: String?,
    val activity_level: String?,
    val fitness_goal: String?,
    val bmi: Double?
)

data class NutritionProfileResponse(
    val message: String?,
    val nutrition: NutritionProfile?
)

data class NutritionProfile(
    val allergies: List<String>?,
    val diet_type: String?,
    val calorie_goal: Int?,
    val protein_goal: Int?,
    val carb_goal: Int?,
    val fat_goal: Int?,
    val meals_per_day: Int?
)

data class PlanResponse(
    val message: String?,
    val plan_id: String?,
    val plan: PlanData?
)

data class PlanData(
    val plan_type: String?,
    val ai_generated: Boolean?,
    val generation_method: String?,
    val diet: List<Map<String, Any?>>?,
    val workouts: List<Map<String, Any?>>?
)

data class ValidationResponse(
    val validation: ValidationData?,
    val timestamp: String?
)

data class ValidationData(
    val overall_score: Double?,
    val recommendations: List<String>?,
    val warnings: List<String>?,
    val alignment_with_goals: String?
)

data class DailyLogResponse(
    val date: String?,
    val daily_log: DailyLog?
)

data class DailyLog(
    val meals: Map<String, Any>?,
    val workout: Map<String, Any>?,
    val water_ml: Int?
)

data class TrackingHistoryResponse(
    val daily_logs: List<Map<String, Any>>?,
    val total: Int?
)

data class WaterResponse(
    val water_intake_ml: Int?
)