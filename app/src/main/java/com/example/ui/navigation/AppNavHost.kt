package com.example.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.core.audio.AudioAnalysisRepository
import com.example.core.ui.ExportViewModel
import com.example.core.work.ExportQueueViewModel
import com.example.feature.about.AboutScreen
import com.example.feature.about.PrivacyPolicyScreen
import com.example.feature.about.SupportProjectScreen
import com.example.feature.assets.AssetLibraryScreen
import com.example.feature.assets.AssetManagerViewModel
import com.example.feature.editor.EditorScreen
import com.example.feature.editor.EditorViewModel
import com.example.feature.diagnostics.DeviceDiagnosticsScreen
import com.example.feature.guide.GuideScreen
import com.example.feature.history.HistoryScreen
import com.example.feature.history.HistoryViewModel
import com.example.feature.home.HomeScreen
import com.example.feature.live.LiveScreen
import com.example.feature.live.LiveViewModel
import com.example.feature.loop.LoopScreen
import com.example.feature.loop.LoopViewModel
import com.example.feature.mastering.MasteringScreen
import com.example.feature.mastering.MasteringViewModel
import com.example.feature.project.ProjectManagerScreen
import com.example.feature.project.ProjectManagerViewModel
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import com.example.feature.slideshow.SlideshowScreen
import com.example.feature.slideshow.SlideshowViewModel
import com.example.feature.studio.StudioScreen
import com.example.feature.visualizer.VisualizerStudioScreen
import com.example.feature.visualizer.VisualizerViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
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
    exportQueueViewModel: ExportQueueViewModel?,
    audioAnalysisRepository: AudioAnalysisRepository? = null,
    assetManagerViewModel: AssetManagerViewModel? = null,
    passedLiveSourceUri: String?,
    onNavigateToGoLive: (String) -> Unit,
    onStartOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavDestination.Beranda.route,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.98f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) +
                scaleOut(targetScale = 0.98f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
        }
    ) {
        // --- Top-Level Tabs ---
        composable(NavDestination.Beranda.route) {
            HomeScreen(
                historyViewModel = historyViewModel,
                onNavigateToLoop = { navController.navigate(NavDestination.Loop.route) },
                onNavigateToEditor = { navController.navigate(NavDestination.Editor.route) },
                onNavigateToMastering = { navController.navigate(NavDestination.Mastering.route) },
                onNavigateToLive = { navController.navigate(NavDestination.Live.route) }
            )
        }
        composable(NavDestination.Studio.route) {
            StudioScreen(
                onOpenLoop = { navController.navigate(NavDestination.Loop.route) },
                onOpenEditor = { navController.navigate(NavDestination.Editor.route) },
                onOpenMastering = { navController.navigate(NavDestination.Mastering.route) },
                onOpenVisualizer = { navController.navigate(NavDestination.Visualizer.route) },
                onOpenSlideshow = { navController.navigate(NavDestination.Slideshow.route) }
            )
        }
        composable(NavDestination.Live.route) {
            LiveScreen(viewModel = liveViewModel, initialSourceUri = passedLiveSourceUri)
        }
        composable(NavDestination.Proyek.route) {
            ProjectManagerScreen(
                viewModel = projectManagerViewModel,
                onOpenHistory = { navController.navigate(NavDestination.History.route) },
                onOpenLoop = { navController.navigate(NavDestination.Loop.route) },
                onOpenEditor = { navController.navigate(NavDestination.Editor.route) },
                onOpenMastering = { navController.navigate(NavDestination.Mastering.route) },
                onOpenSlideshow = { navController.navigate(NavDestination.Slideshow.route) },
                onOpenVisualizer = { navController.navigate(NavDestination.Visualizer.route) }
            )
        }
        composable(NavDestination.History.route) {
            HistoryScreen(viewModel = historyViewModel, onNavigateToGoLive = onNavigateToGoLive)
        }

        // --- Studio Tools ---
        composable(NavDestination.Loop.route) {
            LoopScreen(exportViewModel = exportViewModel, viewModel = loopViewModel, onNavigateToGoLive = onNavigateToGoLive, assetManagerViewModel = assetManagerViewModel)
        }
        composable(NavDestination.Mastering.route) {
            MasteringScreen(exportViewModel = exportViewModel, viewModel = masteringViewModel, onNavigateToGoLive = onNavigateToGoLive, assetManagerViewModel = assetManagerViewModel)
        }
        composable(NavDestination.Editor.route) {
            EditorScreen(
                exportViewModel = exportViewModel,
                viewModel = editorViewModel,
                exportQueueViewModel = exportQueueViewModel,
                audioAnalysisRepository = audioAnalysisRepository,
                onNavigateToGoLive = onNavigateToGoLive,
                assetManagerViewModel = assetManagerViewModel
            )
        }
        composable(NavDestination.Slideshow.route) {
            SlideshowScreen(viewModel = slideshowViewModel, onNavigateToGoLive = onNavigateToGoLive)
        }
        composable(NavDestination.Visualizer.route) {
            VisualizerStudioScreen(viewModel = visualizerViewModel)
        }

        // --- Overflow Menu ---
        composable(NavDestination.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(NavDestination.About.route) },
                onNavigateToSupport = { navController.navigate(NavDestination.SupportProject.route) },
                onNavigateToDiagnostics = { navController.navigate(NavDestination.DeviceDiagnostics.route) },
                onRestartOnboarding = onStartOnboarding
            )
        }
        composable(NavDestination.About.route) {
            AboutScreen(
                onNavigateToPrivacy = { navController.navigate(NavDestination.Privacy.route) },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSupport = { navController.navigate(NavDestination.SupportProject.route) },
                onStartOnboarding = onStartOnboarding
            )
        }
        composable(NavDestination.Panduan.route) {
            GuideScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartOnboarding = onStartOnboarding
            )
        }
        composable(NavDestination.Privacy.route) {
            PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(NavDestination.SupportProject.route) {
            SupportProjectScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(NavDestination.DeviceDiagnostics.route) {
            DeviceDiagnosticsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(NavDestination.AssetLibrary.route) {
            assetManagerViewModel?.let { vm -> AssetLibraryScreen(viewModel = vm) }
        }
    }
}
