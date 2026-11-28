package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.location.GeoPoint
import com.example.data.location.LocationTracker
import com.example.domain.model.ChallengeType
import com.example.domain.model.Treasure
import com.example.ui.HuntSessionState
import com.example.ui.UnlockCelebration
import com.example.ui.components.ArcaneCard
import com.example.ui.components.ArcaneOutlinedButton
import com.example.ui.components.ArcanePrimaryButton
import com.example.ui.components.ArcaneProximityRing
import com.example.ui.components.ArcaneTextField
import com.example.ui.components.ChallengeTypeChip
import com.example.ui.components.DeadDropArtwork
import com.example.ui.components.GoldActionButton
import com.example.ui.components.GoldBadge
import com.example.ui.components.QrMatrixGraphic
import com.example.ui.components.RarityBadge
import com.example.ui.components.XpBadge
import com.example.ui.components.rarityColor
import com.example.ui.theme.AmberForge
import com.example.ui.theme.AmethystGlow
import com.example.ui.theme.ArcaneOutline
import com.example.ui.theme.ArcaneSurface
import com.example.ui.theme.ArcaneSurfaceElevated
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DiscoveryCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TreasureGold
import com.example.ui.theme.VoidBlack

@Composable
fun TreasureDetailAndHuntScreen(
    treasure: Treasure,
    currentLocation: GeoPoint,
    isAlreadyDiscovered: Boolean,
    huntSession: HuntSessionState,
    onBack: () -> Unit,
    onStartJourney: () -> Unit,
    onRevealHint: () -> Unit,
    onToggleSimulatedProximity: (Boolean) -> Unit,
    onRequestLocationUpdates: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onPuzzleInputChange: (String) -> Unit,
    onSubmitPuzzle: () -> Unit,
    onClaimReward: (Double) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            onRequestLocationUpdates()
        }
    }

    val realDistanceMeters = LocationTracker.distanceMeters(
        currentLocation.latitude, currentLocation.longitude,
        treasure.latitude, treasure.longitude
    )
    val inRealGpsRange = currentLocation.isGpsLive && realDistanceMeters <= treasure.radiusMeters
    val effectiveInRange = inRealGpsRange || huntSession.simulatedProximityActive

    val challengeType = treasure.challengeTypeEnum
    val requiresProximity = challengeType == ChallengeType.GPS_PROXIMITY || challengeType == ChallengeType.COMBINED
    val requiresQr = challengeType == ChallengeType.QR_SCAN || challengeType == ChallengeType.COMBINED
    val requiresPuzzle = challengeType == ChallengeType.PUZZLE_ANSWER || challengeType == ChallengeType.COMBINED

    val qrReady = !requiresQr || huntSession.qrVerified
    val puzzleReady = !requiresPuzzle || huntSession.puzzleSolved
    val proximityReady = !requiresProximity || effectiveInRange
    // For COMBINED, allow either QR or Puzzle alongside Proximity if creator set both, or require both when both configured
    val combinedReady = if (challengeType == ChallengeType.COMBINED) {
        val hasQr = !treasure.qrToken.isNullOrBlank()
        val hasPuzzle = !treasure.puzzleAnswer.isNullOrBlank()
        proximityReady && (!hasQr || huntSession.qrVerified) && (!hasPuzzle || huntSession.puzzleSolved)
    } else {
        proximityReady && qrReady && puzzleReady
    }

    val accent = rarityColor(treasure.rarityEnum)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(276.dp)
            ) {
                DeadDropArtwork(
                    imageKey = treasure.imageKey,
                    contentDescription = treasure.name,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    VoidBlack.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    VoidBlack
                                )
                            )
                        )
                )

                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(VoidBlack.copy(alpha = 0.68f))
                            .border(1.dp, ArcaneOutline, CircleShape)
                            .testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = VoidBlack.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, if (treasure.isRealmStarterDemo) ArcaneOutline else EmeraldSuccess)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (treasure.isRealmStarterDemo) Icons.Rounded.Explore else Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = if (treasure.isRealmStarterDemo) DiscoveryCyan else EmeraldSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (treasure.isRealmStarterDemo) "Realm Starter Drop" else "Live Cloud Drop",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (treasure.isRealmStarterDemo) DiscoveryCyan else EmeraldSuccess
                            )
                        }
                    }
                }

                // Bottom Overlay Badges
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RarityBadge(rarity = treasure.rarityEnum)
                    XpBadge(xp = treasure.xpReward)
                    ChallengeTypeChip(type = challengeType)
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title & Metadata
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = treasure.name,
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${treasure.categoryEnum.label} • ${treasure.difficultyEnum.label} • Forged by ${treasure.creatorName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = treasure.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }

                if (isAlreadyDiscovered) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldSuccess.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = EmeraldSuccess)
                            Text(
                                text = "You already discovered this relic! You may replay the trial anytime.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }

                // Cryptic Clue & Hint Card
                ArcaneCard(borderColor = NeonViolet.copy(alpha = 0.5f)) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AmethystGlow)
                            Text("Explorer's Clue", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        Text(
                            text = "“${treasure.clue}”",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        if (!treasure.hint.isNullOrBlank()) {
                            AnimatedVisibility(visible = huntSession.hintRevealed) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TreasureGold.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, TreasureGold.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "Hint: ${treasure.hint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TreasureGold,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            if (!huntSession.hintRevealed) {
                                TextButton(onClick = onRevealHint) {
                                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = TreasureGold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reveal Whispered Hint", color = TreasureGold)
                                }
                            }
                        }
                    }
                }

                // Reward Artifact Preview
                ArcaneCard(borderColor = accent.copy(alpha = 0.55f)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DeadDropArtwork(
                            imageKey = treasure.artifactImageKey,
                            contentDescription = treasure.artifactName,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("SEALED REWARD", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(treasure.artifactName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(treasure.artifactDescription, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }

                // Journey / Verification Section
                if (!huntSession.journeyStarted) {
                    ArcanePrimaryButton(
                        text = "Start Hunt Journey",
                        onClick = {
                            val tracker = LocationTracker(context)
                            if (!tracker.hasLocationPermission()) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                onRequestLocationUpdates()
                            }
                            onStartJourney()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_journey_button")
                    )
                } else {
                    // Active Hunt Console
                    ArcaneCard(
                        borderColor = DiscoveryCyan.copy(alpha = 0.55f),
                        containerColor = MidnightIndigo
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Active Trial", style = MaterialTheme.typography.titleMedium, color = DiscoveryCyan)
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = ArcaneSurfaceElevated
                                ) {
                                    Text(
                                        text = "Radius: ${treasure.radiusMeters}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (requiresProximity) {
                                val displayedDistance = if (huntSession.simulatedProximityActive) {
                                    "12m (SIM)"
                                } else if (!currentLocation.isGpsLive) {
                                    LocationTracker.formatDistance(realDistanceMeters)
                                } else {
                                    LocationTracker.formatDistance(realDistanceMeters)
                                }

                                val progress = if (huntSession.simulatedProximityActive) 1f
                                else (treasure.radiusMeters.toFloat() / realDistanceMeters.toFloat().coerceAtLeast(1f)).coerceIn(0.08f, 1f)

                                ArcaneProximityRing(
                                    progress = progress,
                                    distanceText = displayedDistance,
                                    inRange = effectiveInRange
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = if (currentLocation.isGpsLive) EmeraldSuccess else AmberForge,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (currentLocation.isGpsLive)
                                            "Live GPS Signal Active"
                                        else
                                            "GPS Permission / Signal Unavailable — Grant GPS or use Demo Simulation below",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                // Live tracking: compass to the treasure, your heading, mini map + place label
                                HuntTrackerPanel(
                                    treasure = treasure,
                                    currentLocation = currentLocation,
                                    distanceMeters = realDistanceMeters
                                )

                                // Explicit Demo Simulation Toggle (Clearly Labeled!)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = AmberForge.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, AmberForge.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Rounded.Science, contentDescription = null, tint = AmberForge)
                                            Column {
                                                Text(
                                                    "Demo GPS Proximity Simulation",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = AmberForge
                                                )
                                                Text(
                                                    "Simulated test mode (not real GPS)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = huntSession.simulatedProximityActive,
                                            onCheckedChange = onToggleSimulatedProximity,
                                            modifier = Modifier.testTag("demo_gps_switch"),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = VoidBlack,
                                                checkedTrackColor = AmberForge
                                            )
                                        )
                                    }
                                }
                            }

                            // QR Challenge Block
                            if (requiresQr && !treasure.qrToken.isNullOrBlank()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = AmethystGlow)
                                            Text("QR Seal Verification", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        }
                                        if (huntSession.qrVerified) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                                Text("Verified", style = MaterialTheme.typography.labelMedium, color = EmeraldSuccess)
                                            }
                                        }
                                    }

                                    if (treasure.isRealmStarterDemo) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            QrMatrixGraphic(token = treasure.qrToken, sizeDp = 72.dp)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Starter Seal Token:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                Text(treasure.qrToken, style = MaterialTheme.typography.labelMedium, color = TreasureGold)
                                            }
                                        }
                                    }

                                    ArcanePrimaryButton(
                                        text = if (huntSession.qrVerified) "QR Seal Verified ✓" else "Launch Arcane QR Scanner",
                                        onClick = onOpenQrScanner,
                                        enabled = !huntSession.qrVerified,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("launch_qr_scanner_button")
                                    )
                                }
                            }

                            // Puzzle Challenge Block
                            if (requiresPuzzle && !treasure.puzzleAnswer.isNullOrBlank()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.Key, contentDescription = null, tint = TreasureGold)
                                            Text("Arcane Cipher", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        }
                                        if (huntSession.puzzleSolved) {
                                            Text("Solved ✓", style = MaterialTheme.typography.labelMedium, color = EmeraldSuccess)
                                        }
                                    }

                                    if (!huntSession.puzzleSolved) {
                                        ArcaneTextField(
                                            value = huntSession.puzzleInput,
                                            onValueChange = onPuzzleInputChange,
                                            label = "Speak the Cipher Answer",
                                            placeholder = "Enter secret word...",
                                            isError = huntSession.puzzleError != null,
                                            supportingText = huntSession.puzzleError
                                        )
                                        ArcaneOutlinedButton(
                                            text = "Decrypt Seal",
                                            onClick = onSubmitPuzzle,
                                            borderColor = TreasureGold.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("submit_puzzle_button")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            GoldActionButton(
                                text = if (combinedReady) "Unlock & Claim Relic" else "Complete Trial Requirements",
                                onClick = { onClaimReward(realDistanceMeters) },
                                enabled = combinedReady,
                                isLoading = huntSession.isClaiming,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("claim_treasure_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnlockCelebrationDialog(
    celebration: UnlockCelebration,
    onDismiss: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "unlock")
    val glow by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "unlockGlow"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.88f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ArcaneCard(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                borderColor = TreasureGold
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (celebration.leveledUp) "LEVEL UP! RELIC UNLOCKED" else "TREASURE UNLOCKED",
                        style = MaterialTheme.typography.labelMedium,
                        color = TreasureGold,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            TreasureGold.copy(alpha = 0.45f * glow),
                                            NeonViolet.copy(alpha = 0.25f * glow),
                                            Color.Transparent
                                        )
                                    ),
                                    radius = size.minDimension * 0.72f
                                )
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .border(2.dp, TreasureGold, RoundedCornerShape(24.dp))
                    ) {
                        DeadDropArtwork(
                            imageKey = celebration.artifact.imageKey,
                            contentDescription = celebration.artifact.artifactName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    RarityBadge(rarity = celebration.artifact.rarityEnum)

                    Text(
                        text = celebration.artifact.artifactName,
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = celebration.artifact.artifactDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        XpBadge(xp = celebration.xpEarned)
                        GoldBadge(gold = celebration.goldBonus)
                    }

                    if (celebration.leveledUp) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonViolet.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, AmethystGlow.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "Ascended to Level ${celebration.newLevel}!",
                                style = MaterialTheme.typography.titleSmall,
                                color = AmethystGlow,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    GoldActionButton(
                        text = "Store in Relic Vault",
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("store_in_vault_button")
                    )
                }
            }
        }
    }
}
