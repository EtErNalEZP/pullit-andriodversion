package com.example.pullit.ui.recipes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.pullit.auth.AuthManager
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.components.MiniToasterView
import com.example.pullit.data.model.Recipe
import com.example.pullit.ui.theme.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pullit.viewmodel.BackgroundGenerationState
import com.example.pullit.viewmodel.GenerationTask
import com.example.pullit.viewmodel.CalorieFilter
import com.example.pullit.viewmodel.CookTimeFilter
import com.example.pullit.viewmodel.ImportViewModel
import com.example.pullit.viewmodel.RecipeListViewModel
import com.example.pullit.viewmodel.SortOrder
import java.util.Calendar

// ──────────────────────────────────────────────────────────
// Greeting helper
// ──────────────────────────────────────────────────────────
private fun greetingText(
    displayName: String?,
    goodMorning: String,
    goodAfternoon: String,
    goodEvening: String
): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val timeGreeting = when {
        hour < 12 -> "$goodMorning \u2600\uFE0F"   // ☀️
        hour < 18 -> "$goodAfternoon \uD83D\uDC4B"   // 👋
        else -> "$goodEvening \uD83C\uDF19"           // 🌙
    }
    return if (!displayName.isNullOrBlank()) "$timeGreeting, $displayName" else timeGreeting
}

// ──────────────────────────────────────────────────────────
// Tab enum
// ──────────────────────────────────────────────────────────
private enum class RecipeTab {
    ALL_RECIPES,
    COOKBOOKS
}

// ──────────────────────────────────────────────────────────
// Main screen
// ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    viewModel: RecipeListViewModel,
    importViewModel: ImportViewModel,
    authManager: AuthManager,
    onRecipeTap: (String) -> Unit,
    onRouletteClick: () -> Unit,
    onCookbookTap: (String) -> Unit = {},
    onImportClick: () -> Unit
) {
    val S = LocalStrings.current

    val recipes by viewModel.filteredRecipes.collectAsState()
    val cookbooks by viewModel.cookbooks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedRecipeIds by viewModel.selectedRecipeIds.collectAsState()
    val displayName by authManager.displayName.collectAsState()
    val cookTimeFilter by viewModel.cookTimeFilter.collectAsState()
    val calorieFilter by viewModel.calorieFilter.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val showFilterPanel by viewModel.showFilterPanel.collectAsState()
    val hasActiveFilters by viewModel.hasActiveFilters.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()

    // Import state
    val isGenerating by importViewModel.isGenerating.collectAsState()
    val generatedRecipe by importViewModel.generatedRecipe.collectAsState()
    val importProgress by importViewModel.progress.collectAsState()
    val pendingTitle by importViewModel.pendingTitle.collectAsState()
    val pendingCoverUrl by importViewModel.pendingCoverUrl.collectAsState()

    var selectedTab by remember { mutableStateOf(RecipeTab.ALL_RECIPES) }
    var showCookbookDialog by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var recommendationQueue by remember { mutableStateOf(listOf<Recipe>()) }
    val currentRecommendation = recommendationQueue.firstOrNull()

    // Auto-queue cookbook recommendation when generation completes with labels
    val generationTasks by BackgroundGenerationState.tasks.collectAsState()
    val completedRecipes = remember(generationTasks) {
        generationTasks.filter { it.isCompleted }.mapNotNull { it.generatedRecipe }
    }
    var seenRecipeIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(completedRecipes) {
        for (recipe in completedRecipes) {
            if (recipe.id in seenRecipeIds) continue
            seenRecipeIds = seenRecipeIds + recipe.id
            val labels = recipe.labelsJson?.let {
                runCatching {
                    kotlinx.serialization.json.Json.decodeFromString<com.example.pullit.data.model.RecipeLabels>(it)
                }.getOrNull()
            }
            if (labels != null && !labels.isEmpty) {
                recommendationQueue = recommendationQueue + recipe
            }
        }
    }

    // Also handle legacy single-task path
    LaunchedEffect(generatedRecipe, isGenerating) {
        if (!isGenerating && generatedRecipe != null && generatedRecipe!!.id !in seenRecipeIds) {
            seenRecipeIds = seenRecipeIds + generatedRecipe!!.id
            val labels = generatedRecipe!!.labelsJson?.let {
                runCatching {
                    kotlinx.serialization.json.Json.decodeFromString<com.example.pullit.data.model.RecipeLabels>(it)
                }.getOrNull()
            }
            if (labels != null && !labels.isEmpty) {
                recommendationQueue = recommendationQueue + generatedRecipe!!
            }
        }
    }

    // Cookbook recommendation sheet - shows first in queue, on dismiss shows next
    if (currentRecommendation != null) {
        CookbookRecommendationSheet(
            recipe = currentRecommendation,
            viewModel = viewModel,
            onDismiss = {
                recommendationQueue = recommendationQueue.drop(1)
            }
        )
    }

    // Import bottom sheet
    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            com.example.pullit.ui.import_recipe.ImportSheet(
                importViewModel = importViewModel,
                onDismiss = { showImportSheet = false },
                onStartGenerating = { showImportSheet = false }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                if (!isMultiSelectMode) {
                    FloatingActionButton(
                        onClick = { showImportSheet = true },
                        containerColor = Primary,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Import recipe",
                            tint = Color.White
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // ── Greeting ──
                Text(
                    text = greetingText(displayName, S.goodMorning, S.goodAfternoon, S.goodEvening),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Toolbar Row ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Segmented buttons for tabs
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .weight(1f)
                            .padding(3.dp)
                    ) {
                        RecipeTab.entries.forEach { tab ->
                            val isActive = selectedTab == tab
                            val bgColor by animateColorAsState(
                                if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent,
                                label = "tab_bg"
                            )
                            Surface(
                                onClick = { selectedTab = tab },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = bgColor,
                                shadowElevation = if (isActive) 1.dp else 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when (tab) {
                                            RecipeTab.ALL_RECIPES -> S.allRecipes
                                            RecipeTab.COOKBOOKS -> S.cookbooks
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isActive) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Favorite filter heart icon button
                    IconButton(
                        onClick = { viewModel.toggleFavoritesOnly() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (showFavoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorites",
                            tint = if (showFavoritesOnly) Primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Roulette button
                    IconButton(
                        onClick = onRouletteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Shuffle,
                            contentDescription = "Random recipe",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Multi-select mode toggle
                    IconButton(
                        onClick = { viewModel.toggleMultiSelectMode() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isMultiSelectMode) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Multi-select",
                            tint = if (isMultiSelectMode) Primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Combined Sort & Filter toggle button
                    IconButton(
                        onClick = { viewModel.toggleFilterPanel() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (hasActiveFilters || sortOrder != SortOrder.NEWEST)
                                Icons.Filled.FilterList else Icons.Outlined.FilterList,
                            contentDescription = "Sort & Filter",
                            tint = if (hasActiveFilters || sortOrder != SortOrder.NEWEST)
                                Primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Search Bar ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(Primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    S.searchRecipes,
                                    color = TextTertiary,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // ── Sort & Filter Panel ──
                AnimatedVisibility(visible = showFilterPanel) {
                    FilterPanel(
                        sortOrder = sortOrder,
                        cookTimeFilter = cookTimeFilter,
                        calorieFilter = calorieFilter,
                        selectedTags = selectedTags,
                        availableTags = availableTags,
                        onSortOrderChange = { viewModel.setSortOrder(it) },
                        onCookTimeFilterChange = { viewModel.setCookTimeFilter(it) },
                        onCalorieFilterChange = { viewModel.setCalorieFilter(it) },
                        onTagToggle = { viewModel.toggleTag(it) }
                    )
                }

                // ── Active Filter Tags (when panel hidden but filters active) ──
                if (!showFilterPanel && hasActiveFilters) {
                    ActiveFilterTagsRow(
                        cookTimeFilter = cookTimeFilter,
                        calorieFilter = calorieFilter,
                        selectedTags = selectedTags,
                        onClearCookTime = { viewModel.setCookTimeFilter(CookTimeFilter.ANY) },
                        onClearCalorie = { viewModel.setCalorieFilter(CalorieFilter.ANY) },
                        onClearTag = { viewModel.toggleTag(it) },
                        onClearAll = { viewModel.clearFilters() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Content Area ──
                when (selectedTab) {
                    RecipeTab.ALL_RECIPES -> {
                        RecipeGridContent(
                            recipes = recipes,
                            isGenerating = isGenerating,
                            generatedRecipe = generatedRecipe,
                            importProgress = importProgress,
                            pendingTitle = pendingTitle,
                            pendingCoverUrl = pendingCoverUrl,
                            showFavoritesOnly = showFavoritesOnly,
                            searchQuery = searchQuery,
                            isMultiSelectMode = isMultiSelectMode,
                            selectedRecipeIds = selectedRecipeIds,
                            onRecipeTap = { recipeId ->
                                if (isMultiSelectMode) {
                                    viewModel.toggleRecipeSelection(recipeId)
                                } else {
                                    onRecipeTap(recipeId)
                                }
                            },
                            onFavoriteTap = { recipe -> viewModel.toggleFavorite(recipe) },
                            onGeneratedRecipeTap = { recipe ->
                                onRecipeTap(recipe.id)
                                importViewModel.reset()
                            }
                        )
                    }

                    RecipeTab.COOKBOOKS -> {
                        CookbookListContent(
                            cookbooks = cookbooks,
                            viewModel = viewModel,
                            onCookbookTap = onCookbookTap
                        )
                    }
                }
            }
        }

        // ── Selection Bar (when multi-select is active) ──
        AnimatedVisibility(
            visible = isMultiSelectMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Selection count badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(
                            containerColor = Primary,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "${selectedRecipeIds.size}",
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            S.selected,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Add to Cookbook button
                        OutlinedButton(
                            onClick = { showCookbookDialog = true },
                            enabled = selectedRecipeIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Outlined.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(S.addToCookbook, fontSize = 13.sp)
                        }

                        // Delete button
                        Button(
                            onClick = { viewModel.deleteSelected() },
                            enabled = selectedRecipeIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Error,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(S.delete, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // ── Cookbook selection dialog ──
    var showNewCookbookInDialog by remember { mutableStateOf(false) }
    var newCookbookNameInDialog by remember { mutableStateOf("") }

    if (showCookbookDialog) {
        AlertDialog(
            onDismissRequest = {
                showCookbookDialog = false
                showNewCookbookInDialog = false
                newCookbookNameInDialog = ""
            },
            title = { Text(S.addToCookbook) },
            text = {
                Column {
                    if (showNewCookbookInDialog) {
                        OutlinedTextField(
                            value = newCookbookNameInDialog,
                            onValueChange = { newCookbookNameInDialog = it },
                            placeholder = { Text(S.cookbookName) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        cookbooks.forEach { cookbook ->
                            TextButton(
                                onClick = {
                                    selectedRecipeIds.forEach { recipeId ->
                                        viewModel.addRecipeToCookbook(recipeId, cookbook.id)
                                    }
                                    showCookbookDialog = false
                                    viewModel.toggleMultiSelectMode()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(cookbook.title, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        TextButton(
                            onClick = { showNewCookbookInDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "+ ${S.newCookbook}",
                                modifier = Modifier.fillMaxWidth(),
                                color = Primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (showNewCookbookInDialog) {
                    TextButton(
                        onClick = {
                            if (newCookbookNameInDialog.isNotBlank()) {
                                viewModel.createCookbook(newCookbookNameInDialog.trim())
                                newCookbookNameInDialog = ""
                                showNewCookbookInDialog = false
                            }
                        }
                    ) {
                        Text(S.create)
                    }
                } else {
                    TextButton(onClick = { showCookbookDialog = false }) {
                        Text(S.cancel)
                    }
                }
            },
            dismissButton = {
                if (showNewCookbookInDialog) {
                    TextButton(onClick = {
                        showNewCookbookInDialog = false
                        newCookbookNameInDialog = ""
                    }) {
                        Text(S.back)
                    }
                }
            }
        )
    }
}

// ──────────────────────────────────────────────────────────
// Recipe grid content
// ──────────────────────────────────────────────────────────
@Composable
private fun RecipeGridContent(
    recipes: List<Recipe>,
    isGenerating: Boolean,
    generatedRecipe: Recipe?,
    importProgress: com.example.pullit.data.model.RecipeGenerationProgress?,
    pendingTitle: String?,
    pendingCoverUrl: String?,
    showFavoritesOnly: Boolean,
    searchQuery: String,
    isMultiSelectMode: Boolean,
    selectedRecipeIds: Set<String>,
    onRecipeTap: (String) -> Unit,
    onFavoriteTap: (Recipe) -> Unit,
    onGeneratedRecipeTap: (Recipe) -> Unit
) {
    val S = LocalStrings.current

    // Multi-task generation from BackgroundGenerationState
    val generationTasks by BackgroundGenerationState.tasks.collectAsState()

    // Determine if we should show the generating card (legacy single + multi-task)
    val showGeneratingCard = isGenerating || generatedRecipe != null || generationTasks.isNotEmpty()

    // Filter out generated recipes from normal list to avoid duplicates
    val generatedRecipeIds = generationTasks.mapNotNull { task -> task.generatedRecipe?.id }.toSet() +
        listOfNotNull(generatedRecipe?.id)
    val displayRecipes = if (generatedRecipeIds.isNotEmpty()) {
        recipes.filter { recipe -> recipe.id !in generatedRecipeIds }
    } else recipes

    if (!showGeneratingCard && displayRecipes.isEmpty()) {
        // Empty states
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val (emoji, title, subtitle) = when {
                    showFavoritesOnly -> Triple(
                        "\u2764\uFE0F",
                        S.noSavedRecipes,
                        S.noSavedRecipesHint
                    )
                    searchQuery.isNotBlank() -> Triple(
                        "\uD83D\uDD0D",
                        S.noRecipesFound,
                        S.noRecipesFoundHint
                    )
                    else -> Triple(
                        "\uD83C\uDF73",
                        S.noRecipes,
                        S.noRecipesHint
                    )
                }
                Text(emoji, fontSize = 60.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Show generating cards for all active/completed tasks
        if (generationTasks.isNotEmpty()) {
            items(
                items = generationTasks,
                key = { task -> "generating_${task.id}" }
            ) { task ->
                GeneratingRecipeCard(
                    isGenerating = !task.isCompleted && !task.isError,
                    generatedRecipe = task.generatedRecipe,
                    progress = task.progress,
                    pendingTitle = task.pendingTitle,
                    pendingCoverUrl = task.pendingCoverUrl,
                    onTap = {
                        if (task.isCompleted) {
                            task.generatedRecipe?.let { recipe -> onGeneratedRecipeTap(recipe) }
                            BackgroundGenerationState.remove(task.id)
                        } else if (task.isError) {
                            BackgroundGenerationState.remove(task.id)
                        }
                    }
                )
            }
        } else if (showGeneratingCard) {
            // Legacy single-task fallback
            item(key = "generating_card") {
                GeneratingRecipeCard(
                    isGenerating = isGenerating,
                    generatedRecipe = generatedRecipe,
                    progress = importProgress,
                    pendingTitle = pendingTitle,
                    pendingCoverUrl = pendingCoverUrl,
                    onTap = {
                        generatedRecipe?.let { onGeneratedRecipeTap(it) }
                    }
                )
            }
        }

        items(displayRecipes, key = { it.id }) { recipe ->
            RecipeCardView(
                recipe = recipe,
                onTap = { onRecipeTap(recipe.id) },
                onFavoriteTap = if (!isMultiSelectMode) {
                    { onFavoriteTap(recipe) }
                } else null,
                isSelected = isMultiSelectMode && recipe.id in selectedRecipeIds
            )
        }
    }
}

// ──────────────────────────────────────────────────────────
// Generating recipe card (shown in grid while importing)
// ──────────────────────────────────────────────────────────
@Composable
private fun GeneratingRecipeCard(
    isGenerating: Boolean,
    generatedRecipe: Recipe?,
    progress: com.example.pullit.data.model.RecipeGenerationProgress?,
    pendingTitle: String?,
    pendingCoverUrl: String?,
    onTap: () -> Unit
) {
    val S = LocalStrings.current
    val cardShape = RoundedCornerShape(16.dp)
    val isError = progress?.status == com.example.pullit.data.model.RecipeGenerationProgress.Status.ERROR
    val isCompleted = generatedRecipe != null && !isGenerating && !isError

    // Auto-dismiss error card after 4 seconds
    LaunchedEffect(isError) {
        if (isError) {
            kotlinx.coroutines.delay(4000)
            onTap()
        }
    }

    // Map raw backend error message to a user-friendly localized string
    val errorMessage = if (isError) {
        val raw = progress?.message?.lowercase() ?: ""
        when {
            raw.contains("not a recipe") || raw.contains("not food") ||
            raw.contains("no recipe") || raw.contains("unable to extract") ||
            raw.contains("content is not") -> S.importErrors.importErrorNotRecipe
            raw.contains("network") || raw.contains("connect") ||
            raw.contains("dns") || raw.contains("unreachable") -> S.importErrors.importErrorNetwork
            raw.contains("timeout") || raw.contains("timed out") -> S.importErrors.importErrorTimeout
            else -> S.importErrors.importErrorUnknown
        }
    } else null

    // Display title: completed recipe > pending > fallback
    val displayTitle = when {
        isCompleted -> generatedRecipe!!.title
        isError -> S.importErrors.importFailed
        !pendingTitle.isNullOrBlank() -> pendingTitle
        else -> S.generatingRecipe
    }

    // Display cover URL: completed recipe image > pending cover
    val displayCoverUrl = when {
        isCompleted && !generatedRecipe!!.imageUrl.isNullOrBlank() -> generatedRecipe.imageUrl
        !isError && !pendingCoverUrl.isNullOrBlank() -> pendingCoverUrl
        else -> null
    }

    // Animated progress value
    var progressValue by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = tween(400),
        label = "progress_anim"
    )

    // Simulated progress that monotonically increases
    LaunchedEffect(isCompleted, isError) {
        if (isCompleted) { progressValue = 1f; return@LaunchedEffect }
        if (isError) { progressValue = 0f; return@LaunchedEffect }
        while (true) {
            kotlinx.coroutines.delay(800)
            val remaining = 0.95f - progressValue
            if (remaining <= 0.01f) break
            val increment = remaining * (0.03f + Math.random().toFloat() * 0.05f)
            progressValue = (progressValue + increment).coerceAtMost(0.95f)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isCompleted || isError, onClick = onTap),
        shape = cardShape,
        color = if (isError) Error.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isError) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Error.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚠️", fontSize = 40.sp)
                    }
                } else if (displayCoverUrl != null) {
                    AsyncImage(
                        model = displayCoverUrl,
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Crop,
                        colorFilter = if (isCompleted) null else ColorFilter.colorMatrix(
                            androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0.3f) }
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                    if (!isCompleted) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))
                    }
                } else if (!isCompleted) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            brush = Brush.linearGradient(
                                colors = listOf(Primary.copy(alpha = 0.06f), MaterialTheme.colorScheme.surfaceVariant)
                            )
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        MiniToasterView(modifier = Modifier.size(100.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            brush = Brush.linearGradient(
                                colors = listOf(Primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.surfaceVariant)
                            )
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Primary.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                    }
                }

                if (isCompleted) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .clip(RoundedCornerShape(50)).background(Primary)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(S.newBadge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isCompleted && !isError && isGenerating) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp).height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            // Title + status section
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text(
                    text = displayTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = when {
                        isError -> Error
                        isCompleted -> MaterialTheme.colorScheme.onSurface
                        else -> TextTertiary
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isCompleted -> S.tapToViewRecipe
                        isError -> errorMessage ?: S.importErrors.importErrorUnknown
                        else -> progress?.message ?: S.processing
                    },
                    fontSize = 11.sp,
                    color = when {
                        isCompleted -> Success
                        isError -> Error.copy(alpha = 0.75f)
                        else -> TextTertiary
                    },
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Cookbook list content (matches iOS RecipeListView cookbooks)
// ──────────────────────────────────────────────────────────
@Composable
private fun CookbookListContent(
    cookbooks: List<com.example.pullit.data.model.Cookbook>,
    viewModel: RecipeListViewModel,
    onCookbookTap: (String) -> Unit
) {
    val S = LocalStrings.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCookbookTitle by remember { mutableStateOf("") }

    if (cookbooks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    S.noCookbooks,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    S.noCookbooksHint,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(S.createCookbook, color = Color.White)
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(cookbooks, key = { it.id }) { cookbook ->
                val recipes = viewModel.recipesForCookbook(cookbook)
                val favoriteCount = recipes.count { it.favorited }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCookbookTap(cookbook.id) }
                        .padding(vertical = 12.dp)
                ) {
                    // Title row with counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cookbook.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = TextTertiary
                                )
                                Text(
                                    "${recipes.size}",
                                    fontSize = 12.sp,
                                    color = TextTertiary
                                )
                            }
                            if (favoriteCount > 0) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = TextTertiary
                                    )
                                    Text(
                                        "$favoriteCount",
                                        fontSize = 12.sp,
                                        color = TextTertiary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Recipe preview thumbnails - always 5 columns
                    if (recipes.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val previewCount = 5
                            for (index in 0 until previewCount) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    if (index < recipes.size) {
                                        val recipe = recipes[index]
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
                                                    modifier = Modifier.size(16.dp),
                                                    tint = TextTertiary
                                                )
                                            }
                                        }
                                    } else {
                                        // Empty placeholder with dashed border
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Transparent,
                                            border = BorderStroke(
                                                1.dp,
                                                TextTertiary.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = TextTertiary.copy(alpha = 0.4f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Divider
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }

            // Add cookbook button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateDialog = true }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Primary
                    )
                    Text(
                        S.createCookbook,
                        fontSize = 15.sp,
                        color = Primary
                    )
                }
            }
        }
    }

    // Create cookbook dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newCookbookTitle = "" },
            title = { Text(S.newCookbook) },
            text = {
                OutlinedTextField(
                    value = newCookbookTitle,
                    onValueChange = { newCookbookTitle = it },
                    placeholder = { Text(S.cookbookName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCookbookTitle.isNotBlank()) {
                            viewModel.createCookbook(newCookbookTitle.trim())
                            newCookbookTitle = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text(S.create, color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newCookbookTitle = "" }) {
                    Text(S.cancel)
                }
            }
        )
    }
}

// ──────────────────────────────────────────────────────────
// Filter Panel
// ──────────────────────────────────────────────────────────

@Composable
private fun FilterPanel(
    sortOrder: SortOrder,
    cookTimeFilter: CookTimeFilter,
    calorieFilter: CalorieFilter,
    selectedTags: Set<String>,
    availableTags: List<String>,
    onSortOrderChange: (SortOrder) -> Unit,
    onCookTimeFilterChange: (CookTimeFilter) -> Unit,
    onCalorieFilterChange: (CalorieFilter) -> Unit,
    onTagToggle: (String) -> Unit
) {
    val S = LocalStrings.current
    var tagsExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sort row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    S.filterSort,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChipView(
                            label = S.newestFirst,
                            isSelected = sortOrder == SortOrder.NEWEST,
                            onClick = { onSortOrderChange(SortOrder.NEWEST) }
                        )
                    }
                    item {
                        FilterChipView(
                            label = S.oldestFirst,
                            isSelected = sortOrder == SortOrder.OLDEST,
                            onClick = { onSortOrderChange(SortOrder.OLDEST) }
                        )
                    }
                    item {
                        FilterChipView(
                            label = S.byName,
                            isSelected = sortOrder == SortOrder.NAME,
                            onClick = { onSortOrderChange(SortOrder.NAME) }
                        )
                    }
                }
            }

            // Cook time row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    S.filterCookTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CookTimeFilter.entries.toList()) { filter ->
                        FilterChipView(
                            label = filter.label(S),
                            isSelected = cookTimeFilter == filter,
                            onClick = { onCookTimeFilterChange(filter) }
                        )
                    }
                }
            }

            // Calorie row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    S.filterCalories,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CalorieFilter.entries.toList()) { filter ->
                        FilterChipView(
                            label = filter.label(S),
                            isSelected = calorieFilter == filter,
                            onClick = { onCalorieFilterChange(filter) }
                        )
                    }
                }
            }

            // Tags section (collapsible)
            if (availableTags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            S.filterTags,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { tagsExpanded = !tagsExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (tagsExpanded) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .then(
                                if (!tagsExpanded) Modifier.heightIn(max = 36.dp)
                                else Modifier
                            )
                            .clipToBounds()
                    ) {
                        availableTags.forEach { tag ->
                            FilterChipView(
                                label = tag,
                                isSelected = tag in selectedTags,
                                onClick = { onTagToggle(tag) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipView(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (isSelected) Primary else MaterialTheme.colorScheme.surface,
        border = if (!isSelected) BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ) else null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ActiveFilterTagsRow(
    cookTimeFilter: CookTimeFilter,
    calorieFilter: CalorieFilter,
    selectedTags: Set<String>,
    onClearCookTime: () -> Unit,
    onClearCalorie: () -> Unit,
    onClearTag: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val S = LocalStrings.current
    val activeCount = (if (cookTimeFilter != CookTimeFilter.ANY) 1 else 0) +
        (if (calorieFilter != CalorieFilter.ANY) 1 else 0) + selectedTags.size

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        if (activeCount >= 2) {
            item {
                Surface(
                    onClick = onClearAll,
                    shape = RoundedCornerShape(50),
                    color = PrimaryDark.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text(S.clearAllFilters, color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (cookTimeFilter != CookTimeFilter.ANY) {
            item {
                ActiveFilterTag(
                    icon = Icons.Outlined.Timer,
                    label = cookTimeFilter.label(S),
                    onRemove = onClearCookTime
                )
            }
        }
        if (calorieFilter != CalorieFilter.ANY) {
            item {
                ActiveFilterTag(
                    icon = Icons.Outlined.LocalFireDepartment,
                    label = calorieFilter.label(S),
                    onRemove = onClearCalorie
                )
            }
        }
        items(selectedTags.sorted().toList()) { tag ->
            ActiveFilterTag(
                icon = Icons.Outlined.Label,
                label = tag,
                onRemove = { onClearTag(tag) }
            )
        }
    }
}

@Composable
private fun ActiveFilterTag(icon: ImageVector, label: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(12.dp))
            Text(label, color = PrimaryDark, style = MaterialTheme.typography.labelSmall)
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = PrimaryDark,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onRemove() }
            )
        }
    }
}
