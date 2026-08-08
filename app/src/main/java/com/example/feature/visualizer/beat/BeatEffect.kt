package com.example.feature.visualizer.beat

/**
 * Effects that can fire on a beat marker. Each carries the visual transform the
 * preview renderer and the export pipeline apply while a beat is active.
 */
enum class BeatEffect(val label: String) {
    BASS_PULSE("Bass Pulse"),
    BEAT_FLASH("Beat Flash"),
    KICK_ZOOM("Kick Zoom"),
    SNARE_SHAKE("Snare Shake"),
    COLOR_SHIFT("Color Shift"),
    PARTICLE_BURST("Particle Burst"),
    BACKGROUND_PULSE("Background Pulse"),
    TEXT_BOUNCE("Text Bounce"),
    NEON_STROBE("Neon Strobe"),
    WAVE_RIPPLE("Wave Ripple")
}

/**
 * Per-frame beat state derived from marker positions.
 *
 * @param intensity 1.0 exactly on a beat, decaying to 0.0 by the end of the
 * release window. Renderers multiply their transform by this value so effects
 * animate instead of snapping on and off.
 */
data class BeatPulse(
    val intensity: Float,
    val isOnBeat: Boolean
) {
    companion object {
        val Idle = BeatPulse(intensity = 0f, isOnBeat = false)
    }
}

object BeatPulseCalculator {

    /**
     * Computes the pulse envelope at [positionMs] given sorted beat [markersMs].
     * The envelope rises over [attackMs] then decays across [releaseMs], scaled
     * by [strength].
     */
    fun pulseAt(
        positionMs: Long,
        markersMs: List<Long>,
        attackMs: Long,
        releaseMs: Long,
        strength: Float
    ): BeatPulse {
        if (markersMs.isEmpty()) return BeatPulse.Idle

        val nearest = markersMs.lastOrNull { it <= positionMs } ?: return BeatPulse.Idle
        val elapsed = positionMs - nearest
        val safeAttack = attackMs.coerceAtLeast(1L)
        val safeRelease = releaseMs.coerceAtLeast(1L)

        val envelope = when {
            elapsed < 0L -> 0f
            elapsed <= safeAttack -> elapsed.toFloat() / safeAttack
            elapsed <= safeAttack + safeRelease -> {
                val decayProgress = (elapsed - safeAttack).toFloat() / safeRelease
                1f - decayProgress
            }
            else -> 0f
        }

        val scaled = (envelope * strength).coerceIn(0f, 1f)
        return BeatPulse(intensity = scaled, isOnBeat = scaled > 0f)
    }
}
