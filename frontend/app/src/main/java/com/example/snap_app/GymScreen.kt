package com.example.snap_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName



// Data Classes
data class Exercise(
    @SerializedName("name") val name: String = "",
    @SerializedName("sets") val sets: String = "",
    @SerializedName("reps") val reps: String = ""
)

data class WorkoutSession(
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("exercises") val exercises: Map<String, Exercise> = emptyMap()
)

data class WorkoutData(
    val week: String,
    val workoutName: String,
    val completed: Boolean,
    val exercises: List<Exercise>
)

@Composable
fun GymScreen() {
    // Sample JSON data
    val jsonData = """
    {
      "workouts": [
        {
          "week": "week1",
          "workout1": {
            "completed": false,
            "exercises": {
              "exercise1": {
                "name": "Bench Press",
                "sets": "4",
                "reps": "8-10"
              },
              "exercise2": {
                "name": "Squats",
                "sets": "3",
                "reps": "12"
              },
              "exercise3": {
                "name": "Deadlift",
                "sets": "3",
                "reps": "6-8"
              }
            }
          },
          "workout2": {
            "completed": true,
            "exercises": {
              "exercise1": {
                "name": "Pull Ups",
                "sets": "3",
                "reps": "10"
              },
              "exercise2": {
                "name": "Dumbbell Rows",
                "sets": "4",
                "reps": "12"
              }
            }
          }
        }
      ]
    }
    """

    val workouts = remember { parseWorkouts(jsonData) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Gym Workouts 💪",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonPink,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your personalized workout plan",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(workouts) { workout ->
                    WorkoutCard(workout)
                }
            }
        }
    }
}

@Composable
fun WorkoutCard(workout: WorkoutData) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = workout.week.replace("week", "Week "),
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonPink,
                        fontSize = 12.sp
                    )
                    Text(
                        text = workout.workoutName.replace("workout", "Workout "),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (workout.completed)
                                Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else
                                Color.Gray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (workout.completed) "✓ Done" else "○ Pending",
                        color = if (workout.completed)
                            Color(0xFF4CAF50)
                        else
                            Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exercises
            workout.exercises.forEachIndexed { index, exercise ->
                ExerciseItem(exercise, isLast = index == workout.exercises.size - 1)
            }
        }
    }
}

@Composable
fun ExerciseItem(exercise: Exercise, isLast: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip("${exercise.sets} sets", NeonPink.copy(alpha = 0.2f), NeonPink)
                InfoChip("${exercise.reps} reps", Color(0xFF64B5F6).copy(alpha = 0.2f), Color(0xFF64B5F6))
            }
        }

        if (!isLast) {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun InfoChip(text: String, backgroundColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Helper function to parse JSON
fun parseWorkouts(jsonData: String): List<WorkoutData> {
    val gson = Gson()
    val workoutList = mutableListOf<WorkoutData>()

    try {
        val json = gson.fromJson(jsonData, Map::class.java)
        val workouts = json["workouts"] as? List<Map<String, Any>> ?: emptyList()

        workouts.forEach { weekMap ->
            val week = weekMap["week"] as? String ?: ""

            weekMap.forEach { (key, value) ->
                if (key.startsWith("workout") && value is Map<*, *>) {
                    val workoutMap = value as Map<String, Any>
                    val completed = workoutMap["completed"] as? Boolean ?: false
                    val exercisesMap = workoutMap["exercises"] as? Map<String, Map<String, String>>

                    val exercises = exercisesMap?.values?.map { exerciseData ->
                        Exercise(
                            name = exerciseData["name"] ?: "",
                            sets = exerciseData["sets"] ?: "",
                            reps = exerciseData["reps"] ?: ""
                        )
                    } ?: emptyList()

                    workoutList.add(
                        WorkoutData(
                            week = week,
                            workoutName = key,
                            completed = completed,
                            exercises = exercises
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return workoutList
}

