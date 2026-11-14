package com.deaddrop.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF080B16)
val Panel = Color(0xFF111729)
val PanelLight = Color(0xFF18213A)
val Violet = Color(0xFF9B7BFF)
val Cyan = Color(0xFF64E7F2)
val Muted = Color(0xFF929BB5)
val TextPrimary = Color(0xFFF5F3FF)
val Gold = Color(0xFFFFD37A)

private val colors = darkColorScheme(primary = Violet, secondary = Cyan, tertiary = Gold, background = Ink, surface = Panel, onPrimary = Ink, onBackground = TextPrimary, onSurface = TextPrimary)

@Composable
fun DeadDropTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = androidx.compose.material3.Typography(), content = content)
}
