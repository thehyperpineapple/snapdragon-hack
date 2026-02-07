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

// Data Classes
data class Exercise(
    @SerializedName("name") val name: String = "",
    @SerializedName("sets") val sets: String = "",
    @SerializedName("reps") val reps: String = "",
    val completed: Boolean = false
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

// Hardcoded sample workouts for offline/demo mode
object SampleWorkouts {
    val sampleWorkoutPlan = listOf(
        WorkoutData(
            week = "week1",
            workoutName = "workout1",
            completed = false,
            exercises = listOf(
                Exercise("Barbell Squat", "4", "8-10", false),
                Exercise("Romanian Deadlift", "3", "10-12", false),
                Exercise("Leg Press", "3", "12-15", false),
                Exercise("Leg Curl", "3", "12-15", false),
                Exercise("Calf Raises", "4", "15-20", false)
            )
        ),
        WorkoutData(
            week = "week1",
            workoutName = "workout2",
            completed = false,
            exercises = listOf(
                Exercise("Bench Press", "4", "8-10", false),
                Exercise("Incline Dumbbell Press", "3", "10-12", false),
                Exercise("Cable Flyes", "3", "12-15", false),
                Exercise("Tricep Dips", "3", "10-12", false),
                Exercise("Overhead Tricep Extension", "3", "12-15", false)
            )
        ),
        WorkoutData(
            week = "week1",
            workoutName = "workout3",
            completed = false,
            exercises = listOf(
                Exercise("Pull-ups", "4", "8-10", false),
                Exercise("Barbell Row", "4", "8-10", false),
                Exercise("Lat Pulldown", "3", "10-12", false),
                Exercise("Face Pulls", "3", "15-20", false),
                Exercise("Barbell Curl", "3", "10-12", false),
                Exercise("Hammer Curl", "3", "12-15", false)
            )
        ),
        WorkoutData(
            week = "week2",
            workoutName = "workout1",
            completed = false,
            exercises = listOf(
                Exercise("Deadlift", "4", "6-8", false),
                Exercise("Front Squat", "3", "8-10", false),
                Exercise("Bulgarian Split Squat", "3", "10-12", false),
                Exercise("Leg Extension", "3", "12-15", false),
                Exercise("Standing Calf Raise", "4", "12-15", false)
            )
        ),
        WorkoutData(
            week = "week2",
            workoutName = "workout2",
            completed = false,
            exercises = listOf(
                Exercise("Overhead Press", "4", "8-10", false),
                Exercise("Lateral Raises", "3", "12-15", false),
                Exercise("Front Raises", "3", "12-15", false),
                Exercise("Arnold Press", "3", "10-12", false),
                Exercise("Face Pulls", "3", "15-20", false)
            )
        ),
        WorkoutData(
            week = "week2",
            workoutName = "workout3",
            completed = false,
            exercises = listOf(
                Exercise("Incline Bench Press", "4", "8-10", false),
                Exercise("Dumbbell Flyes", "3", "12-15", false),
                Exercise("Close-Grip Bench Press", "3", "10-12", false),
                Exercise("Skull Crushers", "3", "10-12", false),
                Exercise("Cable Tricep Pushdown", "3", "12-15", false)
            )
        )
    )
}

@Composable
fun GymScreen(viewModel: AppViewModel? = null) {
    // Get data from viewModel if available
    val viewModelWorkouts by viewModel?.workoutPlan?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val planLoading by viewModel?.planLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val planError by viewModel?.planError?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

    // Local state for sample workouts when offline
    var localWorkouts by remember { mutableStateOf(SampleWorkouts.sampleWorkoutPlan) }

    // Determine which workouts to use
    val workouts = if (viewModelWorkouts.isNotEmpty()) {
        viewModelWorkouts
    } else {
        localWorkouts
    }

    val isUsingSampleData = viewModelWorkouts.isEmpty()

    // Only try fetching if viewModel exists
    LaunchedEffect(viewModel) {
        if (viewModel != null) {
            viewModel.fetchPlan()
        }
    }

    // Local toggle functions for sample data
    fun toggleLocalExerciseCompletion(workoutIndex: Int, exerciseIndex: Int) {
        localWorkouts = localWorkouts.toMutableList().apply {
            val workout = this[workoutIndex]
            val updatedExercises = workout.exercises.toMutableList().apply {
                this[exerciseIndex] = this[exerciseIndex].copy(completed = !this[exerciseIndex].completed)
            }
            this[workoutIndex] = workout.copy(exercises = updatedExercises)
        }
    }

    fun toggleLocalWorkoutCompletion(workoutIndex: Int) {
        localWorkouts = localWorkouts.toMutableList().apply {
            val workout = this[workoutIndex]
            val newCompletedState = !workout.completed
            val updatedExercises = workout.exercises.map { it.copy(completed = newCompletedState) }
            this[workoutIndex] = workout.copy(
                completed = newCompletedState,
                exercises = updatedExercises
            )
        }
    }

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
                                text = "Loading your workout plan...",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                planError != null && workouts.isEmpty() -> {
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
                                text = "Could not load workout plan",
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
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(workouts.size) { index ->
                            val workout = workouts[index]
                            WorkoutCard(
                                workout = workout,
                                onExerciseToggle = { exerciseIndex ->
                                    if (isUsingSampleData) {
                                        toggleLocalExerciseCompletion(index, exerciseIndex)
                                    } else {
                                        viewModel?.toggleExerciseCompletion(index, exerciseIndex)
                                    }
                                },
                                onWorkoutToggle = {
                                    if (isUsingSampleData) {
                                        toggleLocalWorkoutCompletion(index)
                                    } else {
                                        viewModel?.toggleWorkoutCompletion(index)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutCard(
    workout: WorkoutData,
    onExerciseToggle: (Int) -> Unit,
    onWorkoutToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
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

                // Status Badge - Clickable to toggle entire workout
                Box(
                    modifier = Modifier
                        .background(
                            color = if (workout.completed)
                                Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else
                                Color.Gray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onWorkoutToggle() }
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

            // Progress indicator
            val completedCount = workout.exercises.count { it.completed }
            val totalCount = workout.exercises.size

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NeonPink,
                    trackColor = Color.Gray.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "$completedCount/$totalCount",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exercises
            workout.exercises.forEachIndexed { index, exercise ->
                ExerciseItem(
                    exercise = exercise,
                    isLast = index == workout.exercises.size - 1,
                    onToggle = { onExerciseToggle(index) }
                )
            }
        }
    }
}

@Composable
fun ExerciseItem(
    exercise: Exercise,
    isLast: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (exercise.completed)
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else
                        Color(0xFF1E3050),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onToggle() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (exercise.completed)
                            NeonPink
                        else
                            Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (exercise.completed) NeonPink else Color.Gray,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (exercise.completed) {
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
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (exercise.completed)
                    Color.White.copy(alpha = 0.6f)
                else
                    Color.White,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (exercise.completed)
                    TextDecoration.LineThrough
                else
                    TextDecoration.None,
                modifier = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(
                    text = "${exercise.sets} sets",
                    backgroundColor = NeonPink.copy(alpha = if (exercise.completed) 0.1f else 0.2f),
                    textColor = if (exercise.completed)
                        NeonPink.copy(alpha = 0.5f)
                    else
                        NeonPink
                )
                InfoChip(
                    text = "${exercise.reps} reps",
                    backgroundColor = Color(0xFF64B5F6).copy(alpha = if (exercise.completed) 0.1f else 0.2f),
                    textColor = if (exercise.completed)
                        Color(0xFF64B5F6).copy(alpha = 0.5f)
                    else
                        Color(0xFF64B5F6)
                )
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
                            reps = exerciseData["reps"] ?: "",
                            completed = false
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