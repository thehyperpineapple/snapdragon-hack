package com.example.snap_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.snap_app.ui.theme.Snap_appTheme

// Custom color scheme
val DarkBlue = Color(0xFF0A1929)
val NeonPink = Color(0xFFFF10F0)
val Purple = Color(0xFF9C27B0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Snap_appTheme {
                MainScreen()
            }
        }
    }
}

/* -------------------- Screens & Routes -------------------- */
sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Messages : Screen("messages", "Messages", Icons.Default.Chat)
    object Camera : Screen("camera", "Camera", Icons.Default.CameraAlt)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}


val bottomNavItems = listOf(
    Screen.Home,
    Screen.Profile,
    Screen.Messages,
    Screen.Camera,
    Screen.Settings
)

/* -------------------- Main Scaffold with Bottom Nav -------------------- */
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = DarkBlue, // Dark blue background
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            // make screens here guys!
            //composable(Screen.Profile.route) { ProfileScreen() }
            //composable(Screen.Messages.route) { MessagesScreen() }
            //composable(Screen.Camera.route) { CameraScreen() }
            //composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

/* -------------------- Bottom Navigation -------------------- */
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    NavigationBar(
        containerColor = DarkBlue.copy(alpha = 0.95f),
        contentColor = NeonPink
    ) {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonPink,
                    selectedTextColor = NeonPink,
                    unselectedIconColor = Purple,
                    unselectedTextColor = Purple,
                    indicatorColor = Purple.copy(alpha = 0.3f)
                ),
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to avoid building up a large back stack
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}



/* -------------------- Preview -------------------- */
@Preview(showBackground = true)
@Composable
fun AppPreview() {
    Snap_appTheme {
        MainScreen()
    }
}