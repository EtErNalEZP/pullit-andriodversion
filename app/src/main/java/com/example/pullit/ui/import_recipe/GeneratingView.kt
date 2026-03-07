package com.example.pullit.ui.import_recipe

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pullit.data.model.RecipeGenerationProgress
import com.example.pullit.ui.theme.Primary

@Composable
fun GeneratingView(
    progress: RecipeGenerationProgress?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "rotation"
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("\uD83C\uDF73", fontSize = 60.sp, modifier = Modifier.rotate(rotation))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Generating Recipe...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            progress?.message ?: "Please wait...",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (progress?.elapsedSeconds != null && progress.elapsedSeconds > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("${progress.elapsedSeconds}s", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = Primary,
            trackColor = Primary.copy(alpha = 0.2f)
        )
    }
}
