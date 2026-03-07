package com.example.pullit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = TextSecondary,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceSecondaryLight,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    error = Error,
    onError = androidx.compose.ui.graphics.Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = TextSecondary,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceSecondaryDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    error = Error,
    onError = androidx.compose.ui.graphics.Color.White
)

@Composable
fun PullitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
