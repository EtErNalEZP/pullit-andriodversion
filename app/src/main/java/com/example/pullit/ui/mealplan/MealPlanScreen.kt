package com.example.pullit.ui.mealplan

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.pullit.data.model.MealPlanItem
import com.example.pullit.data.model.Recipe
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.MealPlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    navController: NavController,
    viewModel: MealPlanViewModel = viewModel()
) {
    val mealPlanItems by viewModel.mealPlanItems.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Add recipe dialog — show all recipes, checkmark for ones already in plan
    if (showAddDialog) {
        val existingRecipeIds = mealPlanItems.map { it.recipeId }.toSet()

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Meal Plan", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(recipes) { recipe ->
                        val isInPlan = recipe.id in existingRecipeIds
                        Surface(
                            onClick = {
                                if (isInPlan) {
                                    val item = mealPlanItems.find { it.recipeId == recipe.id }
                                    if (item != null) viewModel.removeFromMealPlan(item)
                                } else {
                                    viewModel.addToMealPlan(recipe.id)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!recipe.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = recipe.imageUrl,
                                        contentDescription = recipe.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    recipe.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isInPlan) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "In plan",
                                        tint = Primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Done", color = Primary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Plan", fontWeight = FontWeight.ExtraBold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Segmented tab: "Weekly Plan" / "Grocery List"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                val tabs = listOf("Weekly Plan", "Grocery List")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val bgColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        label = "tab_bg"
                    )
                    Surface(
                        onClick = { selectedTab = index },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = bgColor,
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                title,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // When "Grocery List" tab is tapped, navigate to the grocery list screen
            LaunchedEffect(selectedTab) {
                if (selectedTab == 1) {
                    navController.navigate(Screen.GroceryList.route) {
                        launchSingleTop = true
                    }
                    selectedTab = 0 // Reset back so when user returns, Weekly Plan is shown
                }
            }

            WeeklyPlanTab(
                mealPlanItems = mealPlanItems,
                recipes = recipes,
                onAddRecipes = { showAddDialog = true },
                onGenerateGroceryList = {
                    viewModel.generateGroceryList()
                    navController.navigate(Screen.GroceryList.route) {
                        launchSingleTop = true
                    }
                },
                onClearAll = { viewModel.clearMealPlan() },
                onRemoveItem = { viewModel.removeFromMealPlan(it) },
                onUpdateServings = { item, servings -> viewModel.updateServings(item, servings) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyPlanTab(
    mealPlanItems: List<MealPlanItem>,
    recipes: List<Recipe>,
    onAddRecipes: () -> Unit,
    onGenerateGroceryList: () -> Unit,
    onClearAll: () -> Unit,
    onRemoveItem: (MealPlanItem) -> Unit,
    onUpdateServings: (MealPlanItem, Int) -> Unit
) {
    if (mealPlanItems.isEmpty()) {
        // Empty state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = TextTertiary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Start Planning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Add recipes to plan your meals for the week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAddRecipes,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Recipes", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Meal plan items
            items(mealPlanItems, key = { it.id }) { item ->
                val recipe = recipes.find { it.id == item.recipeId }
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            onRemoveItem(item)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Error),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.padding(end = 20.dp)
                            )
                        }
                    }
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Recipe thumbnail (60dp square, 8dp radius)
                            if (recipe != null && !recipe.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = recipe.imageUrl,
                                    contentDescription = recipe.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Restaurant,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Title and servings
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    recipe?.title ?: "Unknown Recipe",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Servings adjuster
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "${item.servings} serving${if (item.servings > 1) "s" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )

                                    Surface(
                                        onClick = { onUpdateServings(item, item.servings - 1) },
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(28.dp),
                                        enabled = item.servings > 1
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Remove,
                                                contentDescription = "Decrease",
                                                modifier = Modifier.size(16.dp),
                                                tint = if (item.servings > 1) MaterialTheme.colorScheme.onSurface
                                                else TextTertiary
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = { onUpdateServings(item, item.servings + 1) },
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Increase",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAddRecipes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Recipes", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = onGenerateGroceryList,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Success),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Grocery List", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear All", color = Error)
                    }
                }
            }
        }
    }
}
