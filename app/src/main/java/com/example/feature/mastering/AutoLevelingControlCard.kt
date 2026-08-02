package com.example.feature.mastering

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.media.AutoLevelingConfig

@Composable
fun AutoLevelingControlCard(
    config: AutoLevelingConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onTargetLoudnessChanged: (Float) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Auto Leveling",
                        tint = if (config.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Dynamic Auto-Leveling",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )
            }

            Text(
                text = "Analyzes the entire clip and applies dynamic normalization to maintain consistent loudness.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )

            if (config.isEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Loudness", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text("${config.targetLoudnessLufs.toInt()} LUFS", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                    Slider(
                        value = config.targetLoudnessLufs,
                        onValueChange = onTargetLoudnessChanged,
                        valueRange = -24f..-8f,
                        steps = 16
                    )
                }
            }
        }
    }
}
