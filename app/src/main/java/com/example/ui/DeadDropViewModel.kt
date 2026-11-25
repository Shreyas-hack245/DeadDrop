package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.DeadDropRepository
import com.example.data.location.GeoPoint
import com.example.data.location.LocationTracker
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.Achievement
import com.example.domain.model.ActivityEntry
import com.example.domain.model.ChallengeType
import com.example.domain.model.CollectedArtifact
import com.example.domain.model.Difficulty
import com.example.domain.model.DiscoveryMethod
import com.example.domain.model.ListingCategory
import com.example.domain.model.MarketplaceListing
import com.example.domain.model.Rarity
import com.example.domain.model.StarterWorldData
import com.example.domain.model.Treasure
import com.example.domain.model.TreasureCategory
import com.example.domain.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class TreasureWithDistance(
    val treasure: Treasure,
    val distanceMeters: Double,
    val isDiscoveredByMe: Boolean
)

data class ExploreFilterState(
    val query: String = "",
    val selectedRarity: Rarity? = null,
    val selectedChallenge: ChallengeType? = null,
    val liveCloudOnly: Boolean = false
)

data class CreateTreasureDraft(
    val step: Int = 0,
    val name: String = "",
    val description: String = "",
    val imageKey: String = "obsidian_chest",
    val customPhotoUri: String? = null,
    val category: TreasureCategory = TreasureCategory.ARCANE_CACHE,
    val difficulty: Difficulty = Difficulty.ADEPT,
    val rarity: Rarity = Rarity.RARE,
    val latitude: Double = LocationTracker.DEFAULT_ANCHOR.latitude + 0.0012,
    val longitude: Double = LocationTracker.DEFAULT_ANCHOR.longitude + 0.0008,
    val radiusMeters: Int = 75,
    val challengeType: ChallengeType = ChallengeType.COMBINED,
    val clue: String = "",
    val hint: String = "",
    val qrToken: String = "DEADDROP-${(1000..9999).random()}",
    val puzzleAnswer: String = "",
    val artifactName: String = "",
    val artifactDescription: String = "",
    val artifactImageKey: String = "celestial_astrolabe",
    val artifactRarity: Rarity = Rarity.RARE,
    val xpReward: Int = 350,
    val isPublishing: Boolean = false,
    val publishedTreasureId: String? = null
)

data class HuntSessionState(
    val activeTreasureId: String? = null,
    val journeyStarted: Boolean = false,
    val hintRevealed: Boolean = false,
    val simulatedProximityActive: Boolean = false,
    val qrVerified: Boolean = false,
    val puzzleSolved: Boolean = false,
    val puzzleInput: String = "",
    val puzzleError: String? = null,
    val isClaiming: Boolean = false
)

data class UnlockCelebration(
    val artifact: CollectedArtifact,
    val xpEarned: Long,
    val goldBonus: Long,
    val newLevel: Long,
    val leveledUp: Boolean
)

class DeadDropViewModel(
    private val repository: DeadDropRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val locationTracker: LocationTracker,
    val currentUserId: String,
    defaultDisplayName: String?
) : ViewModel() {

    private var locationJob: Job? = null

    private val _currentLocation = MutableStateFlow(LocationTracker.DEFAULT_ANCHOR)
    val currentLocation: StateFlow<GeoPoint> = _currentLocation.asStateFlow()

    private val _exploreFilter = MutableStateFlow(ExploreFilterState())
    val exploreFilter: StateFlow<ExploreFilterState> = _exploreFilter.asStateFlow()

    private val _selectedTreasureId = MutableStateFlow<String?>(null)
    val selectedTreasureId: StateFlow<String?> = _selectedTreasureId.asStateFlow()

    private val _huntSession = MutableStateFlow(HuntSessionState())
    val huntSession: StateFlow<HuntSessionState> = _huntSession.asStateFlow()

    private val _unlockCelebration = MutableStateFlow<UnlockCelebration?>(null)
    val unlockCelebration: StateFlow<UnlockCelebration?> = _unlockCelebration.asStateFlow()

    private val _createDraft = MutableStateFlow(CreateTreasureDraft())
    val createDraft: StateFlow<CreateTreasureDraft> = _createDraft.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferences()
        )

    private fun <T> observeWithUnwrap(
        source: kotlinx.coroutines.flow.Flow<T>,
        fallbackMessage: String
    ): StateFlow<UiState<T>> = source
        .map<T, UiState<T>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, fallbackMessage, rootError)
            emit(UiState.Error(rootError.localizedMessage ?: fallbackMessage))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val profileState: StateFlow<UiState<UserProfile>> = observeWithUnwrap(
        repository.observeUserProfile(currentUserId).map { it ?: UserProfile(userId = currentUserId) },
        "Failed to load explorer profile"
    )

    val cloudTreasuresState: StateFlow<UiState<List<Treasure>>> = observeWithUnwrap(
        repository.observePublicTreasures(currentUserId),
        "Failed to observe realm treasures"
    )

    val artifactsState: StateFlow<UiState<List<CollectedArtifact>>> = observeWithUnwrap(
        repository.observeUserArtifacts(currentUserId),
        "Failed to observe artifact vault"
    )

    val activityState: StateFlow<UiState<List<ActivityEntry>>> = observeWithUnwrap(
        repository.observeUserActivity(currentUserId),
        "Failed to observe explorer log"
    )

    val listingsState: StateFlow<UiState<List<MarketplaceListing>>> = observeWithUnwrap(
        repository.observeMarketplaceListings(currentUserId),
        "Failed to observe relic exchange"
    )

    val worldTreasures: StateFlow<List<TreasureWithDistance>> = combine(
        cloudTreasuresState,
        artifactsState,
        currentLocation,
        userPreferences,
        exploreFilter
    ) { treasuresUi, artifactsUi, geo, prefs, filter ->
        val cloudList = (treasuresUi as? UiState.Success)?.data ?: emptyList()
        val starters = if (prefs.showStarterDrops) {
            StarterWorldData.starterTreasuresAround(geo.latitude, geo.longitude)
        } else {
            emptyList()
        }
        val discoveredIds = ((artifactsUi as? UiState.Success)?.data ?: emptyList())
            .map { it.treasureId }
            .toSet()

        val merged = (cloudList.filter { it.status == "ACTIVE" } + starters)
            .map { t ->
                val dist = LocationTracker.distanceMeters(
                    geo.latitude, geo.longitude, t.latitude, t.longitude
                )
                TreasureWithDistance(
                    treasure = t,
                    distanceMeters = dist,
                    isDiscoveredByMe = discoveredIds.contains(t.id)
                )
            }
            .filter { item ->
                val t = item.treasure
                val matchesQuery = filter.query.isBlank() ||
                    t.name.contains(filter.query, ignoreCase = true) ||
                    t.artifactName.contains(filter.query, ignoreCase = true) ||
                    t.categoryEnum.label.contains(filter.query, ignoreCase = true)
                val matchesRarity = filter.selectedRarity == null || t.rarityEnum == filter.selectedRarity
                val matchesChallenge = filter.selectedChallenge == null || t.challengeTypeEnum == filter.selectedChallenge
                val matchesCloud = !filter.liveCloudOnly || !t.isRealmStarterDemo
                matchesQuery && matchesRarity && matchesChallenge && matchesCloud
            }
            .sortedBy { it.distanceMeters }
        merged
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val mergedListings: StateFlow<List<MarketplaceListing>> = listingsState.map { ui ->
        val cloud = (ui as? UiState.Success)?.data
            ?.filter { it.status == "AVAILABLE" }
            ?.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            ?: emptyList()
        cloud + StarterWorldData.simulatedCuratedListings
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StarterWorldData.simulatedCuratedListings
    )

    val achievements: StateFlow<List<Achievement>> = combine(
        profileState,
        artifactsState
    ) { profUi, artUi ->
        val prof = (profUi as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
        val arts = (artUi as? UiState.Success)?.data ?: emptyList()
        StarterWorldData.computeAchievements(prof, arts)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(defaultDisplayName)
        }
        refreshLocationOnce()
    }

    fun startLocationUpdatesIfPermitted() {
        if (!locationTracker.hasLocationPermission()) return
        if (locationJob?.isActive == true) return
        refreshLocationOnce()
        locationJob = viewModelScope.launch {
            locationTracker.observeLocationUpdates()
                .catch { Log.w(TAG, "Location stream ended: ${it.message}") }
                .collect { point ->
                    _currentLocation.value = point
                }
        }
    }

    fun refreshLocationOnce() {
        viewModelScope.launch {
            val fresh = locationTracker.getCurrentLocationOrNull()
            if (fresh != null) {
                _currentLocation.value = fresh
                // Also pre-center draft location if user hasn't picked one yet
                _createDraft.update { draft ->
                    if (draft.step == 0 && draft.name.isBlank()) {
                        draft.copy(
                            latitude = fresh.latitude + 0.0007,
                            longitude = fresh.longitude + 0.0007
                        )
                    } else draft
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _exploreFilter.update { it.copy(query = query) }
    }

    fun toggleRarityFilter(rarity: Rarity?) {
        _exploreFilter.update {
            it.copy(selectedRarity = if (it.selectedRarity == rarity) null else rarity)
        }
    }

    fun toggleChallengeFilter(type: ChallengeType?) {
        _exploreFilter.update {
            it.copy(selectedChallenge = if (it.selectedChallenge == type) null else type)
        }
    }

    fun toggleLiveCloudOnly() {
        _exploreFilter.update { it.copy(liveCloudOnly = !it.liveCloudOnly) }
    }

    fun selectTreasure(treasureId: String?) {
        _selectedTreasureId.value = treasureId
    }

    fun beginHuntFor(treasureId: String) {
        _huntSession.value = HuntSessionState(
            activeTreasureId = treasureId,
            journeyStarted = true
        )
    }

    fun revealHint() {
        _huntSession.update { it.copy(hintRevealed = true) }
    }

    fun setSimulatedProximity(enabled: Boolean) {
        _huntSession.update { it.copy(simulatedProximityActive = enabled) }
    }

    fun updatePuzzleInput(value: String) {
        _huntSession.update { it.copy(puzzleInput = value, puzzleError = null) }
    }

    fun submitPuzzleAnswer(treasure: Treasure) {
        val expected = treasure.puzzleAnswer?.trim().orEmpty()
        val attempt = _huntSession.value.puzzleInput.trim()
        if (attempt.isBlank()) {
            _huntSession.update { it.copy(puzzleError = "Enter an answer to break the seal.") }
            return
        }
        if (expected.isEmpty() || attempt.equals(expected, ignoreCase = true) ||
            (treasure.id == "starter_lantern_cipher" && attempt.equals("SOULFLAME", ignoreCase = true))
        ) {
            _huntSession.update { it.copy(puzzleSolved = true, puzzleError = null) }
            showMessage("Arcane cipher decrypted!")
        } else {
            _huntSession.update { it.copy(puzzleError = "Incorrect rune phrase. Consult the clue or hint.") }
        }
    }

    fun verifyScannedQrCode(treasure: Treasure, scannedRaw: String): Boolean {
        val expected = treasure.qrToken?.trim().orEmpty()
        val normalized = scannedRaw.trim()
        val valid = expected.isEmpty() || normalized.equals(expected, ignoreCase = true)
        if (valid) {
            _huntSession.update { it.copy(qrVerified = true) }
            showMessage("QR Seal verified!")
        } else {
            showMessage("Seal mismatch: expected ${expected.take(24)}, scanned ${normalized.take(24)}")
        }
        return valid
    }

    fun simulateDemoQrScan(treasure: Treasure) {
        _huntSession.update { it.copy(qrVerified = true) }
        showMessage("Demo QR Seal simulated for ${treasure.name}")
    }

    fun claimTreasureReward(treasure: Treasure, actualDistanceMeters: Double) {
        val currentHunt = _huntSession.value
        val profile = (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
        val withinRealGps = actualDistanceMeters <= treasure.radiusMeters
        val proximityPassed = withinRealGps || currentHunt.simulatedProximityActive

        val method = when {
            currentHunt.simulatedProximityActive && !withinRealGps -> DiscoveryMethod.DEMO_SIMULATION
            treasure.challengeTypeEnum == ChallengeType.COMBINED -> DiscoveryMethod.COMBINED
            treasure.challengeTypeEnum == ChallengeType.QR_SCAN -> DiscoveryMethod.QR
            treasure.challengeTypeEnum == ChallengeType.PUZZLE_ANSWER -> DiscoveryMethod.PUZZLE
            else -> DiscoveryMethod.GPS
        }

        if (!proximityPassed && (treasure.challengeTypeEnum == ChallengeType.GPS_PROXIMITY || treasure.challengeTypeEnum == ChallengeType.COMBINED)) {
            showMessage("Move within ${treasure.radiusMeters}m (or enable Demo Simulation) to unlock.")
            return
        }

        viewModelScope.launch {
            _huntSession.update { it.copy(isClaiming = true) }
            val result = repository.unlockTreasureAndClaimArtifact(treasure, profile, method)
            _huntSession.update { it.copy(isClaiming = false) }
            result.onSuccess { claimed ->
                val newXp = profile.xp + claimed.xpEarned
                val newLevel = UserProfile.levelForXp(newXp)
                val goldBonus = when (treasure.artifactRarityEnum) {
                    Rarity.MYTHIC -> 150L
                    Rarity.EPIC -> 95L
                    Rarity.RARE -> 55L
                    Rarity.COMMON -> 30L
                }
                _unlockCelebration.value = UnlockCelebration(
                    artifact = claimed,
                    xpEarned = claimed.xpEarned,
                    goldBonus = goldBonus,
                    newLevel = newLevel,
                    leveledUp = newLevel > profile.level
                )
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to claim relic.")
            }
        }
    }

    fun dismissUnlockCelebration() {
        _unlockCelebration.value = null
        _huntSession.value = HuntSessionState()
    }

    // --- Create Treasure 6-Step Wizard ---
    fun updateDraft(transform: (CreateTreasureDraft) -> CreateTreasureDraft) {
        _createDraft.update(transform)
    }

    fun resetCreateDraft() {
        val loc = _currentLocation.value
        _createDraft.value = CreateTreasureDraft(
            latitude = loc.latitude + 0.0009,
            longitude = loc.longitude + 0.0009,
            qrToken = "DEADDROP-${(1000..9999).random()}"
        )
    }

    fun setDraftToCurrentLocation() {
        refreshLocationOnce()
        val loc = _currentLocation.value
        _createDraft.update { it.copy(latitude = loc.latitude, longitude = loc.longitude) }
        showMessage("Pin anchored to current coordinates")
    }

    fun publishDraftTreasure() {
        val draft = _createDraft.value
        val profile = (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
        val treasure = Treasure(
            creatorId = currentUserId,
            creatorName = profile.username,
            name = draft.name,
            description = draft.description,
            category = draft.category.name,
            difficulty = draft.difficulty.name,
            rarity = draft.rarity.name,
            imageKey = draft.imageKey,
            latitude = draft.latitude,
            longitude = draft.longitude,
            radiusMeters = draft.radiusMeters.toLong(),
            challengeType = draft.challengeType.name,
            clue = draft.clue,
            hint = draft.hint.ifBlank { null },
            qrToken = if (draft.challengeType == ChallengeType.QR_SCAN || draft.challengeType == ChallengeType.COMBINED) {
                draft.qrToken.ifBlank { "DEADDROP-${(1000..9999).random()}" }
            } else null,
            puzzleAnswer = if (draft.challengeType == ChallengeType.PUZZLE_ANSWER || draft.challengeType == ChallengeType.COMBINED) {
                draft.puzzleAnswer.ifBlank { "ARCANE" }
            } else null,
            artifactName = draft.artifactName,
            artifactDescription = draft.artifactDescription,
            artifactImageKey = draft.artifactImageKey,
            artifactRarity = draft.artifactRarity.name,
            xpReward = draft.xpReward.toLong()
        )
        viewModelScope.launch {
            _createDraft.update { it.copy(isPublishing = true) }
            val result = repository.publishTreasure(treasure, profile.username)
            result.onSuccess { newId ->
                _createDraft.update { it.copy(isPublishing = false, publishedTreasureId = newId) }
                showMessage("DeadDrop published live to the realm!")
            }.onFailure { err ->
                _createDraft.update { it.copy(isPublishing = false) }
                showMessage(err.localizedMessage ?: "Failed to publish treasure.")
            }
        }
    }

    // --- Marketplace & Collection Actions ---
    fun listArtifactOnExchange(artifact: CollectedArtifact, priceGold: Long) {
        val profile = (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
        viewModelScope.launch {
            val result = repository.createMarketplaceListing(artifact, priceGold, profile.username)
            result.onSuccess {
                showMessage("Listed ${artifact.artifactName} on the Simulated Exchange!")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Unable to list relic.")
            }
        }
    }

    fun purchaseSimulatedListing(listing: MarketplaceListing) {
        val profile = (profileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
        viewModelScope.launch {
            val result = repository.purchaseSimulatedListing(listing, profile)
            result.onSuccess {
                showMessage("Acquired ${listing.artifactName}! Added to your Vault.")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Purchase failed.")
            }
        }
    }

    // --- Profile & Settings ---
    fun updateProfile(username: String, avatarKey: String) {
        viewModelScope.launch {
            val result = repository.updateProfileDetails(username, avatarKey)
            result.onSuccess {
                showMessage("Explorer sigil updated")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHapticsEnabled(enabled) }
    }

    fun setShowStarterDrops(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowStarterDrops(show) }
    }

    fun setPreferredMapLayer(layer: String) {
        viewModelScope.launch { preferencesRepository.setPreferredMapLayer(layer) }
    }

    fun showMessage(message: String) {
        _statusBanner.value = message
    }

    fun clearMessage() {
        _statusBanner.value = null
    }

    private companion object {
        const val TAG = "DeadDropVM"
    }
}
