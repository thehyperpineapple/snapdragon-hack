package com.example.snap_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// Data Classes for Nutrition
data class Meal(
    @SerializedName("AIdesc") val aiDesc: String = "",
    @SerializedName("actualMeal") val actualMeal: String = "",
    @SerializedName("calories") val calories: String = "",
    @SerializedName("carbs") val carbs: String = "",
    @SerializedName("fats") val fats: String = "",
    @SerializedName("protein") val protein: String = "",
    val completed: Boolean = false
)

data class DietWeek(
    val week: String,
    val breakfast: Meal,
    val lunch: Meal,
    val dinner: Meal
)

@Composable
fun NutritionScreen(viewModel: AppViewModel? = null) {
    val apiWeeks by viewModel?.nutritionPlan?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val planLoading by viewModel?.planLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val planError by viewModel?.planError?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

    var dietWeeks by remember(apiWeeks) {
        mutableStateOf(apiWeeks)
    }

    // Try fetching plan if we have a viewModel and no data yet
    LaunchedEffect(Unit) {
        if (viewModel != null && apiWeeks.isEmpty()) {
            viewModel.fetchPlan()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Nutrition Plan 🍎",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonPink,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your personalized meal plan",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {
                planLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonPink)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading your nutrition plan...",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                planError != null && dietWeeks.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A45))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Could not load nutrition plan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = planError ?: "Unknown error",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel?.fetchPlan() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }
                dietWeeks.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A45))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No nutrition plan yet",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Complete your profile setup to get a personalized meal plan.",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                else -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(dietWeeks.size) { weekIndex ->
                    val dietWeek = dietWeeks[weekIndex]
                    DietWeekCard(
                        dietWeek = dietWeek,
                        onMealToggle = { mealType ->
                            val updatedWeek = when (mealType) {
                                "breakfast" -> {
                                    if (!dietWeek.breakfast.completed) {
                                        viewModel?.logMealCompletion("breakfast", dietWeek.breakfast)
                                    }
                                    dietWeek.copy(
                                        breakfast = dietWeek.breakfast.copy(
                                            completed = !dietWeek.breakfast.completed
                                        )
                                    )
                                }
                                "lunch" -> {
                                    if (!dietWeek.lunch.completed) {
                                        viewModel?.logMealCompletion("lunch", dietWeek.lunch)
                                    }
                                    dietWeek.copy(
                                        lunch = dietWeek.lunch.copy(
                                            completed = !dietWeek.lunch.completed
                                        )
                                    )
                                }
                                "dinner" -> {
                                    if (!dietWeek.dinner.completed) {
                                        viewModel?.logMealCompletion("dinner", dietWeek.dinner)
                                    }
                                    dietWeek.copy(
                                        dinner = dietWeek.dinner.copy(
                                            completed = !dietWeek.dinner.completed
                                        )
                                    )
                                }
                                else -> dietWeek
                            }

                            dietWeeks = dietWeeks.toMutableList().apply {
                                this[weekIndex] = updatedWeek
                            }
                        }
                    )
                }
            }
                } // end else
            } // end when
        }
    }
}

@Composable
fun DietWeekCard(
    dietWeek: DietWeek,
    onMealToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A2A45)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Week Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dietWeek.week.replace("week", "Week "),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                // Daily calories total
                val totalCalories = listOf(
                    dietWeek.breakfast.calories,
                    dietWeek.lunch.calories,
                    dietWeek.dinner.calories
                ).sumOf { it.replace("g", "").toIntOrNull() ?: 0 }

                Box(
                    modifier = Modifier
                        .background(
                            color = NeonPink.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$totalCalories cal/day",
                        color = NeonPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress indicator
            val completedMeals = listOf(
                dietWeek.breakfast.completed,
                dietWeek.lunch.completed,
                dietWeek.dinner.completed
            ).count { it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { completedMeals / 3f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color.Gray.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "$completedMeals/3 meals",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Breakfast
            MealCard(
                mealType = "🌅 Breakfast",
                meal = dietWeek.breakfast,
                onToggle = { onMealToggle("breakfast") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lunch
            MealCard(
                mealType = "☀️ Lunch",
                meal = dietWeek.lunch,
                onToggle = { onMealToggle("lunch") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dinner
            MealCard(
                mealType = "🌙 Dinner",
                meal = dietWeek.dinner,
                onToggle = { onMealToggle("dinner") }
            )
        }
    }
}

@Composable
fun MealCard(
    mealType: String,
    meal: Meal,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (meal.completed)
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                else
                    Color(0xFF1E3050),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onToggle() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (meal.completed)
                            Color(0xFF4CAF50)
                        else
                            Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (meal.completed) Color(0xFF4CAF50) else Color.Gray,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (meal.completed) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = mealType,
                style = MaterialTheme.typography.titleMedium,
                color = if (meal.completed)
                    Color.White.copy(alpha = 0.6f)
                else
                    Color.White,
                fontWeight = FontWeight.Bold,
                textDecoration = if (meal.completed)
                    TextDecoration.LineThrough
                else
                    TextDecoration.None
            )
        }

        if (meal.aiDesc.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = meal.aiDesc,
                style = MaterialTheme.typography.bodyLarge,
                color = if (meal.completed)
                    NeonPink.copy(alpha = 0.5f)
                else
                    NeonPink,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (meal.actualMeal.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = meal.actualMeal,
                style = MaterialTheme.typography.bodyMedium,
                color = if (meal.completed)
                    Color.White.copy(alpha = 0.4f)
                else
                    Color.White.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Macros Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NutritionChip(
                label = meal.calories,
                color = Color(0xFFFF9800),
                isCompleted = meal.completed
            )
            NutritionChip(
                label = "C: ${meal.carbs}",
                color = Color(0xFF2196F3),
                isCompleted = meal.completed
            )
            NutritionChip(
                label = "F: ${meal.fats}",
                color = Color(0xFFFFC107),
                isCompleted = meal.completed
            )
            NutritionChip(
                label = "P: ${meal.protein}",
                color = Color(0xFF4CAF50),
                isCompleted = meal.completed
            )
        }
    }
}

@Composable
fun NutritionChip(
    label: String,
    color: Color,
    isCompleted: Boolean
) {
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = if (isCompleted) 0.1f else 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isCompleted) color.copy(alpha = 0.5f) else color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Helper function to parse diet JSON
fun parseDiet(jsonData: String): List<DietWeek> {
    val gson = Gson()
    val dietWeeks = mutableListOf<DietWeek>()

    try {
        val json = gson.fromJson(jsonData, Map::class.java)
        val diet = json["diet"] as? List<Map<String, Any>> ?: emptyList()

        diet.forEach { weekMap ->
            val week = weekMap["week"] as? String ?: ""

            val breakfastMap = weekMap["breakfast"] as? Map<String, String> ?: emptyMap()
            val lunchMap = weekMap["lunch"] as? Map<String, String> ?: emptyMap()
            val dinnerMap = weekMap["dinner"] as? Map<String, String> ?: emptyMap()

            val breakfast = Meal(
                aiDesc = breakfastMap["AIdesc"] ?: "",
                actualMeal = breakfastMap["actualMeal"] ?: "",
                calories = breakfastMap["calories"] ?: "",
                carbs = breakfastMap["carbs"] ?: "",
                fats = breakfastMap["fats"] ?: "",
                protein = breakfastMap["protein"] ?: "",
                completed = false
            )

            val lunch = Meal(
                aiDesc = lunchMap["AIdesc"] ?: "",
                actualMeal = lunchMap["actualMeal"] ?: "",
                calories = lunchMap["calories"] ?: "",
                carbs = lunchMap["carbs"] ?: "",
                fats = lunchMap["fats"] ?: "",
                protein = lunchMap["protein"] ?: "",
                completed = false
            )

            val dinner = Meal(
                aiDesc = dinnerMap["AIdesc"] ?: "",
                actualMeal = dinnerMap["actualMeal"] ?: "",
                calories = dinnerMap["calories"] ?: "",
                carbs = dinnerMap["carbs"] ?: "",
                fats = dinnerMap["fats"] ?: "",
                protein = dinnerMap["protein"] ?: "",
                completed = false
            )

            dietWeeks.add(
                DietWeek(
                    week = week,
                    breakfast = breakfast,
                    lunch = lunch,
                    dinner = dinner
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return dietWeeks
}