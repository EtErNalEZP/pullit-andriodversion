package com.example.pullit.ui.cooking

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pullit.data.local.AppDatabase
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.theme.*
import com.example.pullit.viewmodel.CookingModeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    recipeId: String,
    navController: NavController,
    viewModel: CookingModeViewModel = viewModel()
) {
    val S = LocalStrings.current
    val context = LocalContext.current
    val steps by viewModel.steps.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val recipeTitle by viewModel.recipeTitle.collectAsState()

    var showTimerDialog by remember { mutableStateOf(false) }
    var timerMinutesInput by remember { mutableStateOf("5") }
    var timerSecondsInput by remember { mutableStateOf("0") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(recipeId) {
        val db = AppDatabase.getInstance(context)
        val recipe = db.recipeDao().getById(recipeId)
        if (recipe != null) {
            viewModel.setup(recipe.title, recipe.stepsJson, recipe.ingredientsJson)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { steps.size.coerceAtLeast(1) }
    )

    // Sync pager with viewmodel
    LaunchedEffect(currentStepIndex) {
        if (pagerState.currentPage != currentStepIndex && currentStepIndex in 0 until pagerState.pageCount) {
            pagerState.animateScrollToPage(currentStepIndex)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentStepIndex) {
            viewModel.goToStep(pagerState.currentPage)
        }
    }

    // Timer picker dialog
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            containerColor = CookingCard,
            titleContentColor = CookingText,
            title = { Text(S.setTimer, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = timerMinutesInput,
                        onValueChange = { timerMinutesInput = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text(S.minutes, color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CookingText,
                            unfocusedTextColor = CookingText,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextSecondary
                        )
                    )
                    OutlinedTextField(
                        value = timerSecondsInput,
                        onValueChange = { timerSecondsInput = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text(S.seconds, color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CookingText,
                            unfocusedTextColor = CookingText,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextSecondary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val mins = timerMinutesInput.toIntOrNull() ?: 0
                    val secs = timerSecondsInput.toIntOrNull() ?: 0
                    val totalSecs = mins * 60 + secs
                    if (totalSecs > 0) viewModel.startTimer(totalSecs)
                    showTimerDialog = false
                }) { Text(S.start, color = Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text(S.cancel, color = TextSecondary)
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CookingBg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CookingText)
                }
                Text(
                    recipeTitle,
                    color = CookingText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }

            // Progress bar
            val progress by animateFloatAsState(
                targetValue = if (steps.isNotEmpty()) (currentStepIndex + 1).toFloat() / steps.size else 0f,
                animationSpec = tween(300),
                label = "progress"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.2f)
            )

            // Step counter
            Text(
                "${currentStepIndex + 1} / ${steps.size}",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // HorizontalPager for swiping between steps
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 16.dp
            ) { page ->
                if (page in steps.indices) {
                    val step = steps[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Large step number circle
                        Surface(
                            shape = CircleShape,
                            color = Primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${page + 1}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Step instruction
                        Text(
                            step.instruction,
                            color = CookingText,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 30.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Step images
                        if (step.imageUrls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            CookingStepImages(step.imageUrls)
                        }
                    }
                }
            }

            // Timer section
            if (timerSeconds > 0 || isTimerRunning) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = CookingCard.copy(alpha = 0.8f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large monospaced timer display
                        Text(
                            "%02d:%02d".format(timerSeconds / 60, timerSeconds % 60),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (timerSeconds < 30) Error else Primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Timer controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play/Pause
                            IconButton(
                                onClick = {
                                    if (isTimerRunning) viewModel.pauseTimer()
                                    else viewModel.resumeTimer()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        Primary.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isTimerRunning) S.pause else S.play,
                                    tint = Primary
                                )
                            }

                            // Reset
                            IconButton(
                                onClick = { viewModel.resetTimer() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        CookingText.copy(alpha = 0.1f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = S.reset,
                                    tint = CookingText
                                )
                            }
                        }
                    }
                }
            }

            // Bottom controls: Previous / Timer / Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                Surface(
                    onClick = {
                        viewModel.previousStep()
                        scope.launch { pagerState.animateScrollToPage(currentStepIndex - 1) }
                    },
                    enabled = currentStepIndex > 0,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    color = if (currentStepIndex > 0) CookingCard.copy(alpha = 0.8f) else CookingCard.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            tint = if (currentStepIndex > 0) CookingText else CookingText.copy(alpha = 0.3f)
                        )
                    }
                }

                // Timer button
                Surface(
                    onClick = { showTimerDialog = true },
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    color = CookingCard.copy(alpha = 0.8f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = Primary
                        )
                    }
                }

                // Next / Done button
                if (currentStepIndex < steps.size - 1) {
                    Surface(
                        onClick = {
                            viewModel.nextStep()
                            scope.launch { pagerState.animateScrollToPage(currentStepIndex + 1) }
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp),
                        color = Primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = { navController.popBackStack() },
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp),
                        color = Success
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CookingStepImages(imageUrls: List<String>) {
    val shape = RoundedCornerShape(12.dp)
    when (imageUrls.size) {
        1 -> {
            AsyncImage(
                model = imageUrls[0],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(shape)
            )
        }
        2 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                imageUrls.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp)
                            .clip(shape)
                    )
                }
            }
        }
        3 -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(
                    model = imageUrls[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(shape)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..2) {
                        AsyncImage(
                            model = imageUrls[i],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(shape)
                        )
                    }
                }
            }
        }
        4 -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 0..1) {
                        AsyncImage(
                            model = imageUrls[i],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(shape)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 2..3) {
                        AsyncImage(
                            model = imageUrls[i],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(shape)
                        )
                    }
                }
            }
        }
    }
}
