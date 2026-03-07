package com.example.pullit.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.ui.theme.Primary
import com.example.pullit.viewmodel.AiRecommendViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionDetailScreen(
    index: Int,
    navController: NavController,
    viewModel: AiRecommendViewModel = viewModel()
) {
    val suggestions by viewModel.suggestions.collectAsState()
    val suggestion = suggestions.getOrNull(index)

    if (suggestion == null) {
        navController.popBackStack()
        return
    }

    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(suggestion.title) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(suggestion.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(suggestion.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                suggestion.calories?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("$it kcal", color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                Button(
                    onClick = { viewModel.saveRecipeFromSuggestion(suggestion); saved = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !saved,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(if (saved) "Saved!" else "Save to Recipes", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                }
            }

            item { Text("Ingredients", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            itemsIndexed(suggestion.ingredients) { _, ing ->
                Text("\u2022 $ing", modifier = Modifier.padding(vertical = 2.dp))
            }

            item { Text("Steps", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            itemsIndexed(suggestion.steps) { i, step ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(alpha = 0.15f), modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("${i + 1}", fontWeight = FontWeight.Bold, color = Primary, fontSize = 13.sp) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(step, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
