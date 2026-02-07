package com.example.snap_app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import org.json.JSONObject

object ApiService {
    private const val BASE_URL = "http://192.168.1.232:5000"
    private val gson = Gson()

    // Register new user
    suspend fun registerUser(email: String, password: String, username: String): RegisterResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/users/register")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val requestBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("username", username)
                }.toString()

                connection.outputStream.write(requestBody.toByteArray())

                if (connection.responseCode == 200 || connection.responseCode == 201) {
                    val response = connection.inputStream.bufferedReader().readText()
                    gson.fromJson(response, RegisterResponse::class.java)
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                    println("Registration failed: $errorResponse")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Get user's nutrition plan
    suspend fun getNutritionPlan(userId: String): NutritionPlanResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/users/$userId/plan")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    gson.fromJson(response, NutritionPlanResponse::class.java)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Log meal completion
    suspend fun logMeal(userId: String, date: String, mealType: String, meal: Meal): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/users/$userId/tracking/meals")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val requestBody = """
                    {
                        "date": "$date",
                        "meal_type": "$mealType",
                        "meal_data": {
                            "name": "${meal.aiDesc}",
                            "calories": ${meal.calories.replace("g", "").toIntOrNull() ?: 0},
                            "protein": "${meal.protein}",
                            "carbs": "${meal.carbs}",
                            "fats": "${meal.fats}"
                        }
                    }
                """.trimIndent()

                connection.outputStream.write(requestBody.toByteArray())

                connection.responseCode == 200
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // Get daily tracking log
    suspend fun getDailyLog(userId: String, date: String): DailyLogResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/users/$userId/tracking/daily?date=$date")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    gson.fromJson(response, DailyLogResponse::class.java)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

// API Response Models
data class RegisterResponse(
    val user_id: String,
    val email: String,
    val username: String,
    val message: String
)

data class NutritionPlanResponse(
    val plan: Plan
)

data class Plan(
    val diet: List<DietWeekApi>
)

data class DietWeekApi(
    val weekName: String,
    val meals: Map<String, Map<String, MealApi>>
)

data class MealApi(
    val name: String,
    val calories: Int,
    val protein: String,
    val carbs: String,
    val fats: String
)

data class DailyLogResponse(
    val date: String,
    val daily_log: DailyLog
)

data class DailyLog(
    val meals: Map<String, List<Map<String, Any>>>
)