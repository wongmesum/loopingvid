package com.example.feature.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.database.ProjectEntity
import com.example.core.database.ProjectSnapshotEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SnapshotHistoryDialog(
    project: ProjectEntity,
    snapshots: List<ProjectSnapshotEntity>,
    onDismiss: () -> Unit,
    onCreateSnapshot: (String) -> Unit,
    onRestore: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(" Riwayat Versi: ${project.name}", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Snapshot akan menimpa konfigurasi proyek aktif jika dipulihkan. Media sumber tidak akan terhapus dari perangkat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (snapshots.isEmpty()) {
                    Text(
                        text = "Belum ada snapshot tersimpan. Buat snapshot sebelum mengedit agar aman.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snapshots, key = { it.id }) { snap ->
                            SnapshotItemCard(
                                snapshot = snap,
                                onRestore = { onRestore(snap.id) },
                                onDelete = { onDelete(snap.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("create_snapshot_button")
            ) {
                Text("Buat Snapshot Baru")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_history_button")) {
                Text("Tutup")
            }
        }
    )

    if (showCreateDialog) {
        CreateSnapshotDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { label ->
                onCreateSnapshot(label)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun SnapshotItemCard(
    snapshot: ProjectSnapshotEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var showRestoreConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("snapshot_card_${snapshot.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(snapshot.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        formatTimestamp(snapshot.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    OutlinedButton(
                        onClick = { showRestoreConfirm = true },
                        modifier = Modifier.testTag("restore_snapshot_${snapshot.id}")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Pulihkan")
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_snapshot_${snapshot.id}")
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Pulihkan Versi") },
            text = { Text("Konfigurasi proyek saat ini akan ditimpa dengan versi '${snapshot.label}'. Lanjutkan?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRestore()
                        showRestoreConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("Ya, Pulihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun CreateSnapshotDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var label by remember { mutableStateOf("") }
    val defaultLabel = "Snapshot " + formatTimestamp(System.currentTimeMillis())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simpan Snapshot") },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Nama versi (opsional)") },
                placeholder = { Text(defaultLabel) },
                modifier = Modifier.fillMaxWidth().testTag("snapshot_label_input")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(label.ifBlank { defaultLabel }) },
                modifier = Modifier.testTag("confirm_create_snapshot_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

private fun formatTimestamp(time: Long): String {
    return SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date(time))
}
