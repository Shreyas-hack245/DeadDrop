package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.ChallengeType
import com.example.domain.model.Rarity
import com.example.ui.theme.AmberForge
import com.example.ui.theme.AmethystGlow
import com.example.ui.theme.ArcaneOutline
import com.example.ui.theme.ArcaneSurface
import com.example.ui.theme.ArcaneSurfaceElevated
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.DiscoveryCyan
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.RarityCommon
import com.example.ui.theme.RarityEpic
import com.example.ui.theme.RarityMythic
import com.example.ui.theme.RarityRare
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TreasureGold
import com.example.ui.theme.VoidBlack
import kotlin.math.abs

fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON -> RarityCommon
    Rarity.RARE -> RarityRare
    Rarity.EPIC -> RarityEpic
    Rarity.MYTHIC -> RarityMythic
}

@DrawableRes
fun artworkResForKey(key: String): Int = when (key) {
    "obsidian_chest" -> R.drawable.img_treasure_obsidian
    "celestial_astrolabe" -> R.drawable.img_artifact_astrolabe
    "shadow_lantern" -> R.drawable.img_artifact_lantern
    "splash_hero" -> R.drawable.img_splash_hero
    else -> R.drawable.img_treasure_obsidian
}

@Composable
fun DeadDropArtwork(
    imageKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    customPhotoUri: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (!customPhotoUri.isNullOrBlank()) {
        AsyncImage(
            model = customPhotoUri,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = painterResource(id = artworkResForKey(imageKey)),
            error = painterResource(id = artworkResForKey(imageKey))
        )
    } else {
        Image(
            painter = painterResource(id = artworkResForKey(imageKey)),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
fun RarityBadge(
    rarity: Rarity,
    modifier: Modifier = Modifier
) {
    val accent = rarityColor(rarity)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = rarity.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
fun XpBadge(
    xp: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = TreasureGold.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, TreasureGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = TreasureGold,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "+$xp XP",
                style = MaterialTheme.typography.labelMedium,
                color = TreasureGold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GoldBadge(
    gold: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = AmberForge.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, TreasureGold.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MonetizationOn,
                contentDescription = "Gold balance",
                tint = TreasureGold,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$gold",
                style = MaterialTheme.typography.labelLarge,
                color = TreasureGold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ChallengeTypeChip(
    type: ChallengeType,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        ChallengeType.GPS_PROXIMITY -> Icons.Rounded.Explore
        ChallengeType.QR_SCAN -> Icons.Rounded.QrCodeScanner
        ChallengeType.PUZZLE_ANSWER -> Icons.Rounded.AutoAwesome
        ChallengeType.COMBINED -> Icons.Rounded.TravelExplore
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = DiscoveryCyan.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, DiscoveryCyan.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = DiscoveryCyan, modifier = Modifier.size(12.dp))
            Text(
                text = type.label,
                style = MaterialTheme.typography.labelSmall,
                color = DiscoveryCyan
            )
        }
    }
}

@Composable
fun ArcaneCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ArcaneOutline,
    containerColor: Color = ArcaneSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            content = content
        )
    }
}

@Composable
fun ArcanePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    accentGradient: Brush = Brush.horizontalGradient(listOf(NeonViolet, DeepPurple))
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = ArcaneSurfaceElevated
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .background(
                    brush = if (enabled) accentGradient else Brush.horizontalGradient(
                        listOf(ArcaneSurfaceElevated, ArcaneSurfaceElevated)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (enabled) AmethystGlow.copy(alpha = 0.55f) else ArcaneOutline,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) TextPrimary else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GoldActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = ArcaneSurfaceElevated
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .background(
                    brush = if (enabled) {
                        Brush.horizontalGradient(listOf(TreasureGold, AmberForge))
                    } else {
                        Brush.horizontalGradient(listOf(ArcaneSurfaceElevated, ArcaneSurfaceElevated))
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = VoidBlack,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) VoidBlack else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ArcaneOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = ArcaneOutline
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MidnightIndigo.copy(alpha = 0.65f),
            contentColor = TextPrimary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ArcaneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    supportingText: String? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = TextMuted) }
        } else null,
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonViolet,
            unfocusedBorderColor = ArcaneOutline,
            focusedContainerColor = MidnightIndigo,
            unfocusedContainerColor = MidnightIndigo,
            focusedLabelColor = AmethystGlow,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Composable
fun LoadingOrb(
    message: String = "Channeling realm data...",
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonViolet.copy(alpha = 0.45f * pulse),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.7f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = AmethystGlow,
                trackColor = ArcaneSurfaceElevated,
                strokeWidth = 3.dp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyRealmState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ArcaneCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        borderColor = ArcaneOutlineSubtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NeonViolet.copy(alpha = 0.14f))
                    .border(1.dp, NeonViolet.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = AmethystGlow,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                ArcanePrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun QrMatrixGraphic(
    token: String,
    sizeDp: Dp = 140.dp,
    modifier: Modifier = Modifier
) {
    val gridSize = 15
    val seed = token.hashCode()
    Surface(
        modifier = modifier.size(sizeDp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF6F2FF),
        border = BorderStroke(2.dp, NeonViolet)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val cell = size.minDimension / gridSize
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val isFinder = (r < 4 && c < 4) ||
                        (r < 4 && c >= gridSize - 4) ||
                        (r >= gridSize - 4 && c < 4)
                    val filled = if (isFinder) {
                        val lr = if (r >= gridSize - 4) r - (gridSize - 4) else r
                        val lc = if (c >= gridSize - 4) c - (gridSize - 4) else c
                        lr == 0 || lr == 3 || lc == 0 || lc == 3 || (lr in 1..2 && lc in 1..2)
                    } else {
                        val hash = abs(seed * 31 + r * 17 + c * 13 + (token.getOrNull((r + c) % token.length.coerceAtLeast(1))?.code ?: 7))
                        hash % 2 == 0
                    }
                    if (filled) {
                        drawRect(
                            color = VoidBlack,
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell * 0.92f, cell * 0.92f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArcaneProximityRing(
    progress: Float,
    distanceText: String,
    inRange: Boolean,
    modifier: Modifier = Modifier
) {
    val ringColor = if (inRange) TreasureGold else DiscoveryCyan
    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = ArcaneSurfaceElevated,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ringColor, NeonViolet, ringColor)
                ),
                startAngle = -90f,
                sweepAngle = (progress.coerceIn(0.05f, 1f)) * 360f,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = distanceText,
                style = MaterialTheme.typography.headlineMedium,
                color = ringColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (inRange) "IN RANGE" else "DISTANCE",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

private val ArcaneOutlineSubtle = com.example.ui.theme.ArcaneOutlineSubtle
