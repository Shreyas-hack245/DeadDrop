package com.deaddrop.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF080914)
val Panel = Color(0xFF111323)
val PanelLight = Color(0xFF202238)
val Violet = Color(0xFF9B59FF)
val Cyan = Color(0xFF65D9FF)
val Muted = Color(0xFFA4A6B9)
val TextPrimary = Color(0xFFF5F3FF)
val Gold = Color(0xFFEAB66A)
val Forest = Color(0xFF9B59FF)
val Border = Color(0xFF302A46)
val SoftGreen = Color(0xFF251B38)
val ArtifactNight = Color(0xFF080914)

private val colors = darkColorScheme(
    primary = Violet, onPrimary = Color.White,
    secondary = Gold, onSecondary = Color(0xFF24180B),
    tertiary = Cyan, background = Ink, onBackground = TextPrimary,
    surface = Panel, onSurface = TextPrimary,
    surfaceVariant = PanelLight, onSurfaceVariant = Muted,
    outline = Border, error = Color(0xFFFF777E), onError = Color(0xFF350B10),
)

private val typography = androidx.compose.material3.Typography().copy(
    displayLarge = TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Normal,fontSize=42.sp,lineHeight=48.sp,letterSpacing=(-1.1).sp),
    headlineLarge = TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Normal,fontSize=32.sp,lineHeight=38.sp,letterSpacing=(-.45).sp),
    headlineMedium = TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Normal,fontSize=26.sp,lineHeight=32.sp),
    titleLarge = TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Normal,fontSize=22.sp,lineHeight=28.sp),
    titleMedium = TextStyle(fontFamily=FontFamily.Default,fontWeight=FontWeight.SemiBold,fontSize=16.sp,lineHeight=22.sp),
    bodyLarge = TextStyle(fontFamily=FontFamily.Default,fontSize=16.sp,lineHeight=24.sp),
    bodyMedium = TextStyle(fontFamily=FontFamily.Default,fontSize=14.sp,lineHeight=20.sp),
    labelLarge = TextStyle(fontFamily=FontFamily.Default,fontWeight=FontWeight.Medium,fontSize=14.sp,lineHeight=20.sp),
)

@Composable fun DeadDropTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=colors,typography=typography,content=content)
}
