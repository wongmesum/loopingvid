package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// --- Professional Creator Studio Premium Dark Palette ---

// Core Surfaces
val ProBackground = Color(0xFF0B0D12)       // Deepest background
val ProSurface = Color(0xFF131720)          // Card/sheet surface
val ProSurfaceElevated = Color(0xFF1A202C)  // Elevated dialogs, modals
val ProSurfaceHigh = Color(0xFF222A38)      // High-elevation containers

// Primary Accent (Purple)
val ProPrimary = Color(0xFF7C5CFF)          // Main action/brand color
val ProPrimaryDim = Color(0xFF5B3FCC)       // Pressed/dimmed primary
val ProPrimaryContainer = Color(0xFF2A1F5C) // Container behind primary content
val ProOnPrimaryContainer = Color(0xFFE0D6FF) // Text on primary container

// Secondary Accent (Cyan)
val ProSecondary = Color(0xFF22D3EE)        // Supporting accent
val ProSecondaryDim = Color(0xFF1AA3B8)     // Dimmed secondary
val ProSecondaryContainer = Color(0xFF0F3D45) // Container behind secondary
val ProOnSecondaryContainer = Color(0xFFCCF7FE) // Text on secondary container

// Status / Semantic
val ProSuccess = Color(0xFF34D399)          // Render complete, success
val ProLive = Color(0xFFFF4D67)             // Live indicator, error, destructive
val ProWarning = Color(0xFFFBBF24)          // Caution, near-clip

// Text & Content
val ProTextPrimary = Color(0xFFF1F3F9)      // High-contrast titles, body
val ProTextSecondary = Color(0xFF8B95A8)    // Muted/secondary text
val ProTextTertiary = Color(0xFF5A6478)     // Placeholder, disabled

// Outline & Divider
val ProOutline = Color(0xFF2A3342)          // Card borders, dividers
val ProOutlineVariant = Color(0xFF3A4556)   // Subtle accent borders

// ============================================================
// BACKWARD COMPATIBILITY ALIASES
// Existing screens reference these. DO NOT REMOVE.
// ============================================================

// Deep Charcoal Canvas & Surface Shades (old palette remapped)
val CharcoalBackground = ProBackground
val CharcoalSurface = ProSurface
val CharcoalSurfaceVariant = ProSurfaceElevated
val CharcoalSurfaceHigh = ProSurfaceHigh

// Subtle Gold → now maps to secondary cyan (closest warm-accent replacement)
val ElegantGold = ProSecondary
val ElegantGoldDim = ProSecondaryDim
val ElegantGoldContainer = ProSecondaryContainer
val OnElegantGoldContainer = ProOnSecondaryContainer
val GoldAccent = ProWarning

// Refined Purple / Lavender
val RoyalPurple = ProPrimary
val RoyalPurpleContainer = ProPrimaryContainer
val OnRoyalPurpleContainer = ProOnPrimaryContainer

// Text & Content Shades
val TextPrimaryDark = ProTextPrimary
val TextSecondaryDark = ProTextSecondary
val OutlineDark = ProOutline

// Functional Status Colors
val StudioLiveRed = ProLive
val StudioSuccessGreen = ProSuccess

// Studio* bindings used throughout the codebase
val StudioPrimary = RoyalPurple
val StudioOnPrimary = Color(0xFF1A0F3C)
val StudioPrimaryContainer = RoyalPurpleContainer
val StudioOnPrimaryContainer = OnRoyalPurpleContainer

val StudioSecondary = ElegantGold
val StudioOnSecondary = Color(0xFF00252B)
val StudioSecondaryContainer = ElegantGoldContainer
val StudioOnSecondaryContainer = OnElegantGoldContainer

val StudioBackgroundDark = CharcoalBackground
val StudioSurfaceDark = CharcoalSurface
val StudioSurfaceVariantDark = CharcoalSurfaceVariant

val StudioOnSurfaceDark = TextPrimaryDark
val StudioOnSurfaceVariantDark = TextSecondaryDark
val StudioOutline = OutlineDark
