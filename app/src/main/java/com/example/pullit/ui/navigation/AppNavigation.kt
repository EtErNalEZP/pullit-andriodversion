package com.example.pullit.ui.navigation

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.example.pullit.viewmodel.ImportViewModel
import com.example.pullit.viewmodel.RecipeListViewModel

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
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.RecipeList, "Recipes", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    BottomNavItem(Screen.MealPlan, "Plan", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.AiRecommend, "AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    BottomNavItem(Screen.More, "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(authManager: AuthManager) {
    val navController = rememberNavController()
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
        }
    }
}
