package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.location.GeoPoint
import com.example.domain.model.ChallengeType
import com.example.domain.model.Difficulty
import com.example.domain.model.Rarity
import com.example.domain.model.TreasureCategory
import com.example.ui.CreateTreasureDraft
import com.example.ui.components.ArcaneCard
import com.example.ui.components.ArcaneOutlinedButton
import com.example.ui.components.ArcanePrimaryButton
import com.example.ui.components.ArcaneTextField
import com.example.ui.components.ChallengeTypeChip
import com.example.ui.components.DeadDropArtwork
import com.example.ui.components.GoldActionButton
import com.example.ui.components.QrMatrixGraphic
import com.example.ui.components.RarityBadge
import com.example.ui.components.XpBadge
import com.example.ui.components.rarityColor
import com.example.ui.theme.AmethystGlow
import com.example.ui.theme.ArcaneOutline
import com.example.ui.theme.ArcaneSurface
import com.example.ui.theme.ArcaneSurfaceElevated
import com.example.ui.theme.DiscoveryCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TreasureGold
import com.example.ui.theme.VoidBlack
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.cos

private val stepTitles = listOf(
    "1. Treasure Info",
    "2. Location Pin",
    "3. Challenge Seal",
    "4. Artifact Reward",
    "5. Realm Preview",
    "6. Publish Drop"
)

private val artworkKeys = listOf(
    "obsidian_chest" to "Obsidian Chest",
    "celestial_astrolabe" to "Astrolabe",
    "shadow_lantern" to "Soul Lantern",
    "splash_hero" to "Shadow Portal"
)

@Composable
fun CreateTreasureScreen(
    draft: CreateTreasureDraft,
    currentLocation: GeoPoint,
    onUpdateDraft: ((CreateTreasureDraft) -> CreateTreasureDraft) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onPublish: () -> Unit,
    onDoneAndClose: () -> Unit
) {
    BackHandler {
        if (draft.publishedTreasureId != null || draft.step == 0) {
            onDoneAndClose()
        } else {
            onUpdateDraft { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateDraft { it.copy(customPhotoUri = uri.toString()) }
        }
    }

    val canAdvance = when (draft.step) {
        0 -> draft.name.trim().length >= 2 && draft.description.trim().length >= 5
        1 -> draft.radiusMeters in 10..1000
        2 -> draft.clue.trim().length >= 3 &&
            (draft.challengeType != ChallengeType.PUZZLE_ANSWER || draft.puzzleAnswer.trim().isNotEmpty())
        3 -> draft.artifactName.trim().length >= 2 && draft.artifactDescription.trim().length >= 5
        else -> true
    }

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
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Wizard Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (draft.publishedTreasureId != null || draft.step == 0) onDoneAndClose()
                        else onUpdateDraft { it.copy(step = it.step - 1) }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ArcaneSurface)
                        .border(1.dp, ArcaneOutline, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FORGE DEADDROP", style = MaterialTheme.typography.labelMedium, color = TreasureGold)
                    Text(stepTitles[draft.step.coerceIn(0, 5)], style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ArcaneSurface,
                    border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${draft.step + 1}/6",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmethystGlow,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // 6-Segment Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(6) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    idx < draft.step -> EmeraldSuccess
                                    idx == draft.step -> NeonViolet
                                    else -> ArcaneSurfaceElevated
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (draft.step) {
                    0 -> StepOneTreasureInfo(
                        draft = draft,
                        onUpdateDraft = onUpdateDraft,
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    1 -> StepTwoLocationPicker(
                        draft = draft,
                        currentLocation = currentLocation,
                        onUpdateDraft = onUpdateDraft,
                        onUseCurrentLocation = onUseCurrentLocation
                    )
                    2 -> StepThreeChallengeConfig(
                        draft = draft,
                        onUpdateDraft = onUpdateDraft
                    )
                    3 -> StepFourArtifactReward(
                        draft = draft,
                        onUpdateDraft = onUpdateDraft
                    )
                    4 -> StepFivePreview(draft = draft)
                    else -> StepSixPublish(
                        draft = draft,
                        onPublish = onPublish,
                        onDoneAndClose = onDoneAndClose
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Stepper Navigation
            if (draft.publishedTreasureId == null && draft.step < 5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightIndigo)
                        .border(1.dp, ArcaneOutline)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (draft.step > 0) {
                        ArcaneOutlinedButton(
                            text = "Back",
                            onClick = { onUpdateDraft { it.copy(step = it.step - 1) } },
                            modifier = Modifier.weight(0.4f)
                        )
                    }
                    ArcanePrimaryButton(
                        text = if (draft.step == 4) "Proceed to Publish" else "Next Step",
                        onClick = { onUpdateDraft { it.copy(step = (it.step + 1).coerceAtMost(5)) } },
                        enabled = canAdvance,
                        modifier = Modifier
                            .weight(if (draft.step > 0) 0.6f else 1f)
                            .testTag("wizard_next_button")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepOneTreasureInfo(
    draft: CreateTreasureDraft,
    onUpdateDraft: ((CreateTreasureDraft) -> CreateTreasureDraft) -> Unit,
    onPickPhoto: () -> Unit
) {
    ArcaneTextField(
        value = draft.name,
        onValueChange = { v -> onUpdateDraft { it.copy(name = v.take(80)) } },
        label = "Treasure Name *",
        placeholder = "e.g., Obsidian Lantern Cache",
        supportingText = "${draft.name.trim().length}/80 characters (min 2)",
        modifier = Modifier.testTag("wizard_name_input")
    )

    ArcaneTextField(
        value = draft.description,
        onValueChange = { v -> onUpdateDraft { it.copy(description = v.take(500)) } },
        label = "Lore & Description *",
        placeholder = "Describe the hidden site and legend behind this drop...",
        singleLine = false,
        minLines = 3,
        supportingText = "${draft.description.trim().length}/500 characters (min 5)",
        modifier = Modifier.testTag("wizard_desc_input")
    )

    Text("Drop Artwork", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        artworkKeys.forEach { (key, label) ->
            val selected = draft.imageKey == key && draft.customPhotoUri == null
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) TreasureGold else ArcaneOutline,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(ArcaneSurface)
                    .clickable { onUpdateDraft { it.copy(imageKey = key, customPhotoUri = null) } }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DeadDropArtwork(
                    imageKey = key,
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            }
        }
    }

    ArcaneOutlinedButton(
        text = if (draft.customPhotoUri != null) "Custom Photo Selected ✓ (Tap to change)" else "Optional: Attach Local Photo Preview",
        onClick = onPickPhoto,
        modifier = Modifier.fillMaxWidth()
    )

    Text("Category", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TreasureCategory.entries.forEach { cat ->
            FilterChip(
                selected = draft.category == cat,
                onClick = { onUpdateDraft { it.copy(category = cat) } },
                label = { Text(cat.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonViolet.copy(alpha = 0.28f),
                    selectedLabelColor = AmethystGlow
                )
            )
        }
    }

    Text("Difficulty", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Difficulty.entries.forEach { diff ->
            FilterChip(
                selected = draft.difficulty == diff,
                onClick = { onUpdateDraft { it.copy(difficulty = diff) } },
                label = { Text(diff.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DiscoveryCyan.copy(alpha = 0.22f),
                    selectedLabelColor = DiscoveryCyan
                )
            )
        }
    }

    Text("Rarity", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Rarity.entries.forEach { rar ->
            val accent = rarityColor(rar)
            FilterChip(
                selected = draft.rarity == rar,
                onClick = { onUpdateDraft { it.copy(rarity = rar, artifactRarity = rar) } },
                label = { Text(rar.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.24f),
                    selectedLabelColor = accent
                )
            )
        }
    }
}

@Composable
private fun StepTwoLocationPicker(
    draft: CreateTreasureDraft,
    currentLocation: GeoPoint,
    onUpdateDraft: ((CreateTreasureDraft) -> CreateTreasureDraft) -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    val useGoogleMaps = remember { hasConfiguredMapsApiKey() }

    ArcaneOutlinedButton(
        text = "Snap Pin to My Current GPS Location",
        onClick = onUseCurrentLocation,
        borderColor = DiscoveryCyan.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    )

    Text("Tap on the Map to Place Drop Pin", style = MaterialTheme.typography.titleSmall, color = TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, ArcaneOutline, RoundedCornerShape(20.dp))
    ) {
        if (useGoogleMaps) {
            val pos = LatLng(draft.latitude, draft.longitude)
            val cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(pos, 16f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                onMapClick = { clicked ->
                    onUpdateDraft { it.copy(latitude = clicked.latitude, longitude = clicked.longitude) }
                }
            ) {
                Circle(
                    center = pos,
                    radius = draft.radiusMeters.toDouble(),
                    fillColor = NeonViolet.copy(alpha = 0.18f),
                    strokeColor = TreasureGold,
                    strokeWidth = 3f
                )
                Marker(state = MarkerState(position = pos), title = "Treasure Pin")
            }
        } else {
            val cosLat = cos(Math.toRadians(currentLocation.latitude)).coerceAtLeast(0.2)
            val pixelsPerMeter = 0.5f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MidnightIndigo)
                    .pointerInput(currentLocation) {
                        detectTapGestures { tap ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dxMeters = (tap.x - cx) / pixelsPerMeter
                            val dyMeters = (cy - tap.y) / pixelsPerMeter
                            val newLat = (currentLocation.latitude + dyMeters / 111111.0).coerceIn(-89.9, 89.9)
                            val newLon = (currentLocation.longitude + dxMeters / (111111.0 * cosLat)).coerceIn(-179.9, 179.9)
                            onUpdateDraft { it.copy(latitude = newLat, longitude = newLon) }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    // Grid
                    for (i in -4..4) {
                        drawLine(ArcaneOutline.copy(alpha = 0.2f), Offset(cx + i * 50.dp.toPx(), 0f), Offset(cx + i * 50.dp.toPx(), size.height))
                        drawLine(ArcaneOutline.copy(alpha = 0.2f), Offset(0f, cy + i * 50.dp.toPx()), Offset(size.width, cy + i * 50.dp.toPx()))
                    }
                    // Current user beacon
                    drawCircle(DiscoveryCyan.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(DiscoveryCyan, radius = 6.dp.toPx(), center = Offset(cx, cy))

                    // Pin position
                    val dyM = (draft.latitude - currentLocation.latitude) * 111111.0
                    val dxM = (draft.longitude - currentLocation.longitude) * 111111.0 * cosLat
                    val pin = Offset(cx + (dxM * pixelsPerMeter).toFloat(), cy - (dyM * pixelsPerMeter).toFloat())
                    drawCircle(NeonViolet.copy(alpha = 0.22f), radius = (draft.radiusMeters * pixelsPerMeter).coerceAtLeast(18.dp.toPx()), center = pin)
                    drawCircle(TreasureGold, radius = 12.dp.toPx(), center = pin, style = Stroke(2.dp.toPx()))
                    drawCircle(TreasureGold, radius = 7.dp.toPx(), center = pin)
                }
                Text(
                    text = "Tap anywhere to relocate drop pin",
                    style = MaterialTheme.typography.labelSmall,
                    color = DiscoveryCyan,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(VoidBlack.copy(alpha = 0.75f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }

    ArcaneCard(borderColor = ArcaneOutline) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = TreasureGold, modifier = Modifier.size(18.dp))
                Text(
                    text = String.format("Pin: %.5f, %.5f", draft.latitude, draft.longitude),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
            }
            Text("Discovery Radius: ${draft.radiusMeters} meters", style = MaterialTheme.typography.titleSmall, color = DiscoveryCyan)
            Slider(
                value = draft.radiusMeters.toFloat(),
                onValueChange = { v -> onUpdateDraft { it.copy(radiusMeters = v.toInt()) } },
                valueRange = 20f..300f,
                colors = SliderDefaults.colors(thumbColor = TreasureGold, activeTrackColor = NeonViolet)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepThreeChallengeConfig(
    draft: CreateTreasureDraft,
    onUpdateDraft: ((CreateTreasureDraft) -> CreateTreasureDraft) -> Unit
) {
    Text("Select Verification Trial", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChallengeType.entries.forEach { type ->
            FilterChip(
                selected = draft.challengeType == type,
                onClick = { onUpdateDraft { it.copy(challengeType = type) } },
                label = { Text(type.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DiscoveryCyan.copy(alpha = 0.24f),
                    selectedLabelColor = DiscoveryCyan
                )
            )
        }
    }

    ArcaneTextField(
        value = draft.clue,
        onValueChange = { v -> onUpdateDraft { it.copy(clue = v.take(350)) } },
        label = "Explorer's Clue *",
        placeholder = "Write the riddle or directional clue for seekers...",
        singleLine = false,
        minLines = 2,
        supportingText = "${draft.clue.trim().length}/350 characters (min 3)",
        modifier = Modifier.testTag("wizard_clue_input")
    )

    ArcaneTextField(
        value = draft.hint,
        onValueChange = { v -> onUpdateDraft { it.copy(hint = v.take(250)) } },
        label = "Optional Whispered Hint",
        placeholder = "Shown only if the explorer requests a hint..."
    )

    if (draft.challengeType == ChallengeType.QR_SCAN || draft.challengeType == ChallengeType.COMBINED) {
        ArcaneCard(borderColor = NeonViolet.copy(alpha = 0.5f)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("QR Seal Payload", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    IconButton(onClick = {
                        onUpdateDraft { it.copy(qrToken = "DEADDROP-${(1000..9999).random()}") }
                    }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Regenerate QR Token", tint = AmethystGlow)
                    }
                }
                ArcaneTextField(
                    value = draft.qrToken,
                    onValueChange = { v -> onUpdateDraft { it.copy(qrToken = v.take(120)) } },
                    label = "QR Seal Token"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    QrMatrixGraphic(token = draft.qrToken.ifBlank { "DEADDROP" }, sizeDp = 112.dp)
                }
            }
        }
    }

    if (draft.challengeType == ChallengeType.PUZZLE_ANSWER || draft.challengeType == ChallengeType.COMBINED) {
        ArcaneTextField(
            value = draft.puzzleAnswer,
            onValueChange = { v -> onUpdateDraft { it.copy(puzzleAnswer = v.take(120)) } },
            label = if (draft.challengeType == ChallengeType.PUZZLE_ANSWER) "Secret Answer *" else "Optional Cipher Answer",
            placeholder = "e.g., OBSIDIAN"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepFourArtifactReward(
    draft: CreateTreasureDraft,
    onUpdateDraft: ((CreateTreasureDraft) -> CreateTreasureDraft) -> Unit
) {
    ArcaneTextField(
        value = draft.artifactName,
        onValueChange = { v -> onUpdateDraft { it.copy(artifactName = v.take(80)) } },
        label = "Reward Artifact Name *",
        placeholder = "e.g., Relic of the Eclipse",
        modifier = Modifier.testTag("wizard_artifact_name_input")
    )

    ArcaneTextField(
        value = draft.artifactDescription,
        onValueChange = { v -> onUpdateDraft { it.copy(artifactDescription = v.take(350)) } },
        label = "Artifact Lore *",
        placeholder = "Describe the mystical artifact added to the finder's Vault...",
        singleLine = false,
        minLines = 2,
        modifier = Modifier.testTag("wizard_artifact_desc_input")
    )

    Text("Artifact Artwork", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        artworkKeys.forEach { (key, label) ->
            val selected = draft.artifactImageKey == key
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) TreasureGold else ArcaneOutline,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(ArcaneSurface)
                    .clickable { onUpdateDraft { it.copy(artifactImageKey = key) } }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DeadDropArtwork(
                    imageKey = key,
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            }
        }
    }

    Text("Artifact Rarity", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Rarity.entries.forEach { rar ->
            val accent = rarityColor(rar)
            FilterChip(
                selected = draft.artifactRarity == rar,
                onClick = { onUpdateDraft { it.copy(artifactRarity = rar) } },
                label = { Text(rar.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.24f),
                    selectedLabelColor = accent
                )
            )
        }
    }

    ArcaneCard(borderColor = TreasureGold.copy(alpha = 0.45f)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("XP Reward", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                XpBadge(xp = draft.xpReward.toLong())
            }
            Slider(
                value = draft.xpReward.toFloat(),
                onValueChange = { v -> onUpdateDraft { it.copy(xpReward = (v.toInt() / 25) * 25) } },
                valueRange = 50f..2500f,
                colors = SliderDefaults.colors(thumbColor = TreasureGold, activeTrackColor = TreasureGold)
            )
        }
    }
}

@Composable
private fun StepFivePreview(draft: CreateTreasureDraft) {
    Text("Realm Card Preview", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    ArcaneCard(borderColor = rarityColor(draft.rarity)) {
        Column {
            DeadDropArtwork(
                imageKey = draft.imageKey,
                customPhotoUri = draft.customPhotoUri,
                contentDescription = draft.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RarityBadge(rarity = draft.rarity)
                    XpBadge(xp = draft.xpReward.toLong())
                    ChallengeTypeChip(type = draft.challengeType)
                }
                Text(draft.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    text = "${draft.category.label} • ${draft.difficulty.label} • ${draft.radiusMeters}m radius",
                    style = MaterialTheme.typography.labelMedium,
                    color = DiscoveryCyan
                )
                Text(draft.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MidnightIndigo,
                    border = BorderStroke(1.dp, ArcaneOutline)
                ) {
                    Text(
                        text = "Clue: “${draft.clue}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeadDropArtwork(
                        imageKey = draft.artifactImageKey,
                        contentDescription = draft.artifactName,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column {
                        Text("Reward: ${draft.artifactName}", style = MaterialTheme.typography.titleSmall, color = TreasureGold)
                        Text(draft.artifactDescription, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepSixPublish(
    draft: CreateTreasureDraft,
    onPublish: () -> Unit,
    onDoneAndClose: () -> Unit
) {
    if (draft.publishedTreasureId == null) {
        ArcaneCard(borderColor = TreasureGold.copy(alpha = 0.6f)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Rounded.Public, contentDescription = null, tint = DiscoveryCyan, modifier = Modifier.size(48.dp))
                Text("Ready to Publish Live", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    text = "Your DeadDrop will be written to the shared Firestore realm so all explorers can locate, unlock, and claim its relic. You also gain +50 Architect XP!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                GoldActionButton(
                    text = "Publish Treasure to Realm",
                    onClick = onPublish,
                    isLoading = draft.isPublishing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_treasure_button")
                )
            }
        }
    } else {
        ArcaneCard(borderColor = EmeraldSuccess) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(56.dp))
                Text("DeadDrop Published Live!", style = MaterialTheme.typography.headlineMedium, color = EmeraldSuccess)
                Text(
                    text = "“${draft.name}” is now active on the shared Explore map for all DeadDrop players.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                if (draft.challengeType == ChallengeType.QR_SCAN || draft.challengeType == ChallengeType.COMBINED) {
                    QrMatrixGraphic(token = draft.qrToken, sizeDp = 128.dp)
                    Text("Seal Token: ${draft.qrToken}", style = MaterialTheme.typography.labelMedium, color = TreasureGold)
                }
                ArcanePrimaryButton(
                    text = "Return to Explore Map",
                    onClick = onDoneAndClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_done_button")
                )
            }
        }
    }
}
