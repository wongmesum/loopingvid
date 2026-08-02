package com.example.feature.live

import java.util.UUID

data class BroadcastTarget(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val platform: LivePlatform = LivePlatform.YOUTUBE,
    val rtmpUrl: String = "rtmp://a.rtmp.youtube.com/live2",
    val streamKey: String = "",
    val isEnabled: Boolean = true,
    val requiredBitrateKbps: Int = 4500,
    val connectionStatus: StreamStatus = StreamStatus.OFFLINE
)
