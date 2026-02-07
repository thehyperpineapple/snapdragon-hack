package com.example.snap_app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * Main dashboard screen displaying daily progress and navigation.
 * Shows animated progress circle, motivational meme based on completion,
 * and quick action cards for core features.
 *
 * @param viewModel App-wide view model tracking user state and progress
 * @param onNavigateToNutrition Callback to navigate to nutrition tracking
 * @param onNavigateToGym Callback to navigate to workout tracking
 * @param onNavigateToChat Callback to navigate to AI coach chat
 */
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToNutrition: () -> Unit = {},
    onNavigateToGym: () -> Unit = {},
    onNavigateToChat: () -> Unit = {}
) {
    val completionPercentage by viewModel.completionPercentage.collectAsState()
    val username by viewModel.username.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = completionPercentage / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    val memeUrl = when {
        completionPercentage < 30 -> "https://i.ytimg.com/vi/jXdbw21SKQg/mqdefault.jpg"
        completionPercentage < 70 -> "https://static.wikia.nocookie.net/belugacinematicuniversefanon/images/6/6a/Beluga.jpg/revision/latest/thumbnail/width/360/height/360?cb=20231226224904"
        else -> "https://i.pinimg.com/736x/ea/de/1f/eade1feca67faed06570cf5495621746.jpg"
    }

    val (statusColor, statusText) = when {
        completionPercentage < 30 -> Pair(Color(0xFFEF5350), "Needs Attention")
        completionPercentage < 70 -> Pair(Color(0xFFFFB74D), "On Track")
        else -> Pair(Color(0xFF66BB6A), "Crushing It!")
    }

    val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBlue,
                        Color(0xFF0D1828)
                    )
                )
            )
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ){
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (username.isNotBlank()) "Welcome, $username" else "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = NeonPink.copy(alpha = 0.1f)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A2A45)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Progress",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(80.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = animatedProgress,
                                modifier = Modifier.size(80.dp),
                                color = statusColor,
                                strokeWidth = 8.dp,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$completionPercentage%",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }

                    AsyncImage(
                        model = memeUrl,
                        contentDescription = "Mood indicator",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfessionalNavigationCard(
                    title = "Nutrition Tracker",
                    subtitle = "Log meals & track calories",
                    icon = Icons.Default.Restaurant,
                    gradientColors = listOf(
                        Color(0xFF1A2A45),
                        Color(0xFF1E3050)
                    ),
                    onClick = onNavigateToNutrition
                )

                ProfessionalNavigationCard(
                    title = "Gym Tracker",
                    subtitle = "Track workouts & progress",
                    icon = Icons.Default.FitnessCenter,
                    gradientColors = listOf(
                        Color(0xFF1A2A45),
                        Color(0xFF1E3050)
                    ),
                    onClick = onNavigateToGym
                )

                ProfessionalNavigationCard(
                    title = "AI Coach",
                    subtitle = "Get personalized advice",
                    icon = Icons.Default.Chat,
                    gradientColors = listOf(
                        Color(0xFF1A2A45),
                        Color(0xFF1E3050)
                    ),
                    onClick = onNavigateToChat
                )
            }
        }
    }
}

/**
 * Clickable card for navigating to main app features.
 * Uses horizontal gradient background with icon, title, and subtitle.
 *
 * @param title Main feature name
 * @param subtitle Brief description of feature
 * @param icon Material icon to display
 * @param gradientColors Left-to-right gradient colors for background
 * @param onClick Navigation callback
 */
@Composable
fun ProfessionalNavigationCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFF1A2A45).copy(alpha = 0.5f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(gradientColors)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}