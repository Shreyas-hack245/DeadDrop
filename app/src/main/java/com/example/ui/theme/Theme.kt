package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DeadDropDarkColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = VoidBlack,
    primaryContainer = DeepPurple,
    onPrimaryContainer = AmethystGlow,
    secondary = TreasureGold,
    onSecondary = VoidBlack,
    secondaryContainer = ColorTokens.GoldContainer,
    onSecondaryContainer = WarmGoldSoft,
    tertiary = DiscoveryCyan,
    onTertiary = VoidBlack,
    tertiaryContainer = ColorTokens.CyanContainer,
    onTertiaryContainer = DiscoveryCyan,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = MidnightIndigo,
    onSurface = TextPrimary,
    surfaceVariant = ArcaneSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = VoidBlack,
    surfaceContainerLow = MidnightIndigo,
    surfaceContainer = ArcaneSurface,
    surfaceContainerHigh = ArcaneSurfaceElevated,
    outline = ArcaneOutline,
    outlineVariant = ArcaneOutlineSubtle,
    error = CrimsonDanger,
    onError = VoidBlack
)

private object ColorTokens {
    val GoldContainer = androidx.compose.ui.graphics.Color(0xFF3A2900)
    val CyanContainer = androidx.compose.ui.graphics.Color(0xFF002C36)
}

@Composable
fun DeadDropTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DeadDropDarkColorScheme,
        typography = DeadDropTypography,
        shapes = DeadDropShapes,
        content = content
    )
}
