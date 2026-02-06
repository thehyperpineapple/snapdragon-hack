package com.example.snap_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Custom colors

val DarkPink = Color(0xFF8B0A7D)

// Number of meals per day - you can change this
const val MEALS_PER_DAY = 3

data class MealReminder(
    val id: Int,
    val name: String,
    val type: String // "meal" or "water"
)

@Composable
fun HomeScreen() {
    var reminders by remember { mutableStateOf(generateReminders()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedReminder by remember { mutableStateOf<MealReminder?>(null) }
    var alternateFood by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlue)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main card containing Hello and all tasks - half screen height
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f) // Takes up 50% of screen height
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Hello text on top left
                    Text(
                        text = "Hello!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // All reminder items
                    reminders.forEachIndexed { index, reminder ->
                        ReminderItem(
                            reminder = reminder,
                            index = index,
                            onYes = {
                                reminders = reminders.filter { it.id != reminder.id }
                            },
                            onNo = {
                                selectedReminder = reminder
                                showDialog = true
                            }
                        )
                        if (index < reminders.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }

    // Dialog for "No" button
    if (showDialog && selectedReminder != null) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkBlue
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "What did you eat instead?",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showDialog = false }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Purple
                            )
                        }
                    }

                    OutlinedTextField(
                        value = alternateFood,
                        onValueChange = { alternateFood = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter what you ate...", color = Purple.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPink,
                            unfocusedBorderColor = Purple,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = NeonPink
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            // Log the alternate food (you can add logic here to save it)
                            reminders = reminders.filter { it.id != selectedReminder?.id }
                            showDialog = false
                            alternateFood = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPink
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Submit",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: MealReminder,
    index: Int,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    // Different purple shades for variety
    val purpleShades = listOf(
        Color(0xFF2D1B3D),
        Color(0xFF3D2554),
        Color(0xFF4A2E6B),
        Color(0xFF372049)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = purpleShades[index % purpleShades.size],
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reminder text (without emojis)
        Text(
            text = reminder.name.replace("🍽️ ", "").replace("💧 ", ""),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Yes/No buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Yes button (darker pink)
            Button(
                onClick = onYes,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPink
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Yes",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // No button
            Button(
                onClick = onNo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "No",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Generate reminders based on number of meals
fun generateReminders(): List<MealReminder> {
    val reminders = mutableListOf<MealReminder>()
    var id = 0

    val mealNames = listOf("Breakfast", "Lunch", "Dinner", "Snack 1", "Snack 2", "Snack 3")

    for (i in 0 until MEALS_PER_DAY) {
        // Add meal reminder
        reminders.add(
            MealReminder(
                id = id++,
                name = "🍽️ ${mealNames.getOrElse(i) { "Meal ${i + 1}" }}",
                type = "meal"
            )
        )

        // Add water reminder after meal
        reminders.add(
            MealReminder(
                id = id++,
                name = "💧 Water after ${mealNames.getOrElse(i) { "Meal ${i + 1}" }}",
                type = "water"
            )
        )
    }

    return reminders
}