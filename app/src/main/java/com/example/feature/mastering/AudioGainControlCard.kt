package com.example.feature.mastering

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Input / output gain trim control. Input gain is applied before the mastering chain (EQ,
 * compressor); output gain is applied after, acting as a final make-up level. Both range -12..+12 dB.
 */
@Composable
fun AudioGainControlCard(
    inputGainDb: Float,
    outputGainDb: Float,
    onInputGainChanged: (Float) -> Unit,
    onOutputGainChanged: (Float) -> Unit,
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
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Gain",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Input / Output Gain",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            GainSlider("Input Gain (pre)", inputGainDb, onInputGainChanged)
            GainSlider("Output Gain (post)", outputGainDb, onOutputGainChanged)
        }
    }
}

@Composable
private fun GainSlider(
    label: String,
    gainDb: Float,
    onGainChanged: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
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
