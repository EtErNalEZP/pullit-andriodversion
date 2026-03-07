package com.example.pullit.ui.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.Primary
import com.example.pullit.viewmodel.AiRecommendViewModel
import com.example.pullit.viewmodel.AiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecommendScreen(
    navController: NavController,
    viewModel: AiRecommendViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val editableIngredients by viewModel.editableIngredients.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val localMatches by viewModel.localMatches.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    var newIngredient by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) viewModel.analyzeImage(bytes)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AI Recommend", fontWeight = FontWeight.ExtraBold) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state) {
                AiState.IDLE -> {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("\uD83E\uDD57", fontSize = 60.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("What's in your fridge?", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Take a photo and we'll suggest recipes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { imagePicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, null, tint = androidx.compose.ui.graphics.Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Pick Image", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }
                AiState.ANALYZING -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Primary)
                                Spacer(Modifier.height(16.dp))
                                Text("Analyzing image...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                AiState.READY, AiState.GENERATING -> {
                    // Editable ingredients
                    item {
                        Text("Found Ingredients", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    itemsIndexed(editableIngredients) { index, ing ->
                        InputChip(
                            selected = true, onClick = { viewModel.removeIngredient(index) },
                            label = { Text(ing) },
                            trailingIcon = { Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp)) }
                        )
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newIngredient, onValueChange = { newIngredient = it },
                                placeholder = { Text("Add ingredient...") },
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            IconButton(onClick = {
                                if (newIngredient.isNotBlank()) { viewModel.addIngredient(newIngredient); newIngredient = "" }
                            }) { Icon(Icons.Default.Add, "Add", tint = Primary) }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.generateSuggestions() },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = state != AiState.GENERATING
                            ) {
                                if (state == AiState.GENERATING) CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                else Text("Get AI Suggestions")
                            }
                            OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("New Photo") }
                        }
                    }

                    // Local matches
                    if (localMatches.isNotEmpty()) {
                        item { Text("Your Matching Recipes", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        items(localMatches) { match ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(Screen.RecipeDetail.createRoute(match.id)) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(match.title, fontWeight = FontWeight.Bold)
                                    Text("${(match.matchScore * 100).toInt()}% match", color = Primary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // AI suggestions
                    if (suggestions.isNotEmpty()) {
                        item { Text("AI Suggestions", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        itemsIndexed(suggestions) { index, suggestion ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(Screen.SuggestionDetail.createRoute(index)) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(suggestion.title, fontWeight = FontWeight.Bold)
                                    Text(suggestion.description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                }
                            }
                        }
                    }
                }
                AiState.ERROR -> {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Something went wrong", color = MaterialTheme.colorScheme.error)
                            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.reset() }) { Text("Try Again") }
                        }
                    }
                }
            }
        }
    }
}
