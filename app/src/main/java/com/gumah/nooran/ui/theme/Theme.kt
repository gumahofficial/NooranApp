package com.gumah.nooran.ui.theme

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
    primary = SoftGold,
    onPrimary = DarkBackground,
    primaryContainer = EmeraldSurface,
    onPrimaryContainer = SoftGoldLight,
    secondary = SageGreen,
    onSecondary = DarkBackground,
    secondaryContainer = DarkCardElevated,
    onSecondaryContainer = MintSoft,
    tertiary = SoftGoldLight,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextLight,
    surface = DarkSurface,
    onSurface = TextLight,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSubtleLight,
    outline = BorderSoftDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = CardIvory,
    primaryContainer = SurfaceIvory,
    onPrimaryContainer = EmeraldDark,
    secondary = SageGreen,
    onSecondary = CardIvory,
    secondaryContainer = SurfaceIvory,
    onSecondaryContainer = EmeraldDark,
    tertiary = SoftGold,
    onTertiary = TextDark,
    background = WarmIvory,
    onBackground = TextDark,
    surface = CardIvory,
    onSurface = TextDark,
    surfaceVariant = SurfaceIvory,
    onSurfaceVariant = TextSubtleDark,
    outline = BorderSoftLight
)

@Composable
fun NooranTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NooranTypography,
        content = content
    )
}
