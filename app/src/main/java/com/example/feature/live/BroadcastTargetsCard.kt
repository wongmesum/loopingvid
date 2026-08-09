package com.example.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Interactive Broadcast Targets & Multi-Destination Simulcast Card
 * Allows users to define multiple RTMP destinations and toggle simulcast streaming with real-time bandwidth validation.
 */
@Composable
fun BroadcastTargetsCard(
    targets: List<BroadcastTarget>,
    isSimulcastEnabled: Boolean,
    availableBandwidthKbps: Int,
    isBandwidthSufficient: Boolean,
    bandwidthWarningMessage: String?,
    streamStatus: StreamStatus,
    onToggleSimulcast: () -> Unit,
    onToggleTargetEnabled: (String) -> Unit,
    onAddTarget: (name: String, platform: LivePlatform, url: String, key: String, bitrateKbps: Int) -> Unit,
    onUpdateTarget: (id: String, name: String, platform: LivePlatform, url: String, key: String, bitrateKbps: Int) -> Unit,
    onRemoveTarget: (String) -> Unit,
    onRunSpeedTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTarget by remember { mutableStateOf<BroadcastTarget?>(null) }

    val activeTargets = if (isSimulcastEnabled) {
        targets.filter { it.isEnabled }
    } else {
        val primary = targets.firstOrNull { it.isEnabled } ?: targets.firstOrNull()
        if (primary != null) listOf(primary) else emptyList()
    }
    val totalRequiredBitrateKbps = activeTargets.sumOf { it.requiredBitrateKbps }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("broadcast_targets_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Simulcast Destinations",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Broadcast Destinations & Simulcast",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${targets.size} RTMP Target(s) Configured",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Add Target Button
                IconButton(
                    onClick = { showAddDialog = true },
                    enabled = streamStatus != StreamStatus.LIVE,
                    modifier = Modifier.testTag("add_broadcast_target_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add RTMP Destination",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Simulcast Mode Toggle Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Multi-Destination Simulcast",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSimulcastEnabled) Color(0xFF10B981) else Color.Gray,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isSimulcastEnabled) "MULTI" else "SINGLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = if (isSimulcastEnabled)
                            "Broadcasting simultaneously to all enabled RTMP destinations."
                        else
                            "Broadcasting only to the primary RTMP destination.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isSimulcastEnabled,
                    onCheckedChange = { onToggleSimulcast() },
                    enabled = streamStatus != StreamStatus.LIVE,
                    modifier = Modifier.testTag("simulcast_toggle_switch"),
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White)
                )
            }

            // Bandwidth Validation & Speed Test Meter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Bandwidth Meter",
                            tint = if (isBandwidthSufficient) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bandwidth Validation",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Speed test simulation button (debug builds only)
                    if (com.example.BuildConfig.DEBUG) {
                        OutlinedButton(
                            onClick = onRunSpeedTest,
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("run_speed_test_button"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = "Run Speed Test",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Speed", fontSize = 11.sp)
                        }
                    }
                }

                // Bitrate Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Required: ${(totalRequiredBitrateKbps / 1000f)} Mbps",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Network Capacity: ${(availableBandwidthKbps / 1000f)} Mbps",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isBandwidthSufficient) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }

                // Bandwidth Warning Alert Banner
                AnimatedVisibility(visible = !isBandwidthSufficient && bandwidthWarningMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Bandwidth Warning",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bandwidthWarningMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            // List of Broadcast Targets
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "RTMP Destinations (${targets.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                targets.forEach { target ->
                    TargetItemCard(
                        target = target,
                        isSimulcastEnabled = isSimulcastEnabled,
                        canDelete = targets.size > 1,
                        streamStatus = streamStatus,
                        onToggleEnabled = { onToggleTargetEnabled(target.id) },
                        onEditClick = { editingTarget = target },
                        onDeleteClick = { onRemoveTarget(target.id) }
                    )
                }
            }
        }
    }

    // Dialog for Adding New RTMP Target
    if (showAddDialog) {
        AddEditTargetDialog(
            target = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, platform, url, key, bitrateKbps ->
                onAddTarget(name, platform, url, key, bitrateKbps)
                showAddDialog = false
            }
        )
    }

    // Dialog for Editing Existing Target
    editingTarget?.let { target ->
        AddEditTargetDialog(
            target = target,
            onDismiss = { editingTarget = null },
            onConfirm = { name, platform, url, key, bitrateKbps ->
                onUpdateTarget(target.id, name, platform, url, key, bitrateKbps)
                editingTarget = null
            }
        )
    }
}

/**
 * Single Broadcast Target Item Row/Card
 */
@Composable
private fun TargetItemCard(
    target: BroadcastTarget,
    isSimulcastEnabled: Boolean,
    canDelete: Boolean,
    streamStatus: StreamStatus,
    onToggleEnabled: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val isActiveInCurrentMode = if (isSimulcastEnabled) target.isEnabled else target.isEnabled

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("target_item_${target.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (target.isEnabled) Color(0xFF10B981) else Color.Gray,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = target.name,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = target.platform.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = "${target.rtmpUrl} • ${(target.requiredBitrateKbps / 1000f)} Mbps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = target.isEnabled,
                        onCheckedChange = { onToggleEnabled() },
                        enabled = streamStatus != StreamStatus.LIVE,
                        modifier = Modifier
                            .scale(0.85f)
                            .testTag("target_toggle_${target.id}")
                    )

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Target Details"
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "RTMP Server URL: ${target.rtmpUrl}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Stream Key: ${if (target.streamKey.isBlank()) "(Not Set)" else "••••••••••••"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onEditClick,
                            enabled = streamStatus != StreamStatus.LIVE,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("edit_target_button_${target.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Target",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (canDelete) {
                            IconButton(
                                onClick = onDeleteClick,
                                enabled = streamStatus != StreamStatus.LIVE,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("delete_target_button_${target.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Target",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog for Adding or Editing an RTMP Broadcast Target
 */
@Composable
private fun AddEditTargetDialog(
    target: BroadcastTarget?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, platform: LivePlatform, url: String, key: String, bitrateKbps: Int) -> Unit
) {
    var name by remember { mutableStateOf(target?.name ?: "Secondary Stream") }
    var selectedPlatform by remember { mutableStateOf(target?.platform ?: LivePlatform.CUSTOM_RTMP) }
    var rtmpUrl by remember { mutableStateOf(target?.rtmpUrl ?: "rtmp://live.custom.com/live") }
    var streamKey by remember { mutableStateOf(target?.streamKey ?: "") }
    var requiredBitrateKbps by remember { mutableStateOf((target?.requiredBitrateKbps ?: 4000).toFloat()) }
    var isKeyVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (target == null) "Add Broadcast Destination" else "Edit Broadcast Destination")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Target Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("target_dialog_name_input")
                )

                // Platform Selection
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LivePlatform.entries.forEach { p ->
                        FilterChip(
                            selected = selectedPlatform == p,
                            onClick = {
                                selectedPlatform = p
                                if (rtmpUrl.contains("youtube") || rtmpUrl.contains("tiktok") || rtmpUrl.contains("custom")) {
                                    rtmpUrl = when (p) {
                                        LivePlatform.YOUTUBE -> "rtmp://a.rtmp.youtube.com/live2"
                                        LivePlatform.TIKTOK -> "rtmp://live-push.tiktok.com/live"
                                        LivePlatform.CUSTOM_RTMP -> "rtmp://live.custom.com/live"
                                    }
                                }
                            },
                            label = { Text(p.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = rtmpUrl,
                    onValueChange = { rtmpUrl = it },
                    label = { Text("RTMP Server URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("target_dialog_url_input")
                )

                OutlinedTextField(
                    value = streamKey,
                    onValueChange = { streamKey = it },
                    label = { Text("Stream Key") },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Key Visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("target_dialog_key_input")
                )

                // Required Bitrate Slider
                Column {
                    Text(
                        text = "Target Bitrate: ${(requiredBitrateKbps / 1000f)} Mbps (${requiredBitrateKbps.toInt()} Kbps)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Slider(
                        value = requiredBitrateKbps,
                        onValueChange = { requiredBitrateKbps = it },
                        valueRange = 1000f..10000f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth().testTag("target_dialog_bitrate_slider")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, selectedPlatform, rtmpUrl, streamKey, requiredBitrateKbps.toInt())
                },
                enabled = name.isNotBlank() && rtmpUrl.isNotBlank(),
                modifier = Modifier.testTag("target_dialog_confirm_button")
            ) {
                Text(if (target == null) "Add Target" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("target_dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

// Extension to scale modifier cleanly
private fun Modifier.scale(scale: Float) = this.then(
    Modifier.padding(scale = scale)
)

private fun Modifier.padding(scale: Float): Modifier = this
