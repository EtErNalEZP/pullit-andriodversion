package com.example.pullit.ui.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.data.model.RecipeSuggestion
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.components.MiniToasterView
import com.example.pullit.ui.navigation.Screen
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.AiRecommendViewModel
import com.example.pullit.viewmodel.AiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecommendScreen(
    navController: NavController,
    viewModel: AiRecommendViewModel = viewModel()
) {
    val S = LocalStrings.current
    val state by viewModel.state.collectAsState()
    val editableIngredients by viewModel.editableIngredients.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val localMatches by viewModel.localMatches.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    var newIngredient by remember { mutableStateOf("") }
    var selectedSuggestionIndex by remember { mutableIntStateOf(-1) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) viewModel.analyzeImage(bytes)
        }
    }

    // Suggestion detail bottom sheet
    if (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < suggestions.size) {
        val suggestion = suggestions[selectedSuggestionIndex]
        ModalBottomSheet(
            onDismissRequest = { selectedSuggestionIndex = -1 },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            SuggestionDetailSheet(
                suggestion = suggestion,
                onSave = {
                    viewModel.saveRecipeFromSuggestion(suggestion)
                    selectedSuggestionIndex = -1
                },
                onDismiss = { selectedSuggestionIndex = -1 }
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(S.aiRecommend, fontWeight = FontWeight.ExtraBold) }
            )
        }
    ) { padding ->
        when (state) {
            // ── Initial state ──
            AiState.IDLE -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            S.aiIngredientRecognition,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            S.aiIngredientHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(S.choosePhoto, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // ── Analyzing state - Toaster loading (matches iOS ToasterLoadingView) ──
            AiState.ANALYZING -> {
                ToasterLoadingScreen(
                    message = S.aiAnalyzing,
                    subtitle = S.processing,
                    modifier = Modifier.padding(padding)
                )
            }

            // ── Ingredients ready / Generating ──
            AiState.READY, AiState.GENERATING -> {
                if (state == AiState.GENERATING && suggestions.isEmpty()) {
                    // Generating state - Toaster loading (matches iOS ToasterLoadingView)
                    ToasterLoadingScreen(
                        message = S.aiGenerating,
                        subtitle = S.processing,
                        modifier = Modifier.padding(padding)
                    )
                } else if (suggestions.isNotEmpty()) {
                    // ── Suggestions ready (matches iOS suggestionsView) ──
                    LazyColumn(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    ) {
                        // Section header
                        item {
                            Text(
                                S.aiRecipeRecommend,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        itemsIndexed(suggestions) { index, suggestion ->
                            Surface(
                                onClick = { selectedSuggestionIndex = index },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        suggestion.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        suggestion.description,
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${S.ingredients}: ${suggestion.ingredients.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (suggestion.steps.isNotEmpty()) {
                                        Text(
                                            "${suggestion.steps.first()}...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }

                        // Retake button
                        item {
                            Button(
                                onClick = { viewModel.reset() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(S.retake)
                            }
                        }
                    }
                } else {
                    // ── Ingredients editing (matches iOS ingredientsView) ──
                    LazyColumn(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    ) {
                        // Section header
                        item {
                            Text(
                                "${S.ingredients} (${editableIngredients.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        // Editable ingredient rows (iOS List style)
                        itemsIndexed(editableIngredients) { index, ing ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    ing,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeIngredient(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.RemoveCircle,
                                        contentDescription = "Remove",
                                        tint = Error,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }

                        // Add ingredient row
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = newIngredient,
                                    onValueChange = { newIngredient = it },
                                    placeholder = { Text(S.addIngredient, color = TextTertiary) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = Primary
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (newIngredient.isNotBlank()) {
                                            viewModel.addIngredient(newIngredient.trim())
                                            newIngredient = ""
                                        }
                                    },
                                    enabled = newIngredient.isNotBlank(),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.AddCircle,
                                        contentDescription = "Add",
                                        tint = if (newIngredient.isNotBlank()) Primary else TextTertiary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Local matches section
                        if (localMatches.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "${S.matchesFromCollection} (${localMatches.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            items(localMatches) { match ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        match.title,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${(match.matchScore * 100).toInt()}% ${S.matchPercent}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Action buttons
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Confirm & generate
                                Button(
                                    onClick = { viewModel.generateSuggestions() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    enabled = editableIngredients.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(S.confirmIngredients, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // Retake
                                Button(
                                    onClick = { viewModel.reset() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(S.retake)
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            // ── Error state ──
            AiState.ERROR -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            S.somethingWentWrong,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        error?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.reset() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(S.tryAgain, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ── Toaster Loading Screen (matches iOS ToasterLoadingView) ──
@Composable
private fun ToasterLoadingScreen(
    message: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    // Animated dots cycling: "", ".", "..", "..."
    var dotCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = (dotCount + 1) % 4
        }
    }

    // Simulated progress bar (asymptotically approaches 95%)
    var progressValue by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = tween(400),
        label = "progress"
    )
    LaunchedEffect(Unit) {
        while (progressValue < 0.95f) {
            delay(800)
            val remaining = 0.95f - progressValue
            val increment = remaining * (0.03f + Math.random().toFloat() * 0.05f)
            progressValue = (progressValue + increment).coerceAtMost(0.95f)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            MiniToasterView(modifier = Modifier.size(200.dp))

            Spacer(modifier = Modifier.height(8.dp))

            // Message with animated dots
            Text(
                text = message + ".".repeat(dotCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar (matching iOS ToasterProgressBar)
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryLight, Primary)
                            )
                        )
                )
            }
        }
    }
}

// ── Suggestion Detail Bottom Sheet (matches iOS SuggestionDetailView) ──
@Composable
private fun SuggestionDetailSheet(
    suggestion: RecipeSuggestion,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val S = LocalStrings.current
    var saved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Description
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    suggestion.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    suggestion.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                suggestion.calories?.let { cal ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDD25", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            cal,
                            style = MaterialTheme.typography.labelMedium,
                            color = Warning
                        )
                    }
                }
            }
        }

        // Ingredients
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Primary.copy(alpha = 0.05f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        S.ingredients,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    suggestion.ingredients.forEach { ing ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Primary.copy(alpha = 0.3f),
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Text(ing, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Steps
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    S.steps,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                suggestion.steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            step,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Save button
        item {
            Button(
                onClick = {
                    onSave()
                    saved = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !saved,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (saved) Success else Primary,
                    disabledContainerColor = Success.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (saved) S.savedToRecipes else S.saveToRecipes,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
