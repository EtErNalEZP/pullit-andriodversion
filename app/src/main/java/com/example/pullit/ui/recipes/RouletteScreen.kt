package com.example.pullit.ui.recipes

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
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
import com.example.pullit.viewmodel.RecipeListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouletteScreen(
    navController: NavController,
    viewModel: RecipeListViewModel = viewModel()
) {
    val recipes by viewModel.recipes.collectAsState()
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe Roulette") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (recipes.isEmpty()) {
                Text("No recipes to choose from", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                if (selectedIndex >= 0 && selectedIndex < recipes.size) {
                    val recipe = recipes[selectedIndex]
                    AnimatedContent(targetState = recipe.id, label = "roulette") {
                        RecipeCard(
                            recipe = recipes.find { r -> r.id == it } ?: recipe,
                            onClick = { navController.navigate(Screen.RecipeDetail.createRoute(recipe.id)) },
                            onFavoriteClick = { viewModel.toggleFavorite(recipe) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text("\uD83C\uDFB0", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tap the button to pick a random recipe!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { selectedIndex = recipes.indices.random() },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Spin!", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}
