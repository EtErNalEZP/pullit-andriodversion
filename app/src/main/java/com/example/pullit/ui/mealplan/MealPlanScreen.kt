package com.example.pullit.ui.mealplan

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.MealPlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    navController: NavController,
    viewModel: MealPlanViewModel = viewModel()
) {
    val S = LocalStrings.current
    val mealPlanItems by viewModel.mealPlanItems.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Add recipe bottom sheet — matches cookbook recipe picker style
    if (showAddDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            MealPlanRecipePicker(
                recipes = recipes,
                mealPlanItems = mealPlanItems,
                viewModel = viewModel,
                onDismiss = { showAddDialog = false }
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(S.mealPlan, fontWeight = FontWeight.ExtraBold) }
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
                val tabs = listOf(S.weeklyPlan, S.groceryList)
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
    val S = LocalStrings.current
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
                    S.startPlanning,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    S.startPlanningHint,
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
                    Text(S.addRecipes, fontWeight = FontWeight.Bold, color = Color.White)
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
                                        "${item.servings} ${if (item.servings > 1) S.servingsUnit else S.serving}",
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
                        Text(S.addRecipes, fontWeight = FontWeight.Bold, color = Color.White)
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
                        Text(S.generateGroceryList, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(S.clearAll, color = Error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealPlanRecipePicker(
    recipes: List<Recipe>,
    mealPlanItems: List<MealPlanItem>,
    viewModel: MealPlanViewModel,
    onDismiss: () -> Unit
) {
    val S = LocalStrings.current
    var searchText by remember { mutableStateOf("") }
    val existingRecipeIds = mealPlanItems.map { it.recipeId }.toSet()

    val filteredRecipes = remember(recipes, searchText) {
        if (searchText.isBlank()) recipes
        else recipes.filter { it.title.contains(searchText, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                S.addRecipes,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onDismiss) {
                Text(S.done, color = Primary)
            }
        }

        // Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text(S.searchRecipes, fontSize = 14.sp, color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    cursorColor = Primary
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Recipe list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            items(filteredRecipes, key = { it.id }) { recipe ->
                val isInPlan = recipe.id in existingRecipeIds

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isInPlan) {
                                val item = mealPlanItems.find { it.recipeId == recipe.id }
                                if (item != null) viewModel.removeFromMealPlan(item)
                            } else {
                                viewModel.addToMealPlan(recipe.id)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        if (!recipe.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = recipe.imageUrl,
                                contentDescription = recipe.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = TextTertiary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        recipe.title,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = if (isInPlan) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (isInPlan) Primary else TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}
