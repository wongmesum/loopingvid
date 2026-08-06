package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.ui.ExportViewModel
import com.example.core.work.ExportQueueViewModel
import com.example.feature.editor.EditorViewModel
import com.example.feature.history.HistoryViewModel
import com.example.feature.live.LiveViewModel
import com.example.feature.loop.LoopViewModel
import com.example.feature.mastering.MasteringViewModel
import com.example.feature.project.ProjectManagerViewModel
import com.example.feature.settings.SettingsViewModel
import com.example.feature.slideshow.SlideshowViewModel
import com.example.feature.visualizer.VisualizerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    loopViewModel: LoopViewModel,
    masteringViewModel: MasteringViewModel,
    editorViewModel: EditorViewModel,
    liveViewModel: LiveViewModel,
    historyViewModel: HistoryViewModel,
    projectManagerViewModel: ProjectManagerViewModel,
    settingsViewModel: SettingsViewModel,
    slideshowViewModel: SlideshowViewModel,
    visualizerViewModel: VisualizerViewModel,
    exportViewModel: ExportViewModel,
    exportQueueViewModel: ExportQueueViewModel? = null,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.Beranda.route

    val exportState by exportViewModel.uiState.collectAsState()
    var passedLiveSourceUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val autoSaveManager = remember(context) { com.example.core.media.EditorAutoSaveManager(context) }
    var restoreDialogSessionInfo by remember { mutableStateOf<com.example.core.media.AutoSaveSessionInfo?>(null) }

    val editorUiState by editorViewModel.uiState.collectAsState()
    LaunchedEffect(editorUiState) {
        com.example.core.media.ProjectAutoSaveService.updateActiveProjectState(editorUiState)
    }

    LaunchedEffect(Unit) {
        com.example.core.media.ProjectAutoSaveService.startService(context)
        if (autoSaveManager.isUnexpectedClosureDetected()) {
            val info = autoSaveManager.getSessionInfo()
            if (info.exists) {
                restoreDialogSessionInfo = info
            }
        }
    }

    var showOnboarding by remember { mutableStateOf(false) }
    var onboardingStepIndex by remember { mutableIntStateOf(0) }

    fun startOnboardingTour() {
        onboardingStepIndex = 0
        showOnboarding = true
        navController.navigate(NavDestination.Loop.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToGoLive(sourceUri: String) {
        passedLiveSourceUri = sourceUri
        liveViewModel.setSourceMedia(sourceUri, "Rendered_Source.mp4")
        navController.navigate(NavDestination.Live.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isTopLevelRoute = NavDestination.topLevelRoutes.contains(currentRoute)
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (isTopLevelRoute) {
                    val currentTitle = NavDestination.bottomNavItems.find { it.route == currentRoute }?.title ?: "LoopingVid"
                    TopAppBar(
                        title = { Text(currentTitle) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        actions = {
                            IconButton(
                                onClick = { navController.navigate(NavDestination.Panduan.route) },
                                modifier = Modifier.testTag("main_onboarding_tour_button")
                            ) {
                                Icon(Icons.Rounded.HelpOutline, contentDescription = "Panduan", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { showMenu = !showMenu },
                                modifier = Modifier.testTag("main_overflow_menu_button")
                            ) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Panduan") },
                                    onClick = { showMenu = false; navController.navigate(NavDestination.Panduan.route) },
                                    leadingIcon = { Icon(Icons.Rounded.HelpOutline, contentDescription = null) },
                                    modifier = Modifier.testTag("menu_item_panduan")
                                )
                                DropdownMenuItem(
                                    text = { Text("Pengaturan") },
                                    onClick = { showMenu = false; navController.navigate(NavDestination.Settings.route) },
                                    leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                    modifier = Modifier.testTag("menu_item_pengaturan")
                                )
                                DropdownMenuItem(
                                    text = { Text("Tentang") },
                                    onClick = { showMenu = false; navController.navigate(NavDestination.About.route) },
                                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                                    modifier = Modifier.testTag("menu_item_tentang")
                                )
                                DropdownMenuItem(
                                    text = { Text("Kebijakan Privasi") },
                                    onClick = { showMenu = false; navController.navigate(NavDestination.Privacy.route) },
                                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                                    modifier = Modifier.testTag("menu_item_privasi")
                                )
                                DropdownMenuItem(
                                    text = { Text("Dukungan") },
                                    onClick = { showMenu = false; navController.navigate(NavDestination.SupportProject.route) },
                                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                                    modifier = Modifier.testTag("menu_item_dukungan")
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavDestination.bottomNavItems.forEach { destination ->
                            val selected = currentRoute == destination.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != destination.route) {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = destination.title) },
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
            Column(
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
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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

                AppNavHost(
                    navController = navController,
                    loopViewModel = loopViewModel,
                    masteringViewModel = masteringViewModel,
                    editorViewModel = editorViewModel,
                    liveViewModel = liveViewModel,
                    historyViewModel = historyViewModel,
                    projectManagerViewModel = projectManagerViewModel,
                    settingsViewModel = settingsViewModel,
                    slideshowViewModel = slideshowViewModel,
                    visualizerViewModel = visualizerViewModel,
                    exportViewModel = exportViewModel,
                    exportQueueViewModel = exportQueueViewModel,
                    passedLiveSourceUri = passedLiveSourceUri,
                    onNavigateToGoLive = ::navigateToGoLive,
                    onStartOnboarding = ::startOnboardingTour,
                    modifier = Modifier.weight(1f)
                )
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
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
