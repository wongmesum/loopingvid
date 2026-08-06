package com.example.feature.visualizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.ffmpeg.JobProgressState
import com.example.ui.theme.ProPrimary

/**
 * Export card shown in Visualizer Studio. Provides file name input, export
 * button, progress indicator, and cancel action.
 */
@Composable
fun VisualizerExportCard(
    outputName: String,
    canExport: Boolean,
    jobProgress: JobProgressState,
    validationMessage: String?,
    onOutputNameChange: (String) -> Unit,
    onExport: () -> Unit,
    onCancel: () -> Unit,
    onDismissValidation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.VideoFile, contentDescription = null, tint = ProPrimary)
                Text("Export Video", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = outputName,
                onValueChange = onOutputNameChange,
                label = { Text("Nama file") },
                placeholder = { Text("Visualizer_...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("visualizer_output_name")
            )

            AnimatedVisibility(visible = validationMessage != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(onClick = onDismissValidation) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup pesan")
                    }
                }
            }

            AnimatedVisibility(visible = jobProgress.isProcessing) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { jobProgress.progress / 100f },
                        modifier = Modifier.fillMaxWidth().testTag("visualizer_export_progress")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = jobProgress.statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onCancel, modifier = Modifier.testTag("visualizer_cancel_export")) {
                            Text("Batalkan")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = jobProgress.outputFilePath.isNotBlank() && !jobProgress.isProcessing) {
                Text(
                    text = "Tersimpan di galeri",
                    style = MaterialTheme.typography.bodySmall,
                    color = ProPrimary
                )
            }

            AnimatedVisibility(visible = jobProgress.errorMessage != null && !jobProgress.isProcessing) {
                Text(
                    text = jobProgress.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onExport,
                enabled = canExport,
                modifier = Modifier.fillMaxWidth().testTag("visualizer_export_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ProPrimary)
            ) {
                Icon(Icons.Rounded.VideoFile, contentDescription = null)
                Text("  Export Visualizer", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
