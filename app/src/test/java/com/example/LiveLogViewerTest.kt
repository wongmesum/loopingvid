package com.example

import com.example.feature.live.LiveLogCategory
import com.example.feature.live.LiveLogEvent
import com.example.feature.live.LiveLogLevel
import com.example.feature.live.LiveUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveLogViewerTest {

    @Test
    fun defaultUiState_startsWithNoFabricatedLogs() {
        // Logs are populated only with REAL events (RTMP connect, bitrate changes, errors) as they
        // happen. There are no pre-seeded "hardware initialized" entries for events that never
        // occurred, so the list is empty on a fresh UI state.
        val state = LiveUiState()
        assertTrue("Log list should be empty until real stream events occur", state.logs.isEmpty())
    }

    @Test
    fun liveLogEvent_formatsTimestampAndPropertiesCorrectly() {
        val log = LiveLogEvent(
            level = LiveLogLevel.SUCCESS,
            category = LiveLogCategory.NETWORK,
            message = "RTMP Connection established"
        )

        assertNotNull(log.id)
        assertNotNull(log.timestamp)
        assertTrue("Timestamp should contain time format", log.timestamp.contains(":"))
        assertEquals(LiveLogLevel.SUCCESS, log.level)
        assertEquals(LiveLogCategory.NETWORK, log.category)
        assertEquals("RTMP Connection established", log.message)
    }

    @Test
    fun logFiltering_filtersByCategoryAndLevel() {
        val logs = listOf(
            LiveLogEvent(level = LiveLogLevel.INFO, category = LiveLogCategory.ENCODER, message = "Encoder init"),
            LiveLogEvent(level = LiveLogLevel.WARN, category = LiveLogCategory.NETWORK, message = "Network drop"),
            LiveLogEvent(level = LiveLogLevel.ERROR, category = LiveLogCategory.HEALTH, message = "High temp"),
            LiveLogEvent(level = LiveLogLevel.SUCCESS, category = LiveLogCategory.RTMP, message = "RTMP connected")
        )

        val encoderLogs = logs.filter { it.category == LiveLogCategory.ENCODER }
        assertEquals(1, encoderLogs.size)
        assertEquals("Encoder init", encoderLogs[0].message)

        val warningLogs = logs.filter { it.level == LiveLogLevel.WARN }
        assertEquals(1, warningLogs.size)
        assertEquals(LiveLogCategory.NETWORK, warningLogs[0].category)
    }
}
