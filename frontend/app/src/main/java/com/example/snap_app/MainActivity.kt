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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

val DarkBlue = Color(0xFF111F35)
val NeonPink = Color(0xFFF63049)
val Purple = Color(0xFFD02752)
val Burgundy = Color(0xFF8A244B)

/**
 * Main activity and application entry point.
 * Configures edge-to-edge display and initializes the compose UI.
 */
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

/**
 * Navigation destination definitions with associated UI metadata.
 * Each screen includes route string, label, and Material icon.
 */
sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Auth : Screen("auth", "Auth", Icons.Default.Info)
    object Login : Screen("login", "Login", Icons.Default.Info)
    object SignUp : Screen("signup", "SignUp", Icons.Default.Info)
    object Welcome : Screen("welcome", "Welcome", Icons.Default.Info)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Nutrition : Screen("nutrition", "Nutrition", Icons.Default.Fastfood)
    object Reminders : Screen("reminders", "Reminders", Icons.Default.Alarm)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Gym : Screen("gym", "Gym", Icons.Default.SportsGymnastics)
    object Chat : Screen("chat", "Chat", Icons.Default.Mms)
    object DonutShops : Screen("donut_shops", "Donut Shops", Icons.Default.Fastfood)
}

/**
 * Screens displayed in bottom navigation bar.
 */
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Chat,
    Screen.Nutrition,
    Screen.Gym,
    Screen.Reminders
)

/**
 * Root composable managing navigation, app bar, and bottom nav.
 * Maintains navigation state and coordinates screen transitions.
 * Preserves onboarding form state across configuration changes.
 */
@Composable
fun MainScreen() {
    val viewModel: AppViewModel = viewModel()

    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    var name by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }

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
                TopAppBarWithMenu(navController = navController, currentRoute = currentRoute)
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
                    onLoginSuccess = { userId, email, username ->
                        viewModel.setUser(userId, email, username)
                        viewModel.fetchPlan()
                        navController.navigate(Screen.Home.route)
                    },
                    onBack = { navController.popBackStack() },
                    onSignUpClick = { navController.navigate(Screen.SignUp.route) }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onSignUpSuccess = { userId, email, username ->
                        viewModel.setUser(userId, email, username)
                        navController.navigate(Screen.Welcome.route)
                    },
                    onBack = { navController.popBackStack() },
                    onLoginClick = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    viewModel = viewModel,
                    name = name,
                    onNameChange = { name = it },
                    gender = gender,
                    onGenderChange = { gender = it },
                    height = height,
                    onHeightChange = { height = it },
                    weight = weight,
                    onWeightChange = { weight = it },
                    age = age,
                    onAgeChange = { age = it },
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
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToNutrition = {
                        navController.navigate("nutrition") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToGym = {
                        navController.navigate("gym") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToChat = {
                        navController.navigate("chat") {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Reminders.route) { RemindersScreen(viewModel = viewModel)}
            composable(Screen.Gym.route) { GymScreen(viewModel = viewModel) }
            composable(Screen.Nutrition.route) { NutritionScreen(viewModel = viewModel) }
            composable(Screen.DonutShops.route) {
                DonutShopsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Profile.route) { ScreenLayout("Profile") }
            composable(Screen.Settings.route) {
                val displayName = viewModel.username.collectAsState().value.ifBlank { name.ifBlank { "User" } }
                SettingsScreen(
                    userName = displayName,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        viewModel.logout {
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable(Screen.Chat.route) { ChatScreen() }
        }
    }
}

/**
 * Top navigation bar with app branding and quick actions.
 * Displays logo, app name, and icons for donut shops and settings.
 *
 * @param navController Navigation controller for handling menu actions
 * @param currentRoute Current active route for conditional navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithMenu(navController: NavHostController, currentRoute: String?) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "FuelForm",
                    modifier = Modifier.height(32.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    "FuelForm",
                    color = NeonPink,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBlue
        ),
        actions = {
            IconButton(
                onClick = {
                    if (currentRoute != Screen.DonutShops.route) {
                        navController.navigate(Screen.DonutShops.route)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DonutSmall,
                    contentDescription = "Donut Shops",
                    tint = NeonPink,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(
                onClick = {
                    if (currentRoute == Screen.Settings.route) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Settings.route)
                    }
                }
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = NeonPink,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    )
}

/**
 * Bottom navigation bar for primary app sections.
 * Implements tab-based navigation with state preservation.
 * Home tab clears back stack to ensure clean navigation reset.
 */
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
                    if (screen.route == Screen.Home.route) {
                        // Clear back stack on home tap to provide fresh dashboard view
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    Snap_appTheme {
        MainScreen()
    }
}