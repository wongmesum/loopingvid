package com.example.core

import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Regression guard: ensures production source paths never present simulated/dummy data as real.
 *
 * These tests scan the main source set for known simulation patterns and assert they are either
 * removed or gated behind [BuildConfig.DEBUG]. They are deliberately strict about resolving the
 * source root: a wrong working directory must FAIL, never silently pass.
 */
class SimulationGuardTest {

    private companion object {
        const val DEBUG_GUARD = "BuildConfig.DEBUG"

        /**
         * Gradle runs unit tests with the module directory as working dir (`<repo>/app`), but IDE
         * runners sometimes use the repo root. Resolve both instead of assuming one.
         */
        private val CANDIDATE_ROOTS = listOf(
            "src/main/java/com/example",
            "app/src/main/java/com/example",
            "../app/src/main/java/com/example"
        )
    }

    private val mainSourceRoot: File by lazy {
        val resolved = CANDIDATE_ROOTS
            .map { File(it).absoluteFile.normalize() }
            .firstOrNull { it.isDirectory }

        assertNotNull(
            "Could not locate the main source root. Working dir=${File("").absolutePath}, " +
                "tried=${CANDIDATE_ROOTS.joinToString()}",
            resolved
        )
        resolved!!
    }

    /** Reads a main-source file, failing loudly if it is missing or was moved. */
    private fun readMainSource(relativePath: String): String {
        val file = File(mainSourceRoot, relativePath)
        assertTrue(
            "Expected main source file is missing: ${file.absolutePath}. " +
                "If it moved, update this guard instead of deleting the assertion.",
            file.isFile
        )
        return file.readText()
    }

    /** Sanity check so the other tests cannot pass against an empty/incorrect tree. */
    @Test
    fun `source root resolves and contains kotlin files`() {
        val kotlinFileCount = mainSourceRoot.walkTopDown().count { it.isFile && it.extension == "kt" }
        assertTrue(
            "Resolved source root ${mainSourceRoot.absolutePath} contains no .kt files",
            kotlinFileCount > 0
        )
    }

    /**
     * WaveformAnalyzer.generateSimulatedWaveform() must NOT be called from production ViewModels
     * or UI Composables. It may only exist in its own definition file and in test source sets.
     */
    @Test
    fun `production path must not call generateSimulatedWaveform`() {
        val violations = mutableListOf<String>()
        var scannedFiles = 0

        mainSourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                scannedFiles++
                file.readLines().forEachIndexed { lineIndex, line ->
                    val code = line.trimStart()
                    val isComment = code.startsWith("//") || code.startsWith("*")
                    if (line.contains("generateSimulatedWaveform") && !isComment) {
                        violations.add("${file.relativeTo(mainSourceRoot)}:${lineIndex + 1}: ${line.trim()}")
                    }
                }
            }

        assertTrue("Guard scanned no files — source root resolution is wrong", scannedFiles > 0)
        assertTrue(
            "Production files must not call generateSimulatedWaveform():\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    /**
     * Every simulate* function in LiveViewModel must early-return unless BuildConfig.DEBUG,
     * so a release build cannot trigger fabricated bandwidth, thermal, storage or log events.
     */
    @Test
    fun `simulate functions must be guarded by BuildConfig DEBUG`() {
        val content = readMainSource("feature/live/LiveViewModel.kt")
        val simulateFunctions = Regex("""fun (simulate\w+)\([^)]*\)\s*\{""").findAll(content).toList()

        assertTrue(
            "Expected at least one simulate* function in LiveViewModel.kt; " +
                "if they were all removed, delete this guard deliberately.",
            simulateFunctions.isNotEmpty()
        )

        val unguarded = simulateFunctions.filterNot { match ->
            val bodyStart = match.range.last
            // The guard must be the first executable statement, so look at a short prefix only.
            content.substring(bodyStart, minOf(content.length, bodyStart + 200)).contains(DEBUG_GUARD)
        }.map { it.groupValues[1] }

        assertTrue(
            "simulate* functions missing $DEBUG_GUARD guard: $unguarded",
            unguarded.isEmpty()
        )
    }

    /**
     * The live telemetry loop must not fabricate bitrate, RTT, jitter, viewers or VU levels in a
     * release build. It gates them behind a single simulateTelemetry flag derived from DEBUG.
     */
    @Test
    fun `live telemetry loop must gate fabricated metrics behind DEBUG`() {
        val content = readMainSource("feature/live/LiveViewModel.kt")

        assertTrue(
            "startLiveTelemetryLoop must derive its simulation flag from $DEBUG_GUARD",
            Regex("""val simulateTelemetry\s*=\s*[\w.]*BuildConfig\.DEBUG""").containsMatchIn(content)
        )
        assertTrue(
            "LiveUiState must expose isTelemetrySimulated so the UI can render an honest placeholder",
            content.contains("isTelemetrySimulated")
        )
        assertTrue(
            "Fabricated bitrate must be gated by simulateTelemetry",
            Regex("""currentBitrate\s*=\s*if \(simulateTelemetry\)""").containsMatchIn(content)
        )
        assertTrue(
            "Fabricated viewer delta must be gated by simulateTelemetry",
            Regex("""viewerChange\s*=\s*if \(simulateTelemetry\)""").containsMatchIn(content)
        )
    }

    /**
     * LiveUiState must not seed demo audience/network numbers, otherwise a release build shows
     * fabricated values before any stream starts.
     */
    @Test
    fun `LiveUiState must not seed demo telemetry defaults`() {
        val content = readMainSource("feature/live/LiveViewModel.kt")

        val forbiddenDefaults = listOf(
            Regex("""val viewerCount: Int = [1-9]"""),
            Regex("""val peakViewerCount: Int = [1-9]"""),
            Regex("""val bandwidthMbps: Float = [1-9]"""),
            Regex("""val currentBitrateKbps: Int = [1-9]"""),
            Regex("""val latencyMs: Int = [1-9]"""),
            Regex("""val videoVuLevel: Float = 0\.[1-9]""")
        )

        val violations = forbiddenDefaults.filter { it.containsMatchIn(content) }.map { it.pattern }

        assertTrue(
            "LiveUiState seeds fabricated telemetry defaults (must start empty/zero): $violations",
            violations.isEmpty()
        )
    }

    /**
     * Debug-only simulation controls must not render in a release build, otherwise the user sees
     * buttons whose handlers no-op — dead controls.
     */
    @Test
    fun `simulation trigger buttons must be debug-only`() {
        val gatedControls = mapOf(
            "core/ui/BatteryStatusCard.kt" to "simulate_low_battery_button",
            "feature/live/LiveHealthDashboard.kt" to "simulate_network_loss_button",
            "feature/live/StorageWatcherCard.kt" to "simulate_low_storage_button",
            "feature/live/ThermalWarningBanner.kt" to "simulate_thermal_test_button",
            "feature/live/LiveLogViewerCard.kt" to "simulate_log_event_button",
            "feature/live/BroadcastTargetsCard.kt" to "run_speed_test_button"
        )

        val violations = mutableListOf<String>()

        gatedControls.forEach { (path, marker) ->
            val content = readMainSource(path)
            assertTrue(
                "$path no longer contains the expected control marker '$marker'; update this guard.",
                content.contains(marker)
            )

            val markerIndex = content.indexOf(marker)
            val precedingBlock = content.substring(maxOf(0, markerIndex - 600), markerIndex)
            if (!precedingBlock.contains(DEBUG_GUARD)) {
                violations.add("$path ('$marker' not wrapped in a $DEBUG_GUARD block)")
            }
        }

        assertTrue(
            "Simulation controls must render only in debug builds:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    /**
     * LiveDashboard random viewer generation must only run in debug builds.
     */
    @Test
    fun `LiveDashboard random data is debug-only`() {
        val content = readMainSource("feature/live/LiveDashboard.kt")
        if (content.contains(".random()") || content.contains("Random(")) {
            assertTrue(
                "LiveDashboard random data generation must be gated by $DEBUG_GUARD",
                content.contains(DEBUG_GUARD)
            )
        }
    }

    /**
     * EditorViewModel inline spectrum animation must be debug-only.
     */
    @Test
    fun `EditorViewModel spectrum animation is debug-only`() {
        val content = readMainSource("feature/editor/EditorViewModel.kt")
        if (content.contains("Math.random()")) {
            assertTrue(
                "EditorViewModel simulated spectrum must be gated by $DEBUG_GUARD",
                content.contains(DEBUG_GUARD)
            )
        }
    }

    /**
     * Mastering screens must not paint a flat synthetic waveform when analysis is pending.
     */
    @Test
    fun `mastering waveform must not fall back to fabricated points`() {
        val content = readMainSource("feature/mastering/AudioMasteringComponent.kt")
        val fabricatedFallback = Regex("""waveformPoints \?: List\(\d+\)""")

        assertFalse(
            "AudioMasteringComponent must render an empty/placeholder state, not a flat fake waveform",
            fabricatedFallback.containsMatchIn(content)
        )
    }

    /**
     * The camera fallback must not claim a simulated hardware feed.
     */
    @Test
    fun `camera fallback must not claim a simulated feed`() {
        val content = readMainSource("feature/live/CameraXLiveStreamScreen.kt")

        assertFalse(
            "CameraX fallback must not advertise a simulated 1080p60 feed",
            content.contains("feed simulated") || content.contains("Emulator Mode")
        )
    }

    /**
     * The simulated waveform generator is gone for good. Keeping the file around would let a
     * future caller reintroduce fabricated audio data with a one-line import.
     */
    @Test
    fun `simulated waveform generator source file must not exist`() {
        val legacyAnalyzer = File(mainSourceRoot, "core/media/WaveformAnalyzer.kt")

        assertFalse(
            "core/media/WaveformAnalyzer.kt must stay deleted — real analysis comes from " +
                "AudioAnalysisRepository. Found it at ${legacyAnalyzer.absolutePath}",
            legacyAnalyzer.exists()
        )
    }

    /**
     * The mastering ViewModels and the segment trimmer must obtain their waveform from the real
     * analyzer. If these references disappear, the UI has silently lost its only real data source.
     */
    @Test
    fun `mastering and trimmer must consume the real analysis repository`() {
        val consumers = listOf(
            "feature/mastering/MasteringViewModel.kt",
            "feature/mastering/AudioMasteringViewModel.kt",
            "core/ui/Media3SegmentTrimmer.kt"
        )

        val missing = consumers.filterNot { path ->
            val content = readMainSource(path)
            content.contains("AudioAnalysisRepository") && content.contains("toAudioAnalysisData")
        }

        assertTrue(
            "These files must read real analysis via AudioAnalysisRepository.getOrAnalyze() and " +
                "map it with toAudioAnalysisData(): $missing",
            missing.isEmpty()
        )
    }
}
