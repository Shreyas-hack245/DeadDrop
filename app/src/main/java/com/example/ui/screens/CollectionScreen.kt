package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.CollectedArtifact
import com.example.domain.model.Rarity
import com.example.ui.UiState
import com.example.ui.components.ArcaneCard
import com.example.ui.components.ArcaneOutlinedButton
import com.example.ui.components.ArcanePrimaryButton
import com.example.ui.components.ArcaneTextField
import com.example.ui.components.DeadDropArtwork
import com.example.ui.components.EmptyRealmState
import com.example.ui.components.GoldActionButton
import com.example.ui.components.LoadingOrb
import com.example.ui.components.RarityBadge
import com.example.ui.components.XpBadge
import com.example.ui.components.rarityColor
import com.example.ui.theme.AmethystGlow
import com.example.ui.theme.ArcaneOutline
import com.example.ui.theme.DiscoveryCyan
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TreasureGold
import com.example.ui.theme.VoidBlack
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CollectionScreen(
    artifactsState: UiState<List<CollectedArtifact>>,
    onExploreClick: () -> Unit,
    onListOnMarketplace: (CollectedArtifact, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRarity by remember { mutableStateOf<Rarity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var inspectedArtifact by remember { mutableStateOf<CollectedArtifact?>(null) }
    var listingArtifact by remember { mutableStateOf<CollectedArtifact?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        when (artifactsState) {
            UiState.Loading -> LoadingOrb("Opening your Relic Vault...")
            is UiState.Error -> EmptyRealmState(
                title = "Vault Unavailable",
                subtitle = artifactsState.message,
                modifier = Modifier.align(Alignment.Center)
            )
            is UiState.Success -> {
                val all = artifactsState.data.sortedByDescending { it.discoveredAt ?: Timestamp(0, 0) }
                val filtered = all.filter { art ->
                    val matchesRarity = selectedRarity == null || art.rarityEnum == selectedRarity
                    val matchesSearch = searchQuery.isBlank() ||
                        art.artifactName.contains(searchQuery, ignoreCase = true) ||
                        art.treasureName.contains(searchQuery, ignoreCase = true)
                    matchesRarity && matchesSearch
                }
                val totalXp = all.sumOf { it.xpEarned }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 840.dp)
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("RELIC VAULT", style = MaterialTheme.typography.labelMedium, color = TreasureGold)
                                    Text("Artifact Collection", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MidnightIndigo,
                                    border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("${all.size} Relics", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text("$totalXp XP", style = MaterialTheme.typography.labelSmall, color = TreasureGold)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vault_search_input"),
                                placeholder = { Text("Search collected artifacts...", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = AmethystGlow) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MidnightIndigo,
                                    unfocusedContainerColor = MidnightIndigo,
                                    focusedBorderColor = NeonViolet,
                                    unfocusedBorderColor = ArcaneOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = selectedRarity == null,
                                        onClick = { selectedRarity = null },
                                        label = { Text("All (${all.size})") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonViolet.copy(alpha = 0.28f),
                                            selectedLabelColor = AmethystGlow
                                        )
                                    )
                                }
                                items(Rarity.entries.reversed()) { rarity ->
                                    val count = all.count { it.rarityEnum == rarity }
                                    val accent = rarityColor(rarity)
                                    FilterChip(
                                        selected = selectedRarity == rarity,
                                        onClick = { selectedRarity = if (selectedRarity == rarity) null else rarity },
                                        label = { Text("${rarity.label} ($count)") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accent.copy(alpha = 0.24f),
                                            selectedLabelColor = accent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyRealmState(
                                title = if (all.isEmpty()) "Your Relic Vault Awaits" else "No Matching Relics",
                                subtitle = if (all.isEmpty())
                                    "Explore the map, solve arcane trials, and claim your first hidden artifact."
                                else
                                    "Try clearing your rarity filter or search phrase.",
                                actionLabel = if (all.isEmpty()) "Explore Nearby Drops" else null,
                                onAction = if (all.isEmpty()) onExploreClick else null
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { artifact ->
                            ArtifactGridCard(
                                artifact = artifact,
                                onClick = { inspectedArtifact = artifact }
                            )
                        }
                    }
                }
            }
        }
    }

    inspectedArtifact?.let { artifact ->
        ArtifactDetailDialog(
            artifact = artifact,
            onDismiss = { inspectedArtifact = null },
            onListForExchange = {
                inspectedArtifact = null
                listingArtifact = artifact
            }
        )
    }

    listingArtifact?.let { artifact ->
        CreateExchangeListingFromArtifactDialog(
            artifact = artifact,
            onDismiss = { listingArtifact = null },
            onConfirm = { priceGold ->
                onListOnMarketplace(artifact, priceGold)
                listingArtifact = null
            }
        )
    }
}

@Composable
private fun ArtifactGridCard(
    artifact: CollectedArtifact,
    onClick: () -> Unit
) {
    val accent = rarityColor(artifact.rarityEnum)
    ArcaneCard(
        onClick = onClick,
        borderColor = accent.copy(alpha = 0.6f),
        modifier = Modifier.testTag("artifact_card_${artifact.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                DeadDropArtwork(
                    imageKey = artifact.imageKey,
                    contentDescription = artifact.artifactName,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    RarityBadge(rarity = artifact.rarityEnum)
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = artifact.artifactName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artifact.treasureName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                XpBadge(xp = artifact.xpEarned)
            }
        }
    }
}

@Composable
private fun ArtifactDetailDialog(
    artifact: CollectedArtifact,
    onDismiss: () -> Unit,
    onListForExchange: () -> Unit
) {
    val dateStr = remember(artifact.discoveredAt) {
        val ts = artifact.discoveredAt ?: Timestamp.now()
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(ts.toDate())
    }
    val accent = rarityColor(artifact.rarityEnum)

    Dialog(onDismissRequest = onDismiss) {
        ArcaneCard(borderColor = accent) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeadDropArtwork(
                    imageKey = artifact.imageKey,
                    contentDescription = artifact.artifactName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RarityBadge(rarity = artifact.rarityEnum)
                    XpBadge(xp = artifact.xpEarned)
                }
                Text(artifact.artifactName, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(artifact.artifactDescription, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MidnightIndigo,
                    border = BorderStroke(1.dp, ArcaneOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Source: ${artifact.treasureName}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text("Method: ${artifact.methodEnum.label}", style = MaterialTheme.typography.bodySmall, color = DiscoveryCyan)
                        Text("Claimed: $dateStr", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                GoldActionButton(
                    text = "List on Simulated Exchange",
                    onClick = onListForExchange,
                    modifier = Modifier.fillMaxWidth()
                )
                ArcaneOutlinedButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CreateExchangeListingFromArtifactDialog(
    artifact: CollectedArtifact,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var priceText by remember { mutableStateOf("180") }
    val parsedPrice = priceText.toLongOrNull()
    val valid = parsedPrice != null && parsedPrice in 10L..100000L

    Dialog(onDismissRequest = onDismiss) {
        ArcaneCard(borderColor = TreasureGold) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = TreasureGold)
                    Text("Simulated Relic Listing", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Text(
                    "List a copy of ${artifact.artifactName} on the Simulated Relic Exchange for in-game Gold Coins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                ArcaneTextField(
                    value = priceText,
                    onValueChange = { v -> priceText = v.filter { it.isDigit() }.take(6) },
                    label = "Price (Gold Coins)",
                    isError = !valid,
                    supportingText = if (!valid) "Enter between 10 and 100,000 Gold" else null
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArcaneOutlinedButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    ArcanePrimaryButton(
                        text = "Publish Listing",
                        onClick = { if (parsedPrice != null) onConfirm(parsedPrice) },
                        enabled = valid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
