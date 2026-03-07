package com.example.pullit.ui.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.Primary
import com.example.pullit.viewmodel.RecipeListViewModel
import com.example.pullit.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    navController: NavController,
    viewModel: RecipeListViewModel = viewModel()
) {
    val recipes by viewModel.filteredRecipes.collectAsState()
    val cookbooks by viewModel.cookbooks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val selectedCookbookId by viewModel.selectedCookbookId.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipes", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Roulette.route) }) {
                        Icon(Icons.Outlined.Casino, contentDescription = "Roulette")
                    }
                    IconButton(onClick = { viewModel.toggleFavoritesOnly() }) {
                        Icon(
                            if (showFavoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorites",
                            tint = if (showFavoritesOnly) Primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Newest first") }, onClick = { viewModel.setSortOrder(SortOrder.NEWEST); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Oldest first") }, onClick = { viewModel.setSortOrder(SortOrder.OLDEST); showSortMenu = false })
                            DropdownMenuItem(text = { Text("By name") }, onClick = { viewModel.setSortOrder(SortOrder.NAME); showSortMenu = false })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Import.route) },
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import recipe", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search recipes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            // Cookbook filter chips
            if (cookbooks.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = if (selectedCookbookId == null) 0 else cookbooks.indexOfFirst { it.id == selectedCookbookId } + 1,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 16.dp
                ) {
                    Tab(selected = selectedCookbookId == null, onClick = { viewModel.setSelectedCookbook(null) }) {
                        Text("All", modifier = Modifier.padding(vertical = 12.dp))
                    }
                    cookbooks.forEach { cookbook ->
                        Tab(selected = selectedCookbookId == cookbook.id, onClick = { viewModel.setSelectedCookbook(cookbook.id) }) {
                            Text(cookbook.title, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }

            if (recipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\uD83C\uDF73", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No recipes yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap + to import your first recipe", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { navController.navigate(Screen.RecipeDetail.createRoute(recipe.id)) },
                            onFavoriteClick = { viewModel.toggleFavorite(recipe) }
                        )
                    }
                }
            }
        }
    }
}
