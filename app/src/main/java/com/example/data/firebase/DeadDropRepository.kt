package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.domain.model.ActivityEntry
import com.example.domain.model.ActivityType
import com.example.domain.model.CollectedArtifact
import com.example.domain.model.DiscoveryMethod
import com.example.domain.model.ListingCategory
import com.example.domain.model.MarketplaceListing
import com.example.domain.model.Rarity
import com.example.domain.model.Treasure
import com.example.domain.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DeadDropRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> unwrapSnapshotFlow(
        path: String,
        uid: String,
        source: Flow<T>
    ): Flow<T> = source.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId"
        emitAll(
            unwrapSnapshotFlow(
                path = path,
                uid = userId,
                source = db.collection("users").document(userId)
                    .snapshots()
                    .map { snapshot ->
                        if (!snapshot.exists()) null
                        else snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
            )
        )
    }

    fun observePublicTreasures(userId: String): Flow<List<Treasure>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "treasures"
        emitAll(
            unwrapSnapshotFlow(
                path = path,
                uid = userId,
                source = db.collection("treasures")
                    .whereEqualTo("visibility", "PUBLIC")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            Treasure::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
            )
        )
    }

    fun observeUserArtifacts(userId: String): Flow<List<CollectedArtifact>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/artifacts"
        emitAll(
            unwrapSnapshotFlow(
                path = path,
                uid = userId,
                source = db.collection("users").document(userId).collection("artifacts")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            CollectedArtifact::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
            )
        )
    }

    fun observeUserActivity(userId: String): Flow<List<ActivityEntry>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/activity"
        emitAll(
            unwrapSnapshotFlow(
                path = path,
                uid = userId,
                source = db.collection("users").document(userId).collection("activity")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            ActivityEntry::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
            )
        )
    }

    fun observeMarketplaceListings(userId: String): Flow<List<MarketplaceListing>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "marketplace_listings"
        emitAll(
            unwrapSnapshotFlow(
                path = path,
                uid = userId,
                source = db.collection("marketplace_listings")
                    .whereEqualTo("visibility", "PUBLIC")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            MarketplaceListing::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
            )
        )
    }

    suspend fun ensureUserProfile(defaultDisplayName: String?): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = defaultDisplayName?.trim()?.take(40)?.takeIf { it.length >= 2 }
                    ?: "ShadowSeeker"
                val profile = UserProfile(
                    userId = uid,
                    username = safeName,
                    title = UserProfile.titleForLevel(1L),
                    avatarKey = "splash_hero",
                    xp = 0L,
                    level = 1L,
                    goldCoins = 400L,
                    treasuresDiscovered = 0L,
                    challengesCompleted = 0L,
                    treasuresCreated = 0L
                )
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfileDetails(username: String, avatarKey: String): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val cleanName = username.trim()
            require(cleanName.length in 2..40) { "Explorer name must be 2 to 40 characters." }
            require(avatarKey.isNotBlank() && avatarKey.length <= 40) { "Invalid avatar key." }
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "username" to cleanName,
                    "avatarKey" to avatarKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun publishTreasure(treasure: Treasure, creatorUsername: String): Result<String> {
        var docPath = "treasures"
        return try {
            val uid = requireUserId()
            val cleanName = treasure.name.trim()
            val cleanDesc = treasure.description.trim()
            val cleanClue = treasure.clue.trim()
            val cleanArtifactName = treasure.artifactName.trim()
            val cleanArtifactDesc = treasure.artifactDescription.trim()
            require(cleanName.length in 2..80) { "Treasure name must be 2–80 characters." }
            require(cleanDesc.length in 5..500) { "Description must be 5–500 characters." }
            require(cleanClue.length in 3..350) { "Clue must be 3–350 characters." }
            require(cleanArtifactName.length in 2..80) { "Artifact name must be 2–80 characters." }
            require(cleanArtifactDesc.length in 5..350) { "Artifact description must be 5–350 characters." }

            val docRef = db.collection("treasures").document()
            docPath = docRef.path
            val payload = treasure.copy(
                creatorId = uid,
                creatorName = creatorUsername.trim().take(40).ifBlank { "Explorer" },
                discoveryCount = 0L,
                status = "ACTIVE",
                visibility = "PUBLIC"
            ).toCreateMap()
            docRef.set(payload).await()

            // Update creator's profile count & log activity
            val userRef = db.collection("users").document(uid)
            userRef.update(
                mapOf(
                    "treasuresCreated" to FieldValue.increment(1L),
                    "xp" to FieldValue.increment(50L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val activityRef = userRef.collection("activity").document()
            activityRef.set(
                ActivityEntry(
                    userId = uid,
                    title = "Forged $cleanName",
                    subtitle = "Published ${treasure.rarityEnum.label} Drop (+50 Architect XP)",
                    xpDelta = 50L,
                    type = ActivityType.CREATED.name
                ).toCreateMap()
            ).await()

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unlockTreasureAndClaimArtifact(
        treasure: Treasure,
        currentProfile: UserProfile,
        method: DiscoveryMethod
    ): Result<CollectedArtifact> {
        var docPath = "users/${auth.currentUser?.uid ?: "unknown"}/artifacts"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val artifactRef = userRef.collection("artifacts").document()
            docPath = artifactRef.path

            val xpEarned = treasure.xpReward.coerceIn(25L, 10000L)
            val goldBonus = when (treasure.artifactRarityEnum) {
                Rarity.MYTHIC -> 150L
                Rarity.EPIC -> 95L
                Rarity.RARE -> 55L
                Rarity.COMMON -> 30L
            }
            val newXp = currentProfile.xp + xpEarned
            val newLevel = UserProfile.levelForXp(newXp)
            val newTitle = UserProfile.titleForLevel(newLevel)

            val artifact = CollectedArtifact(
                id = artifactRef.id,
                userId = uid,
                treasureId = treasure.id.ifBlank { "unknown_drop" },
                treasureName = treasure.name,
                artifactName = treasure.artifactName,
                artifactDescription = treasure.artifactDescription,
                rarity = treasure.artifactRarityEnum.name,
                imageKey = treasure.artifactImageKey,
                xpEarned = xpEarned,
                discoveryMethod = method.name
            )

            artifactRef.set(artifact.toCreateMap()).await()

            val hadChallenge = treasure.challengeType != "GPS_PROXIMITY"
            userRef.update(
                mapOf(
                    "xp" to newXp,
                    "level" to newLevel,
                    "title" to newTitle,
                    "goldCoins" to FieldValue.increment(goldBonus),
                    "treasuresDiscovered" to FieldValue.increment(1L),
                    "challengesCompleted" to FieldValue.increment(if (hadChallenge) 1L else 0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val activityRef = userRef.collection("activity").document()
            activityRef.set(
                ActivityEntry(
                    userId = uid,
                    title = "Claimed ${treasure.artifactName}",
                    subtitle = "${treasure.name} • +$xpEarned XP, +$goldBonus Gold",
                    xpDelta = xpEarned,
                    type = ActivityType.DISCOVERED.name
                ).toCreateMap()
            ).await()

            // Increment discoveryCount on live cloud treasures
            if (!treasure.isRealmStarterDemo && treasure.id.isNotBlank() && treasure.status == "ACTIVE") {
                try {
                    db.collection("treasures").document(treasure.id)
                        .update(
                            mapOf(
                                "discoveryCount" to FieldValue.increment(1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, "treasures/${treasure.id}")
                }
            }

            Result.success(artifact)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createMarketplaceListing(
        artifact: CollectedArtifact,
        priceGold: Long,
        sellerUsername: String
    ): Result<Unit> {
        var docPath = "marketplace_listings"
        return try {
            val uid = requireUserId()
            require(priceGold in 10L..100000L) { "Price must be between 10 and 100,000 Gold." }
            val listingRef = db.collection("marketplace_listings").document()
            docPath = listingRef.path
            val listing = MarketplaceListing(
                sellerId = uid,
                sellerName = sellerUsername.trim().take(40).ifBlank { "Explorer" },
                artifactName = artifact.artifactName,
                artifactDescription = artifact.artifactDescription,
                rarity = artifact.rarityEnum.name,
                imageKey = artifact.imageKey,
                priceGold = priceGold,
                category = ListingCategory.NEW.name,
                status = "AVAILABLE",
                visibility = "PUBLIC"
            )
            listingRef.set(listing.toCreateMap()).await()

            val userRef = db.collection("users").document(uid)
            userRef.collection("activity").document().set(
                ActivityEntry(
                    userId = uid,
                    title = "Listed ${artifact.artifactName}",
                    subtitle = "Simulated Exchange • $priceGold Gold",
                    xpDelta = 0L,
                    type = ActivityType.LISTED.name
                ).toCreateMap()
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun purchaseSimulatedListing(
        listing: MarketplaceListing,
        currentProfile: UserProfile
    ): Result<Unit> {
        var docPath = "marketplace_listings/${listing.id}"
        return try {
            val uid = requireUserId()
            require(currentProfile.goldCoins >= listing.priceGold) {
                "Not enough Gold Coins (${currentProfile.goldCoins}/${listing.priceGold})."
            }

            val userRef = db.collection("users").document(uid)
            val artifactRef = userRef.collection("artifacts").document()
            docPath = artifactRef.path

            val purchasedArtifact = CollectedArtifact(
                userId = uid,
                treasureId = listing.id.ifBlank { "exchange_relic" },
                treasureName = "Simulated Relic Exchange",
                artifactName = listing.artifactName,
                artifactDescription = listing.artifactDescription,
                rarity = listing.rarityEnum.name,
                imageKey = listing.imageKey,
                xpEarned = 25L,
                discoveryMethod = DiscoveryMethod.MARKETPLACE.name
            )
            artifactRef.set(purchasedArtifact.toCreateMap()).await()

            val newXp = currentProfile.xp + 25L
            val newLevel = UserProfile.levelForXp(newXp)
            userRef.update(
                mapOf(
                    "goldCoins" to (currentProfile.goldCoins - listing.priceGold).coerceAtLeast(0L),
                    "xp" to newXp,
                    "level" to newLevel,
                    "title" to UserProfile.titleForLevel(newLevel),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            userRef.collection("activity").document().set(
                ActivityEntry(
                    userId = uid,
                    title = "Acquired ${listing.artifactName}",
                    subtitle = "Simulated Exchange (-${listing.priceGold} Gold, +25 XP)",
                    xpDelta = 25L,
                    type = ActivityType.PURCHASED.name
                ).toCreateMap()
            ).await()

            if (!listing.isSimulatedCurated && listing.id.isNotBlank() && listing.status == "AVAILABLE") {
                db.collection("marketplace_listings").document(listing.id)
                    .update(
                        mapOf(
                            "status" to "SOLD",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
