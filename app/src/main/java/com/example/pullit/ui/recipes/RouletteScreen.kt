package com.example.pullit.ui.recipes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.pullit.data.model.Recipe
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.RecipeListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouletteScreen(
    navController: NavController,
    viewModel: RecipeListViewModel = viewModel()
) {
    val S = LocalStrings.current
    val recipes by viewModel.recipes.collectAsState()
    val cookbooks by viewModel.cookbooks.collectAsState()
    var selectedCookbookId by remember { mutableStateOf<String?>(null) }
    var isShuffling by remember { mutableStateOf(false) }
    var hasResult by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredRecipes = remember(recipes, cookbooks, selectedCookbookId) {
        if (selectedCookbookId == null) {
            recipes
        } else {
            val cookbook = cookbooks.find { it.id == selectedCookbookId }
            val recipeIds = cookbook?.recipeIdsJson?.let {
                runCatching {
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(it)
                }.getOrDefault(emptyList())
            } ?: emptyList()
            recipes.filter { it.id in recipeIds }
        }
    }

    val slotCount = maxOf(filteredRecipes.size, 11)
    val scrollPosition = remember { Animatable(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val effectiveScroll = scrollPosition.value + dragOffset

    fun recipeIndex(scroll: Float): Int {
        val count = filteredRecipes.size
        if (count == 0) return 0
        var slot = (round(scroll).toInt()) % slotCount
        if (slot < 0) slot += slotCount
        return slot % count
    }

    fun shuffle() {
        if (isShuffling || filteredRecipes.isEmpty()) return
        isShuffling = true
        hasResult = false

        val targetRecipe = (0 until filteredRecipes.size).random()
        val currentSlot = round(scrollPosition.value).toInt() % slotCount
        val currentRecipe = if (filteredRecipes.isNotEmpty()) currentSlot % filteredRecipes.size else 0
        var distance = targetRecipe - currentRecipe
        if (distance <= 0) distance += filteredRecipes.size
        distance += slotCount * (3 + (0..1).random())

        scope.launch {
            scrollPosition.animateTo(
                targetValue = scrollPosition.value + distance.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.92f,
                    stiffness = 12f
                )
            )
            delay(200)
            isShuffling = false
            hasResult = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(S.recipeRoulette, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(S.spinToPick, fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, S.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cookbook filter chips
            if (cookbooks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCookbookId == null,
                        onClick = {
                            if (!isShuffling) {
                                selectedCookbookId = null
                                hasResult = false
                                scope.launch { scrollPosition.snapTo(0f) }
                            }
                        },
                        label = { Text(S.allRecipes) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    cookbooks.forEach { cookbook ->
                        FilterChip(
                            selected = selectedCookbookId == cookbook.id,
                            onClick = {
                                if (!isShuffling) {
                                    selectedCookbookId = cookbook.id
                                    hasResult = false
                                    scope.launch { scrollPosition.snapTo(0f) }
                                }
                            },
                            label = { Text(cookbook.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            if (filteredRecipes.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(S.noRecipesToSpin, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Text(S.addSomeRecipes, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                }
            } else {
                // 3D Card Carousel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .pointerInput(isShuffling) {
                            if (!isShuffling) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        val snap = round(dragOffset).toFloat()
                                        val finalPos = scrollPosition.value + snap
                                        dragOffset = 0f
                                        scope.launch {
                                            scrollPosition.snapTo(finalPos)
                                            scrollPosition.animateTo(
                                                targetValue = round(scrollPosition.value),
                                                animationSpec = spring(
                                                    dampingRatio = 0.75f,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    },
                                    onDragCancel = {
                                        val snap = round(dragOffset).toFloat()
                                        dragOffset = 0f
                                        scope.launch { scrollPosition.snapTo(scrollPosition.value + snap) }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragOffset += -dragAmount / 75f
                                    }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    for (slot in 0 until slotCount) {
                        val recIdx = if (filteredRecipes.isNotEmpty()) slot % filteredRecipes.size else 0
                        val recipe = filteredRecipes[recIdx]

                        // Calculate relative position with wrapping
                        var pos = (slot.toFloat() - effectiveScroll) % slotCount.toFloat()
                        if (pos > slotCount / 2f) pos -= slotCount.toFloat()
                        if (pos < -slotCount / 2f) pos += slotCount.toFloat()
                        val absPos = abs(pos)

                        val fadeEdge = minOf(3.8f, slotCount / 2f - 0.5f)
                        val normalizedDist = absPos / maxOf(fadeEdge, 1f)

                        // Scale
                        val scale = maxOf(0.55f, 1f - 0.38f * normalizedDist.pow(0.85f))
                        // Y offset (cards sink away from center)
                        val yOffset = normalizedDist.pow(1.6f) * fadeEdge * 5f
                        // X offset (horizontal spread)
                        val xOffset = pos * 62f * (1f + absPos * 0.05f)
                        // Y-axis rotation (perspective tilt)
                        val yRotation = (-pos * 13f).coerceIn(-55f, 55f)
                        // X-axis tumble
                        val xRotation = (sin(pos.toDouble() * PI) * 10f).toFloat()
                        // Z lean
                        val zRotation = (pos * 1f).coerceIn(-5f, 5f)
                        // Opacity
                        val opacity = if (absPos >= fadeEdge) 0f
                        else maxOf(0f, 1f - absPos / fadeEdge).pow(1.5f)
                        // Shadow
                        val shadowRadius = maxOf(0f, 12f * (1f - normalizedDist))

                        if (opacity > 0.01f) {
                            CarouselCard(
                                recipe = recipe,
                                modifier = Modifier
                                    .width(210.dp)
                                    .height(290.dp)
                                    .zIndex(100f - absPos)
                                    .graphicsLayer {
                                        this.scaleX = scale
                                        this.scaleY = scale
                                        this.translationX = xOffset * density
                                        this.translationY = yOffset * density
                                        this.rotationY = yRotation
                                        this.rotationX = xRotation
                                        this.rotationZ = zRotation
                                        this.alpha = opacity
                                        this.shadowElevation = shadowRadius * density
                                        this.cameraDistance = 8f * density
                                    }
                            )
                        }
                    }
                }

                // Recipe info below carousel
                val currentIdx = recipeIndex(effectiveScroll)
                if (filteredRecipes.isNotEmpty()) {
                    val recipe = filteredRecipes[currentIdx]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 8.dp)
                    ) {
                        Text(
                            recipe.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!recipe.cookTime.isNullOrBlank()) {
                                Text(
                                    "\u23F1 ${recipe.cookTime}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                "${currentIdx + 1} / ${filteredRecipes.size}",
                                fontSize = 12.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Bottom buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasResult && filteredRecipes.isNotEmpty()) {
                    Button(
                        onClick = {
                            val idx = recipeIndex(scrollPosition.value)
                            navController.navigate(Screen.RecipeDetail.createRoute(filteredRecipes[idx].id))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.viewRecipe, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            hasResult = false
                            shuffle()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.spinAgain, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { shuffle() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(14.dp),
                        enabled = filteredRecipes.isNotEmpty() && !isShuffling
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.tapToSpin, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CarouselCard(recipe: Recipe, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column {
            // Image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                if (!recipe.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Primary.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // Title + calories
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    recipe.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                if (!recipe.calories.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        recipe.calories!!,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
