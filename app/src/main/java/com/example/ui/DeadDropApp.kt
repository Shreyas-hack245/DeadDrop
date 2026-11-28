package com.example.ui

import android.Manifest
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.firebase.DeadDropRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.data.location.LocationTracker
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.CreateTreasureScreen
import com.example.ui.screens.LocationRationaleDialog
import com.example.ui.screens.findActivity
import com.example.ui.screens.openAppSettings
import com.example.ui.screens.ExploreMapScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TreasureDetailAndHuntScreen
import com.example.ui.screens.UnlockCelebrationDialog
import com.example.ui.theme.AmethystGlow
import com.example.ui.theme.ArcaneSurface
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TreasureGold
import com.example.ui.theme.VoidBlack
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object DeadDropRoutes {
    const val EXPLORE = "explore"
    const val VAULT = "vault"
    const val EXCHANGE = "exchange"
    const val PROFILE = "profile"
    const val CREATE_TREASURE = "create_treasure"
    const val TREASURE_DETAIL = "treasure_detail/{treasureId}"
    const val QR_SCANNER = "qr_scanner/{treasureId}"

    fun detailRoute(treasureId: String) = "treasure_detail/${Uri.encode(treasureId)}"
    fun qrRoute(treasureId: String) = "qr_scanner/${Uri.encode(treasureId)}"
}

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val mainTabs = listOf(
    BottomTab(DeadDropRoutes.EXPLORE, "Explore", Icons.Rounded.Explore, Icons.Outlined.Explore, "nav_explore"),
    BottomTab(DeadDropRoutes.VAULT, "Vault", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_vault"),
    BottomTab(DeadDropRoutes.EXCHANGE, "Exchange", Icons.Rounded.Storefront, Icons.Outlined.Storefront, "nav_exchange"),
    BottomTab(DeadDropRoutes.PROFILE, "Profile", Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
fun DeadDropApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val preferences by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = UserPreferences())
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var showSplash by remember { mutableStateOf(true) }

    when {
        showSplash -> {
            SplashScreen(onFinished = { showSplash = false })
        }
        !preferences.onboardingCompleted -> {
            OnboardingScreen(
                onComplete = {
                    scope.launch { prefsRepo.setOnboardingCompleted(true) }
                }
            )
        }
        currentUser == null -> {
            AuthScreen(onAuthSuccess = {})
        }
        else -> {
            val user = currentUser!!
            key(user.uid) {
                AuthenticatedDeadDropShell(
                    currentUserId = user.uid,
                    defaultDisplayName = user.displayName
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedDeadDropShell(
    currentUserId: String,
    defaultDisplayName: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DeadDropViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DeadDropViewModel(
                    repository = DeadDropRepository(db),
                    preferencesRepository = UserPreferencesRepository(app),
                    locationTracker = LocationTracker(app),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showMainNav = currentRoute in mainTabs.map { it.route }

    val treasures by viewModel.worldTreasures.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val selectedTreasureId by viewModel.selectedTreasureId.collectAsStateWithLifecycle()
    val exploreFilter by viewModel.exploreFilter.collectAsStateWithLifecycle()
    val huntSession by viewModel.huntSession.collectAsStateWithLifecycle()
    val unlockCelebration by viewModel.unlockCelebration.collectAsStateWithLifecycle()
    val createDraft by viewModel.createDraft.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val artifactsState by viewModel.artifactsState.collectAsStateWithLifecycle()
    val activityState by viewModel.activityState.collectAsStateWithLifecycle()
    val listings by viewModel.mergedListings.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val locationState by viewModel.locationUiState.collectAsStateWithLifecycle()
    val pendingGpsResolution by viewModel.gpsResolution.collectAsStateWithLifecycle()

    // ---- Location permission + GPS-settings flow ------------------------------------------------
    // "Not now" is remembered across rotation AND process death (rememberSaveable), but not across
    // fresh launches, so the rationale is offered again next time while permission is still missing.
    var rationaleDismissed by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val activity = context.findActivity()
        // Denied + system will not show the dialog again => "don't ask again": send the user to settings next time.
        val permanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        viewModel.onLocationPermissionResult(granted, permanentlyDenied)
    }

    val gpsResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onGpsResolutionResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(pendingGpsResolution) {
        pendingGpsResolution?.let { pending ->
            viewModel.consumeGpsResolution()
            gpsResolutionLauncher.launch(IntentSenderRequest.Builder(pending).build())
        }
    }

    // Re-read the real permission state whenever the app returns to the foreground
    // (first start, back from system settings, process restore). Never shows a dialog by itself.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.syncPermissionFromSystem()
    }

    val requestLocationAccess: () -> Unit = {
        when {
            locationState.isPermissionGranted -> viewModel.retryLocation()
            locationState.permission == LocationPermissionStatus.PERMANENTLY_DENIED -> openAppSettings(context)
            else -> permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val showLocationRationale = !rationaleDismissed &&
        locationState.permission != LocationPermissionStatus.UNKNOWN &&
        !locationState.isPermissionGranted

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = VoidBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = ArcaneSurface,
                        contentColor = TextPrimary,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            bottomBar = {
                if (showMainNav && !useRail) {
                    NavigationBar(
                        containerColor = MidnightIndigo.copy(alpha = 0.96f),
                        windowInsets = NavigationBarDefaults.windowInsets,
                        tonalElevation = 8.dp
                    ) {
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag(tab.testTag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TreasureGold,
                                    selectedTextColor = TreasureGold,
                                    indicatorColor = NeonViolet.copy(alpha = 0.25f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showMainNav && useRail) {
                    NavigationRail(
                        containerColor = MidnightIndigo,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = TreasureGold,
                                    selectedTextColor = TreasureGold,
                                    indicatorColor = NeonViolet.copy(alpha = 0.25f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = DeadDropRoutes.EXPLORE,
                        enterTransition = {
                            fadeIn(tween(220)) + slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Up,
                                tween(220)
                            )
                        },
                        exitTransition = { fadeOut(tween(180)) }
                    ) {
                        composable(DeadDropRoutes.EXPLORE) {
                            ExploreMapScreen(
                                treasures = treasures,
                                currentLocation = currentLocation,
                                selectedTreasureId = selectedTreasureId,
                                filterState = exploreFilter,
                                onSearchChange = viewModel::updateSearchQuery,
                                onToggleRarity = viewModel::toggleRarityFilter,
                                onToggleChallenge = viewModel::toggleChallengeFilter,
                                onToggleLiveCloudOnly = viewModel::toggleLiveCloudOnly,
                                onSelectTreasure = viewModel::selectTreasure,
                                onOpenTreasureDetail = { id ->
                                    navController.navigate(DeadDropRoutes.detailRoute(id))
                                },
                                onCreateTreasureClick = {
                                    viewModel.resetCreateDraft()
                                    navController.navigate(DeadDropRoutes.CREATE_TREASURE)
                                },
                                locationState = locationState,
                                onEnableLocation = requestLocationAccess,
                                onTurnOnGps = viewModel::retryLocation,
                                onRetryLocation = viewModel::retryLocation
                            )
                        }

                        composable(DeadDropRoutes.VAULT) {
                            CollectionScreen(
                                artifactsState = artifactsState,
                                onExploreClick = {
                                    navController.navigate(DeadDropRoutes.EXPLORE) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onListOnMarketplace = viewModel::listArtifactOnExchange
                            )
                        }

                        composable(DeadDropRoutes.EXCHANGE) {
                            MarketplaceScreen(
                                listings = listings,
                                profileState = profileState,
                                artifactsState = artifactsState,
                                onPurchaseListing = viewModel::purchaseSimulatedListing,
                                onCreateListing = viewModel::listArtifactOnExchange
                            )
                        }

                        composable(DeadDropRoutes.PROFILE) {
                            ProfileScreen(
                                profileState = profileState,
                                activityState = activityState,
                                achievements = achievements,
                                preferences = preferences,
                                onUpdateProfile = viewModel::updateProfile,
                                onToggleHaptics = viewModel::setHapticsEnabled,
                                onToggleStarterDrops = viewModel::setShowStarterDrops,
                                onRegenerateStarterDrops = viewModel::regenerateStarterDropsNearMe,
                                onSignOut = {
                                    signOut(credentialManager, scope)
                                }
                            )
                        }

                        composable(DeadDropRoutes.CREATE_TREASURE) {
                            CreateTreasureScreen(
                                draft = createDraft,
                                currentLocation = currentLocation,
                                onUpdateDraft = viewModel::updateDraft,
                                onUseCurrentLocation = viewModel::setDraftToCurrentLocation,
                                onPublish = viewModel::publishDraftTreasure,
                                onDoneAndClose = {
                                    viewModel.resetCreateDraft()
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = DeadDropRoutes.TREASURE_DETAIL,
                            arguments = listOf(navArgument("treasureId") { type = NavType.StringType })
                        ) { entry ->
                            val id = Uri.decode(entry.arguments?.getString("treasureId").orEmpty())
                            val match = treasures.firstOrNull { it.treasure.id == id }
                            if (match != null) {
                                TreasureDetailAndHuntScreen(
                                    treasure = match.treasure,
                                    currentLocation = currentLocation,
                                    isAlreadyDiscovered = match.isDiscoveredByMe,
                                    huntSession = if (huntSession.activeTreasureId == id) huntSession else HuntSessionState(activeTreasureId = id),
                                    onBack = { navController.popBackStack() },
                                    onStartJourney = { viewModel.beginHuntFor(id) },
                                    onRevealHint = viewModel::revealHint,
                                    onToggleSimulatedProximity = viewModel::setSimulatedProximity,
                                    onRequestLocationUpdates = viewModel::startLocationUpdatesIfPermitted,
                                    onOpenQrScanner = {
                                        navController.navigate(DeadDropRoutes.qrRoute(id))
                                    },
                                    onPuzzleInputChange = viewModel::updatePuzzleInput,
                                    onSubmitPuzzle = { viewModel.submitPuzzleAnswer(match.treasure) },
                                    onClaimReward = { dist -> viewModel.claimTreasureReward(match.treasure, dist) }
                                )
                            } else {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            }
                        }

                        composable(
                            route = DeadDropRoutes.QR_SCANNER,
                            arguments = listOf(navArgument("treasureId") { type = NavType.StringType })
                        ) { entry ->
                            val id = Uri.decode(entry.arguments?.getString("treasureId").orEmpty())
                            val match = treasures.firstOrNull { it.treasure.id == id }
                            if (match != null) {
                                QrScannerScreen(
                                    treasure = match.treasure,
                                    onBack = { navController.popBackStack() },
                                    onQrScanned = { raw -> viewModel.verifyScannedQrCode(match.treasure, raw) },
                                    onSimulateDemoQr = { viewModel.simulateDemoQrScan(match.treasure) }
                                )
                            } else {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            }
                        }
                    }
                }
            }
        }

        if (showLocationRationale) {
            LocationRationaleDialog(
                permanentlyDenied = locationState.permission == LocationPermissionStatus.PERMANENTLY_DENIED,
                onEnable = {
                    rationaleDismissed = true
                    requestLocationAccess()
                },
                onNotNow = { rationaleDismissed = true }
            )
        }

        unlockCelebration?.let { celebration ->
            UnlockCelebrationDialog(
                celebration = celebration,
                onDismiss = {
                    viewModel.dismissUnlockCelebration()
                    navController.popBackStack(DeadDropRoutes.EXPLORE, inclusive = false)
                }
            )
        }
    }
}
