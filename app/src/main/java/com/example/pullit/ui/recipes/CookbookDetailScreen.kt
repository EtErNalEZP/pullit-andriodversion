package com.example.pullit.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.pullit.data.model.Recipe
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.RecipeListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookDetailScreen(
    cookbookId: String,
    navController: NavController,
    viewModel: RecipeListViewModel = viewModel()
) {
    val S = LocalStrings.current
    val cookbooks by viewModel.cookbooks.collectAsState()
    val allRecipes by viewModel.recipes.collectAsState()
    val cookbook = cookbooks.find { it.id == cookbookId }
    var showManageSheet by remember { mutableStateOf(false) }

    val recipes = remember(cookbook, allRecipes) {
        if (cookbook == null) emptyList()
        else viewModel.recipesForCookbook(cookbook)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cookbook?.title ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, S.back)
                    }
                },
                actions = {
                    IconButton(onClick = { showManageSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = S.manageRecipes)
                    }
                }
            )
        }
    ) { padding ->
        if (recipes.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
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
                        S.emptyCookbook,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        S.addRecipesFromCollection,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showManageSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(S.manageRecipes, color = Color.White)
                    }
                }
            }
        } else {
            // Recipe grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    RecipeCardView(
                        recipe = recipe,
                        onTap = {
                            navController.navigate(Screen.RecipeDetail.createRoute(recipe.id))
                        },
                        onFavoriteTap = null,
                        isSelected = false
                    )
                }
            }
        }
    }

    // Manage recipes bottom sheet
    if (showManageSheet && cookbook != null) {
        ModalBottomSheet(
            onDismissRequest = { showManageSheet = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            CookbookRecipePicker(
                cookbookId = cookbookId,
                viewModel = viewModel,
                onDismiss = { showManageSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CookbookRecipePicker(
    cookbookId: String,
    viewModel: RecipeListViewModel,
    onDismiss: () -> Unit
) {
    val S = LocalStrings.current
    val allRecipes by viewModel.recipes.collectAsState()
    val cookbooks by viewModel.cookbooks.collectAsState()
    val cookbook = cookbooks.find { it.id == cookbookId }
    var searchText by remember { mutableStateOf("") }

    val cookbookRecipeIds = remember(cookbook) {
        cookbook?.recipeIdsJson?.let {
            runCatching {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(it)
            }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    val filteredRecipes = remember(allRecipes, searchText) {
        if (searchText.isBlank()) allRecipes
        else allRecipes.filter { it.title.contains(searchText, ignoreCase = true) }
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
                S.manageRecipes,
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
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
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
                val isInCookbook = recipe.id in cookbookRecipeIds

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isInCookbook) {
                                viewModel.removeRecipeFromCookbook(recipe.id, cookbookId)
                            } else {
                                viewModel.addRecipeToCookbook(recipe.id, cookbookId)
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
                        imageVector = if (isInCookbook) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (isInCookbook) Primary else TextTertiary,
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
