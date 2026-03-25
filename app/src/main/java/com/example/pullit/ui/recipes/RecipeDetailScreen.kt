package com.example.pullit.ui.recipes

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.RecipeDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val S = LocalStrings.current

    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }

    val r = recipe ?: run {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
        return
    }

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

    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showCookbookDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
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
            title = { Text(S.deleteRecipe) },
            text = { Text(S.cannotUndo) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.deleteRecipe().join()
                        navController.popBackStack()
                    }
                }) {
                    Text(S.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(S.cancel)
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
            title = { Text(S.addToCookbook) },
            text = {
                Column {
                    if (creatingNew) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text(S.cookbookName) },
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
                                "+ ${S.newCookbook}",
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
                    }) { Text(S.create) }
                } else {
                    TextButton(onClick = { showCookbookDialog = false }) { Text(S.cancel) }
                }
            },
            dismissButton = {
                if (creatingNew) {
                    TextButton(onClick = { creatingNew = false; newName = "" }) { Text(S.back) }
                }
            }
        )
    }

    // Share sheet
    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    S.shareRecipe,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Share as Text
                Surface(
                    onClick = {
                        showShareSheet = false
                        shareAsText(context, r, ingredients, steps, S)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                S.shareAsText,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                S.shareAsTextHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Share as Image
                Surface(
                    onClick = {
                        showShareSheet = false
                        scope.launch { shareAsImage(context, r, ingredients, steps) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                S.shareAsImage,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                S.shareAsImageHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {},
                windowInsets = WindowInsets.statusBars,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, S.back)
                    }
                },
                actions = {
                    // Share
                    IconButton(onClick = { showShareSheet = true }) {
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
                                    text = { Text(S.openSourceUrl) },
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
                                text = { Text(S.delete, color = MaterialTheme.colorScheme.error) },
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
                                value = scaleNutritionValue(nutrition.calories.ifBlank { "--" }, servingRatio),
                                label = S.calories,
                                iconTint = Error
                            )
                            NutritionInfoItem(
                                icon = Icons.Outlined.FitnessCenter,
                                value = scaleNutritionValue(nutrition.protein.ifBlank { "--" }, servingRatio),
                                label = S.protein,
                                iconTint = Info
                            )
                            NutritionInfoItem(
                                icon = Icons.Outlined.WaterDrop,
                                value = scaleNutritionValue(nutrition.fat.ifBlank { "--" }, servingRatio),
                                label = S.fat,
                                iconTint = Warning
                            )
                            NutritionInfoItem(
                                icon = Icons.Outlined.Grass,
                                value = scaleNutritionValue(nutrition.carbs.ifBlank { "--" }, servingRatio),
                                label = S.carbs,
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
                        S.servings,
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
                            S.addToCookbookBtn,
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
                            if (addedToMealPlan) S.added else S.addToMealPlan,
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
                            S.ingredients,
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
                            S.steps,
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
                                S.startCooking,
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
                            .height(IntrinsicSize.Min)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Number circle + connector line
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxHeight()
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
                                        .weight(1f)
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
                                .padding(bottom = if (index < steps.size - 1) 20.dp else 0.dp)
                        ) {
                            Text(
                                text = buildHighlightedText(step.instruction, ingredientNames),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (step.imageUrls.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                StepImageDisplayGrid(step.imageUrls)
                            }
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
 * Scales a nutrition value string (e.g., "730 kcal", "35g") by the serving ratio.
 */
private fun scaleNutritionValue(value: String, ratio: Float): String {
    if (ratio == 1f || value.isBlank() || value == "--") return value
    val regex = Regex("""^(\d+\.?\d*)\s*(.*)""")
    val match = regex.find(value.trim()) ?: return value
    val number = match.groupValues[1].toDoubleOrNull() ?: return value
    val suffix = match.groupValues[2]
    val scaled = (number * ratio).let { if (it >= 1) kotlin.math.round(it).toLong() else it }
    val formatted = if (scaled is Long) scaled.toString() else "%.1f".format(scaled)
    return if (suffix.isNotEmpty()) "$formatted $suffix".trim() else formatted
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

// ──────────────────────────────────────────────────────────
// Share helpers
// ──────────────────────────────────────────────────────────

private fun shareAsText(
    context: android.content.Context,
    recipe: com.example.pullit.data.model.Recipe,
    ingredients: List<Ingredient>,
    steps: List<Step>,
    S: com.example.pullit.ui.AppStrings
) {
    val text = buildString {
        appendLine(recipe.title)
        recipe.desc?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        appendLine()
        recipe.cookTime?.takeIf { it.isNotBlank() }?.let { appendLine("\u23F1 $it") }
        appendLine("\uD83D\uDCCB ${S.ingredients}")
        for (ing in ingredients) {
            appendLine("\u2022 ${ing.name} ${ing.amount}")
        }
        appendLine()
        appendLine("\uD83D\uDC69\u200D\uD83C\uDF73 ${S.steps}")
        for (step in steps) {
            appendLine("${step.order}. ${step.instruction}")
        }
        recipe.sourceUrl?.takeIf { it.isNotBlank() && !it.startsWith("local:") }?.let {
            appendLine()
            append("${S.shareSource}: $it")
        }
        appendLine()
        append("\u2014 Pullit Recipes")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private suspend fun shareAsImage(
    context: android.content.Context,
    recipe: com.example.pullit.data.model.Recipe,
    ingredients: List<Ingredient>,
    steps: List<Step>
) {
    val bitmap = withContext(Dispatchers.IO) {
        // Download cover image if available
        val coverBitmap = recipe.imageUrl?.let { url ->
            runCatching {
                java.net.URL(url).openStream().use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
        renderRecipeCardBitmap(context, recipe, ingredients, steps, coverBitmap)
    }

    val file = java.io.File(context.cacheDir, "share_recipe.png")
    withContext(Dispatchers.IO) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun renderRecipeCardBitmap(
    context: android.content.Context,
    recipe: com.example.pullit.data.model.Recipe,
    ingredients: List<Ingredient>,
    steps: List<Step>,
    coverBitmap: Bitmap?
): Bitmap {
    val dp = context.resources.displayMetrics.density
    val width = (390 * dp).toInt()
    val pad = 24 * dp
    val cw = (width - pad * 2).toInt()

    val textCol = 0xFF1A1A1A.toInt()
    val secCol = 0xFF888888.toInt()
    val priCol = 0xFFE8A87C.toInt()
    val divCol = 0xFFE8E8E8.toInt()
    val sectionCol = 0xFF666666.toInt()

    fun paint(color: Int, size: Float, bold: Boolean = false) = android.text.TextPaint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        this.color = color; textSize = size * dp
        if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val titleP = paint(textCol, 22f, true)
    val descP = paint(secCol, 14f)
    val metaP = paint(secCol, 12f)
    val headP = paint(textCol, 16f, true)
    val sectionP = paint(sectionCol, 13f, true)
    val bodyP = paint(textCol, 13f)
    val amtP = paint(secCol, 13f)
    val numP = paint(priCol, 13f, true)
    val sourceP = paint(secCol, 11f)
    val footP = paint(0xFFAAAAAA.toInt(), 11f)
    val lineP = android.graphics.Paint().apply { color = divCol; strokeWidth = dp }
    val imgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    fun layout(text: String, p: android.text.TextPaint, w: Int = cw) =
        android.text.StaticLayout.Builder.obtain(text, 0, text.length, p, w).build()

    val gap = 10 * dp
    val smallGap = 4 * dp

    // --- Measure total height ---
    var h = 0f

    // Cover image: full width, 4:3 aspect ratio
    val coverHeight = if (coverBitmap != null) (width * 3f / 4f) else 0f
    h += coverHeight

    h += pad  // top padding after cover (or top of card if no cover)

    val titleL = layout(recipe.title, titleP); h += titleL.height + gap
    val descL = recipe.desc?.takeIf { it.isNotBlank() }?.let { layout(it, descP) }
    descL?.let { h += it.height + gap }
    val metaParts = mutableListOf<String>()
    recipe.cookTime?.takeIf { it.isNotBlank() }?.let { metaParts.add("\u23F1 $it") }
    recipe.calories?.takeIf { it.isNotBlank() }?.let { metaParts.add("\uD83D\uDD25 $it") }
    metaParts.add("\uD83D\uDC64 ${recipe.servings}\u4EFD")
    val metaL = layout(metaParts.joinToString("   "), metaP); h += metaL.height + gap * 1.5f
    h += dp + gap  // divider

    // Ingredients with sections
    val ingHead = layout("\u98DF\u6750", headP); h += ingHead.height + gap

    // Group ingredients by section
    data class IngRow(val l: android.text.StaticLayout, val amount: String)
    data class IngGroup(val section: String?, val rows: List<IngRow>)

    val ingGroups = mutableListOf<IngGroup>()
    var curSection: String? = null
    var curRows = mutableListOf<IngRow>()
    for (ing in ingredients) {
        if (ing.section != curSection && (ing.section != null || curSection != null)) {
            if (curRows.isNotEmpty()) ingGroups.add(IngGroup(curSection, curRows.toList()))
            curSection = ing.section
            curRows = mutableListOf()
        }
        curRows.add(IngRow(layout("\u2022 ${ing.name}", bodyP, (cw * 0.65f).toInt()), ing.amount))
    }
    if (curRows.isNotEmpty()) ingGroups.add(IngGroup(curSection, curRows.toList()))

    for (group in ingGroups) {
        group.section?.let {
            val sl = layout(it, sectionP); h += sl.height + smallGap
        }
        group.rows.forEach { h += it.l.height + smallGap }
        h += smallGap
    }
    h += gap; h += dp + gap  // divider

    // Steps
    val stepHead = layout("\u6B65\u9AA4", headP); h += stepHead.height + gap
    val numW = (24 * dp).toInt()
    data class StepRow(val order: Int, val l: android.text.StaticLayout)
    val stepRows = steps.map { StepRow(it.order, layout(it.instruction, bodyP, cw - numW)) }
    stepRows.forEach { h += it.l.height + gap * 0.8f }
    h += gap; h += dp + gap  // divider

    // Source URL
    val sourceL = recipe.sourceUrl?.takeIf { it.isNotBlank() && !it.startsWith("local:") }?.let {
        layout("\u6765\u6E90: $it", sourceP)
    }
    sourceL?.let { h += it.height + gap }

    // Footer
    val footL = layout("Pullit Recipes", footP); h += footL.height + pad

    // --- Draw ---
    val bmp = Bitmap.createBitmap(width, h.toInt(), Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    c.drawColor(android.graphics.Color.WHITE)
    var y = 0f

    fun drawLayout(l: android.text.StaticLayout, x: Float = pad) {
        c.save(); c.translate(x, y); l.draw(c); c.restore()
    }
    fun divider() { c.drawLine(pad, y, width - pad, y, lineP); y += dp + gap }

    // Cover image - center crop to fill width x coverHeight
    if (coverBitmap != null && coverHeight > 0) {
        val srcW = coverBitmap.width.toFloat()
        val srcH = coverBitmap.height.toFloat()
        val dstW = width.toFloat()
        val dstH = coverHeight
        val scale = maxOf(dstW / srcW, dstH / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val srcRect = android.graphics.Rect(
            ((scaledW - dstW) / 2 / scale).toInt(),
            ((scaledH - dstH) / 2 / scale).toInt(),
            (srcW - (scaledW - dstW) / 2 / scale).toInt(),
            (srcH - (scaledH - dstH) / 2 / scale).toInt()
        )
        val dstRect = android.graphics.Rect(0, 0, width, coverHeight.toInt())
        c.drawBitmap(coverBitmap, srcRect, dstRect, imgPaint)
        y = coverHeight
    }

    y += pad

    drawLayout(titleL); y += titleL.height + gap
    descL?.let { drawLayout(it); y += it.height + gap }
    drawLayout(metaL); y += metaL.height + gap * 1.5f
    divider()

    drawLayout(ingHead); y += ingHead.height + gap
    for (group in ingGroups) {
        group.section?.let {
            val sl = layout(it, sectionP)
            drawLayout(sl); y += sl.height + smallGap
        }
        for (row in group.rows) {
            drawLayout(row.l)
            val aw = amtP.measureText(row.amount)
            c.drawText(row.amount, width - pad - aw, y + row.l.height * 0.75f, amtP)
            y += row.l.height + smallGap
        }
        y += smallGap
    }
    y += gap; divider()

    drawLayout(stepHead); y += stepHead.height + gap
    for (row in stepRows) {
        c.drawText("${row.order}.", pad, y + bodyP.textSize, numP)
        drawLayout(row.l, pad + numW)
        y += row.l.height + gap * 0.8f
    }
    y += gap; divider()

    sourceL?.let { drawLayout(it); y += it.height + gap }

    val fw = footP.measureText("Pullit Recipes")
    c.drawText("Pullit Recipes", width - pad - fw, y + footP.textSize, footP)

    return bmp
}

/**
 * Read-only image grid for step images.
 * Layout: 1→full width, 2→side by side, 3→top full + 2 bottom, 4→2×2 grid
 */
@Composable
private fun StepImageDisplayGrid(imageUrls: List<String>) {
    val shape = RoundedCornerShape(10.dp)
    when (imageUrls.size) {
        1 -> {
            AsyncImage(
                model = imageUrls[0],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(shape)
            )
        }
        2 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                imageUrls.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clip(shape)
                    )
                }
            }
        }
        3 -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AsyncImage(
                    model = imageUrls[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(shape)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..2) {
                        AsyncImage(
                            model = imageUrls[i],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clip(shape)
                        )
                    }
                }
            }
        }
        4 -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0..1) {
                        AsyncImage(
                            model = imageUrls[i],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clip(shape)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 2..3) {
                        AsyncImage(
                            model = imageUrls[i],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clip(shape)
                        )
                    }
                }
            }
        }
    }
}
