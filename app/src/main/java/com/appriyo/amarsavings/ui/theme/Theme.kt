package com.appriyo.amarsavings.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = Gray950,

    primaryContainer = DarkContainerPrimary,
    onPrimaryContainer = Indigo200,

    inversePrimary = Indigo600,

    secondary = Violet400,
    onSecondary = Gray950,

    secondaryContainer = DarkContainerSecondary,
    onSecondaryContainer = Violet400,

    tertiary = Amber400,
    onTertiary = Gray950,

    tertiaryContainer = DarkContainerTertiary,
    onTertiaryContainer = Amber300,

    error = Coral400,
    onError = Gray950,

    errorContainer = DarkContainerError,
    onErrorContainer = Coral300,

    background = SurfaceDarkBase,
    onBackground = Gray50,

    surface = SurfaceDark,
    onSurface = Gray50,

    surfaceVariant = SurfaceDarkHigh,
    onSurfaceVariant = Gray300,

    surfaceTint = Indigo400,
    inverseSurface = Gray50,
    inverseOnSurface = Gray900,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo500,
    onPrimary = White,

    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo800,

    inversePrimary = Indigo300,

    secondary = Violet500,
    onSecondary = White,

    secondaryContainer = LightContainerSecondary,
    onSecondaryContainer = Violet600,

    tertiary = Amber500,
    onTertiary = White,

    tertiaryContainer = LightContainerTertiary,
    onTertiaryContainer = LightOnTertiaryContainer,

    error = Coral500,
    onError = White,

    errorContainer = Coral100,
    onErrorContainer = Coral600,

    background = SurfaceLightBase,
    onBackground = Gray900,

    surface = SurfaceLight,
    onSurface = Gray900,

    surfaceVariant = SurfaceLightHigh,
    onSurfaceVariant = Gray500,

    surfaceTint = Indigo500,
    inverseSurface = Gray900,
    inverseOnSurface = Gray50,

    outline = Gray200,
    outlineVariant = Gray100,
    scrim = Color(0x66000000)
)

@Composable
fun AmarSavingsTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

// Encodes a dark/light preference for persistence. Values must stay stable
// (they are written to DataStore) and align with AppPreferences.THEME_*.
internal fun encodeThemeMode(isDark: Boolean): String =
    if (isDark) "dark" else "light"

// Decodes a stored preference value. Anything other than "dark" is treated as light,
// so a missing/unknown value falls back to light.
internal fun decodeThemeMode(value: String?): Boolean = value == "dark"