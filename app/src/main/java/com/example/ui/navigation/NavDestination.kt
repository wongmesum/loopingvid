package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for the app using Jetpack Navigation Compose.
 */
sealed class NavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    object Loop : NavDestination("loop", "Loop", Icons.Default.Loop, "tab_loop")
    object Mastering : NavDestination("mastering", "Mastering", Icons.Default.GraphicEq, "tab_mastering")
    object Editor : NavDestination("editor", "Editor", Icons.Default.Edit, "tab_editor")
    object Live : NavDestination("live", "Live", Icons.Default.Radio, "tab_live")
    object History : NavDestination("history", "History", Icons.Default.History, "tab_history")
    object Settings : NavDestination("settings", "Settings", Icons.Default.Settings, "tab_settings")
    object About : NavDestination("about", "Tentang", Icons.Default.Info, "tab_about")
    object Privacy : NavDestination("privacy", "Kebijakan Privasi", Icons.Default.Info, "tab_privacy")
    object SupportProject : NavDestination("support", "Dukung proyek", Icons.Default.FavoriteBorder, "tab_support")

    companion object {
        val bottomNavItems: List<NavDestination>
            get() = listOf(
                Loop,
                Mastering,
                Editor,
                Live,
                History
            )
    }
}
