package com.example.feature.live

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer

/**
 * Live engagement dashboard. Renders the REAL concurrent-viewer history supplied by the
 * platform API (currently YouTube Data API via [LiveViewModel]). When no live viewer data
 * is available (unsupported platform or missing credentials), it clearly states so instead
 * of fabricating numbers.
 */
@Composable
fun LiveDashboard(
    viewerCount: Int,
    viewerHistory: List<Int>,
    isViewerCountLive: Boolean,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { ChartEntryModelProducer() }

    // Feed the chart from the real viewer history whenever it changes.
    LaunchedEffect(viewerHistory) {
        if (viewerHistory.isNotEmpty()) {
            val entries = viewerHistory.mapIndexed { index, value ->
                FloatEntry(index.toFloat(), value.toFloat())
            }
            modelProducer.setEntries(entries)
        } else {
            modelProducer.setEntries(emptyList<FloatEntry>())
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Engagement Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isViewerCountLive) "🔴 LIVE: $viewerCount Viewers" else "Viewers: N/A",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isViewerCountLive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (!isViewerCountLive) {
                Text(
                    text = "Real viewer data is only available on YouTube with a Data API key + live video ID. " +
                        "TikTok and custom RTMP do not expose a public viewer API.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (viewerHistory.isNotEmpty()) {
                Chart(
                    chart = lineChart(),
                    chartModelProducer = modelProducer,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No viewer data yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
