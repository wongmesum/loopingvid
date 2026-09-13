package com.example.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Log viewer component in LiveStreamingPage that displays a scrollable list of real-time events
 * such as encoder initialization, network reconnect attempts, and stream health status messages.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LiveLogViewerCard(
    logs: List<LiveLogEvent>,
    onClearLogs: () -> Unit,
    onSimulateLogEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<LiveLogCategory?>(null) }
    var selectedLevelFilter by remember { mutableStateOf<LiveLogLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isAutoScrollEnabled by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Filter logs based on category, level, and search text
    val filteredLogs = remember(logs, selectedCategoryFilter, selectedLevelFilter, searchQuery) {
        logs.filter { event ->
            val matchCategory = selectedCategoryFilter == null || event.category == selectedCategoryFilter
            val matchLevel = selectedLevelFilter == null || event.level == selectedLevelFilter
            val matchQuery = searchQuery.isEmpty() ||
                    event.message.contains(searchQuery, ignoreCase = true) ||
                    event.category.label.contains(searchQuery, ignoreCase = true)
            matchCategory && matchLevel && matchQuery
        }
    }

    // Auto scroll to bottom on new log arrival if enabled
    LaunchedEffect(logs.size, isAutoScrollEnabled) {
        if (isAutoScrollEnabled && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_log_viewer_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Stream Log Console",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Stream Event Log Viewer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Real-time encoder, network & health telemetry (${filteredLogs.size} events)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Simulate Event Button
                    IconButton(
                        onClick = onSimulateLogEvent,
                        modifier = Modifier.testTag("simulate_log_event_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Simulate Real-time Event",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Auto-Scroll Toggle Button
                    IconButton(
                        onClick = { isAutoScrollEnabled = !isAutoScrollEnabled },
                        modifier = Modifier.testTag("toggle_auto_scroll_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignBottom,
                            contentDescription = "Toggle Auto Scroll",
                            tint = if (isAutoScrollEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Clear Logs Button
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear Log Entries",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logs (e.g., encoder, reconnect, 1080p)...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Clear Search", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Category Filter Chips. FlowRow so all 6 category chips wrap onto a second line
            // on narrow phone screens instead of overflowing off-screen.
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("ALL", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("log_category_chip_all")
                )

                LiveLogCategory.entries.forEach { category ->
                    val isSelected = selectedCategoryFilter == category
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategoryFilter = if (isSelected) null else category
                        },
                        label = { Text(category.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("log_category_chip_${category.name}")
                    )
                }
            }

            // Terminal Console View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A)) // Dark slate terminal background
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.matchParentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No log events matching filter criteria",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.testTag("live_log_list")
                    ) {
                        itemsIndexed(
                            items = filteredLogs,
                            key = { _, item -> item.id }
                        ) { index, event ->
                            LogEntryRow(
                                event = event,
                                modifier = Modifier.testTag("live_log_item_$index")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Log Event Row Item
 */
@Composable
private fun LogEntryRow(
    event: LiveLogEvent,
    modifier: Modifier = Modifier
) {
    val levelColor = when (event.level) {
        LiveLogLevel.INFO -> Color(0xFF3B82F6) // Blue
        LiveLogLevel.WARN -> Color(0xFFF59E0B) // Warning Yellow/Orange
        LiveLogLevel.ERROR -> Color(0xFFEF4444) // Error Red
        LiveLogLevel.SUCCESS -> Color(0xFF10B981) // Success Emerald
    }

    val categoryBg = when (event.category) {
        LiveLogCategory.ENCODER -> Color(0xFF1E293B)
        LiveLogCategory.NETWORK -> Color(0xFF1E1B4B)
        LiveLogCategory.HEALTH -> Color(0xFF064E3B)
        LiveLogCategory.SYSTEM -> Color(0xFF1F2937)
        LiveLogCategory.RTMP -> Color(0xFF312E81)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timestamp
        Text(
            text = event.timestamp,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.Gray,
            modifier = Modifier.width(76.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Level Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(levelColor.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = event.level.name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = levelColor
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Category Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(categoryBg)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = event.category.label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.LightGray
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Message
        Text(
            text = event.message,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFE2E8F0),
            modifier = Modifier.weight(1f)
        )
    }
}
