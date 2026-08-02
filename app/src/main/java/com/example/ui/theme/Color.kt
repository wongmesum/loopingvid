package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// --- Elegant Dark Palette (Deep Charcoal with Subtle Gold & Purple Accents) ---

// Deep Charcoal Canvas & Surface Shades
val CharcoalBackground = Color(0xFF121214) // Deep charcoal base canvas
val CharcoalSurface = Color(0xFF1A1A1E)    // Elevated surface card background
val CharcoalSurfaceVariant = Color(0xFF23232A) // Surface variant / navigation bar background
val CharcoalSurfaceHigh = Color(0xFF2C2C36) // High elevation container / dialog surface

// Subtle Gold Accents
val ElegantGold = Color(0xFFE5C158) // Warm subtle gold primary
val ElegantGoldDim = Color(0xFFC5A23C) // Muted gold for dim states
val ElegantGoldContainer = Color(0xFF3B3114) // Dark gold container background
val OnElegantGoldContainer = Color(0xFFFBEBB5) // Light gold text on container
val GoldAccent = Color(0xFFFFD700) // Vibrant gold highlight

// Refined Purple / Lavender Accents
val RoyalPurple = Color(0xFFD0BCFF) // Elegant purple accent
val RoyalPurpleContainer = Color(0xFF381E72) // Deep purple container
val OnRoyalPurpleContainer = Color(0xFFEADDFF)

// Text & Content Shades
val TextPrimaryDark = Color(0xFFF4F3F7) // Crisp high-contrast title/body text
val TextSecondaryDark = Color(0xFFA09FA6) // Muted secondary body text
val OutlineDark = Color(0xFF3E3D48) // Divider and card outline border

// Functional Status Colors
val StudioLiveRed = Color(0xFFFF5449) // Live/Record indicator red
val StudioSuccessGreen = Color(0xFF81C784) // Render success green

// Backward Compatibility Bindings for Existing UI References
val StudioPrimary = RoyalPurple
val StudioOnPrimary = Color(0xFF260D5C)
val StudioPrimaryContainer = RoyalPurpleContainer
val StudioOnPrimaryContainer = OnRoyalPurpleContainer

val StudioSecondary = ElegantGold
val StudioOnSecondary = Color(0xFF2B2100)
val StudioSecondaryContainer = ElegantGoldContainer
val StudioOnSecondaryContainer = OnElegantGoldContainer

val StudioBackgroundDark = CharcoalBackground
val StudioSurfaceDark = CharcoalSurface
val StudioSurfaceVariantDark = CharcoalSurfaceVariant

val StudioOnSurfaceDark = TextPrimaryDark
val StudioOnSurfaceVariantDark = TextSecondaryDark
val StudioOutline = OutlineDark


