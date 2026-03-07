package com.example.pullit.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.pullit.data.model.Recipe
import com.example.pullit.ui.theme.Primary
import com.example.pullit.ui.theme.TextTertiary

/**
 * Determines the source platform display string based on the recipe's sourceUrl.
 */
private fun sourcePlatformLabel(sourceUrl: String?): String {
    if (sourceUrl == null) return "\uD83C\uDF7D\uFE0F Recipe"
    val lower = sourceUrl.lowercase()
    return when {
        lower.contains("xiaohongshu.com") || lower.contains("xhslink") -> "\uD83D\uDCD5 \u5C0F\u7EA2\u4E66"
        lower.contains("douyin.com") -> "\uD83C\uDFB5 \u6296\u97F3"
        lower.contains("bilibili.com") || lower.contains("b23.tv") -> "\uD83D\uDCFA B\u7AD9"
        lower.contains("xiachufang.com") -> "\uD83C\uDF73 \u4E0B\u53A8\u623F"
        lower.contains("tiktok.com") -> "\uD83C\uDFB5 TikTok"
        lower.contains("instagram.com") -> "\uD83D\uDCF7 Instagram"
        lower.contains("youtube.com") || lower.contains("youtu.be") -> "\u25B6\uFE0F YouTube"
        else -> "\uD83C\uDF7D\uFE0F Recipe"
    }
}

@Composable
fun RecipeCardView(
    recipe: Recipe,
    onTap: () -> Unit,
    onFavoriteTap: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = cardShape,
                ambientColor = Color(0x0F3D2C1E),
                spotColor = Color(0x0F3D2C1E)
            )
            .clip(cardShape)
            .clickable(onClick = onTap)
            .then(
                if (isSelected) Modifier.border(2.dp, Primary, cardShape)
                else Modifier
            ),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image section - 160dp height, clipped to top rounded corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
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
                    // Gradient placeholder with fork.knife-like icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Primary.copy(alpha = 0.3f),
                                        Primary.copy(alpha = 0.6f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                // Overlay: Top right - Favorite heart button (circle background with semi-transparency)
                if (onFavoriteTap != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable(onClick = onFavoriteTap),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (recipe.favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(18.dp),
                            tint = if (recipe.favorited) Color(0xFFFF6B6B) else Color.White
                        )
                    }
                }

                // Overlay: Bottom left - Cook time badge (capsule shape, semi-transparent background)
                if (!recipe.cookTime.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "\uD83D\uDD50 ${recipe.cookTime}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Info section - 10dp padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Title: 15sp, fontWeight SemiBold, maxLines 2, minHeight 38dp
                Text(
                    text = recipe.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom row: source platform (left) and calories (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Source platform indicator
                    Text(
                        text = sourcePlatformLabel(recipe.sourceUrl),
                        fontSize = 11.sp,
                        color = TextTertiary,
                        maxLines = 1
                    )

                    // Right: Calories with fire icon
                    if (!recipe.calories.isNullOrBlank()) {
                        Text(
                            text = "\uD83D\uDD25 ${recipe.calories}",
                            fontSize = 11.sp,
                            color = Primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
