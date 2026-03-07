package com.example.pullit.ui.recipes

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.pullit.data.model.Ingredient
import com.example.pullit.data.model.Nutrition
import com.example.pullit.data.model.Step
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.*
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
    val isInMealPlan by viewModel.isInMealPlan.collectAsState()
    val json = remember { Json { ignoreUnknownKeys = true } }
    val context = LocalContext.current

    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }

    val r = recipe ?: return

    val ingredients = remember(r.ingredientsJson) {
        r.ingredientsJson?.let {
            runCatching { json.decodeFromString<List<Ingredient>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
    }
    val steps = remember(r.stepsJson) {
        r.stepsJson?.let {
            runCatching { json.decodeFromString<List<Step>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
    }
    val nutrition = remember(r.nutritionJson) {
        r.nutritionJson?.let {
            runCatching { json.decodeFromString<Nutrition>(it) }.getOrNull()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showCookbookDialog by remember { mutableStateOf(false) }
    val cookbooks by viewModel.cookbooks.collectAsState()
    var servingCount by remember(r) { mutableIntStateOf(r.servings) }
    val servingRatio = if (r.servings > 0) servingCount.toFloat() / r.servings.toFloat() else 1f
    var checkedIngredients by remember { mutableStateOf(setOf<Int>()) }
    val addedToMealPlan = isInMealPlan

    // Ingredient names for keyword highlighting in steps
    val ingredientNames = remember(ingredients) {
        ingredients.map { it.name.lowercase() }.filter { it.length > 2 }
    }

    // Group ingredients by section
    val groupedIngredients = remember(ingredients) {
        val groups = mutableListOf<Pair<String?, List<IndexedValue<Ingredient>>>>()
        var currentSection: String? = null
        var currentGroup = mutableListOf<IndexedValue<Ingredient>>()

        ingredients.forEachIndexed { index, ing ->
            val section = ing.section
            if (section != currentSection && (section != null || currentSection != null)) {
                if (currentGroup.isNotEmpty()) {
                    groups.add(currentSection to currentGroup.toList())
                }
                currentSection = section
                currentGroup = mutableListOf()
            }
            currentGroup.add(IndexedValue(index, ing))
        }
        if (currentGroup.isNotEmpty()) {
            groups.add(currentSection to currentGroup.toList())
        }
        groups
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete recipe?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecipe()
                    navController.popBackStack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Cookbook picker dialog
    if (showCookbookDialog) {
        var creatingNew by remember { mutableStateOf(false) }
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCookbookDialog = false; creatingNew = false; newName = "" },
            title = { Text("\u52A0\u5165\u98DF\u8C31\u96C6") },
            text = {
                Column {
                    if (creatingNew) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("\u98DF\u8C31\u96C6\u540D\u79F0") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        cookbooks.forEach { cookbook ->
                            TextButton(
                                onClick = {
                                    viewModel.addToCookbook(cookbook.id)
                                    showCookbookDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(cookbook.title, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        TextButton(
                            onClick = { creatingNew = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "+ \u65B0\u5EFA\u98DF\u8C31\u96C6",
                                modifier = Modifier.fillMaxWidth(),
                                color = Primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (creatingNew) {
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.createCookbook(newName.trim())
                            newName = ""; creatingNew = false
                        }
                    }) { Text("\u521B\u5EFA") }
                } else {
                    TextButton(onClick = { showCookbookDialog = false }) { Text("\u53D6\u6D88") }
                }
            },
            dismissButton = {
                if (creatingNew) {
                    TextButton(onClick = { creatingNew = false; newName = "" }) { Text("\u8FD4\u56DE") }
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {},
                windowInsets = WindowInsets.statusBars,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Share
                    IconButton(onClick = {
                        val shareText = buildString {
                            append(r.title)
                            r.desc?.let { append("\n$it") }
                            r.sourceUrl?.let { append("\n$it") }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Recipe"))
                    }) {
                        Icon(Icons.Outlined.Share, "Share")
                    }

                    // Favorite
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (r.favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            "Favorite",
                            tint = if (r.favorited) Primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Edit
                    IconButton(onClick = {
                        navController.navigate(Screen.RecipeEdit.createRoute(r.id))
                    }) {
                        Icon(Icons.Outlined.Edit, "Edit")
                    }

                    // More menu
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, "More")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            if (r.sourceUrl != null) {
                                DropdownMenuItem(
                                    text = { Text("Open Source URL") },
                                    onClick = {
                                        showMoreMenu = false
                                        val intent = Intent(Intent.ACTION_VIEW,
                                            android.net.Uri.parse(r.sourceUrl))
                                        context.startActivity(intent)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── Header Image ──
            item {
                if (r.imageUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    ) {
                        AsyncImage(
                            model = r.imageUrl,
                            contentDescription = r.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Gradient overlay at bottom for readability
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                                    )
                                )
                        )
                    }
                } else {
                    // Gradient placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryLight, Primary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // ── Title Section ──
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = r.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (!r.desc.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = r.desc!!,
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // ── Meta Info Card ──
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetaInfoItem(
                            icon = Icons.Outlined.Timer,
                            text = r.cookTime ?: "--"
                        )
                        VerticalDivider(
                            modifier = Modifier.height(28.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        MetaInfoItem(
                            icon = Icons.Outlined.Restaurant,
                            text = "${r.servings}"
                        )
                        VerticalDivider(
                            modifier = Modifier.height(28.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        MetaInfoItem(
                            icon = Icons.Outlined.LocalFireDepartment,
                            text = r.calories?.let { "$it kcal" } ?: "--"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Nutrition Card ──
            if (nutrition != null && listOf(nutrition.calories, nutrition.protein, nutrition.fat, nutrition.carbs).any { it.isNotBlank() }) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            NutritionInfoItem(
                                icon = Icons.Outlined.LocalFireDepartment,
                                value = nutrition.calories.ifBlank { "--" },
                                label = "Calories",
                                iconTint = Error
                            )
                            NutritionInfoItem(
                                icon = Icons.Outlined.FitnessCenter,
                                value = nutrition.protein.ifBlank { "--" },
                                label = "Protein",
                                iconTint = Info
                            )
                            NutritionInfoItem(
                                icon = Icons.Outlined.WaterDrop,
                                value = nutrition.fat.ifBlank { "--" },
                                label = "Fat",
                                iconTint = Warning
                            )
                            NutritionInfoItem(
                                icon = Icons.Outlined.Grass,
                                value = nutrition.carbs.ifBlank { "--" },
                                label = "Carbs",
                                iconTint = Success
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // ── Serving Adjuster ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Servings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { if (servingCount > 1) servingCount-- },
                        enabled = servingCount > 1,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (servingCount > 1) Primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Decrease servings",
                            tint = if (servingCount > 1) Primary else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "$servingCount",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = { if (servingCount < 20) servingCount++ },
                        enabled = servingCount < 20,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (servingCount < 20) Primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase servings",
                            tint = if (servingCount < 20) Primary else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Action Buttons Row ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCookbookDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Primary),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        Icon(
                            Icons.Outlined.BookmarkAdd,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Add to Cookbook",
                            color = Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val mealPlanButtonColor by animateColorAsState(
                        targetValue = if (addedToMealPlan) Primary else Color.Transparent,
                        label = "mealPlanColor"
                    )
                    val mealPlanTextColor = if (addedToMealPlan) Color.White else Primary

                    OutlinedButton(
                        onClick = {
                            if (!addedToMealPlan) {
                                viewModel.addToMealPlan()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = mealPlanButtonColor
                        ),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        Icon(
                            if (addedToMealPlan) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = mealPlanTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (addedToMealPlan) "Added" else "Add to Meal Plan",
                            color = mealPlanTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Ingredients Section ──
            if (ingredients.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ingredients",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                    Spacer(modifier = Modifier.height(10.dp))
                }

                groupedIngredients.forEach { (sectionName, sectionIngredients) ->
                    if (sectionName != null) {
                        item {
                            Text(
                                sectionName,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                    sectionIngredients.forEach { (originalIndex, ing) ->
                        item(key = "ingredient_$originalIndex") {
                            val isChecked = originalIndex in checkedIngredients
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        checkedIngredients = if (isChecked) {
                                            checkedIngredients - originalIndex
                                        } else {
                                            checkedIngredients + originalIndex
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        checkedIngredients = if (isChecked) {
                                            checkedIngredients - originalIndex
                                        } else {
                                            checkedIngredients + originalIndex
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Primary,
                                        uncheckedColor = TextTertiary
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = ing.name,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 15.sp,
                                    color = if (isChecked)
                                        TextTertiary
                                    else
                                        MaterialTheme.colorScheme.onBackground,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = scaleAmount(ing.amount, servingRatio),
                                    fontSize = 14.sp,
                                    color = if (isChecked)
                                        TextTertiary.copy(alpha = 0.6f)
                                    else
                                        TextSecondary,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else null
                                )
                            }
                            if (originalIndex < ingredients.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }

            // ── Steps Section ──
            if (steps.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Steps",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                navController.navigate(Screen.CookingMode.createRoute(r.id))
                            }
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Start Cooking",
                                color = Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                itemsIndexed(steps) { index, step ->
                    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Number circle + connector line
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Primary
                                    )
                                }
                            }
                            if (index < steps.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(
                                            // Variable height connector
                                            if (step.instruction.length > 100) 120.dp else 60.dp
                                        )
                                        .drawBehind {
                                            drawLine(
                                                color = borderColor,
                                                start = Offset(size.width / 2, 0f),
                                                end = Offset(size.width / 2, size.height),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                        }
                                )
                            }
                        }

                        // Step content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (index < steps.size - 1) 16.dp else 0.dp)
                        ) {
                            Text(
                                text = buildHighlightedText(step.instruction, ingredientNames),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Helper composables ──

@Composable
private fun MetaInfoItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = TextSecondary
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NutritionInfoItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

/**
 * Scales a numeric amount string by the given ratio.
 * Tries to parse the leading number from the amount and multiply it.
 * If parsing fails, returns the original string unchanged.
 */
private fun scaleAmount(amount: String, ratio: Float): String {
    if (ratio == 1f || amount.isBlank()) return amount

    val regex = Regex("""^(\d+\.?\d*)(.*)""")
    val match = regex.find(amount.trim()) ?: return amount
    val number = match.groupValues[1].toDoubleOrNull() ?: return amount
    val suffix = match.groupValues[2]

    val scaled = number * ratio
    val formatted = if (scaled == scaled.toLong().toDouble()) {
        scaled.toLong().toString()
    } else {
        "%.1f".format(scaled)
    }
    return "$formatted$suffix"
}

/**
 * Builds an annotated string that highlights ingredient keywords in Primary color.
 */
@Composable
private fun buildHighlightedText(
    text: String,
    keywords: List<String>
): androidx.compose.ui.text.AnnotatedString {
    if (keywords.isEmpty()) {
        return buildAnnotatedString { append(text) }
    }

    val lowerText = text.lowercase()
    // Build ranges of keyword matches
    data class MatchRange(val start: Int, val end: Int)

    val matches = mutableListOf<MatchRange>()
    for (keyword in keywords) {
        var startIndex = 0
        while (true) {
            val found = lowerText.indexOf(keyword, startIndex)
            if (found == -1) break
            // Check word boundary - only match whole words or at start/end
            val beforeOk = found == 0 || !lowerText[found - 1].isLetter()
            val afterOk = (found + keyword.length) >= lowerText.length ||
                    !lowerText[found + keyword.length].isLetter()
            if (beforeOk && afterOk) {
                matches.add(MatchRange(found, found + keyword.length))
            }
            startIndex = found + 1
        }
    }

    if (matches.isEmpty()) {
        return buildAnnotatedString { append(text) }
    }

    // Sort and merge overlapping matches
    val sorted = matches.sortedBy { it.start }
    val merged = mutableListOf<MatchRange>()
    var current = sorted[0]
    for (i in 1 until sorted.size) {
        val next = sorted[i]
        current = if (next.start <= current.end) {
            MatchRange(current.start, maxOf(current.end, next.end))
        } else {
            merged.add(current)
            next
        }
    }
    merged.add(current)

    return buildAnnotatedString {
        var lastEnd = 0
        for (m in merged) {
            if (m.start > lastEnd) {
                append(text.substring(lastEnd, m.start))
            }
            withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.SemiBold)) {
                append(text.substring(m.start, m.end))
            }
            lastEnd = m.end
        }
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
}
