package com.example.pullit.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.pullit.data.model.Recipe
import com.example.pullit.ui.theme.Primary

@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                if (recipe.imageUrl != null) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("\uD83C\uDF73", fontSize = 40.sp)
                        }
                    }
                }
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        if (recipe.favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (recipe.favorited) Primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (recipe.cookTime != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(recipe.cookTime!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
