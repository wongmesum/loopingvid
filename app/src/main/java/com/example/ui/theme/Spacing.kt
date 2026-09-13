package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized spacing scale for the app. Screens previously used inconsistent magic-number `.dp`
 * values (8, 10, 12, 14, 16, 24...) for padding and `Arrangement.spacedBy`, which made layouts
 * feel uneven across features. New/updated composables should reference these tokens instead of
 * literal `.dp` values so spacing stays consistent app-wide.
 *
 * Existing call sites are not required to migrate all at once; this object is additive.
 */
object Spacing {
    /** 4dp - tightest spacing, icon-to-label gaps, badge padding. */
    val xs: Dp = 4.dp

    /** 8dp - compact spacing within a control row, chip gaps. */
    val sm: Dp = 8.dp

    /** 12dp - default gap between related controls inside a card. */
    val md: Dp = 12.dp

    /** 16dp - standard screen padding and gap between top-level cards/sections. */
    val lg: Dp = 16.dp

    /** 24dp - generous spacing for section breaks or empty-state padding. */
    val xl: Dp = 24.dp

    /** 32dp - large separation, rarely used (e.g. onboarding/empty states). */
    val xxl: Dp = 32.dp
}
