package com.example.pullit.ui.mealplan

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.Primary
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add to Meal Plan") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(recipes) { recipe ->
                        TextButton(
                            onClick = { viewModel.addToMealPlan(recipe.id); showAddDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(recipe.title, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Plan", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.GroceryList.route) }) {
                        Icon(Icons.Outlined.ShoppingCart, "Grocery List")
                    }
                    if (mealPlanItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearMealPlan() }) {
                            Icon(Icons.Outlined.DeleteSweep, "Clear")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Primary) {
                Icon(Icons.Default.Add, "Add recipe", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        if (mealPlanItems.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDCC5", fontSize = 60.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No meals planned", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to add recipes", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Button(
                        onClick = { viewModel.generateGroceryList(); navController.navigate(Screen.GroceryList.route) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, null, tint = androidx.compose.ui.graphics.Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Grocery List", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
                items(mealPlanItems, key = { it.id }) { item ->
                    val recipe = recipes.find { it.id == item.recipeId }
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipe?.title ?: "Unknown", fontWeight = FontWeight.Bold)
                                Text("${item.servings} servings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.removeFromMealPlan(item) }) {
                                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
