package com.example.pullit.ui.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.data.model.Ingredient
import com.example.pullit.data.model.Step
import com.example.pullit.ui.theme.Primary
import com.example.pullit.viewmodel.RecipeDetailViewModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    recipeId: String,
    navController: NavController,
    viewModel: RecipeDetailViewModel = viewModel()
) {
    val recipe by viewModel.recipe.collectAsState()
    val json = remember { Json { ignoreUnknownKeys = true } }

    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }
    val r = recipe ?: return

    var title by remember(r) { mutableStateOf(r.title) }
    var desc by remember(r) { mutableStateOf(r.desc ?: "") }
    var cookTime by remember(r) { mutableStateOf(r.cookTime ?: "") }
    var servings by remember(r) { mutableStateOf(r.servings.toString()) }
    var ingredients by remember(r) {
        mutableStateOf(
            r.ingredientsJson?.let { runCatching { json.decodeFromString<List<Ingredient>>(it) }.getOrDefault(emptyList()) } ?: emptyList()
        )
    }
    var steps by remember(r) {
        mutableStateOf(
            r.stepsJson?.let { runCatching { json.decodeFromString<List<Step>>(it) }.getOrDefault(emptyList()) } ?: emptyList()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Recipe") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = {
                        val updated = r.copy(
                            title = title,
                            desc = desc.ifBlank { null },
                            cookTime = cookTime.ifBlank { null },
                            servings = servings.toIntOrNull() ?: 2,
                            ingredientsJson = json.encodeToString(ListSerializer(Ingredient.serializer()), ingredients),
                            stepsJson = json.encodeToString(ListSerializer(Step.serializer()), steps)
                        )
                        viewModel.updateRecipe(updated)
                        navController.popBackStack()
                    }) {
                        Text("Save", color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = cookTime, onValueChange = { cookTime = it }, label = { Text("Cook time") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = servings, onValueChange = { servings = it }, label = { Text("Servings") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }

            // Ingredients
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Ingredients", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { ingredients = ingredients + Ingredient(name = "", amount = "") }) {
                        Icon(Icons.Default.Add, "Add ingredient", tint = Primary)
                    }
                }
            }
            itemsIndexed(ingredients) { index, ing ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ing.name, onValueChange = { ingredients = ingredients.toMutableList().also { list -> list[index] = ing.copy(name = it) } },
                        label = { Text("Name") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = ing.amount, onValueChange = { ingredients = ingredients.toMutableList().also { list -> list[index] = ing.copy(amount = it) } },
                        label = { Text("Amount") }, modifier = Modifier.weight(0.6f), singleLine = true
                    )
                    IconButton(onClick = { ingredients = ingredients.toMutableList().also { it.removeAt(index) } }) {
                        Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Steps
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Steps", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { steps = steps + Step(order = steps.size + 1, instruction = "") }) {
                        Icon(Icons.Default.Add, "Add step", tint = Primary)
                    }
                }
            }
            itemsIndexed(steps) { index, step ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1}.", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                    OutlinedTextField(
                        value = step.instruction,
                        onValueChange = { steps = steps.toMutableList().also { list -> list[index] = step.copy(instruction = it) } },
                        modifier = Modifier.weight(1f), minLines = 2
                    )
                    IconButton(onClick = { steps = steps.toMutableList().also { it.removeAt(index) } }) {
                        Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
