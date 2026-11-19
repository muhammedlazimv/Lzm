package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NeonCutDarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerCyan,
    onPrimaryContainer = OnPrimaryFixed,
    secondary = SecondaryAzure,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerBlue,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = TertiaryPink,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerPink,
    onTertiaryContainer = OnTertiaryContainer,
    error = ErrorRed,
    onError = OnErrorDark,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerLight,
    background = SurfaceDark,
    onBackground = OnSurfaceLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariantMuted,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = OutlineMuted,
    outlineVariant = OutlineVariantDark
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeonCutDarkColorScheme,
        typography = Typography,
        content = content
    )
}
