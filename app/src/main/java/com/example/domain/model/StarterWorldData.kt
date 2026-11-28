package com.example.domain.model

import kotlin.random.Random

/** Where one starter drop was placed for a given user. Everything else about the drop is static template data. */
data class StarterPlacement(
    val id: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * The saved starter world for one user: the base location it was generated around plus the
 * coordinates of every starter drop. Persisted per user (see UserPreferencesRepository).
 */
data class StarterWorldSnapshot(
    val baseLatitude: Double,
    val baseLongitude: Double,
    val seed: Long,
    val generatedAtMillis: Long,
    val placements: List<StarterPlacement>,
    val version: Int = StarterWorldData.SNAPSHOT_VERSION
)

object StarterWorldData {

    /** Bump when templates change in a way that requires re-placing drops. */
    const val SNAPSHOT_VERSION = 1

    /** Starter drops are placed between these distances from the user's base location. */
    const val MIN_DISTANCE_METERS = 80.0
    const val MAX_DISTANCE_METERS = 800.0

    /** Saved starter drops are regenerated once the user is further than this from their base. */
    const val REGENERATE_DISTANCE_METERS = 5_000.0

    /** Fixed seed so the no-location fallback world is stable between launches. */
    private const val FALLBACK_SEED = 20240611L

    /**
     * Static content of every starter drop (names, rarities, categories, difficulties, challenge types,
     * clues and XP rewards). Latitude/longitude here are placeholders - real coordinates come from
     * [generateSnapshot] / [buildTreasures].
     */
    private val templates: List<Treasure> = listOf(
        Treasure(
            id = "starter_obsidian_vault",
            creatorId = "realm_architect",
            creatorName = "Archivist Vael",
            name = "Obsidian Spire Cache",
            description = "An ancient obsidian coffer sealed beneath twilight arches. Violet runes shimmer whenever a seeker draws within range.",
            category = TreasureCategory.SHADOW_VAULT.name,
            difficulty = Difficulty.MASTER.name,
            rarity = Rarity.MYTHIC.name,
            imageKey = "obsidian_chest",
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 80L,
            challengeType = ChallengeType.COMBINED.name,
            clue = "Approach the spire perimeter, then scan the Dread Seal or speak the stone's true metal.",
            hint = "The seal token is DEADDROP-SPIRE-01 (or answer: OBSIDIAN).",
            qrToken = "DEADDROP-SPIRE-01",
            puzzleAnswer = "OBSIDIAN",
            artifactName = "Crown of Midnight",
            artifactDescription = "A forged obsidian circlet humming with amethyst starlight.",
            artifactImageKey = "obsidian_chest",
            artifactRarity = Rarity.MYTHIC.name,
            xpReward = 650L,
            discoveryCount = 14L,
            status = "ACTIVE",
            visibility = "PUBLIC",
            isRealmStarterDemo = true
        ),
        Treasure(
            id = "starter_celestial_beacon",
            creatorId = "realm_architect",
            creatorName = "Lyra Starborn",
            name = "Astral Observatory Drop",
            description = "Left by celestial cartographers to calibrate the arcane meridian. Approach the glowing beacon to claim its brass relic.",
            category = TreasureCategory.ANCIENT_RELIC.name,
            difficulty = Difficulty.ADEPT.name,
            rarity = Rarity.EPIC.name,
            imageKey = "celestial_astrolabe",
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 75L,
            challengeType = ChallengeType.GPS_PROXIMITY.name,
            clue = "Step within 75 meters of the eastern meridian coordinates to attune the astrolabe.",
            hint = "Enable live GPS or test with Simulated Proximity if indoors.",
            qrToken = null,
            puzzleAnswer = null,
            artifactName = "Astrolabe of Dusk",
            artifactDescription = "Rotating bronze rings aligned to forgotten constellations.",
            artifactImageKey = "celestial_astrolabe",
            artifactRarity = Rarity.EPIC.name,
            xpReward = 400L,
            discoveryCount = 29L,
            status = "ACTIVE",
            visibility = "PUBLIC",
            isRealmStarterDemo = true
        ),
        Treasure(
            id = "starter_lantern_cipher",
            creatorId = "realm_architect",
            creatorName = "Kael the Scribe",
            name = "Hollow Lantern Shrine",
            description = "A spectral lantern hidden at the crossroads. Only seekers who decode its riddle or scan its carved rune can open the latch.",
            category = TreasureCategory.URBAN_CIPHER.name,
            difficulty = Difficulty.ADEPT.name,
            rarity = Rarity.RARE.name,
            imageKey = "shadow_lantern",
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 60L,
            challengeType = ChallengeType.PUZZLE_ANSWER.name,
            clue = "I devour light yet guide the lost through shadow. What burns inside the gothic cage?",
            hint = "One word: FLAME (or SOULFLAME).",
            qrToken = "DEADDROP-LANTERN-09",
            puzzleAnswer = "FLAME",
            artifactName = "Lantern of Echoes",
            artifactDescription = "A gothic iron lantern housing a cold cyan ember.",
            artifactImageKey = "shadow_lantern",
            artifactRarity = Rarity.RARE.name,
            xpReward = 275L,
            discoveryCount = 41L,
            status = "ACTIVE",
            visibility = "PUBLIC",
            isRealmStarterDemo = true
        ),
        Treasure(
            id = "starter_qr_sigil",
            creatorId = "realm_architect",
            creatorName = "Warden Thorne",
            name = "Sigil of the Crossroads",
            description = "An urban rune cache bound by a physical QR seal. Present the matching rune code to the scanner lens to unlock the vault.",
            category = TreasureCategory.ARCANE_CACHE.name,
            difficulty = Difficulty.NOVICE.name,
            rarity = Rarity.RARE.name,
            imageKey = "splash_hero",
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 90L,
            challengeType = ChallengeType.QR_SCAN.name,
            clue = "Scan the Crossroads QR seal using the DeadDrop Arcane Lens.",
            hint = "Expected QR payload: DEADDROP-SIGIL-42",
            qrToken = "DEADDROP-SIGIL-42",
            puzzleAnswer = null,
            artifactName = "Pathfinder's Compass",
            artifactDescription = "Points unerringly toward the nearest hidden DeadDrop.",
            artifactImageKey = "splash_hero",
            artifactRarity = Rarity.RARE.name,
            xpReward = 220L,
            discoveryCount = 63L,
            status = "ACTIVE",
            visibility = "PUBLIC",
            isRealmStarterDemo = true
        ),
        Treasure(
            id = "starter_whispering_cache",
            creatorId = "realm_architect",
            creatorName = "Sable Courier",
            name = "Whispering Waystone",
            description = "A beginner's cache tucked right along the starter trail. Ideal for calibrating your explorer compass.",
            category = TreasureCategory.MYSTIC_SHRINE.name,
            difficulty = Difficulty.NOVICE.name,
            rarity = Rarity.COMMON.name,
            imageKey = "obsidian_chest",
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 100L,
            challengeType = ChallengeType.GPS_PROXIMITY.name,
            clue = "Stand near the central waystone marker to claim your first field token.",
            hint = "Within 100m of the starter anchor.",
            qrToken = null,
            puzzleAnswer = null,
            artifactName = "Rune-Etched Coin",
            artifactDescription = "A tarnished silver token stamped with the DeadDrop crest.",
            artifactImageKey = "celestial_astrolabe",
            artifactRarity = Rarity.COMMON.name,
            xpReward = 120L,
            discoveryCount = 112L,
            status = "ACTIVE",
            visibility = "PUBLIC",
            isRealmStarterDemo = true
        )
    )

    val templateIds: List<String> get() = templates.map { it.id }

    /**
     * Places every starter template around ([baseLatitude], [baseLongitude]).
     *
     * Bearings are spread evenly around the compass (with jitter) and distances are spread evenly
     * across [MIN_DISTANCE_METERS]..[MAX_DISTANCE_METERS] (stratified, with jitter). The two are shuffled
     * independently so a drop's direction does not predict its distance. The same [seed] always gives
     * the same layout.
     */
    fun generateSnapshot(
        baseLatitude: Double,
        baseLongitude: Double,
        seed: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): StarterWorldSnapshot {
        val rnd = Random(seed)
        val n = templates.size
        val sector = 360.0 / n
        val band = (MAX_DISTANCE_METERS - MIN_DISTANCE_METERS) / n
        val startBearing = rnd.nextDouble() * 360.0
        val bearingSlots = (0 until n).shuffled(rnd)
        val distanceBands = (0 until n).shuffled(rnd)

        val placements = templates.mapIndexed { i, template ->
            val bearing = (startBearing + bearingSlots[i] * sector + (rnd.nextDouble() - 0.5) * sector * 0.6)
                .mod(360.0)
            val distance = MIN_DISTANCE_METERS + distanceBands[i] * band + rnd.nextDouble() * band
            val point = GeoMath.offset(baseLatitude, baseLongitude, distance, bearing)
            StarterPlacement(template.id, point.latitude, point.longitude)
        }
        return StarterWorldSnapshot(
            baseLatitude = baseLatitude,
            baseLongitude = baseLongitude,
            seed = seed,
            generatedAtMillis = nowMillis,
            placements = placements
        )
    }

    /** Joins the static templates with a saved snapshot's coordinates. */
    fun buildTreasures(snapshot: StarterWorldSnapshot): List<Treasure> {
        val byId = snapshot.placements.associateBy { it.id }
        return templates.mapNotNull { template ->
            val placement = byId[template.id] ?: return@mapNotNull null
            template.copy(latitude = placement.latitude, longitude = placement.longitude)
        }
    }

    /** Layout used when no real location is available (permission declined / no fix). Never persisted. */
    fun fallbackTreasuresAround(centerLatitude: Double, centerLongitude: Double): List<Treasure> =
        buildTreasures(generateSnapshot(centerLatitude, centerLongitude, FALLBACK_SEED, nowMillis = 0L))

    /** True when [saved] is missing, outdated, incomplete, or the user is far from where it was generated. */
    fun needsRegeneration(
        saved: StarterWorldSnapshot?,
        distanceFromBaseMeters: Double?
    ): Boolean {
        if (saved == null) return true
        if (saved.version != SNAPSHOT_VERSION) return true
        if (saved.placements.map { it.id }.toSet() != templateIds.toSet()) return true
        return distanceFromBaseMeters != null && distanceFromBaseMeters > REGENERATE_DISTANCE_METERS
    }

    val simulatedCuratedListings: List<MarketplaceListing> = listOf(
        MarketplaceListing(
            id = "sim_listing_chronos",
            sellerId = "guild_vault",
            sellerName = "Arcane Guildmaster",
            artifactName = "Chronos Star-Astrolabe",
            artifactDescription = "A masterwork brass astrolabe forged to track eclipse drops across shadowed citadels.",
            rarity = Rarity.MYTHIC.name,
            imageKey = "celestial_astrolabe",
            priceGold = 320L,
            category = ListingCategory.FEATURED.name,
            status = "AVAILABLE",
            visibility = "PUBLIC",
            isSimulatedCurated = true
        ),
        MarketplaceListing(
            id = "sim_listing_abyssal_chest",
            sellerId = "guild_vault",
            sellerName = "Obsidian Broker",
            artifactName = "Abyssal Lockbox Relic",
            artifactDescription = "Sealed with violet crystal wards; prized by master treasure architects.",
            rarity = Rarity.EPIC.name,
            imageKey = "obsidian_chest",
            priceGold = 210L,
            category = ListingCategory.POPULAR.name,
            status = "AVAILABLE",
            visibility = "PUBLIC",
            isSimulatedCurated = true
        ),
        MarketplaceListing(
            id = "sim_listing_cyan_lantern",
            sellerId = "guild_vault",
            sellerName = "Nightwatch Quartermaster",
            artifactName = "Tidecaller Soul Lantern",
            artifactDescription = "Illuminates hidden trails in deep twilight zones with steady cyan flame.",
            rarity = Rarity.RARE.name,
            imageKey = "shadow_lantern",
            priceGold = 140L,
            category = ListingCategory.POPULAR.name,
            status = "AVAILABLE",
            visibility = "PUBLIC",
            isSimulatedCurated = true
        ),
        MarketplaceListing(
            id = "sim_listing_warden_sigil",
            sellerId = "guild_vault",
            sellerName = "Citadel Archivist",
            artifactName = "Warden's Wayfarer Seal",
            artifactDescription = "Ceremonial explorer emblem carried by pioneers of the Shadowed Realm.",
            rarity = Rarity.COMMON.name,
            imageKey = "splash_hero",
            priceGold = 75L,
            category = ListingCategory.NEW.name,
            status = "AVAILABLE",
            visibility = "PUBLIC",
            isSimulatedCurated = true
        )
    )

    fun computeAchievements(
        profile: UserProfile,
        artifacts: List<CollectedArtifact>
    ): List<Achievement> = listOf(
        Achievement(
            id = "ach_first_drop",
            title = "First Shadow Drop",
            description = "Discover and claim your first treasure relic.",
            targetCount = 1,
            currentCount = profile.treasuresDiscovered.toInt(),
            rarity = Rarity.COMMON
        ),
        Achievement(
            id = "ach_cipher_breaker",
            title = "Cipher Breaker",
            description = "Complete 3 treasure challenges or QR scans.",
            targetCount = 3,
            currentCount = profile.challengesCompleted.toInt(),
            rarity = Rarity.RARE
        ),
        Achievement(
            id = "ach_world_architect",
            title = "Realm Architect",
            description = "Publish a custom DeadDrop treasure for other explorers.",
            targetCount = 1,
            currentCount = profile.treasuresCreated.toInt(),
            rarity = Rarity.EPIC
        ),
        Achievement(
            id = "ach_mythic_seeker",
            title = "Mythic Seeker",
            description = "Collect at least 1 Mythic or Epic artifact in your Vault.",
            targetCount = 1,
            currentCount = artifacts.count {
                it.rarityEnum == Rarity.MYTHIC || it.rarityEnum == Rarity.EPIC
            },
            rarity = Rarity.MYTHIC
        )
    )
}
