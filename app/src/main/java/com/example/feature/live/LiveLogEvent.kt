package com.example.feature.live

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class LiveLogLevel {
    INFO,
    WARN,
    ERROR,
    SUCCESS
}

enum class LiveLogCategory(val label: String) {
    ENCODER("Encoder"),
    NETWORK("Network"),
    HEALTH("Health"),
    SYSTEM("System"),
    RTMP("RTMP")
}

data class LiveLogEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val level: LiveLogLevel = LiveLogLevel.INFO,
    val category: LiveLogCategory = LiveLogCategory.SYSTEM,
    val message: String
)
