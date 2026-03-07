package com.example.pullit.ui.import_recipe

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.pullit.data.model.RecipeGenerationProgress
import com.example.pullit.ui.theme.*

@Composable
fun GeneratingRecipeCard(
    progress: RecipeGenerationProgress?,
    coverUrl: String?,
    title: String?,
    isCompleted: Boolean,
    generatedRecipeId: String?,
    onRetry: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isError = progress?.status == RecipeGenerationProgress.Status.ERROR

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isCompleted && generatedRecipeId != null) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image area (160dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Dim overlay when still generating
                    if (!isCompleted) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                        )
                    }
                } else {
                    // Animated gradient background
                    AnimatedGradientBackground()
                }

                // Cooking emoji animation overlay (when generating and no cover)
                if (!isCompleted && !isError) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedCookingEmoji()
                    }
                }

                // Error overlay
                if (isError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Error.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\u26A0\uFE0F", fontSize = 36.sp)
                    }
                }

                // NEW badge when completed
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Primary)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Info area
            Column(modifier = Modifier.padding(12.dp)) {
                // Title or status
                Text(
                    text = when {
                        isCompleted && title != null -> title
                        isError -> "Import Failed"
                        title != null -> title
                        else -> "Generating Recipe..."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isError) Error else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (!isCompleted) {
                    // Status message
                    Text(
                        text = when {
                            isError -> progress?.message ?: "Something went wrong"
                            else -> progress?.message ?: "Please wait..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) Error.copy(alpha = 0.8f) else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isError) {
                        // Retry button
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Primary
                            )
                        ) {
                            Text(
                                "Retry",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Progress bar
                        ProgressBar(progress = progress)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: RecipeGenerationProgress?) {
    val progressFraction = when (progress?.status) {
        RecipeGenerationProgress.Status.PENDING -> 0.05f
        RecipeGenerationProgress.Status.CHECKING_CONTENT -> 0.1f
        RecipeGenerationProgress.Status.DOWNLOADING -> 0.25f
        RecipeGenerationProgress.Status.EXTRACTING_AUDIO -> 0.4f
        RecipeGenerationProgress.Status.UPLOADING -> 0.5f
        RecipeGenerationProgress.Status.TRANSCRIBING -> 0.65f
        RecipeGenerationProgress.Status.GENERATING_RECIPE -> 0.85f
        RecipeGenerationProgress.Status.COMPLETED -> 1f
        RecipeGenerationProgress.Status.ERROR -> 0f
        null -> 0.05f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Primary.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Primary, PrimaryDark)
                    )
                )
        )
    }
}

@Composable
private fun AnimatedCookingEmoji() {
    val infiniteTransition = rememberInfiniteTransition(label = "cooking")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val emojiIndex by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3.99f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "emojiCycle"
    )

    val emojis = listOf("\uD83C\uDF73", "\uD83E\uDD58", "\uD83C\uDF72", "\uD83C\uDF5C")
    val currentEmoji = emojis[emojiIndex.toInt().coerceIn(0, emojis.lastIndex)]

    Text(
        currentEmoji,
        fontSize = (36 * scale).sp
    )
}

@Composable
private fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientX"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PrimaryLight.copy(alpha = 0.3f),
                        SurfaceSecondaryLight,
                        PrimaryLight.copy(alpha = 0.5f),
                        SurfaceSecondaryLight
                    ),
                    start = Offset(offsetX, 0f),
                    end = Offset(offsetX + 500f, 500f)
                )
            )
    )
}
