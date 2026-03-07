package com.example.pullit.ui.mealplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.ui.theme.Primary
import com.example.pullit.viewmodel.MealPlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryListScreen(
    navController: NavController,
    viewModel: MealPlanViewModel = viewModel()
) {
    val groceryItems by viewModel.groceryItems.collectAsState()
    var newItemName by remember { mutableStateOf("") }
    var showAddField by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grocery List") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (groceryItems.any { it.checked }) {
                        TextButton(onClick = { viewModel.clearCheckedGroceries() }) { Text("Clear bought") }
                    }
                    IconButton(onClick = { viewModel.clearAllGroceries() }) {
                        Icon(Icons.Outlined.DeleteSweep, "Clear all")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddField = true }, containerColor = Primary) {
                Icon(Icons.Default.Add, "Add", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showAddField) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newItemName, onValueChange = { newItemName = it },
                            placeholder = { Text("Item name") },
                            modifier = Modifier.weight(1f), singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            if (newItemName.isNotBlank()) { viewModel.addCustomGroceryItem(newItemName); newItemName = ""; showAddField = false }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Add") }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (groceryItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No items yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(groceryItems.sortedBy { it.checked }, key = { it.id }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { viewModel.toggleGroceryChecked(item) },
                        colors = CheckboxDefaults.colors(checkedColor = Primary)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.name,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        if (item.amount.isNotBlank()) {
                            Text(item.amount, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
