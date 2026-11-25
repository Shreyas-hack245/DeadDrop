package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class Rarity(val label: String) {
    COMMON("Common"),
    RARE("Rare"),
    EPIC("Epic"),
    MYTHIC("Mythic");

    companion object {
        fun safeValueOf(raw: String): Rarity =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: COMMON
    }
}

enum class TreasureCategory(val label: String) {
    ANCIENT_RELIC("Ancient Relic"),
    ARCANE_CACHE("Arcane Cache"),
    URBAN_CIPHER("Urban Cipher"),
    SHADOW_VAULT("Shadow Vault"),
    MYSTIC_SHRINE("Mystic Shrine");

    companion object {
        fun safeValueOf(raw: String): TreasureCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ARCANE_CACHE
    }
}

enum class Difficulty(val label: String) {
    NOVICE("Novice"),
    ADEPT("Adept"),
    MASTER("Master"),
    LEGENDARY("Legendary");

    companion object {
        fun safeValueOf(raw: String): Difficulty =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ADEPT
    }
}

enum class ChallengeType(val label: String) {
    GPS_PROXIMITY("GPS Proximity"),
    QR_SCAN("QR Seal Scan"),
    PUZZLE_ANSWER("Arcane Cipher"),
    COMBINED("GPS + Seal/Cipher");

    companion object {
        fun safeValueOf(raw: String): ChallengeType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: GPS_PROXIMITY
    }
}

enum class DiscoveryMethod(val label: String) {
    GPS("GPS Proximity"),
    QR("QR Scan"),
    PUZZLE("Cipher Solved"),
    COMBINED("Combined Trial"),
    MARKETPLACE("Simulated Exchange"),
    DEMO_SIMULATION("Simulated Demo");

    companion object {
        fun safeValueOf(raw: String): DiscoveryMethod =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: GPS
    }
}

enum class ActivityType {
    DISCOVERED,
    CREATED,
    PURCHASED,
    LISTED;

    companion object {
        fun safeValueOf(raw: String): ActivityType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DISCOVERED
    }
}

enum class ListingCategory(val label: String) {
    FEATURED("Featured"),
    POPULAR("Popular"),
    NEW("New");

    companion object {
        fun safeValueOf(raw: String): ListingCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NEW
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "ShadowSeeker",
    val title: String = "Novice Pathfinder",
    val avatarKey: String = "splash_hero",
    val xp: Long = 0L,
    val level: Long = 1L,
    val goldCoins: Long = 400L,
    val treasuresDiscovered: Long = 0L,
    val challengesCompleted: Long = 0L,
    val treasuresCreated: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "username" to username.take(40).ifBlank { "Explorer" },
        "title" to title.take(40).ifBlank { "Pathfinder" },
        "avatarKey" to avatarKey.ifBlank { "splash_hero" },
        "xp" to xp.coerceAtLeast(0L),
        "level" to level.coerceAtLeast(1L),
        "goldCoins" to goldCoins.coerceAtLeast(0L),
        "treasuresDiscovered" to treasuresDiscovered.coerceAtLeast(0L),
        "challengesCompleted" to challengesCompleted.coerceAtLeast(0L),
        "treasuresCreated" to treasuresCreated.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun levelForXp(xp: Long): Long = (xp / 500L) + 1L
        fun xpIntoCurrentLevel(xp: Long): Long = xp % 500L
        fun xpPerLevel(): Long = 500L
        fun titleForLevel(level: Long): String = when {
            level >= 10 -> "Mythic Archivist"
            level >= 6 -> "Obsidian Vanguard"
            level >= 4 -> "Astral Seeker"
            level >= 2 -> "Arcane Scout"
            else -> "Novice Pathfinder"
        }
    }
}

@Keep
@IgnoreExtraProperties
data class Treasure(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = TreasureCategory.ARCANE_CACHE.name,
    val difficulty: String = Difficulty.ADEPT.name,
    val rarity: String = Rarity.RARE.name,
    val imageKey: String = "obsidian_chest",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val radiusMeters: Long = 60L,
    val challengeType: String = ChallengeType.GPS_PROXIMITY.name,
    val clue: String = "",
    val hint: String? = null,
    val qrToken: String? = null,
    val puzzleAnswer: String? = null,
    val artifactName: String = "",
    val artifactDescription: String = "",
    val artifactImageKey: String = "celestial_astrolabe",
    val artifactRarity: String = Rarity.RARE.name,
    val xpReward: Long = 200L,
    val discoveryCount: Long = 0L,
    val status: String = "ACTIVE",
    val visibility: String = "PUBLIC",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    // Client-only flag (not persisted to Firestore when using toCreateMap)
    val isRealmStarterDemo: Boolean = false
) {
    val rarityEnum: Rarity get() = Rarity.safeValueOf(rarity)
    val categoryEnum: TreasureCategory get() = TreasureCategory.safeValueOf(category)
    val difficultyEnum: Difficulty get() = Difficulty.safeValueOf(difficulty)
    val challengeTypeEnum: ChallengeType get() = ChallengeType.safeValueOf(challengeType)
    val artifactRarityEnum: Rarity get() = Rarity.safeValueOf(artifactRarity)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "creatorId" to creatorId,
            "creatorName" to creatorName.trim().take(40).ifBlank { "Explorer" },
            "name" to name.trim().take(80),
            "description" to description.trim().take(500),
            "category" to categoryEnum.name,
            "difficulty" to difficultyEnum.name,
            "rarity" to rarityEnum.name,
            "imageKey" to imageKey.ifBlank { "obsidian_chest" },
            "latitude" to latitude.coerceIn(-90.0, 90.0),
            "longitude" to longitude.coerceIn(-180.0, 180.0),
            "radiusMeters" to radiusMeters.coerceIn(10L, 1000L),
            "challengeType" to challengeTypeEnum.name,
            "clue" to clue.trim().take(350),
            "hint" to hint?.trim()?.take(250)?.takeIf { it.isNotEmpty() },
            "qrToken" to qrToken?.trim()?.take(120)?.takeIf { it.isNotEmpty() },
            "puzzleAnswer" to puzzleAnswer?.trim()?.take(120)?.takeIf { it.isNotEmpty() },
            "artifactName" to artifactName.trim().take(80),
            "artifactDescription" to artifactDescription.trim().take(350),
            "artifactImageKey" to artifactImageKey.ifBlank { "celestial_astrolabe" },
            "artifactRarity" to artifactRarityEnum.name,
            "xpReward" to xpReward.coerceIn(25L, 10000L),
            "discoveryCount" to 0L,
            "status" to "ACTIVE",
            "visibility" to "PUBLIC",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class CollectedArtifact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val treasureId: String = "",
    val treasureName: String = "",
    val artifactName: String = "",
    val artifactDescription: String = "",
    val rarity: String = Rarity.RARE.name,
    val imageKey: String = "celestial_astrolabe",
    val xpEarned: Long = 0L,
    val discoveryMethod: String = DiscoveryMethod.GPS.name,
    val discoveredAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val rarityEnum: Rarity get() = Rarity.safeValueOf(rarity)
    val methodEnum: DiscoveryMethod get() = DiscoveryMethod.safeValueOf(discoveryMethod)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "treasureId" to treasureId.take(128).ifBlank { "unknown" },
        "treasureName" to treasureName.trim().take(80).ifBlank { "Unknown Drop" },
        "artifactName" to artifactName.trim().take(80).ifBlank { "Arcane Relic" },
        "artifactDescription" to artifactDescription.trim().take(350).ifBlank { "Recovered relic." },
        "rarity" to rarityEnum.name,
        "imageKey" to imageKey.ifBlank { "celestial_astrolabe" },
        "xpEarned" to xpEarned.coerceIn(0L, 10000L),
        "discoveryMethod" to methodEnum.name,
        "discoveredAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ActivityEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val xpDelta: Long = 0L,
    val type: String = ActivityType.DISCOVERED.name,
    val createdAt: Timestamp? = null
) {
    val typeEnum: ActivityType get() = ActivityType.safeValueOf(type)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.trim().take(100),
        "subtitle" to subtitle.trim().take(180),
        "xpDelta" to xpDelta.coerceIn(0L, 10000L),
        "type" to typeEnum.name,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class MarketplaceListing(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val artifactName: String = "",
    val artifactDescription: String = "",
    val rarity: String = Rarity.RARE.name,
    val imageKey: String = "shadow_lantern",
    val priceGold: Long = 100L,
    val category: String = ListingCategory.NEW.name,
    val status: String = "AVAILABLE",
    val visibility: String = "PUBLIC",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isSimulatedCurated: Boolean = false
) {
    val rarityEnum: Rarity get() = Rarity.safeValueOf(rarity)
    val categoryEnum: ListingCategory get() = ListingCategory.safeValueOf(category)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "sellerId" to sellerId,
        "sellerName" to sellerName.trim().take(40).ifBlank { "Explorer" },
        "artifactName" to artifactName.trim().take(80),
        "artifactDescription" to artifactDescription.trim().take(350),
        "rarity" to rarityEnum.name,
        "imageKey" to imageKey.ifBlank { "shadow_lantern" },
        "priceGold" to priceGold.coerceIn(10L, 100000L),
        "category" to categoryEnum.name,
        "status" to "AVAILABLE",
        "visibility" to "PUBLIC",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val targetCount: Int,
    val currentCount: Int,
    val rarity: Rarity
) {
    val unlocked: Boolean get() = currentCount >= targetCount
}
