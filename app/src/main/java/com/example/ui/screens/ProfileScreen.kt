package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.preferences.UserPreferences
import com.example.domain.model.Achievement
import com.example.domain.model.ActivityEntry
import com.example.domain.model.ActivityType
import com.example.domain.model.UserProfile
import com.example.ui.UiState
import com.example.ui.components.ArcaneCard
import com.example.ui.components.ArcaneOutlinedButton
import com.example.ui.components.ArcanePrimaryButton
import com.example.ui.components.ArcaneTextField
import com.example.ui.components.DeadDropArtwork
import com.example.ui.components.GoldBadge
import com.example.ui.components.LoadingOrb
import com.example.ui.components.rarityColor
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
import com.google.firebase.Timestamp

@Composable
fun ProfileScreen(
    profileState: UiState<UserProfile>,
    activityState: UiState<List<ActivityEntry>>,
    achievements: List<Achievement>,
    preferences: UserPreferences,
    onUpdateProfile: (String, String) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleStarterDrops: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        when (profileState) {
            UiState.Loading -> LoadingOrb("Summoning explorer dossier...")
            is UiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(profileState.message, color = CrimsonDanger)
                Spacer(modifier = Modifier.height(12.dp))
                ArcaneOutlinedButton(text = "Sign Out", onClick = onSignOut)
            }
            is UiState.Success -> {
                val profile = profileState.data
                val activities = (activityState as? UiState.Success)?.data
                    ?.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    ?.take(12)
                    ?: emptyList()

                val xpIntoLevel = UserProfile.xpIntoCurrentLevel(profile.xp)
                val xpPerLevel = UserProfile.xpPerLevel()
                val levelProgress = (xpIntoLevel.toFloat() / xpPerLevel.toFloat()).coerceIn(0.02f, 1f)

                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Explorer Hero Card
                    item {
                        ArcaneCard(borderColor = NeonViolet.copy(alpha = 0.65f)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(ArcaneSurfaceElevated.copy(alpha = 0.65f), ArcaneSurface)
                                        )
                                    )
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, TreasureGold, CircleShape)
                                        ) {
                                            DeadDropArtwork(
                                                imageKey = profile.avatarKey,
                                                contentDescription = profile.username,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Column {
                                            Text(profile.username, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                                            Text(profile.title, style = MaterialTheme.typography.labelLarge, color = AmethystGlow)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            GoldBadge(gold = profile.goldCoins)
                                        }
                                    }

                                    IconButton(
                                        onClick = { showEditDialog = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MidnightIndigo)
                                            .border(1.dp, ArcaneOutline, CircleShape)
                                            .testTag("edit_profile_button")
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit profile", tint = TextPrimary)
                                    }
                                }

                                // Level & XP Progress Bar
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Level ${profile.level}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TreasureGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "$xpIntoLevel / $xpPerLevel XP (${profile.xp} total)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { levelProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(9.dp)
                                            .clip(CircleShape),
                                        color = TreasureGold,
                                        trackColor = MidnightIndigo
                                    )
                                }
                            }
                        }
                    }

                    // 3 Stat Medallions
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatMedallion(
                                label = "Discovered",
                                value = profile.treasuresDiscovered.toString(),
                                color = DiscoveryCyan,
                                modifier = Modifier.weight(1f)
                            )
                            StatMedallion(
                                label = "Challenges",
                                value = profile.challengesCompleted.toString(),
                                color = AmethystGlow,
                                modifier = Modifier.weight(1f)
                            )
                            StatMedallion(
                                label = "Created",
                                value = profile.treasuresCreated.toString(),
                                color = TreasureGold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Achievements
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = TreasureGold)
                                Text("Achievements", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                            achievements.forEach { ach ->
                                val accent = rarityColor(ach.rarity)
                                ArcaneCard(
                                    borderColor = if (ach.unlocked) accent else ArcaneOutline,
                                    containerColor = MidnightIndigo
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(if (ach.unlocked) accent.copy(alpha = 0.2f) else ArcaneSurfaceElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (ach.unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                                contentDescription = null,
                                                tint = if (ach.unlocked) accent else TextMuted
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ach.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            Text(ach.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        }
                                        Text(
                                            text = "${ach.currentCount.coerceAtMost(ach.targetCount)}/${ach.targetCount}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (ach.unlocked) accent else TextMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }

                    // Settings Card
                    item {
                        ArcaneCard(borderColor = ArcaneOutline) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = DiscoveryCyan)
                                    Text("Expedition Settings", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                }

                                SettingSwitchRow(
                                    title = "Tactile Haptic Feedback",
                                    subtitle = "Vibrate on beacon proximity & unlocks",
                                    checked = preferences.hapticsEnabled,
                                    onCheckedChange = onToggleHaptics
                                )
                                SettingSwitchRow(
                                    title = "Show Realm Starter Drops",
                                    subtitle = "Include curated local starter quests on map",
                                    checked = preferences.showStarterDrops,
                                    onCheckedChange = onToggleStarterDrops
                                )

                                ArcaneOutlinedButton(
                                    text = "Sign Out of DeadDrop",
                                    onClick = onSignOut,
                                    borderColor = CrimsonDanger.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sign_out_button")
                                )
                            }
                        }
                    }

                    // Activity Feed
                    item {
                        Text("Recent Activity", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }

                    if (activities.isEmpty()) {
                        item {
                            Text(
                                text = "No expeditions logged yet. Unlock a drop or forge a treasure to begin your chronicle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    } else {
                        items(activities, key = { it.id }) { entry ->
                            ArcaneCard(borderColor = ArcaneOutline, containerColor = MidnightIndigo) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val icon = when (entry.typeEnum) {
                                        ActivityType.DISCOVERED -> Icons.Rounded.Explore
                                        ActivityType.CREATED -> Icons.Rounded.Public
                                        ActivityType.PURCHASED, ActivityType.LISTED -> Icons.Rounded.AutoAwesome
                                    }
                                    Icon(icon, contentDescription = null, tint = AmethystGlow)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    if (entry.xpDelta > 0) {
                                        Text("+${entry.xpDelta} XP", style = MaterialTheme.typography.labelMedium, color = TreasureGold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (showEditDialog) {
                    EditProfileDialog(
                        currentUsername = profile.username,
                        currentAvatarKey = profile.avatarKey,
                        onDismiss = { showEditDialog = false },
                        onSave = { name, avatar ->
                            onUpdateProfile(name, avatar)
                            showEditDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMedallion(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    ArcaneCard(modifier = modifier, borderColor = color.copy(alpha = 0.5f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VoidBlack,
                checkedTrackColor = DiscoveryCyan
            )
        )
    }
}

@Composable
private fun EditProfileDialog(
    currentUsername: String,
    currentAvatarKey: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentUsername) }
    var avatar by remember { mutableStateOf(currentAvatarKey) }
    val valid = name.trim().length in 2..40
    val avatarChoices = listOf("splash_hero", "obsidian_chest", "celestial_astrolabe", "shadow_lantern")

    Dialog(onDismissRequest = onDismiss) {
        ArcaneCard(borderColor = NeonViolet) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Customize Explorer Sigil", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                ArcaneTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = "Explorer Name",
                    isError = !valid,
                    supportingText = if (!valid) "Must be 2–40 characters" else null
                )
                Text("Choose Emblem", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    avatarChoices.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (avatar == key) 2.5.dp else 1.dp,
                                    color = if (avatar == key) TreasureGold else ArcaneOutline,
                                    shape = CircleShape
                                )
                                .clickable { avatar = key }
                        ) {
                            DeadDropArtwork(
                                imageKey = key,
                                contentDescription = key,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArcaneOutlinedButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    ArcanePrimaryButton(
                        text = "Save",
                        onClick = { onSave(name.trim(), avatar) },
                        enabled = valid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
