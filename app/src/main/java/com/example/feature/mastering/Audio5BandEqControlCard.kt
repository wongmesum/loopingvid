package com.example.feature.mastering

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.media.EqBandConfig

/**
 * 5-band graphic equalizer control. Each band maps to a fixed center frequency handled by the
 * FFmpeg mastering chain (60 Hz / 250 Hz / 1 kHz / 4 kHz / 12 kHz). Gains range from -12 dB to +12 dB.
 */
@Composable
fun Audio5BandEqControlCard(
    eqConfig: EqBandConfig,
    onEqLowChanged: (Float) -> Unit,
    onEqMidLowChanged: (Float) -> Unit,
    onEqMidChanged: (Float) -> Unit,
    onEqMidHighChanged: (Float) -> Unit,
    onEqHighChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Equalizer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "5-Band Equalizer",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            EqBandSlider("Bass", "60 Hz", eqConfig.lowGainDb, onEqLowChanged)
            EqBandSlider("Warmth", "250 Hz", eqConfig.midLowGainDb, onEqMidLowChanged)
            EqBandSlider("Presence", "1 kHz", eqConfig.midGainDb, onEqMidChanged)
            EqBandSlider("Clarity", "4 kHz", eqConfig.midHighGainDb, onEqMidHighChanged)
            EqBandSlider("Air", "12 kHz", eqConfig.highGainDb, onEqHighChanged)
        }
    }
}

@Composable
private fun EqBandSlider(
    label: String,
    frequency: String,
    gainDb: Float,
    onGainChanged: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
                Text(frequency, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text(
                text = "%+.1f dB".format(gainDb),
                style = MaterialTheme.typography.labelMedium,
                color = if (gainDb == 0f) Color.Gray else MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = gainDb,
            onValueChange = onGainChanged,
            valueRange = -12f..12f,
            steps = 47
        )
    }
}
