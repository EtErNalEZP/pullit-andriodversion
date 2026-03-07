package com.example.pullit.ui.recipes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.pullit.data.model.Ingredient
import com.example.pullit.data.model.Nutrition
import com.example.pullit.data.model.Step
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.theme.*
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
    val S = LocalStrings.current
    val recipe by viewModel.recipe.collectAsState()
    val json = remember { Json { ignoreUnknownKeys = true } }

    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }
    val r = recipe ?: return

    var title by remember(r) { mutableStateOf(r.title) }
    var desc by remember(r) { mutableStateOf(r.desc ?: "") }
    var cookTime by remember(r) { mutableStateOf(r.cookTime ?: "") }
    var servings by remember(r) { mutableIntStateOf(r.servings) }
    var imageUrl by remember(r) { mutableStateOf(r.imageUrl ?: "") }
    var ingredients by remember(r) {
        mutableStateOf(
            r.ingredientsJson?.let {
                runCatching { json.decodeFromString<List<Ingredient>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
        )
    }
    var steps by remember(r) {
        mutableStateOf(
            r.stepsJson?.let {
                runCatching { json.decodeFromString<List<Step>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
        )
    }
    val nutrition = remember(r) {
        r.nutritionJson?.let {
            runCatching { json.decodeFromString<Nutrition>(it) }.getOrNull()
        }
    }
    var calories by remember(r) { mutableStateOf(nutrition?.calories ?: "") }
    var protein by remember(r) { mutableStateOf(nutrition?.protein ?: "") }
    var fat by remember(r) { mutableStateOf(nutrition?.fat ?: "") }
    var carbs by remember(r) { mutableStateOf(nutrition?.carbs ?: "") }
    var nutritionExpanded by remember { mutableStateOf(nutrition != null) }

    // Track which ingredient sections are expanded
    var expandedIngredientSections by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        S.editRecipe,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, S.cancel)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val nutritionObj = if (calories.isNotBlank() || protein.isNotBlank() ||
                                fat.isNotBlank() || carbs.isNotBlank()
                            ) {
                                Nutrition(
                                    calories = calories,
                                    protein = protein,
                                    fat = fat,
                                    carbs = carbs
                                )
                            } else null

                            val updated = r.copy(
                                title = title,
                                desc = desc.ifBlank { null },
                                cookTime = cookTime.ifBlank { null },
                                servings = servings,
                                imageUrl = imageUrl.ifBlank { null },
                                ingredientsJson = json.encodeToString(
                                    ListSerializer(Ingredient.serializer()), ingredients
                                ),
                                stepsJson = json.encodeToString(
                                    ListSerializer(Step.serializer()), steps
                                ),
                                nutritionJson = nutritionObj?.let {
                                    json.encodeToString(Nutrition.serializer(), it)
                                }
                            )
                            viewModel.updateRecipe(updated)
                            navController.popBackStack()
                        }
                    ) {
                        Text(
                            S.save,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Cover Image Section ──
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        S.coverImage,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (imageUrl.isNotBlank()) {
                        // Image preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Cover image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Overlay buttons
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SmallFloatingActionButton(
                                    onClick = { /* Change image - future implementation */ },
                                    containerColor = Color.White.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.PhotoCamera,
                                        contentDescription = "Change",
                                        tint = TextPrimaryLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                SmallFloatingActionButton(
                                    onClick = { imageUrl = "" },
                                    containerColor = Error.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Placeholder
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = TextTertiary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    S.addCoverPhoto,
                                    fontSize = 14.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }

            // ── Basic Info Section ──
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(S.basicInfo)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(S.title) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = editFieldColors()
                    )

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(S.description) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        colors = editFieldColors()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = cookTime,
                            onValueChange = { cookTime = it },
                            label = { Text(S.cookTime) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = editFieldColors(),
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Timer,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        // Servings counter
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                S.servings,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = { if (servings > 1) servings-- },
                                    enabled = servings > 1,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (servings > 1) Primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = if (servings > 1) Primary else TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    "$servings",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                IconButton(
                                    onClick = { servings++ },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Primary.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Nutrition Section (Collapsible) ──
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(S.nutrition, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { nutritionExpanded = !nutritionExpanded }
                        ) {
                            Icon(
                                if (nutritionExpanded) Icons.Default.ExpandLess
                                else Icons.Default.ExpandMore,
                                contentDescription = if (nutritionExpanded) S.collapse else S.expand,
                                tint = TextSecondary
                            )
                        }
                    }

                    AnimatedVisibility(visible = nutritionExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = calories,
                                    onValueChange = { calories = it },
                                    label = { Text(S.calories) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = editFieldColors(),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                                OutlinedTextField(
                                    value = protein,
                                    onValueChange = { protein = it },
                                    label = { Text(S.protein) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = editFieldColors(),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.FitnessCenter,
                                            contentDescription = null,
                                            tint = Info,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = fat,
                                    onValueChange = { fat = it },
                                    label = { Text(S.fat) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = editFieldColors(),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.WaterDrop,
                                            contentDescription = null,
                                            tint = Warning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                                OutlinedTextField(
                                    value = carbs,
                                    onValueChange = { carbs = it },
                                    label = { Text(S.carbs) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = editFieldColors(),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Grass,
                                            contentDescription = null,
                                            tint = Success,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Ingredients Section ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(S.ingredients, modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${ingredients.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(ingredients, key = { index, _ -> "edit_ing_$index" }) { index, ing ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ing.name,
                            onValueChange = { newName ->
                                ingredients = ingredients.toMutableList().also { list ->
                                    list[index] = ing.copy(name = newName)
                                }
                            },
                            label = { Text(S.name) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = editFieldColors()
                        )
                        OutlinedTextField(
                            value = ing.amount,
                            onValueChange = { newAmount ->
                                ingredients = ingredients.toMutableList().also { list ->
                                    list[index] = ing.copy(amount = newAmount)
                                }
                            },
                            label = { Text(S.amount) },
                            modifier = Modifier.weight(0.55f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = editFieldColors()
                        )
                        IconButton(
                            onClick = {
                                ingredients = ingredients.toMutableList().also {
                                    it.removeAt(index)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.RemoveCircleOutline,
                                "Remove",
                                tint = Error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Section field (collapsible)
                    val sectionExpanded = index in expandedIngredientSections || !ing.section.isNullOrBlank()
                    if (!sectionExpanded) {
                        TextButton(
                            onClick = {
                                expandedIngredientSections = expandedIngredientSections + index
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                S.section,
                                fontSize = 12.sp,
                                color = TextTertiary
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = ing.section ?: "",
                            onValueChange = { newSection ->
                                ingredients = ingredients.toMutableList().also { list ->
                                    list[index] = ing.copy(
                                        section = newSection.ifBlank { null }
                                    )
                                }
                            },
                            label = { Text(S.sectionOptional) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = editFieldColors()
                        )
                    }

                    if (index < ingredients.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }
                }
            }

            item {
                TextButton(
                    onClick = {
                        ingredients = ingredients + Ingredient(name = "", amount = "")
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        S.addIngredient2,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Steps Section ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(S.steps, modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${steps.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(steps, key = { index, _ -> "edit_step_$index" }) { index, step ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Step number circle
                        Surface(
                            shape = CircleShape,
                            color = Primary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(28.dp)
                                .offset(y = 14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = step.instruction,
                            onValueChange = { newInstruction ->
                                steps = steps.toMutableList().also { list ->
                                    list[index] = step.copy(instruction = newInstruction)
                                }
                            },
                            label = { Text("Step ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            minLines = 2,
                            maxLines = 6,
                            shape = RoundedCornerShape(10.dp),
                            colors = editFieldColors()
                        )

                        IconButton(
                            onClick = {
                                steps = steps.toMutableList().also { it.removeAt(index) }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .offset(y = 10.dp)
                        ) {
                            Icon(
                                Icons.Outlined.RemoveCircleOutline,
                                "Remove",
                                tint = Error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (index < steps.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }
                }
            }

            item {
                TextButton(
                    onClick = {
                        steps = steps + Step(order = steps.size + 1, instruction = "")
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        S.addStep,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ── Helper composables for the edit screen ──

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun editFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = Primary,
        unfocusedLabelColor = TextSecondary,
        cursorColor = Primary
    )
}
