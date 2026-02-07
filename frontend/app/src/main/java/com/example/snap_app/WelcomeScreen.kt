package com.example.snap_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    viewModel: AppViewModel,
    name: String,
    onNameChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    workoutsPerWeek: String,
    onWorkoutsPerWeekChange: (String) -> Unit,
    workoutDuration: String,
    onWorkoutDurationChange: (String) -> Unit,
    targetPhysique: String,
    onTargetPhysiqueChange: (String) -> Unit,
    dietaryRestriction: String,
    onDietaryRestrictionChange: (String) -> Unit,
    preferredCuisine: String,
    onPreferredCuisineChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    var stepIndex by remember { mutableStateOf(0) }
    val totalSteps = 10
    var heightUnit by remember { mutableStateOf("cm") } // "cm" or "ft"
    var feet by remember { mutableStateOf("") }
    var inches by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val canContinue = name.isNotBlank() &&
        gender.isNotBlank() &&
        height.isNotBlank() &&
        weight.isNotBlank() &&
        age.isNotBlank() &&
        workoutsPerWeek.isNotBlank() &&
        workoutDuration.isNotBlank() &&
        targetPhysique.isNotBlank() &&
        dietaryRestriction.isNotBlank() &&
        preferredCuisine.isNotBlank()

    // Validation for current step
    val canProceedToNext = when (stepIndex) {
        0 -> name.isNotBlank()
        1 -> gender.isNotBlank()
        2 -> if (heightUnit == "cm") height.isNotBlank() else (feet.isNotBlank() && inches.isNotBlank())
        3 -> weight.isNotBlank()
        4 -> age.isNotBlank()
        5 -> workoutsPerWeek.isNotBlank()
        6 -> workoutDuration.isNotBlank()
        7 -> targetPhysique.isNotBlank()
        8 -> dietaryRestriction.isNotBlank()
        9 -> preferredCuisine.isNotBlank()
        else -> false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlue)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A45))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                LinearProgressIndicator(
                    progress = (stepIndex + 1).toFloat() / totalSteps.toFloat(),
                    color = NeonPink,
                    trackColor = Purple.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedContent(
                    targetState = stepIndex,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        val enter = slideInHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            initialOffsetX = { it * direction }
                        ) + fadeIn(animationSpec = tween(300))
                        val exit = slideOutHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            targetOffsetX = { -it * direction }
                        ) + fadeOut(animationSpec = tween(300))
                        ContentTransform(enter, exit)
                    },
                    label = "question-transition"
                ) { step ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (step) {
                            0 -> {
                                SectionTitle(text = "Prerequisite")
                                QuestionTitle(text = "What is your name?")
                                OutlinedInput(
                                    label = "Name",
                                    value = name,
                                    onValueChange = onNameChange
                                )
                            }
                            1 -> {
                                SectionTitle(text = "Prerequisite")
                                QuestionTitle(text = "What is your gender?")
                                SimpleDropdown(
                                    label = "Gender",
                                    value = gender,
                                    options = listOf("Female", "Male", "Non-binary", "Prefer not to say"),
                                    onValueChange = onGenderChange
                                )
                            }
                            2 -> {
                                SectionTitle(text = "Prerequisite")
                                QuestionTitle(text = "What is your height?")
                                
                                // Unit toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = heightUnit == "cm",
                                            onClick = { heightUnit = "cm" },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = NeonPink,
                                                unselectedColor = Purple
                                            )
                                        )
                                        Text(
                                            text = "Centimeters",
                                            color = Color.White,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = heightUnit == "ft",
                                            onClick = { heightUnit = "ft" },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = NeonPink,
                                                unselectedColor = Purple
                                            )
                                        )
                                        Text(
                                            text = "Feet/Inches",
                                            color = Color.White,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                                
                                if (heightUnit == "cm") {
                                    NumericInput(
                                        label = "Height (cm)",
                                        value = height,
                                        onValueChange = onHeightChange
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        NumericInput(
                                            label = "Feet",
                                            value = feet,
                                            onValueChange = { 
                                                feet = it
                                                // Convert to cm and update height
                                                if (feet.isNotBlank() && inches.isNotBlank()) {
                                                    val totalCm = (feet.toIntOrNull() ?: 0) * 30.48 + (inches.toIntOrNull() ?: 0) * 2.54
                                                    onHeightChange(totalCm.toInt().toString())
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        NumericInput(
                                            label = "Inches",
                                            value = inches,
                                            onValueChange = { 
                                                inches = it
                                                // Convert to cm and update height
                                                if (feet.isNotBlank() && inches.isNotBlank()) {
                                                    val totalCm = (feet.toIntOrNull() ?: 0) * 30.48 + (inches.toIntOrNull() ?: 0) * 2.54
                                                    onHeightChange(totalCm.toInt().toString())
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            3 -> {
                                SectionTitle(text = "Prerequisite")
                                QuestionTitle(text = "What is your weight?")
                                NumericInput(
                                    label = "Weight (kg)",
                                    value = weight,
                                    onValueChange = onWeightChange
                                )
                            }
                            4 -> {
                                SectionTitle(text = "Prerequisite")
                                QuestionTitle(text = "How old are you?")
                                NumericInput(
                                    label = "Age",
                                    value = age,
                                    onValueChange = onAgeChange
                                )
                            }
                            5 -> {
                                SectionTitle(text = "Context")
                                QuestionTitle(text = "How often do you work out each week?")
                                SimpleDropdown(
                                    label = "Workouts per week",
                                    value = workoutsPerWeek,
                                    options = listOf("0", "1-2", "3-4", "5-6", "7+"),
                                    onValueChange = onWorkoutsPerWeekChange
                                )
                            }
                            6 -> {
                                SectionTitle(text = "Context")
                                QuestionTitle(text = "How much time per workout?")
                                SimpleDropdown(
                                    label = "Time per workout",
                                    value = workoutDuration,
                                    options = listOf("15-30 min", "30-45 min", "45-60 min", "60+ min"),
                                    onValueChange = onWorkoutDurationChange
                                )
                            }
                            7 -> {
                                SectionTitle(text = "Context")
                                QuestionTitle(text = "What is your target physique?")
                                SimpleDropdown(
                                    label = "Target physique",
                                    value = targetPhysique,
                                    options = listOf("Lose weight", "Build muscle", "Be active"),
                                    onValueChange = onTargetPhysiqueChange
                                )
                            }
                            8 -> {
                                SectionTitle(text = "Nutrition")
                                QuestionTitle(text = "Any dietary restriction?")
                                SimpleDropdown(
                                    label = "Dietary restriction",
                                    value = dietaryRestriction,
                                    options = listOf("Vegan", "Vegetarian", "Non-vegetarian", "Pescatarian", "Other"),
                                    onValueChange = onDietaryRestrictionChange
                                )
                            }
                            else -> {
                                SectionTitle(text = "Nutrition")
                                QuestionTitle(text = "Preferred cuisine?")
                                OutlinedInput(
                                    label = "Preferred cuisine",
                                    value = preferredCuisine,
                                    onValueChange = onPreferredCuisineChange
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { if (stepIndex > 0) stepIndex -= 1 },
                        enabled = stepIndex > 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text(
                            text = "Back",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val isLastStep = stepIndex == totalSteps - 1
                    Button(
                        onClick = {
                            if (isLastStep) {
                                if (canContinue) {
                                    // Map UI values to API values
                                    val activityLevel = when (workoutsPerWeek) {
                                        "0" -> "sedentary"
                                        "1-2" -> "light"
                                        "3-4" -> "moderate"
                                        "5-6" -> "active"
                                        "7+" -> "very_active"
                                        else -> "moderate"
                                    }
                                    val fitnessGoal = when (targetPhysique) {
                                        "Lose weight" -> "weight_loss"
                                        "Build muscle" -> "muscle_gain"
                                        "Be active" -> "maintain"
                                        else -> "weight_loss"
                                    }
                                    val dietTypeApi = when (dietaryRestriction) {
                                        "Vegan" -> "vegan"
                                        "Vegetarian" -> "vegetarian"
                                        "Non-vegetarian" -> "non-vegetarian"
                                        "Pescatarian" -> "pescatarian"
                                        else -> "none"
                                    }

                                    // Create health profile
                                    val workoutsPerDayNum = when (workoutsPerWeek) {
                                        "0" -> 0
                                        "1-2" -> 2
                                        "3-4" -> 4
                                        "5-6" -> 6
                                        "7+" -> 7
                                        else -> 3
                                    }
                                    val workoutDurationMin = when (workoutDuration) {
                                        "15-30 min" -> 22
                                        "30-45 min" -> 37
                                        "45-60 min" -> 52
                                        "60+ min" -> 75
                                        else -> 30
                                    }

                                    viewModel.createHealthProfile(
                                        weight = weight,
                                        height = height,
                                        age = age.toIntOrNull() ?: 25,
                                        gender = gender,
                                        activityLevel = activityLevel,
                                        fitnessGoal = fitnessGoal,
                                        workoutsPerDay = workoutsPerDayNum,
                                        workoutsDuration = workoutDurationMin
                                    )

                                    // Create nutrition profile
                                    viewModel.createNutritionProfile(
                                        dietType = dietTypeApi,
                                        cuisinePreferences = if (preferredCuisine.isNotBlank()) listOf(preferredCuisine) else emptyList()
                                    )

                                    // Generate AI plan
                                    viewModel.generatePlan(
                                        intensity = when (workoutDuration) {
                                            "15-30 min" -> "light"
                                            "30-45 min" -> "moderate"
                                            "45-60 min" -> "high"
                                            "60+ min" -> "intense"
                                            else -> "moderate"
                                        },
                                        goals = listOf(fitnessGoal)
                                    )

                                    onContinue()
                                }
                            } else {
                                if (canProceedToNext) stepIndex += 1
                            }
                        },
                        enabled = if (isLastStep) canContinue else canProceedToNext,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPink,
                            disabledContainerColor = Purple.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = if (isLastStep) "Continue" else "Next",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun QuestionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White.copy(alpha = 0.9f),
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun OutlinedInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = Purple.copy(alpha = 0.7f)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonPink,
            unfocusedBorderColor = Purple,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = NeonPink
        )
    )
}

@Composable
private fun NumericInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Only allow digits
            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                onValueChange(newValue)
            }
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, color = Purple.copy(alpha = 0.7f)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonPink,
            unfocusedBorderColor = Purple,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = NeonPink
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label, color = Purple.copy(alpha = 0.7f)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonPink,
                unfocusedBorderColor = Purple,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = NeonPink
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
