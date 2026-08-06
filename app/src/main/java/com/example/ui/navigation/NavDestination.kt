package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for the Professional Creator Studio shell.
 *
 * Bottom navigation exposes four top-level tabs (Beranda, Studio, Live, Proyek).
 * The previous feature routes are preserved verbatim so existing deep links,
 * ViewModels, and "Go Live with this" hand-offs keep working; they are now
 * reachable through the Studio launcher instead of the bottom bar.
 */
sealed class NavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    // --- Top-level tabs ---
    object Beranda : NavDestination("beranda", "Beranda", Icons.Rounded.Home, "tab_beranda")
    object Studio : NavDestination("studio", "Studio", Icons.Rounded.AutoAwesomeMosaic, "tab_studio")
    object Live : NavDestination("live", "Live", Icons.Rounded.Radio, "tab_live")
    object Proyek : NavDestination("projects", "Proyek", Icons.Rounded.FolderSpecial, "tab_proyek")

    // --- Studio tools (existing routes preserved) ---
    object Loop : NavDestination("loop", "Video Loop", Icons.Rounded.Loop, "studio_loop")
    object Editor : NavDestination("editor", "Video Editor", Icons.Rounded.Movie, "studio_editor")
    object Mastering : NavDestination("mastering", "Audio Mastering", Icons.Rounded.Equalizer, "studio_mastering")
    object Visualizer : NavDestination("visualizer", "Visualizer Studio", Icons.Rounded.GraphicEq, "studio_visualizer")
    object Slideshow : NavDestination("slideshow", "Slideshow", Icons.Rounded.Slideshow, "studio_slideshow")

    /**
     * The former "Proyek" tab content. Preserved as a reachable sub-page (deep
     * links, "Go Live with this" hand-offs) now that Proyek hosts the real
     * Project Manager.
     */
    object History : NavDestination("history_log", "Riwayat", Icons.Rounded.FolderSpecial, "menu_history")

    // --- Additional menu ---
    object Settings : NavDestination("settings", "Pengaturan", Icons.Rounded.Settings, "menu_pengaturan")
    object Panduan : NavDestination("panduan", "Panduan", Icons.Rounded.HelpOutline, "menu_panduan")
    object About : NavDestination("about", "Tentang", Icons.Rounded.Info, "menu_tentang")
    object Privacy : NavDestination("privacy", "Kebijakan Privasi", Icons.Rounded.PrivacyTip, "menu_privasi")
    object SupportProject : NavDestination("support", "Dukungan", Icons.Rounded.Favorite, "menu_dukungan")

    companion object {
        val bottomNavItems: List<NavDestination>
            get() = listOf(Beranda, Studio, Live, Proyek)

        /** Routes that keep the bottom navigation visible. */
        val topLevelRoutes: Set<String>
            get() = bottomNavItems.map { it.route }.toSet()

        /** Routes rendered without the bottom navigation (focused tool/sub pages). */
        val subPageRoutes: Set<String>
            get() = setOf(
                Loop.route,
                Editor.route,
                Mastering.route,
                Visualizer.route,
                Slideshow.route,
                History.route,
                Settings.route,
                About.route,
                Privacy.route,
                SupportProject.route
            )
    }
}
