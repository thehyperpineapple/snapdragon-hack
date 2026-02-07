package com.example.snap_app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.*

// Custom colors
val DarkPink = Color(0xFF8B0A7D)
val Yellow = Color(0xFFFFC107)

data class MealReminder(
    val id: Int,
    val name: String,
    val type: String, // "meal", "water", or "gym"
    val timeWindow: String, // Time window like "7:00 AM - 9:00 AM"
    val isCompleted: Boolean = false
)

@Composable
fun RemindersScreen(viewModel: AppViewModel) {
    var reminders by remember { mutableStateOf(generateReminders()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedReminder by remember { mutableStateOf<MealReminder?>(null) }
    var alternateFood by remember { mutableStateOf("") }
    var showCompletedSection by remember { mutableStateOf(false) }
    var lastResetDate by remember { mutableStateOf(getCurrentDate()) }

    // Check if it's a new day and reset if needed
    LaunchedEffect(Unit) {
        val currentDate = getCurrentDate()
        if (currentDate != lastResetDate) {
            reminders = generateReminders()
            lastResetDate = currentDate
        }
    }

    val completedReminders = reminders.filter { it.isCompleted }
    val activeReminders = reminders.filter { !it.isCompleted }
    val completionPercentage = if (reminders.isNotEmpty()) {
        (completedReminders.size.toFloat() / reminders.size.toFloat() * 100).toInt()
    } else 0

    // Update ViewModel with completion percentage
    LaunchedEffect(completionPercentage) {
        viewModel.updateCompletionPercentage(completionPercentage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlue)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main card containing Daily Reminders and all tasks - full page
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up remaining space
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
                    // Top row with Daily Reminders and circular progress
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Daily Reminders text on top left
                        Text(
                            text = "Daily Reminders",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 28.sp
                        )

                        // Circular progress indicator
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(60.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = completionPercentage / 100f,
                                modifier = Modifier.size(60.dp),
                                color = Yellow,
                                strokeWidth = 6.dp,
                                trackColor = Purple.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "$completionPercentage%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Yellow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Motivational blurb
                    Text(
                        text = "Stay on track with your daily goals. Keep logging your progress!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Scrollable reminder items
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Active reminders only
                        items(activeReminders) { reminder ->
                            ReminderItem(
                                reminder = reminder,
                                index = reminders.indexOf(reminder),
                                onYes = {
                                    reminders = reminders.map {
                                        if (it.id == reminder.id) it.copy(isCompleted = true)
                                        else it
                                    }
                                },
                                onNo = {
                                    selectedReminder = reminder
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Completed section - fixed at bottom, outside the main card
            Spacer(modifier = Modifier.height(16.dp))
            CompletedSection(
                completedReminders = completedReminders,
                showCompletedSection = showCompletedSection,
                onToggle = { showCompletedSection = !showCompletedSection },
                onUncomplete = { reminder ->
                    reminders = reminders.map {
                        if (it.id == reminder.id) it.copy(isCompleted = false)
                        else it
                    }
                }
            )
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
                            text = if (selectedReminder?.type == "gym")
                                "What did you do instead?"
                            else
                                "What did you eat instead?",
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
                        placeholder = {
                            Text(
                                if (selectedReminder?.type == "gym")
                                    "Enter your workout..."
                                else
                                    "Enter what you ate...",
                                color = Purple.copy(alpha = 0.6f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Yellow,
                            unfocusedBorderColor = Purple,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Yellow
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            // Log the alternate food/activity and mark as completed
                            reminders = reminders.map {
                                if (it.id == selectedReminder?.id) it.copy(isCompleted = true)
                                else it
                            }
                            showDialog = false
                            alternateFood = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Yellow
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Submit",
                            color = Color.Black,
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
fun CompletedSection(
    completedReminders: List<MealReminder>,
    showCompletedSection: Boolean,
    onToggle: () -> Unit,
    onUncomplete: (MealReminder) -> Unit
) {
    val hasCompletedItems = completedReminders.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF2D1B3D),
                    shape = RoundedCornerShape(12.dp)
                )
                .then(
                    if (hasCompletedItems) Modifier.clickable { onToggle() }
                    else Modifier
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Yellow,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(${completedReminders.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Only show expand/collapse icon if there are completed items
            if (hasCompletedItems) {
                Icon(
                    imageVector = if (showCompletedSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showCompletedSection) "Collapse" else "Expand",
                    tint = Yellow
                )
            }
        }

        // Completed items (expandable) - only show if there are items AND section is expanded
        if (showCompletedSection && hasCompletedItems) {
            Spacer(modifier = Modifier.height(12.dp))
            completedReminders.forEach { reminder ->
                CompletedItem(
                    reminder = reminder,
                    onUncomplete = { onUncomplete(reminder) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CompletedItem(
    reminder: MealReminder,
    onUncomplete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF1F1F1F),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.name.replace("🍽️ ", "").replace("💧 ", "").replace("🏋️ ", ""),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = reminder.timeWindow,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.3f)
            )
        }

        TextButton(onClick = onUncomplete) {
            Text(
                text = "Undo",
                color = Yellow,
                fontWeight = FontWeight.Bold
            )
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
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reminder text with time window
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.name.replace("🍽️ ", "").replace("💧 ", "").replace("🏋️ ", ""),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = reminder.timeWindow,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }

        // Yes/No buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Yes button (darker pink)
            Button(
                onClick = onYes,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPink
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Yes",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // No button
            Button(
                onClick = onNo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "No",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// Generate reminders with time windows
fun generateReminders(): List<MealReminder> {
    val reminders = mutableListOf<MealReminder>()
    var id = 0

    // Breakfast
    reminders.add(
        MealReminder(
            id = id++,
            name = "🍽️ Breakfast",
            type = "meal",
            timeWindow = "7:00 AM - 9:00 AM"
        )
    )
    reminders.add(
        MealReminder(
            id = id++,
            name = "💧 Water after Breakfast",
            type = "water",
            timeWindow = "7:00 AM - 9:00 AM"
        )
    )

    // Lunch
    reminders.add(
        MealReminder(
            id = id++,
            name = "🍽️ Lunch",
            type = "meal",
            timeWindow = "12:00 PM - 2:00 PM"
        )
    )
    reminders.add(
        MealReminder(
            id = id++,
            name = "💧 Water after Lunch",
            type = "water",
            timeWindow = "12:00 PM - 2:00 PM"
        )
    )

    // Dinner
    reminders.add(
        MealReminder(
            id = id++,
            name = "🍽️ Dinner",
            type = "meal",
            timeWindow = "6:00 PM - 8:00 PM"
        )
    )
    reminders.add(
        MealReminder(
            id = id++,
            name = "💧 Water after Dinner",
            type = "water",
            timeWindow = "6:00 PM - 8:00 PM"
        )
    )

    // Gym
    reminders.add(
        MealReminder(
            id = id++,
            name = "🏋️ Gym Workout",
            type = "gym",
            timeWindow = "5:00 PM - 7:00 PM"
        )
    )

    return reminders
}

// Get current date as string (format: YYYY-MM-DD)
fun getCurrentDate(): String {
    val calendar = Calendar.getInstance()
    return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
}