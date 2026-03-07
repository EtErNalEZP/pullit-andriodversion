package com.example.pullit.ui.mealplan

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.data.model.GroceryItem
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.MealPlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryListScreen(
    navController: NavController,
    viewModel: MealPlanViewModel = viewModel()
) {
    val S = LocalStrings.current
    val groceryItems by viewModel.groceryItems.collectAsState()
    var newItemName by remember { mutableStateOf("") }

    val toBuyItems = groceryItems.filter { !it.checked }
    val boughtItems = groceryItems.filter { it.checked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(S.groceryList, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Add item field at top
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        placeholder = { Text(S.addAnItem, color = TextTertiary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newItemName.isNotBlank()) {
                                viewModel.addCustomGroceryItem(newItemName.trim())
                                newItemName = ""
                            }
                        },
                        enabled = newItemName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(S.add, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            if (groceryItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                S.noItemsYet,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary
                            )
                            Text(
                                S.noItemsHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }

            // "To Buy" section
            if (toBuyItems.isNotEmpty()) {
                item {
                    Text(
                        S.toBuy,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(toBuyItems, key = { it.id }) { item ->
                    GroceryItemRow(
                        item = item,
                        onToggle = { viewModel.toggleGroceryChecked(item) }
                    )
                }
            }

            // "Bought" section
            if (boughtItems.isNotEmpty()) {
                item {
                    Text(
                        S.bought,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(boughtItems, key = { it.id }) { item ->
                    GroceryItemRow(
                        item = item,
                        onToggle = { viewModel.toggleGroceryChecked(item) }
                    )
                }
            }

            // Bottom buttons
            if (groceryItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (boughtItems.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.clearCheckedGroceries() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextSecondary
                                )
                            ) {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(S.clearBought)
                            }
                        }

                        TextButton(
                            onClick = { viewModel.clearAllGroceries() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(S.clearAll, color = Error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroceryItemRow(
    item: GroceryItem,
    onToggle: () -> Unit
) {
    val isChecked = item.checked

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom checkbox: filled circle when checked, empty circle when not
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .then(
                    if (isChecked) Modifier.background(Success)
                    else Modifier.border(2.dp, TextTertiary, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Checked",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (isChecked) Modifier.padding() else Modifier)
        ) {
            Text(
                item.name,
                fontWeight = FontWeight.Medium,
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isChecked)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (item.amount.isNotBlank()) {
                Text(
                    item.amount,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isChecked) TextTertiary.copy(alpha = 0.5f) else TextSecondary,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
            }
            if (!item.fromRecipesJson.isNullOrBlank() && item.fromRecipesJson != "[]") {
                Text(
                    LocalStrings.current.fromMealPlan,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
