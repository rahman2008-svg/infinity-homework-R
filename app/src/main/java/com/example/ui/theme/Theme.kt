package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = InfinityOrange,
    secondary = InfinityBlue,
    tertiary = AccentGold,
    background = SlateDark,
    surface = SlateSurface,
    surfaceVariant = SlateCard,
    onPrimary = OnInfinityOrange,
    onSecondary = OnInfinityBlue,
    onBackground = OnSlateDark,
    onSurface = OnSlateSurface,
    onSurfaceVariant = OnSlateCard,
    error = RedError
)

private val LightColorScheme = lightColorScheme(
    primary = InfinityOrange,
    secondary = InfinityBlue,
    tertiary = AccentGold,
    background = OnSlateDark, // light theme matches
    surface = OnSlateSurface,
    surfaceVariant = OnSlateCard,
    onPrimary = OnInfinityOrange,
    onSecondary = OnInfinityBlue,
    onBackground = SlateDark,
    onSurface = SlateSurface,
    onSurfaceVariant = SlateCard,
    error = RedError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // We default to the gorgeous Professional Polish light theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
