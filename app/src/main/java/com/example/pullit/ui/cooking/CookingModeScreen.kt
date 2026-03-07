package com.example.pullit.ui.cooking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.data.local.AppDatabase
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.CookingModeViewModel
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    recipeId: String,
    navController: NavController,
    viewModel: CookingModeViewModel = viewModel()
) {
    val context = LocalContext.current
    val steps by viewModel.steps.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val recipeTitle by viewModel.recipeTitle.collectAsState()

    var showTimerDialog by remember { mutableStateOf(false) }
    var timerMinutes by remember { mutableStateOf("5") }
    var showIngredients by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) {
        val db = AppDatabase.getInstance(context)
        val recipe = db.recipeDao().getById(recipeId)
        if (recipe != null) {
            viewModel.setup(recipe.title, recipe.stepsJson, recipe.ingredientsJson)
        }
    }

    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("Set Timer") },
            text = {
                OutlinedTextField(
                    value = timerMinutes,
                    onValueChange = { timerMinutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Minutes") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val mins = timerMinutes.toIntOrNull() ?: 5
                    viewModel.startTimer(mins * 60)
                    showTimerDialog = false
                }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showTimerDialog = false }) { Text("Cancel") } }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CookingBg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CookingText)
                }
                Text(recipeTitle, color = CookingText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { showIngredients = !showIngredients }) {
                    Icon(Icons.Default.List, "Ingredients", tint = CookingText)
                }
                IconButton(onClick = { showTimerDialog = true }) {
                    Icon(Icons.Default.Timer, "Timer", tint = CookingText)
                }
            }

            // Timer display
            if (timerSeconds > 0 || isTimerRunning) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CookingCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "%02d:%02d".format(timerSeconds / 60, timerSeconds % 60),
                            fontSize = 32.sp, fontWeight = FontWeight.Bold,
                            color = if (timerSeconds < 30) Error else Primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        if (isTimerRunning) {
                            IconButton(onClick = { viewModel.pauseTimer() }) {
                                Icon(Icons.Default.Pause, "Pause", tint = CookingText)
                            }
                        } else {
                            IconButton(onClick = { viewModel.resumeTimer() }) {
                                Icon(Icons.Default.PlayArrow, "Resume", tint = CookingText)
                            }
                        }
                        IconButton(onClick = { viewModel.resetTimer() }) {
                            Icon(Icons.Default.Stop, "Reset", tint = CookingText)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Ingredients panel
            if (showIngredients && ingredients.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 200.dp),
                    colors = CardDefaults.cardColors(containerColor = CookingCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        itemsIndexed(ingredients) { _, ing ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ing.name, color = CookingText)
                                Text(ing.amount, color = TextSecondary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Step progress
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center) {
                Text("Step ${currentStepIndex + 1} of ${steps.size}", color = TextSecondary)
            }
            LinearProgressIndicator(
                progress = { if (steps.isNotEmpty()) (currentStepIndex + 1).toFloat() / steps.size else 0f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                color = Primary, trackColor = Primary.copy(alpha = 0.2f)
            )

            // Current step
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                if (steps.isNotEmpty() && currentStepIndex in steps.indices) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.2f), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${currentStepIndex + 1}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            steps[currentStepIndex].instruction,
                            color = CookingText, fontSize = 20.sp, textAlign = TextAlign.Center,
                            lineHeight = 30.sp
                        )
                    }
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { viewModel.previousStep() },
                    enabled = currentStepIndex > 0,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CookingText)
                ) { Text("Previous") }

                if (currentStepIndex < steps.size - 1) {
                    Button(
                        onClick = { viewModel.nextStep() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Next", color = androidx.compose.ui.graphics.Color.White) }
                } else {
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Success)
                    ) { Text("Done!", color = androidx.compose.ui.graphics.Color.White) }
                }
            }
        }
    }
}
