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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.domain.model.ListingCategory
import com.example.domain.model.MarketplaceListing
import com.example.domain.model.UserProfile
import com.example.ui.UiState
import com.example.ui.components.ArcaneCard
import com.example.ui.components.ArcaneOutlinedButton
import com.example.ui.components.ArcanePrimaryButton
import com.example.ui.components.ArcaneTextField
import com.example.ui.components.DeadDropArtwork
import com.example.ui.components.EmptyRealmState
import com.example.ui.components.GoldActionButton
import com.example.ui.components.GoldBadge
import com.example.ui.components.RarityBadge
import com.example.ui.components.rarityColor
import com.example.ui.theme.AmberForge
import com.example.ui.theme.AmethystGlow
import com.example.ui.theme.ArcaneOutline
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
fun MarketplaceScreen(
    listings: List<MarketplaceListing>,
    profileState: UiState<UserProfile>,
    artifactsState: UiState<List<CollectedArtifact>>,
    onPurchaseListing: (MarketplaceListing) -> Unit,
    onCreateListing: (CollectedArtifact, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data ?: UserProfile()
    val userArtifacts = (artifactsState as? UiState.Success)?.data ?: emptyList()

    var selectedCategory by remember { mutableStateOf<ListingCategory?>(null) }
    var selectedListing by remember { mutableStateOf<MarketplaceListing?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }

    val featured = listings.filter { it.categoryEnum == ListingCategory.FEATURED || it.rarityEnum.name == "MYTHIC" }
    val filtered = listings.filter { selectedCategory == null || it.categoryEnum == selectedCategory }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SIMULATED EXCHANGE", style = MaterialTheme.typography.labelMedium, color = TreasureGold)
                        Text("Relic Marketplace", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                    }
                    GoldBadge(gold = profile.goldCoins)
                }
            }

            // Explicit Honest Simulated Exchange Notice
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AmberForge.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, AmberForge.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = AmberForge)
                        Text(
                            text = "Simulated Relic Exchange: All trades use in-game Gold Coins earned from treasure hunts. Collectibles are in-app game relics, not real-money or blockchain/NFT assets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (featured.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Featured Collectibles", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(featured, key = { "feat_${it.id}" }) { item ->
                                FeaturedListingCard(
                                    listing = item,
                                    onClick = { selectedListing = item }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("All") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonViolet.copy(alpha = 0.28f),
                                    selectedLabelColor = AmethystGlow
                                )
                            )
                        }
                        items(ListingCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DiscoveryCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = DiscoveryCyan
                                )
                            )
                        }
                    }

                    if (userArtifacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = { showCreateSheet = true },
                            shape = RoundedCornerShape(14.dp),
                            color = NeonViolet,
                            modifier = Modifier.testTag("create_listing_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Text("Sell", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyRealmState(
                        title = "No Listings in Category",
                        subtitle = "Switch tabs or list one of your Vault relics for fellow explorers."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { listing ->
                    MarketplaceRowCard(
                        listing = listing,
                        isMine = listing.sellerId == profile.userId,
                        onClick = { selectedListing = listing }
                    )
                }
            }
        }
    }

    selectedListing?.let { listing ->
        ListingDetailDialog(
            listing = listing,
            userGold = profile.goldCoins,
            isMine = listing.sellerId == profile.userId,
            onDismiss = { selectedListing = null },
            onPurchase = {
                onPurchaseListing(listing)
                selectedListing = null
            }
        )
    }

    if (showCreateSheet && userArtifacts.isNotEmpty()) {
        CreateListingDialog(
            artifacts = userArtifacts,
            onDismiss = { showCreateSheet = false },
            onConfirm = { art, price ->
                onCreateListing(art, price)
                showCreateSheet = false
            }
        )
    }
}

@Composable
private fun FeaturedListingCard(
    listing: MarketplaceListing,
    onClick: () -> Unit
) {
    val accent = rarityColor(listing.rarityEnum)
    ArcaneCard(
        onClick = onClick,
        borderColor = accent,
        modifier = Modifier.width(248.dp)
    ) {
        Column {
            DeadDropArtwork(
                imageKey = listing.imageKey,
                contentDescription = listing.artifactName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            )
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RarityBadge(rarity = listing.rarityEnum)
                Text(listing.artifactName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("by ${listing.sellerName}", style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                    GoldBadge(gold = listing.priceGold)
                }
            }
        }
    }
}

@Composable
private fun MarketplaceRowCard(
    listing: MarketplaceListing,
    isMine: Boolean,
    onClick: () -> Unit
) {
    val accent = rarityColor(listing.rarityEnum)
    ArcaneCard(
        onClick = onClick,
        borderColor = accent.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("listing_card_${listing.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeadDropArtwork(
                imageKey = listing.imageKey,
                contentDescription = listing.artifactName,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RarityBadge(rarity = listing.rarityEnum)
                    if (!listing.isSimulatedCurated) {
                        Surface(shape = RoundedCornerShape(999.dp), color = EmeraldSuccess.copy(alpha = 0.16f)) {
                            Text(
                                text = if (isMine) "YOURS" else "PLAYER",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(listing.artifactName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listing.artifactDescription, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            GoldBadge(gold = listing.priceGold)
        }
    }
}

@Composable
private fun ListingDetailDialog(
    listing: MarketplaceListing,
    userGold: Long,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit
) {
    val canAfford = userGold >= listing.priceGold
    val accent = rarityColor(listing.rarityEnum)
    Dialog(onDismissRequest = onDismiss) {
        ArcaneCard(borderColor = accent) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeadDropArtwork(
                    imageKey = listing.imageKey,
                    contentDescription = listing.artifactName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RarityBadge(rarity = listing.rarityEnum)
                    GoldBadge(gold = listing.priceGold)
                }
                Text(listing.artifactName, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(listing.artifactDescription, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Seller: ${listing.sellerName} • Simulated Trade (+25 XP)", style = MaterialTheme.typography.labelSmall, color = DiscoveryCyan)

                if (!canAfford && !isMine) {
                    Text(
                        text = "Insufficient Gold Coins ($userGold / ${listing.priceGold}). Discover more treasures to earn Gold!",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberForge
                    )
                }

                GoldActionButton(
                    text = if (isMine) "Your Active Listing" else "Acquire Relic (${listing.priceGold} Gold)",
                    onClick = onPurchase,
                    enabled = canAfford && !isMine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("purchase_listing_button")
                )
                ArcaneOutlinedButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CreateListingDialog(
    artifacts: List<CollectedArtifact>,
    onDismiss: () -> Unit,
    onConfirm: (CollectedArtifact, Long) -> Unit
) {
    var selected by remember { mutableStateOf(artifacts.first()) }
    var priceText by remember { mutableStateOf("150") }
    val parsedPrice = priceText.toLongOrNull()
    val valid = parsedPrice != null && parsedPrice in 10L..100000L

    Dialog(onDismissRequest = onDismiss) {
        ArcaneCard(borderColor = NeonViolet) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Storefront, contentDescription = null, tint = AmethystGlow)
                    Text("List Vault Relic", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Text("Select an artifact from your Vault:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(artifacts, key = { it.id }) { art ->
                        FilterChip(
                            selected = selected.id == art.id,
                            onClick = { selected = art },
                            label = { Text(art.artifactName, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TreasureGold.copy(alpha = 0.22f),
                                selectedLabelColor = TreasureGold
                            )
                        )
                    }
                }
                ArcaneTextField(
                    value = priceText,
                    onValueChange = { v -> priceText = v.filter { it.isDigit() }.take(6) },
                    label = "Asking Price (In-Game Gold)",
                    isError = !valid,
                    supportingText = if (!valid) "Must be 10–100,000 Gold" else null
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArcaneOutlinedButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    ArcanePrimaryButton(
                        text = "List Relic",
                        onClick = { if (parsedPrice != null) onConfirm(selected, parsedPrice) },
                        enabled = valid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
