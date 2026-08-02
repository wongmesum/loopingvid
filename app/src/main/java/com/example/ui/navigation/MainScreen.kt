package com.example.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.feature.about.AboutScreen
import com.example.feature.about.SupportProjectScreen
import com.example.feature.about.PrivacyPolicyScreen
import com.example.feature.editor.EditorScreen
import com.example.feature.editor.EditorViewModel
import com.example.feature.history.HistoryScreen
import com.example.feature.history.HistoryViewModel
import com.example.feature.live.LiveScreen
import com.example.feature.live.LiveViewModel
import com.example.feature.loop.LoopScreen
import com.example.feature.loop.LoopViewModel
import com.example.feature.mastering.MasteringScreen
import com.example.feature.mastering.MasteringViewModel
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel

/**
 * Main application screen structure built with Jetpack Navigation Compose.
 * Manages destination routing and Bottom Navigation switching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    loopViewModel: LoopViewModel,
    masteringViewModel: MasteringViewModel,
    editorViewModel: EditorViewModel,
    liveViewModel: LiveViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    exportViewModel: com.example.core.ui.ExportViewModel,
    exportQueueViewModel: com.example.core.work.ExportQueueViewModel? = null,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.Loop.route

    val exportState by exportViewModel.uiState.collectAsState()
    var passedLiveSourceUri by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val autoSaveManager = remember(context) { com.example.core.media.EditorAutoSaveManager(context) }
    var restoreDialogSessionInfo by remember { mutableStateOf<com.example.core.media.AutoSaveSessionInfo?>(null) }

    val editorUiState by editorViewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(editorUiState) {
        com.example.core.media.ProjectAutoSaveService.updateActiveProjectState(editorUiState)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.example.core.media.ProjectAutoSaveService.startService(context)
        if (autoSaveManager.isUnexpectedClosureDetected()) {
            val info = autoSaveManager.getSessionInfo()
            if (info.exists) {
                restoreDialogSessionInfo = info
            }
        }
    }

    var showOnboarding by remember {
        mutableStateOf(false)
    }
    var onboardingStepIndex by remember { mutableStateOf(0) }

    fun startOnboardingTour() {
        onboardingStepIndex = 0
        showOnboarding = true
        navController.navigate(NavDestination.Loop.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToGoLive(sourceUri: String) {
        passedLiveSourceUri = sourceUri
        liveViewModel.setSourceMedia(sourceUri, "Rendered_Source.mp4")
        navController.navigate(NavDestination.Live.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isSubPage = currentRoute == NavDestination.About.route || currentRoute == NavDestination.SupportProject.route
    var showMenu by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isSubPage) {
                val currentTitle = NavDestination.bottomNavItems.find { it.route == currentRoute }?.title ?: "LoopingVid"
                TopAppBar(
                    title = { Text(currentTitle) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    actions = {
                        IconButton(
                            onClick = { startOnboardingTour() },
                            modifier = Modifier.testTag("main_onboarding_tour_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Panduan Interaktif",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier.testTag("main_overflow_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Panduan Interaktif Studio") },
                                onClick = { 
                                    showMenu = false
                                    startOnboardingTour()
                                },
                                leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                                modifier = Modifier.testTag("menu_item_onboarding")
                            )
                            DropdownMenuItem(
                                text = { Text("Pengaturan") },
                                onClick = { 
                                    showMenu = false
                                    navController.navigate(NavDestination.Settings.route)
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                modifier = Modifier.testTag("menu_item_pengaturan")
                            )
                            DropdownMenuItem(
                                text = { Text("Tentang") },
                                onClick = { 
                                    showMenu = false
                                    navController.navigate(NavDestination.About.route)
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                modifier = Modifier.testTag("menu_item_tentang")
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isSubPage) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    NavDestination.bottomNavItems.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = { Text(destination.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(destination.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            restoreDialogSessionInfo?.let { info ->
                com.example.core.ui.RestoreSessionDialog(
                    sessionInfo = info,
                    onRestore = {
                        restoreDialogSessionInfo = null
                        editorViewModel.restoreAutoSavedSession()
                        navController.navigate(NavDestination.Editor.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onDiscard = {
                        restoreDialogSessionInfo = null
                        editorViewModel.discardAutoSavedSession()
                    }
                )
            }

            NavHost(
                navController = navController,
                startDestination = NavDestination.Loop.route,
                modifier = Modifier.weight(1f),
            enterTransition = {
                fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.98f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.98f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            },
            popEnterTransition = {
                fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.98f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            },
            popExitTransition = {
                fadeOut(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.98f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            }
        ) {
            composable(NavDestination.Loop.route) {
                LoopScreen(
                    exportViewModel = exportViewModel,
                    viewModel = loopViewModel,
                    onNavigateToGoLive = ::navigateToGoLive
                )
            }
            composable(NavDestination.Mastering.route) {
                MasteringScreen(
                    exportViewModel = exportViewModel,
                    viewModel = masteringViewModel,
                    onNavigateToGoLive = ::navigateToGoLive
                )
            }
            composable(NavDestination.Editor.route) {
                EditorScreen(
                    exportViewModel = exportViewModel,
                    viewModel = editorViewModel,
                    exportQueueViewModel = exportQueueViewModel,
                    onNavigateToGoLive = ::navigateToGoLive
                )
            }
            composable(NavDestination.Live.route) {
                LiveScreen(
                    viewModel = liveViewModel,
                    initialSourceUri = passedLiveSourceUri
                )
            }
            composable(NavDestination.History.route) {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToGoLive = ::navigateToGoLive
                )
            }
            composable(NavDestination.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAbout = {
                        navController.navigate(NavDestination.About.route)
                    },
                    onNavigateToSupport = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.facebook.com/share/1LwG2YHbik/"))
                        navController.context.startActivity(intent)
                    },
                    onRestartOnboarding = ::startOnboardingTour
                )
            }
            composable(NavDestination.About.route) {
                AboutScreen(
                    onNavigateToPrivacy = {
                        navController.navigate(NavDestination.Privacy.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSupport = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.facebook.com/share/1LwG2YHbik/"))
                        navController.context.startActivity(intent)
                    },
                    onStartOnboarding = ::startOnboardingTour
                )
            }
            composable(NavDestination.Privacy.route) {
                PrivacyPolicyScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(NavDestination.SupportProject.route) {
                SupportProjectScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }

        com.example.core.ui.ExportDialog(
            showDialog = exportState.showDialog,
            onDismiss = { exportViewModel.dismissDialog() },
            onConfirm = { fileName, format, destination, resolution, frameRate, bitrate, aspectRatio ->
                exportViewModel.updateFileName(fileName)
                exportViewModel.selectFormat(format)
                exportViewModel.selectDestination(destination)
                exportViewModel.setExportSettings(resolution, frameRate, bitrate, aspectRatio)
                exportViewModel.confirmExport()
            },
            onEnqueue = { fileName, format, destination, resolution, frameRate, bitrate, aspectRatio ->
                exportViewModel.setExportSettings(resolution, frameRate, bitrate, aspectRatio)
                val request = exportViewModel.getCurrentBatchRequest(fileName, format, destination)
                request?.let { exportQueueViewModel?.enqueueProjectExport(it) }
                exportViewModel.dismissDialog()
            },
            defaultFileName = exportState.fileName,
            availableFormats = exportState.availableFormats
        )

        com.example.core.ui.ExportSummaryDialog(
            summaryData = exportState.completedExportSummary,
            onDismiss = { exportViewModel.dismissSummary() }
        )

        com.example.core.ui.OnboardingOverlay(
            showOnboarding = showOnboarding,
            currentStepIndex = onboardingStepIndex,
            onStepChange = { onboardingStepIndex = it },
            onNavigateToRoute = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onFinishOnboarding = {
                showOnboarding = false
                com.example.core.ui.OnboardingPrefs.setCompleted(context, true)
            },
            onSkipOnboarding = {
                showOnboarding = false
                com.example.core.ui.OnboardingPrefs.setCompleted(context, true)
            }
        )
    }
    }
}
