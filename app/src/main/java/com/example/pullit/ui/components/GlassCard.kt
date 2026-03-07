package com.example.pullit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.shadow(
            elevation = 6.dp,
            shape = RoundedCornerShape(radius),
            ambientColor = Color(0x0F3D2C1E),
            spotColor = Color(0x0F3D2C1E)
        ),
        shape = RoundedCornerShape(radius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        content = { content() }
    )
}
