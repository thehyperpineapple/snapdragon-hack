package com.example.snap_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToNutrition: () -> Unit = {},
    onNavigateToGym: () -> Unit = {},
    onNavigateToChat: () -> Unit = {}
) {
    val completionPercentage by viewModel.completionPercentage.collectAsState()

    // Determine which meme to show based on completion
    val memeUrl = when {
        completionPercentage < 30 -> "https://i.ytimg.com/vi/jXdbw21SKQg/mqdefault.jpg" // Sad
        completionPercentage < 70 -> "https://static.wikia.nocookie.net/belugacinematicuniversefanon/images/6/6a/Beluga.jpg/revision/latest/thumbnail/width/360/height/360?cb=20231226224904" // Medium
        else -> "https://i.pinimg.com/736x/ea/de/1f/eade1feca67faed06570cf5495621746.jpg" // Happy
    }

    val memeText = when {
        completionPercentage < 30 -> "Bruh... 😔"
        completionPercentage < 70 -> "Not bad, keep going! 💪"
        else -> "Absolute legend! 🔥"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlue)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hello at the top
            Text(
                text = "Hello! 👋",
                style = MaterialTheme.typography.headlineLarge,
                color = NeonPink,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Meme section - bigger image
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = memeUrl,
                        contentDescription = "Mood meme",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = memeText,
                        style = MaterialTheme.typography.titleLarge,
                        color = Yellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Text(
                        text = "$completionPercentage% completed today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nutrition Screen Button
                NavigationButton(
                    text = "Nutrition Tracker",
                    icon = Icons.Default.Restaurant,
                    backgroundColor = Color(0xFF2E7D32),
                    onClick = onNavigateToNutrition
                )

                // Gym Screen Button
                NavigationButton(
                    text = "Gym Tracker",
                    icon = Icons.Default.FitnessCenter,
                    backgroundColor = DarkPink,
                    onClick = onNavigateToGym
                )

                // Chat Screen Button
                NavigationButton(
                    text = "Chat with AI",
                    icon = Icons.Default.Chat,
                    backgroundColor = Purple,
                    onClick = onNavigateToChat
                )
            }
        }
    }
}

@Composable
fun NavigationButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}