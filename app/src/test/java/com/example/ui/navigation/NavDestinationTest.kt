package com.example.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavDestinationTest {

    @Test
    fun `bottom nav items exactly match expected top level routes`() {
        val bottomItems = NavDestination.bottomNavItems.map { it.route }
        assertEquals(4, bottomItems.size)
        assertTrue(bottomItems.contains(NavDestination.Beranda.route))
        assertTrue(bottomItems.contains(NavDestination.Studio.route))
        assertTrue(bottomItems.contains(NavDestination.Live.route))
        assertTrue(bottomItems.contains(NavDestination.Proyek.route))

        val topRoutes = NavDestination.topLevelRoutes
        assertEquals(bottomItems.toSet(), topRoutes)
    }

    @Test
    fun `sub pages exclude bottom nav and include all features`() {
        val subPages = NavDestination.subPageRoutes

        // Studio tools
        assertTrue(subPages.contains(NavDestination.Loop.route))
        assertTrue(subPages.contains(NavDestination.Editor.route))
        assertTrue(subPages.contains(NavDestination.Mastering.route))
        assertTrue(subPages.contains(NavDestination.Visualizer.route))
        assertTrue(subPages.contains(NavDestination.Slideshow.route))

        // Additional menus
        assertTrue(subPages.contains(NavDestination.History.route))
        assertTrue(subPages.contains(NavDestination.Settings.route))
        assertTrue(subPages.contains(NavDestination.Panduan.route))
        assertTrue(subPages.contains(NavDestination.About.route))
        assertTrue(subPages.contains(NavDestination.Privacy.route))
        assertTrue(subPages.contains(NavDestination.SupportProject.route))

        // Exclude bottom nav items
        assertTrue(subPages.intersect(NavDestination.topLevelRoutes).isEmpty())
    }
}
