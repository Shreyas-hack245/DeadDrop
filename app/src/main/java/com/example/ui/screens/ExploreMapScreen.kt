package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.location.GeoPoint
import com.example.data.location.LocationTracker
import com.example.domain.model.ChallengeType
import com.example.domain.model.Rarity
import com.example.ui.ExploreFilterState
import com.example.ui.TreasureWithDistance
import com.example.ui.components.ArcaneCard
import com.example.ui.components.ChallengeTypeChip
import com.example.ui.components.DeadDropArtwork
import com.example.ui.components.RarityBadge
import com.example.ui.components.XpBadge
import com.example.ui.components.rarityColor
import com.example.ui.theme.AmberForge
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val DARK_FANTASY_MAP_STYLE = """
[
  {"elementType": "geometry", "stylers": [{"color": "#0d0a1a"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#9f94c9"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#07050f"}]},
  {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#1c1538"}]},
  {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#2f2259"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#060b1c"}]},
  {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#101b24"}]}
]
"""

fun hasConfiguredMapsApiKey(): Boolean {
    val key = BuildConfig.MAPS_API_KEY
    return key.isNotBlank() && key != "MY_MAPS_API_KEY"
}

@Composable
fun ExploreMapScreen(
    treasures: List<TreasureWithDistance>,
    currentLocation: GeoPoint,
    selectedTreasureId: String?,
    filterState: ExploreFilterState,
    onSearchChange: (String) -> Unit,
    onToggleRarity: (Rarity) -> Unit,
    onToggleChallenge: (ChallengeType) -> Unit,
    onToggleLiveCloudOnly: () -> Unit,
    onSelectTreasure: (String) -> Unit,
    onOpenTreasureDetail: (String) -> Unit,
    onCreateTreasureClick: () -> Unit,
    onRequestLocationUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val useLiveGoogleMaps = remember { hasConfiguredMapsApiKey() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) onRequestLocationUpdates()
    }

    LaunchedEffect(Unit) {
        val tracker = LocationTracker(context)
        if (tracker.hasLocationPermission()) {
            onRequestLocationUpdates()
        }
    }

    var zoomFactor by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(currentLocation.latitude, currentLocation.longitude), 15.5f
        )
    }

    LaunchedEffect(selectedTreasureId) {
        val target = treasures.firstOrNull { it.treasure.id == selectedTreasureId }?.treasure
        if (target != null && useLiveGoogleMaps) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), 16.5f)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        if (useLiveGoogleMaps) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapStyleOptions = MapStyleOptions(DARK_FANTASY_MAP_STYLE),
                    isMyLocationEnabled = currentLocation.isGpsLive
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                treasures.forEach { item ->
                    val t = item.treasure
                    val pos = LatLng(t.latitude, t.longitude)
                    val hue = when (t.rarityEnum) {
                        Rarity.MYTHIC -> BitmapDescriptorFactory.HUE_YELLOW
                        Rarity.EPIC -> BitmapDescriptorFactory.HUE_VIOLET
                        Rarity.RARE -> BitmapDescriptorFactory.HUE_CYAN
                        Rarity.COMMON -> BitmapDescriptorFactory.HUE_AZURE
                    }
                    Circle(
                        center = pos,
                        radius = t.radiusMeters.toDouble(),
                        fillColor = rarityColor(t.rarityEnum).copy(alpha = 0.14f),
                        strokeColor = rarityColor(t.rarityEnum).copy(alpha = 0.6f),
                        strokeWidth = 3f
                    )
                    Marker(
                        state = MarkerState(position = pos),
                        title = t.name,
                        snippet = "${t.rarityEnum.label} • +${t.xpReward} XP",
                        icon = BitmapDescriptorFactory.defaultMarker(hue),
                        onClick = {
                            onSelectTreasure(t.id)
                            false
                        }
                    )
                }
            }
        } else {
            DarkFantasyCartographicMap(
                treasures = treasures,
                currentLocation = currentLocation,
                selectedTreasureId = selectedTreasureId,
                zoomFactor = zoomFactor,
                panOffset = panOffset,
                onPan = { delta -> panOffset += delta },
                onSelectTreasure = onSelectTreasure,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Floating Search + Filter Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp)
        ) {
            OutlinedTextField(
                value = filterState.query,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("explore_search_input"),
                placeholder = { Text("Search drops, relics, or categories...", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = AmethystGlow)
                },
                trailingIcon = {
                    if (filterState.query.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MidnightIndigo.copy(alpha = 0.92f),
                    unfocusedContainerColor = MidnightIndigo.copy(alpha = 0.88f),
                    focusedBorderColor = NeonViolet,
                    unfocusedBorderColor = ArcaneOutline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterState.liveCloudOnly,
                        onClick = onToggleLiveCloudOnly,
                        label = { Text("Live Cloud") },
                        leadingIcon = {
                            Icon(Icons.Rounded.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MidnightIndigo.copy(alpha = 0.86f),
                            selectedContainerColor = DiscoveryCyan.copy(alpha = 0.25f),
                            labelColor = TextSecondary,
                            selectedLabelColor = DiscoveryCyan
                        )
                    )
                }
                items(Rarity.entries.reversed()) { rarity ->
                    val selected = filterState.selectedRarity == rarity
                    val accent = rarityColor(rarity)
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleRarity(rarity) },
                        label = { Text(rarity.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MidnightIndigo.copy(alpha = 0.86f),
                            selectedContainerColor = accent.copy(alpha = 0.24f),
                            labelColor = TextSecondary,
                            selectedLabelColor = accent
                        )
                    )
                }
                items(listOf(ChallengeType.QR_SCAN, ChallengeType.PUZZLE_ANSWER)) { type ->
                    val selected = filterState.selectedChallenge == type
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleChallenge(type) },
                        label = { Text(type.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MidnightIndigo.copy(alpha = 0.86f),
                            selectedContainerColor = NeonViolet.copy(alpha = 0.28f),
                            labelColor = TextSecondary,
                            selectedLabelColor = AmethystGlow
                        )
                    )
                }
            }

            if (!useLiveGoogleMaps) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MidnightIndigo.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, ArcaneOutline)
                ) {
                    Text(
                        text = if (currentLocation.isGpsLive)
                            "Cartographic Radar • Live GPS Active"
                        else
                            "Cartographic Radar • Demo Anchor (Add MAPS_API_KEY in Secrets for Google Maps)",
                        style = MaterialTheme.typography.labelSmall,
                        color = DiscoveryCyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right-side Map Controls (GPS Recenter & Zoom)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
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
                        panOffset = Offset.Zero
                        if (useLiveGoogleMaps) {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(currentLocation.latitude, currentLocation.longitude), 16f
                                    )
                                )
                            }
                        }
                    }
                },
                containerColor = ArcaneSurface,
                contentColor = DiscoveryCyan,
                modifier = Modifier.testTag("recenter_gps_button")
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = "Center on GPS location")
            }

            if (!useLiveGoogleMaps) {
                SmallFloatingActionButton(
                    onClick = { zoomFactor = (zoomFactor * 1.3f).coerceAtMost(3.0f) },
                    containerColor = ArcaneSurface,
                    contentColor = TextPrimary
                ) {
                    Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom in map")
                }
                SmallFloatingActionButton(
                    onClick = { zoomFactor = (zoomFactor / 1.3f).coerceAtLeast(0.55f) },
                    containerColor = ArcaneSurface,
                    contentColor = TextPrimary
                ) {
                    Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom out map")
                }
            }
        }

        // Bottom Deck: Prominent Forge Drop FAB + Nearby Treasures Horizontal Carousel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MidnightIndigo.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, ArcaneOutline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = null,
                            tint = TreasureGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${treasures.size} Nearby Drops",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }

                ExtendedFloatingActionButton(
                    onClick = onCreateTreasureClick,
                    containerColor = NeonViolet,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("create_treasure_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Forge Drop", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (treasures.isEmpty()) {
                ArcaneCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    borderColor = ArcaneOutline
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AmethystGlow)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No matching drops nearby", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Adjust your filters or forge a new treasure right here.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(treasures, key = { it.treasure.id }) { item ->
                        NearbyTreasurePreviewCard(
                            item = item,
                            isSelected = item.treasure.id == selectedTreasureId,
                            onClick = {
                                onSelectTreasure(item.treasure.id)
                                onOpenTreasureDetail(item.treasure.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyTreasurePreviewCard(
    item: TreasureWithDistance,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val t = item.treasure
    val accent = rarityColor(t.rarityEnum)
    ArcaneCard(
        onClick = onClick,
        modifier = Modifier
            .width(292.dp)
            .testTag("treasure_preview_${t.id}"),
        borderColor = if (isSelected) TreasureGold else accent.copy(alpha = 0.65f),
        containerColor = MidnightIndigo.copy(alpha = 0.94f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            ) {
                DeadDropArtwork(
                    imageKey = t.imageKey,
                    contentDescription = t.name,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.isDiscoveredByMe) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(VoidBlack.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Discovered",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RarityBadge(rarity = t.rarityEnum)
                    Text(
                        text = LocationTracker.formatDistance(item.distanceMeters),
                        style = MaterialTheme.typography.labelMedium,
                        color = DiscoveryCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = t.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XpBadge(xp = t.xpReward)
                    if (!t.isRealmStarterDemo) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = EmeraldSuccess.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                ChallengeTypeChip(type = t.challengeTypeEnum)
            }
        }
    }
}

@Composable
fun DarkFantasyCartographicMap(
    treasures: List<TreasureWithDistance>,
    currentLocation: GeoPoint,
    selectedTreasureId: String?,
    zoomFactor: Float,
    panOffset: Offset,
    onPan: (Offset) -> Unit,
    onSelectTreasure: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "radar")
    val radarPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPhase"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconPulse"
    )

    // Scale: ~111,000 meters per degree lat
    val pixelsPerMeterBase = 0.62f * zoomFactor

    Box(
        modifier = modifier
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF151030), MidnightIndigo, VoidBlack)
                )
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onPan(dragAmount)
                }
            }
            .pointerInput(treasures, currentLocation, zoomFactor, panOffset) {
                detectTapGestures { tap ->
                    val centerX = size.width / 2f + panOffset.x
                    val centerY = size.height / 2f + panOffset.y
                    val cosLat = cos(Math.toRadians(currentLocation.latitude)).coerceAtLeast(0.2)
                    val hit = treasures.minByOrNull { item ->
                        val dyMeters = (item.treasure.latitude - currentLocation.latitude) * 111111.0
                        val dxMeters = (item.treasure.longitude - currentLocation.longitude) * 111111.0 * cosLat
                        val mx = centerX + (dxMeters * pixelsPerMeterBase).toFloat()
                        val my = centerY - (dyMeters * pixelsPerMeterBase).toFloat()
                        hypot(tap.x - mx, tap.y - my)
                    }
                    if (hit != null) {
                        val dyMeters = (hit.treasure.latitude - currentLocation.latitude) * 111111.0
                        val dxMeters = (hit.treasure.longitude - currentLocation.longitude) * 111111.0 * cosLat
                        val mx = centerX + (dxMeters * pixelsPerMeterBase).toFloat()
                        val my = centerY - (dyMeters * pixelsPerMeterBase).toFloat()
                        if (hypot(tap.x - mx, tap.y - my) <= 72f) {
                            onSelectTreasure(hit.treasure.id)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + panOffset.x, size.height / 2f + panOffset.y)

            // Subtle arcane cartography grid
            val gridStep = 64.dp.toPx() * zoomFactor.coerceIn(0.6f, 1.5f)
            var gx = center.x % gridStep
            while (gx < size.width) {
                drawLine(
                    color = ArcaneOutline.copy(alpha = 0.16f),
                    start = Offset(gx, 0f),
                    end = Offset(gx, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                gx += gridStep
            }
            var gy = center.y % gridStep
            while (gy < size.height) {
                drawLine(
                    color = ArcaneOutline.copy(alpha = 0.16f),
                    start = Offset(0f, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = 1.dp.toPx()
                )
                gy += gridStep
            }

            // Concentric distance rings (100m, 250m, 500m)
            listOf(100f, 250f, 500f).forEach { meters ->
                drawCircle(
                    color = DiscoveryCyan.copy(alpha = 0.18f),
                    radius = meters * pixelsPerMeterBase,
                    center = center,
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                )
            }

            // Animated radar sweep wave
            drawCircle(
                color = NeonViolet.copy(alpha = (1f - radarPhase) * 0.25f),
                radius = radarPhase * 520f * pixelsPerMeterBase,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw faint compass lines
            val angle = radarPhase * 360.0
            val sweepRad = Math.toRadians(angle)
            val sweepRadius = 320f * pixelsPerMeterBase
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(DiscoveryCyan.copy(alpha = 0.4f), Color.Transparent),
                    start = center,
                    end = Offset(
                        center.x + (cos(sweepRad) * sweepRadius).toFloat(),
                        center.y + (sin(sweepRad) * sweepRadius).toFloat()
                    )
                ),
                start = center,
                end = Offset(
                    center.x + (cos(sweepRad) * sweepRadius).toFloat(),
                    center.y + (sin(sweepRad) * sweepRadius).toFloat()
                ),
                strokeWidth = 2.dp.toPx()
            )

            val cosLat = cos(Math.toRadians(currentLocation.latitude)).coerceAtLeast(0.2)

            // Draw treasure markers & proximity rings
            treasures.forEach { item ->
                val t = item.treasure
                val dyMeters = (t.latitude - currentLocation.latitude) * 111111.0
                val dxMeters = (t.longitude - currentLocation.longitude) * 111111.0 * cosLat
                val markerPos = Offset(
                    x = center.x + (dxMeters * pixelsPerMeterBase).toFloat(),
                    y = center.y - (dyMeters * pixelsPerMeterBase).toFloat()
                )
                val accent = rarityColor(t.rarityEnum)
                val selected = t.id == selectedTreasureId

                if (selected) {
                    drawLine(
                        color = TreasureGold.copy(alpha = 0.55f),
                        start = center,
                        end = markerPos,
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                }

                // Discovery radius aura
                drawCircle(
                    color = accent.copy(alpha = if (selected) 0.22f else 0.12f),
                    radius = (t.radiusMeters.toFloat() * pixelsPerMeterBase).coerceAtLeast(22.dp.toPx()),
                    center = markerPos
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.5f), Color.Transparent),
                        center = markerPos,
                        radius = 24.dp.toPx() * pulse
                    ),
                    radius = 24.dp.toPx() * pulse,
                    center = markerPos
                )
                drawCircle(
                    color = VoidBlack,
                    radius = if (selected) 13.dp.toPx() else 10.dp.toPx(),
                    center = markerPos
                )
                drawCircle(
                    color = if (item.isDiscoveredByMe) EmeraldSuccess else accent,
                    radius = if (selected) 9.dp.toPx() else 7.dp.toPx(),
                    center = markerPos
                )
                if (selected) {
                    drawCircle(
                        color = TreasureGold,
                        radius = 16.dp.toPx(),
                        center = markerPos,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Draw User Location Beacon
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(DiscoveryCyan.copy(alpha = 0.45f), Color.Transparent),
                    center = center,
                    radius = 28.dp.toPx()
                ),
                radius = 28.dp.toPx(),
                center = center
            )
            drawCircle(color = TextPrimary, radius = 9.dp.toPx(), center = center)
            drawCircle(color = DiscoveryCyan, radius = 6.dp.toPx(), center = center)
        }
    }
}
