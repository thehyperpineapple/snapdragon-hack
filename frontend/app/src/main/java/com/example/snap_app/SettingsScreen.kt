package com.example.snap_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    userName: String = "User",
    onBack: () -> Unit
) {
    var isDarkMode by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var mealRemindersEnabled by remember { mutableStateOf(true) }
    var workoutRemindersEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onBack() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.85f)
                .background(DarkBlue)
                .verticalScroll(rememberScrollState())
                .clickable(enabled = false) { }
        ) {
            // Header with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
                
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonPink,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Divider(color = Purple.copy(alpha = 0.3f), thickness = 1.dp)

            // User Profile Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Purple.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "User",
                            tint = NeonPink,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "SnapFit User",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Settings Items
            CompactSettingItem(
                icon = if (isDarkMode) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                title = "Dark Mode",
                isToggle = true,
                isEnabled = isDarkMode,
                onToggle = { isDarkMode = it }
            )

            CompactSettingItem(
                icon = if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                title = "Push Notifications",
                isToggle = true,
                isEnabled = notificationsEnabled,
                onToggle = { notificationsEnabled = it }
            )

            CompactSettingItem(
                icon = Icons.Default.Notifications,
                title = "Meal Reminders",
                isToggle = true,
                isEnabled = mealRemindersEnabled,
                onToggle = { mealRemindersEnabled = it },
                isEnabled2 = notificationsEnabled
            )

            CompactSettingItem(
                icon = Icons.Default.Notifications,
                title = "Workout Reminders",
                isToggle = true,
                isEnabled = workoutRemindersEnabled,
                onToggle = { workoutRemindersEnabled = it },
                isEnabled2 = notificationsEnabled
            )

            Spacer(modifier = Modifier.height(8.dp))

            Divider(color = Purple.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 12.dp))

            Spacer(modifier = Modifier.height(8.dp))

            CompactSettingItem(
                icon = null,
                title = "App Version",
                subtitle = "1.0.0",
                isToggle = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { /* TODO: Logout functionality */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPink.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Logout",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun CompactSettingItem(
    icon: ImageVector?,
    title: String,
    subtitle: String = "",
    isToggle: Boolean = false,
    isEnabled: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null,
    isEnabled2: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = if (isEnabled2) NeonPink else Purple.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (isToggle) {
            Switch(
                checked = isEnabled && isEnabled2,
                onCheckedChange = { if (isEnabled2) onToggle?.invoke(it) },
                modifier = Modifier.padding(start = 8.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonPink,
                    checkedTrackColor = NeonPink.copy(alpha = 0.3f),
                    uncheckedThumbColor = Purple,
                    uncheckedTrackColor = Purple.copy(alpha = 0.3f)
                ),
                enabled = isEnabled2
            )
        }
    }
}
