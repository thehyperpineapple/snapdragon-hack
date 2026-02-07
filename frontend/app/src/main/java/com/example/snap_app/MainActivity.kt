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
import androidx.compose.runtime.saveable.rememberSaveable
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
    object Auth : Screen("auth", "Auth", Icons.Default.Info)
    object Login : Screen("login", "Login", Icons.Default.Info)
    object SignUp : Screen("signup", "SignUp", Icons.Default.Info)
    object Welcome : Screen("welcome", "Welcome", Icons.Default.Info)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Messages : Screen("messages", "Messages", Icons.Default.Chat)
    object Reminders : Screen("reminders", "Reminders", Icons.Default.Alarm)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Gym : Screen("gym", "Gym", Icons.Default.SportsGymnastics)

}


val bottomNavItems = listOf(
    Screen.Home,
    Screen.Messages,
    Screen.Gym,
    Screen.Reminders
)

/* -------------------- Main Scaffold with Top App Bar & Bottom Nav -------------------- */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    var name by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }

    var workoutsPerWeek by rememberSaveable { mutableStateOf("") }
    var workoutDuration by rememberSaveable { mutableStateOf("") }
    var targetPhysique by rememberSaveable { mutableStateOf("") }

    var dietaryRestriction by rememberSaveable { mutableStateOf("") }
    var preferredCuisine by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = DarkBlue,
        topBar = {
            if (currentRoute !in listOf(
                Screen.Auth.route,
                Screen.Login.route,
                Screen.SignUp.route,
                Screen.Welcome.route
            )) {
                TopAppBarWithMenu(navController = navController)
            }
        },
        bottomBar = {
            if (currentRoute !in listOf(
                Screen.Auth.route,
                Screen.Login.route,
                Screen.SignUp.route,
                Screen.Welcome.route
            )) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Auth.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onSignUpClick = { navController.navigate(Screen.SignUp.route) },
                    onDevSkipClick = { navController.navigate(Screen.Home.route) }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Screen.Welcome.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onSignUpSuccess = { navController.navigate(Screen.Welcome.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    name = name,
                    onNameChange = { name = it },
                    gender = gender,
                    onGenderChange = { gender = it },
                    height = height,
                    onHeightChange = { height = it },
                    weight = weight,
                    onWeightChange = { weight = it },
                    workoutsPerWeek = workoutsPerWeek,
                    onWorkoutsPerWeekChange = { workoutsPerWeek = it },
                    workoutDuration = workoutDuration,
                    onWorkoutDurationChange = { workoutDuration = it },
                    targetPhysique = targetPhysique,
                    onTargetPhysiqueChange = { targetPhysique = it },
                    dietaryRestriction = dietaryRestriction,
                    onDietaryRestrictionChange = { dietaryRestriction = it },
                    preferredCuisine = preferredCuisine,
                    onPreferredCuisineChange = { preferredCuisine = it },
                    onContinue = { navController.navigate(Screen.Home.route) }
                )
            }
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Reminders.route) { RemindersScreen() }
            composable(Screen.Gym.route) { GymScreen() }
            composable(Screen.Profile.route) { ScreenLayout("Profile") }
            composable(Screen.Settings.route) { ScreenLayout("Settings") }
            //composable(Screen.Messages.route) { MessagesScreen() }
            //composable(Screen.Camera.route) { CameraScreen() }
        }
    }
}

/* -------------------- Top App Bar with Menu -------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithMenu(navController: NavHostController) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                "SnapFit",
                color = NeonPink,
                style = MaterialTheme.typography.titleLarge
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBlue
        ),
        actions = {
            Box {
                IconButton(
                    onClick = { menuExpanded = !menuExpanded }
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Menu",
                        tint = NeonPink,
                        modifier = Modifier.size(28.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Profile", color = Color.White) },
                        onClick = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = Color.White) },
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    )
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