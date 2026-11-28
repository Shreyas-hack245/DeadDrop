package com.example.ui

import android.app.PendingIntent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.DeadDropRepository
import com.example.data.location.GeoPoint
import com.example.data.location.LocationSettingsStatus
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
import com.example.domain.model.StarterWorldSnapshot
import com.example.domain.model.Treasure
import com.example.domain.model.TreasureCategory
import com.example.domain.model.UserProfile
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class LocationPermissionStatus { UNKNOWN, GRANTED, DENIED, PERMANENTLY_DENIED }

/**
 * Everything the UI needs to know about location access, as one immutable state.
 *
 * @property latLng the most recent REAL position, or null until the first fix arrives.
 * @property isLoading true while waiting for the first fix.
 * @property timedOut true if no first fix arrived within the timeout (a later fix clears it).
 */
data class LocationUiState(
    val permission: LocationPermissionStatus = LocationPermissionStatus.UNKNOWN,
    val gpsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val hasFix: Boolean = false,
    val timedOut: Boolean = false,
    val errorMessage: String? = null,
    val latLng: LatLng? = null
) {
    val isPermissionGranted: Boolean get() = permission == LocationPermissionStatus.GRANTED

    /** No real location is available and none is on the way (declined, or the first fix timed out). */
    val isLocationUnavailable: Boolean
        get() = !hasFix && when (permission) {
            LocationPermissionStatus.UNKNOWN -> false
            LocationPermissionStatus.GRANTED -> timedOut
            else -> true
        }
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

    private var updatesJob: Job? = null
    private var acquireJob: Job? = null
    private var timeoutJob: Job? = null

    // Guards against ON_RESUME and the permission-result callback both starting the flow at once
    // (which could otherwise show the "Turn on location" dialog twice).
    private var locationStartInProgress = false

    private val _currentLocation = MutableStateFlow(LocationTracker.DEFAULT_ANCHOR)
    val currentLocation: StateFlow<GeoPoint> = _currentLocation.asStateFlow()

    private val _locationUi = MutableStateFlow(LocationUiState())
    val locationUiState: StateFlow<LocationUiState> = _locationUi.asStateFlow()

    /** One-shot request for the UI to show the system "Turn on location" dialog. Cleared once consumed. */
    private val _gpsResolution = MutableStateFlow<PendingIntent?>(null)
    val gpsResolution: StateFlow<PendingIntent?> = _gpsResolution.asStateFlow()

    // Starter world (per user, persisted in DataStore). null = nothing generated/loaded yet.
    private val _starterSnapshot = MutableStateFlow<StarterWorldSnapshot?>(null)
    private val starterSnapshotLoaded = CompletableDeferred<Unit>()
    private val starterMutex = Mutex()

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

    /**
     * Starter drops currently visible on the map:
     * - hidden when the "Starter drops" setting is off;
     * - the saved per-user layout (placed around the user's real location) when one exists;
     * - nothing while the very first fix is still pending (avoids flashing drops in the wrong city);
     * - a stable layout around the default anchor when no location is available at all (never saved).
     */
    private val starterTreasures: StateFlow<List<Treasure>> = combine(
        _starterSnapshot,
        _locationUi,
        userPreferences
    ) { snapshot, loc, prefs ->
        val waitingForFirstFix = (loc.isPermissionGranted && !loc.hasFix && !loc.timedOut) ||
            loc.permission == LocationPermissionStatus.UNKNOWN
        when {
            !prefs.showStarterDrops -> emptyList()
            snapshot != null ->
                if (waitingForFirstFix) emptyList() else StarterWorldData.buildTreasures(snapshot)
            waitingForFirstFix -> emptyList()
            else -> StarterWorldData.fallbackTreasuresAround(
                LocationTracker.DEFAULT_ANCHOR.latitude,
                LocationTracker.DEFAULT_ANCHOR.longitude
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val worldTreasures: StateFlow<List<TreasureWithDistance>> = combine(
        cloudTreasuresState,
        artifactsState,
        currentLocation,
        starterTreasures,
        exploreFilter
    ) { treasuresUi, artifactsUi, geo, starters, filter ->
        val cloudList = (treasuresUi as? UiState.Success)?.data ?: emptyList()
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
        viewModelScope.launch {
            _starterSnapshot.value = preferencesRepository.loadStarterWorld(currentUserId)
            starterSnapshotLoaded.complete(Unit)
        }
        syncPermissionFromSystem()
    }

    // ------------------------------------------------------------------------------------------
    // Location permission + live location
    // ------------------------------------------------------------------------------------------

    /**
     * Re-reads the real permission state from the system. Call on every ON_RESUME: it covers first launch,
     * process death, returning from the app-settings screen, and permission revoked while away.
     * It never shows a system dialog by itself.
     */
    fun syncPermissionFromSystem() {
        val granted = locationTracker.hasLocationPermission()
        val previous = _locationUi.value.permission
        if (granted) {
            if (previous != LocationPermissionStatus.GRANTED) {
                startLocationFlow(promptForGps = true)
            } else if (!_locationUi.value.gpsEnabled) {
                recheckGpsSilently()
            }
        } else {
            if (previous == LocationPermissionStatus.GRANTED) stopLocationWork()
            _locationUi.update {
                it.copy(
                    permission = if (previous == LocationPermissionStatus.PERMANENTLY_DENIED) {
                        LocationPermissionStatus.PERMANENTLY_DENIED
                    } else {
                        LocationPermissionStatus.DENIED
                    },
                    isLoading = false,
                    timedOut = false
                )
            }
        }
    }

    /**
     * Result of the system permission dialog. [permanentlyDenied] is computed by the UI
     * (needs an Activity: denied and shouldShowRequestPermissionRationale == false).
     */
    fun onLocationPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        if (granted && locationTracker.hasLocationPermission()) {
            startLocationFlow(promptForGps = true)
        } else {
            stopLocationWork()
            _locationUi.update {
                it.copy(
                    permission = if (permanentlyDenied) {
                        LocationPermissionStatus.PERMANENTLY_DENIED
                    } else {
                        LocationPermissionStatus.DENIED
                    },
                    isLoading = false,
                    timedOut = false
                )
            }
        }
    }

    /** Banner "Retry" / recenter: re-runs the GPS check and a fresh acquisition (permission must be granted). */
    fun retryLocation() {
        if (!locationTracker.hasLocationPermission()) return
        startLocationFlow(promptForGps = true)
    }

    /** Kept for screens that only want "make sure updates are running if we are allowed to". */
    fun startLocationUpdatesIfPermitted() {
        syncPermissionFromSystem()
    }

    fun onGpsResolutionResult(userEnabledGps: Boolean) {
        if (userEnabledGps) {
            _locationUi.update { it.copy(gpsEnabled = true) }
            beginAcquisition()
        } else {
            _locationUi.update { it.copy(gpsEnabled = false) }
        }
    }

    fun consumeGpsResolution() {
        _gpsResolution.value = null
    }

    private fun startLocationFlow(promptForGps: Boolean) {
        if (locationStartInProgress) return
        locationStartInProgress = true
        _locationUi.update {
            it.copy(
                permission = LocationPermissionStatus.GRANTED,
                isLoading = !it.hasFix,
                timedOut = false,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                when (val settings = locationTracker.checkLocationSettings()) {
                    is LocationSettingsStatus.Satisfied ->
                        _locationUi.update { it.copy(gpsEnabled = true) }
                    is LocationSettingsStatus.Resolvable -> {
                        _locationUi.update { it.copy(gpsEnabled = false) }
                        if (promptForGps) _gpsResolution.value = settings.resolution
                    }
                    is LocationSettingsStatus.Unavailable ->
                        _locationUi.update { it.copy(gpsEnabled = true) }
                }
                // Always try: a cached last location may still work even while GPS is off.
                beginAcquisition()
            } finally {
                locationStartInProgress = false
            }
        }
    }

    private fun recheckGpsSilently() {
        viewModelScope.launch {
            if (locationTracker.checkLocationSettings() is LocationSettingsStatus.Satisfied) {
                _locationUi.update { it.copy(gpsEnabled = true) }
                beginAcquisition()
            }
        }
    }

    /**
     * Starts (or restarts) the live stream, a one-shot "current location, fall back to last known" request,
     * and the ~10 s first-fix timeout. The first position from either source ends the loading state.
     */
    private fun beginAcquisition() {
        if (!locationTracker.hasLocationPermission()) return
        timeoutJob?.cancel()
        acquireJob?.cancel()
        updatesJob?.cancel()

        _locationUi.update { if (it.hasFix) it else it.copy(isLoading = true, timedOut = false) }

        updatesJob = viewModelScope.launch {
            locationTracker.observeLocationUpdates()
                .catch { Log.w(TAG, "Location stream ended: ${it.message}") }
                .collect { point -> onFix(point) }
        }
        acquireJob = viewModelScope.launch {
            val point = locationTracker.getCurrentLocationOrNull()
            // If the live stream already delivered something fresher, do not overwrite it.
            if (point != null && !_locationUi.value.hasFix) onFix(point)
        }
        timeoutJob = viewModelScope.launch {
            delay(FIRST_FIX_TIMEOUT_MS)
            if (!_locationUi.value.hasFix) {
                _locationUi.update {
                    it.copy(
                        isLoading = false,
                        timedOut = true,
                        errorMessage = "We couldn't find your location yet."
                    )
                }
            }
        }
    }

    private fun stopLocationWork() {
        timeoutJob?.cancel()
        acquireJob?.cancel()
        updatesJob?.cancel()
        timeoutJob = null
        acquireJob = null
        updatesJob = null
    }

    private fun onFix(point: GeoPoint) {
        _currentLocation.value = point
        _locationUi.update {
            it.copy(
                hasFix = true,
                isLoading = false,
                timedOut = false,
                errorMessage = null,
                latLng = LatLng(point.latitude, point.longitude)
            )
        }
        // Pre-centre the "forge drop" draft if the user has not started one yet
        _createDraft.update { draft ->
            if (draft.step == 0 && draft.name.isBlank()) {
                draft.copy(
                    latitude = point.latitude + 0.0007,
                    longitude = point.longitude + 0.0007
                )
            } else draft
        }
        ensureStarterWorld(point)
    }

    /** One-shot refresh used by "anchor to current location" in the forge wizard. */
    fun refreshLocationOnce() {
        viewModelScope.launch {
            locationTracker.getCurrentLocationOrNull()?.let { onFix(it) }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Starter drops placed relative to the user
    // ------------------------------------------------------------------------------------------

    private fun ensureStarterWorld(point: GeoPoint) {
        if (!point.isGpsLive) return
        // Cheap pre-check so a location update every ~3 s does not spawn work when nothing changed.
        val cached = _starterSnapshot.value
        if (starterSnapshotLoaded.isCompleted && cached != null &&
            !StarterWorldData.needsRegeneration(cached, distanceFromBase(cached, point))
        ) return

        viewModelScope.launch {
            starterSnapshotLoaded.await()
            starterMutex.withLock {
                val saved = _starterSnapshot.value
                if (StarterWorldData.needsRegeneration(saved, saved?.let { distanceFromBase(it, point) })) {
                    generateAndSaveStarterWorld(point)
                }
            }
        }
    }

    private fun distanceFromBase(snapshot: StarterWorldSnapshot, point: GeoPoint): Double =
        LocationTracker.distanceMeters(
            snapshot.baseLatitude, snapshot.baseLongitude, point.latitude, point.longitude
        )

    private suspend fun generateAndSaveStarterWorld(point: GeoPoint) {
        val snapshot = StarterWorldData.generateSnapshot(
            baseLatitude = point.latitude,
            baseLongitude = point.longitude,
            seed = Random.nextLong()
        )
        _starterSnapshot.value = snapshot
        try {
            preferencesRepository.saveStarterWorld(currentUserId, snapshot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist starter world", e)
        }
    }

    /** Profile > "Regenerate starter drops near me". */
    fun regenerateStarterDropsNearMe() {
        val point = _currentLocation.value
        if (!point.isGpsLive) {
            showMessage("Enable location first so starter drops can be placed near you.")
            return
        }
        viewModelScope.launch {
            starterSnapshotLoaded.await()
            starterMutex.withLock { generateAndSaveStarterWorld(point) }
            showMessage("Starter drops regenerated around your current location.")
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
        const val FIRST_FIX_TIMEOUT_MS = 10_000L
    }
}
