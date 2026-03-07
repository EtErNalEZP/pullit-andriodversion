package com.example.pullit.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pullit.BuildConfig
import com.example.pullit.service.ApiClient
import com.example.pullit.ui.ai.AiRecommendScreen
import com.example.pullit.ui.auth.AuthScreen
import com.example.pullit.ui.auth.DisplayNameSetupScreen
import com.example.pullit.ui.cooking.CookingModeScreen
import com.example.pullit.ui.mealplan.GroceryListScreen
import com.example.pullit.ui.mealplan.MealPlanScreen
import com.example.pullit.ui.more.MoreScreen
import com.example.pullit.ui.recipes.RecipeDetailScreen
import com.example.pullit.ui.recipes.RecipeEditScreen
import com.example.pullit.ui.recipes.RecipeListScreen
import com.example.pullit.ui.recipes.RouletteScreen
import com.example.pullit.ui.ai.SuggestionDetailScreen
import com.example.pullit.auth.AuthManager
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.theme.Primary
import com.example.pullit.viewmodel.ImportViewModel
import com.example.pullit.viewmodel.RecipeListViewModel
import kotlinx.serialization.Serializable

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object DisplayNameSetup : Screen("display_name_setup")
    data object RecipeList : Screen("recipe_list")
    data object RecipeDetail : Screen("recipe_detail/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe_detail/$recipeId"
    }
    data object RecipeEdit : Screen("recipe_edit/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe_edit/$recipeId"
    }
    data object Import : Screen("import")
    data object Roulette : Screen("roulette")
    data object CookingMode : Screen("cooking/{recipeId}") {
        fun createRoute(recipeId: String) = "cooking/$recipeId"
    }
    data object MealPlan : Screen("meal_plan")
    data object GroceryList : Screen("grocery_list")
    data object AiRecommend : Screen("ai_recommend")
    data object SuggestionDetail : Screen("suggestion_detail/{index}") {
        fun createRoute(index: Int) = "suggestion_detail/$index"
    }
    data object More : Screen("more")
    data object CookbookDetail : Screen("cookbook_detail/{cookbookId}") {
        fun createRoute(cookbookId: String) = "cookbook_detail/$cookbookId"
    }
    data object PrivacyInfo : Screen("privacy_info")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Serializable
private data class VersionResponse(val android: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(authManager: AuthManager) {
    val S = LocalStrings.current
    val context = LocalContext.current
    val navController = rememberNavController()

    // ── Version check ──
    var showUpdateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.get<VersionResponse>("/api/version")
            if (response.android != BuildConfig.VERSION_NAME) {
                showUpdateDialog = true
            }
        } catch (_: Exception) {
            // Silently ignore — don't block app if check fails
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text(S.updateAvailable, fontWeight = FontWeight.Bold) },
            text = { Text(S.updateMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://pullit-landing.up.railway.app"))
                    )
                }) {
                    Text(S.updateNow, color = Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text(S.updateLater)
                }
            }
        )
    }

    val bottomNavItems = listOf(
        BottomNavItem(Screen.RecipeList, S.recipes, Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
        BottomNavItem(Screen.MealPlan, S.mealPlan, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        BottomNavItem(Screen.AiRecommend, "AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
        BottomNavItem(Screen.More, S.more, Icons.Filled.Settings, Icons.Outlined.Settings)
    )
    val isAuthenticated by authManager.isAuthenticated.collectAsState()
    val isLoading by authManager.isLoading.collectAsState()
    val needsDisplayName by authManager.needsDisplayName.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.RecipeList.route, Screen.MealPlan.route,
        Screen.AiRecommend.route, Screen.More.route
    )

    if (isLoading) {
        // Show loading
        return
    }

    val startDestination = when {
        !isAuthenticated -> Screen.Auth.route
        needsDisplayName -> Screen.DisplayNameSetup.route
        else -> Screen.RecipeList.route
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.RecipeList.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(150)) }
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(authManager = authManager, onAuthSuccess = {
                    navController.navigate(Screen.RecipeList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }, onNeedsDisplayName = {
                    navController.navigate(Screen.DisplayNameSetup.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.DisplayNameSetup.route) {
                DisplayNameSetupScreen(authManager = authManager, onComplete = {
                    navController.navigate(Screen.RecipeList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
            composable(Screen.RecipeList.route) {
                val recipeListViewModel: RecipeListViewModel = viewModel()
                val importViewModel: ImportViewModel = viewModel()
                RecipeListScreen(
                    viewModel = recipeListViewModel,
                    importViewModel = importViewModel,
                    authManager = authManager,
                    onRecipeTap = { recipeId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    },
                    onRouletteClick = {
                        navController.navigate(Screen.Roulette.route)
                    },
                    onCookbookTap = { cookbookId ->
                        navController.navigate(Screen.CookbookDetail.createRoute(cookbookId))
                    },
                    onImportClick = { /* handled by internal bottom sheet */ }
                )
            }
            composable(Screen.RecipeDetail.route) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                RecipeDetailScreen(recipeId = recipeId, navController = navController)
            }
            composable(Screen.RecipeEdit.route) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                RecipeEditScreen(recipeId = recipeId, navController = navController)
            }
            // ImportSheet is now shown as modal from RecipeListScreen
            composable(Screen.Roulette.route) {
                RouletteScreen(navController = navController)
            }
            composable(Screen.CookingMode.route) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                CookingModeScreen(recipeId = recipeId, navController = navController)
            }
            composable(Screen.MealPlan.route) {
                MealPlanScreen(navController = navController)
            }
            composable(Screen.GroceryList.route) {
                GroceryListScreen(navController = navController)
            }
            composable(Screen.AiRecommend.route) {
                AiRecommendScreen(navController = navController)
            }
            composable(Screen.SuggestionDetail.route) { backStackEntry ->
                val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                SuggestionDetailScreen(index = index, navController = navController)
            }
            composable(Screen.More.route) {
                MoreScreen(authManager = authManager, navController = navController)
            }
            composable(Screen.CookbookDetail.route) { backStackEntry ->
                val cookbookId = backStackEntry.arguments?.getString("cookbookId") ?: return@composable
                com.example.pullit.ui.recipes.CookbookDetailScreen(
                    cookbookId = cookbookId,
                    navController = navController
                )
            }
            composable(Screen.PrivacyInfo.route) {
                com.example.pullit.ui.more.PrivacyInfoScreen(navController = navController)
            }
        }
    }
}
