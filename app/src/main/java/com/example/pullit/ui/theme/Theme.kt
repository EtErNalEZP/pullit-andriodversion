package com.example.pullit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.pullit.data.AppAppearance
import com.example.pullit.data.AppLanguage
import com.example.pullit.data.AppSettings
import com.example.pullit.ui.ChineseStrings
import com.example.pullit.ui.EnglishStrings
import com.example.pullit.ui.LocalStrings
import java.util.Locale

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
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settings = AppSettings.getInstance(context)
    val appearance by settings.appearance.collectAsState()
    val language by settings.language.collectAsState()

    val darkTheme = when (appearance) {
        AppAppearance.LIGHT -> false
        AppAppearance.DARK -> true
        AppAppearance.SYSTEM -> isSystemInDarkTheme()
    }

    val strings = when (language) {
        AppLanguage.SYSTEM -> {
            val systemLang = Locale.getDefault().language
            if (systemLang == "zh") ChineseStrings else EnglishStrings
        }
        AppLanguage.CHINESE -> ChineseStrings
        AppLanguage.ENGLISH -> EnglishStrings
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalStrings provides strings) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
