package com.example.pullit.ui.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.pullit.data.model.Ingredient
import com.example.pullit.data.model.Nutrition
import com.example.pullit.data.model.Step
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.Primary
import com.example.pullit.viewmodel.RecipeDetailViewModel
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    navController: NavController,
    viewModel: RecipeDetailViewModel = viewModel()
) {
    val recipe by viewModel.recipe.collectAsState()
    val json = remember { Json { ignoreUnknownKeys = true } }

    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }

    val r = recipe ?: return

    val ingredients = remember(r.ingredientsJson) {
        r.ingredientsJson?.let { runCatching { json.decodeFromString<List<Ingredient>>(it) }.getOrDefault(emptyList()) } ?: emptyList()
    }
    val steps = remember(r.stepsJson) {
        r.stepsJson?.let { runCatching { json.decodeFromString<List<Step>>(it) }.getOrDefault(emptyList()) } ?: emptyList()
    }
    val nutrition = remember(r.nutritionJson) {
        r.nutritionJson?.let { runCatching { json.decodeFromString<Nutrition>(it) }.getOrNull() }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete recipe?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRecipe(); navController.popBackStack() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (r.favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            "Favorite", tint = if (r.favorited) Primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.RecipeEdit.createRoute(r.id)) }) {
                        Icon(Icons.Outlined.Edit, "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.Delete, "Delete")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Image
            item {
                if (r.imageUrl != null) {
                    AsyncImage(
                        model = r.imageUrl,
                        contentDescription = r.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    )
                }
            }

            // Title and meta
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(r.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    if (r.desc != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(r.desc!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        r.cookTime?.let { InfoChip(icon = Icons.Outlined.Timer, text = it) }
                        r.calories?.let { InfoChip(icon = Icons.Outlined.LocalFireDepartment, text = "$it kcal") }
                        InfoChip(icon = Icons.Outlined.People, text = "${r.servings} servings")
                    }
                }
            }

            // Cook button
            item {
                if (steps.isNotEmpty()) {
                    Button(
                        onClick = { navController.navigate(Screen.CookingMode.createRoute(r.id)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Cooking", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Add to meal plan
            item {
                OutlinedButton(
                    onClick = { viewModel.addToMealPlan() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Meal Plan")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Ingredients
            if (ingredients.isNotEmpty()) {
                item {
                    Text("Ingredients", modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(ingredients.size) { index ->
                    val ing = ingredients[index]
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ing.name, modifier = Modifier.weight(1f))
                        Text(ing.amount, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (index < ingredients.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Steps
            if (steps.isNotEmpty()) {
                item {
                    Text("Steps", modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                itemsIndexed(steps) { index, step ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(step.instruction, modifier = Modifier.weight(1f))
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Nutrition
            if (nutrition != null) {
                item {
                    Text("Nutrition", modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            NutritionItem("Calories", nutrition!!.calories)
                            NutritionItem("Protein", nutrition!!.protein)
                            NutritionItem("Fat", nutrition!!.fat)
                            NutritionItem("Carbs", nutrition!!.carbs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NutritionItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.ifBlank { "-" }, fontWeight = FontWeight.Bold, color = Primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
