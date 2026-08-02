package com.example.feature.live

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * D3DataOverlayCard provides a high-performance real-time data overlay
 * powered by D3.js running inside a WebView to visualize viewer count and
 * network bandwidth fluctuations during live broadcasts.
 */
@Composable
fun D3DataOverlayCard(
    uiState: LiveUiState,
    onToggleOverlay: () -> Unit,
    onSetOpacity: (Float) -> Unit,
    onSetTimeWindow: (Int) -> Unit,
    onSetVisMode: (D3VisMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("d3_data_overlay_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0C20)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "D3.js Real-time Visualizer",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "D3.js Live Data Overlay",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "D3 v7",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = "Real-time viewer & bandwidth telemetry curves",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                // Toggle Overlay Visibility Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (uiState.showD3OverlayOnVideo) "Video Overlay" else "Hidden",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.showD3OverlayOnVideo) Color(0xFF00E5FF) else Color.Gray,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Switch(
                        checked = uiState.showD3OverlayOnVideo,
                        onCheckedChange = { onToggleOverlay() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00E5FF)
                        ),
                        modifier = Modifier.testTag("toggle_d3_overlay_switch")
                    )
                }
            }

            // Real-time Metric Pill Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Viewer Count Metric Badge
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF00E676).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Viewers",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "%,d".format(uiState.viewerCount),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Peak: %,d".format(uiState.peakViewerCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Bandwidth Fluctuation Metric Badge
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Bandwidth",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "%.1f Mbps".format(uiState.bandwidthMbps),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${uiState.currentBitrateKbps} Kbps target",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Mode Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.d3VisualizationMode == D3VisMode.DUAL_METRICS,
                    onClick = { onSetVisMode(D3VisMode.DUAL_METRICS) },
                    label = { Text("Dual Telemetry", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF7C4DFF),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.d3VisualizationMode == D3VisMode.VIEWER_HEATMAP,
                    onClick = { onSetVisMode(D3VisMode.VIEWER_HEATMAP) },
                    label = { Text("Viewer Trend", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E676),
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.d3VisualizationMode == D3VisMode.BANDWIDTH_STABILITY,
                    onClick = { onSetVisMode(D3VisMode.BANDWIDTH_STABILITY) },
                    label = { Text("Bandwidth Curve", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // D3.js Real-time Interactive Visualizer Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF05030A))
                    .border(1.dp, Color(0xFF221A3E), RoundedCornerShape(12.dp))
            ) {
                D3TelemetryWebView(
                    viewerHistory = uiState.viewerHistory,
                    bandwidthHistory = uiState.bandwidthHistoryMbps,
                    currentViewers = uiState.viewerCount,
                    currentBandwidth = uiState.bandwidthMbps,
                    visMode = uiState.d3VisualizationMode,
                    timeWindowSec = uiState.d3OverlayTimeWindowSec,
                    modifier = Modifier.matchParentSize()
                )
            }

            // Control Bar: Time Window & Opacity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Window Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Window:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    listOf(30 to "30s", 60 to "60s", 300 to "5m").forEach { (sec, label) ->
                        Surface(
                            color = if (uiState.d3OverlayTimeWindowSec == sec) Color(0xFF7C4DFF) else Color(0xFF1E1738),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("time_window_$sec"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (uiState.d3OverlayTimeWindowSec == sec) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Opacity Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(150.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = "Opacity",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Slider(
                        value = uiState.d3OverlayOpacity,
                        onValueChange = { onSetOpacity(it) },
                        valueRange = 0.3f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF)
                        ),
                        modifier = Modifier.testTag("d3_opacity_slider")
                    )
                }
            }
        }
    }
}

/**
 * Android WebView Composable hosting the embedded D3.js chart engine.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun D3TelemetryWebView(
    viewerHistory: List<Int>,
    bandwidthHistory: List<Float>,
    currentViewers: Int,
    currentBandwidth: Float,
    visMode: D3VisMode,
    timeWindowSec: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewError by remember { mutableStateOf(false) }
    val htmlContent = remember { getD3OverlayHtmlTemplate() }

    val isWebViewAvailable = remember(context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.webkit.WebView.getCurrentWebViewPackage() != null
            } else {
                // Fallback for older APIs (assuming it's available or catching error later)
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    if (webViewError || !isWebViewAvailable) {
        D3TelemetryCanvasFallback(
            viewerHistory = viewerHistory,
            bandwidthHistory = bandwidthHistory,
            currentViewers = currentViewers,
            currentBandwidth = currentBandwidth,
            modifier = modifier
        )
    } else {
        AndroidView(
            factory = { ctx ->
                try {
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                try {
                                    sendDataToD3(
                                        view,
                                        viewerHistory,
                                        bandwidthHistory,
                                        currentViewers,
                                        currentBandwidth,
                                        visMode,
                                        timeWindowSec
                                    )
                                } catch (t: Throwable) {
                                    t.printStackTrace()
                                }
                            }
                        }

                        loadDataWithBaseURL(
                            "https://d3js.org",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    webViewError = true
                    android.view.View(ctx)
                }
            },
            update = { webView ->
                if (webView is WebView) {
                    try {
                        sendDataToD3(
                            webView,
                            viewerHistory,
                            bandwidthHistory,
                            currentViewers,
                            currentBandwidth,
                            visMode,
                            timeWindowSec
                        )
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
            },
            modifier = modifier.testTag("d3_telemetry_webview")
        )
    }
}

@Composable
private fun D3TelemetryCanvasFallback(
    viewerHistory: List<Int>,
    bandwidthHistory: List<Float>,
    currentViewers: Int,
    currentBandwidth: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF0F0C20), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0 || h <= 0) return@Canvas

            val pts = viewerHistory.ifEmpty { listOf(100, 120, 110, 130, 125) }
            val maxVal = pts.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
            val stepX = w / (pts.size - 1).coerceAtLeast(1)

            val path = androidx.compose.ui.graphics.Path()
            pts.forEachIndexed { i, v ->
                val x = i * stepX
                val y = h - ((v.toFloat() / maxVal) * (h * 0.8f))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = Color(0xFF00E676),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }
    }
}

private fun sendDataToD3(
    webView: WebView?,
    viewerHistory: List<Int>,
    bandwidthHistory: List<Float>,
    currentViewers: Int,
    currentBandwidth: Float,
    visMode: D3VisMode,
    timeWindowSec: Int
) {
    if (webView == null) return
    try {
        val vJson = viewerHistory.joinToString(prefix = "[", postfix = "]")
        val bJson = bandwidthHistory.joinToString(prefix = "[", postfix = "]")
        val modeStr = visMode.name.lowercase()

        val jsCommand = "if (window.renderD3Chart) { window.renderD3Chart($vJson, $bJson, $currentViewers, $currentBandwidth, '$modeStr', $timeWindowSec); }"
        webView.evaluateJavascript(jsCommand, null)
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

/**
 * Returns self-contained D3.js v7 HTML template with responsive SVG rendering,
 * dual y-axes, smooth gradients, glowing paths, and live tooltip interaction.
 */
private fun getD3OverlayHtmlTemplate(): String {
    val d = "${'$'}"
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/d3/7.8.5/d3.min.js"></script>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
        body {
            background-color: transparent;
            font-family: system-ui, -apple-system, Roboto, sans-serif;
            color: #ffffff;
            overflow: hidden;
            width: 100vw;
            height: 100vh;
        }
        #chart-container {
            width: 100%;
            height: 100%;
            position: relative;
            padding: 8px;
        }
        svg {
            width: 100%;
            height: 100%;
            overflow: visible;
        }
        .axis text {
            fill: #9CA3AF;
            font-size: 10px;
            font-weight: 500;
        }
        .axis path, .axis line {
            stroke: #2A244D;
            stroke-width: 1px;
        }
        .grid line {
            stroke: #1E1B38;
            stroke-opacity: 0.7;
            stroke-dasharray: 2,2;
        }
        .viewer-line {
            fill: none;
            stroke: #00E676;
            stroke-width: 2.5px;
            filter: drop-shadow(0px 0px 4px rgba(0, 230, 118, 0.6));
        }
        .viewer-area {
            fill: url(#viewer-gradient);
            opacity: 0.35;
        }
        .bandwidth-line {
            fill: none;
            stroke: #00E5FF;
            stroke-width: 2.5px;
            filter: drop-shadow(0px 0px 4px rgba(0, 229, 255, 0.6));
        }
        .bandwidth-area {
            fill: url(#bandwidth-gradient);
            opacity: 0.25;
        }
        .legend {
            position: absolute;
            top: 6px;
            left: 10px;
            display: flex;
            gap: 12px;
            font-size: 10px;
            font-weight: 600;
        }
        .legend-item {
            display: flex;
            align-items: center;
            gap: 4px;
            background: rgba(18, 14, 36, 0.75);
            padding: 3px 8px;
            border-radius: 12px;
            border: 1px solid rgba(255, 255, 255, 0.1);
            backdrop-filter: blur(4px);
        }
        .dot-v { width: 8px; height: 8px; border-radius: 50%; background: #00E676; box-shadow: 0 0 6px #00E676; }
        .dot-b { width: 8px; height: 8px; border-radius: 50%; background: #00E5FF; box-shadow: 0 0 6px #00E5FF; }
    </style>
</head>
<body>
    <div id="chart-container">
        <div class="legend">
            <div class="legend-item"><div class="dot-v"></div><span id="legend-v">Viewers: --</span></div>
            <div class="legend-item"><div class="dot-b"></div><span id="legend-b">Bandwidth: -- Mbps</span></div>
        </div>
        <svg id="d3-svg"></svg>
    </div>

    <script>
        // Real-time D3.js Chart Renderer Engine
        window.renderD3Chart = function(viewerData, bandwidthData, currentV, currentB, mode, windowSec) {
            document.getElementById('legend-v').innerText = 'Viewers: ' + currentV.toLocaleString();
            document.getElementById('legend-b').innerText = 'Bandwidth: ' + currentB.toFixed(1) + ' Mbps';

            const svg = d3.select("#d3-svg");
            svg.selectAll("*").remove(); // Clean previous frame

            const container = document.getElementById("chart-container");
            const width = container.clientWidth || 360;
            const height = container.clientHeight || 180;
            const margin = { top: 32, right: 36, bottom: 24, left: 36 };

            const innerWidth = width - margin.left - margin.right;
            const innerHeight = height - margin.top - margin.bottom;

            const g = svg.append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            // Definitions for linear gradients
            const defs = svg.append("defs");

            const vGrad = defs.append("linearGradient")
                .attr("id", "viewer-gradient")
                .attr("x1", "0%").attr("y1", "0%")
                .attr("x2", "0%").attr("y2", "100%");
            vGrad.append("stop").attr("offset", "0%").attr("stop-color", "#00E676").attr("stop-opacity", 0.6);
            vGrad.append("stop").attr("offset", "100%").attr("stop-color", "#00E676").attr("stop-opacity", 0.0);

            const bGrad = defs.append("linearGradient")
                .attr("id", "bandwidth-gradient")
                .attr("x1", "0%").attr("y1", "0%")
                .attr("x2", "0%").attr("y2", "100%");
            bGrad.append("stop").attr("offset", "0%").attr("stop-color", "#00E5FF").attr("stop-opacity", 0.5);
            bGrad.append("stop").attr("offset", "100%").attr("stop-color", "#00E5FF").attr("stop-opacity", 0.0);

            const n = Math.max(viewerData.length, bandwidthData.length);
            const xScale = d3.scaleLinear()
                .domain([0, Math.max(1, n - 1)])
                .range([0, innerWidth]);

            const vMin = d3.min(viewerData) || 0;
            const vMax = (d3.max(viewerData) || 2000) * 1.1;
            const yScaleV = d3.scaleLinear()
                .domain([vMin * 0.9, vMax])
                .range([innerHeight, 0]);

            const bMin = d3.min(bandwidthData) || 0;
            const bMax = (d3.max(bandwidthData) || 12) * 1.15;
            const yScaleB = d3.scaleLinear()
                .domain([0, bMax])
                .range([innerHeight, 0]);

            // Grid Lines
            g.append("g")
                .attr("class", "grid")
                .call(d3.axisLeft(yScaleV).ticks(4).tickSize(-innerWidth).tickFormat(""));

            // Axes
            const formatV = d3.format("~s");
            if (mode !== 'bandwidth_stability') {
                g.append("g")
                    .attr("class", "axis")
                    .call(d3.axisLeft(yScaleV).ticks(4).tickFormat(function(d) { return formatV(d); }));
            }

            if (mode !== 'viewer_heatmap') {
                g.append("g")
                    .attr("class", "axis")
                    .attr("transform", "translate(" + innerWidth + ",0)")
                    .call(d3.axisRight(yScaleB).ticks(4).tickFormat(function(d) { return d + "M"; }));
            }

            // D3 Area & Line Generators
            if (mode === 'dual_metrics' || mode === 'viewer_heatmap') {
                const areaV = d3.area()
                    .x(function(d, i) { return xScale(i); })
                    .y0(innerHeight)
                    .y1(function(d) { return yScaleV(d); })
                    .curve(d3.curveMonotoneX);

                const lineV = d3.line()
                    .x(function(d, i) { return xScale(i); })
                    .y(function(d) { return yScaleV(d); })
                    .curve(d3.curveMonotoneX);

                g.append("path")
                    .datum(viewerData)
                    .attr("class", "viewer-area")
                    .attr("d", areaV);

                g.append("path")
                    .datum(viewerData)
                    .attr("class", "viewer-line")
                    .attr("d", lineV);

                // Pulse dot at current tip
                const lastIdxV = viewerData.length - 1;
                if (lastIdxV >= 0) {
                    g.append("circle")
                        .attr("cx", xScale(lastIdxV))
                        .attr("cy", yScaleV(viewerData[lastIdxV]))
                        .attr("r", 5)
                        .attr("fill", "#00E676")
                        .attr("stroke", "#FFFFFF")
                        .attr("stroke-width", 2);
                }
            }

            if (mode === 'dual_metrics' || mode === 'bandwidth_stability') {
                const areaB = d3.area()
                    .x(function(d, i) { return xScale(i); })
                    .y0(innerHeight)
                    .y1(function(d) { return yScaleB(d); })
                    .curve(d3.curveMonotoneX);

                const lineB = d3.line()
                    .x(function(d, i) { return xScale(i); })
                    .y(function(d) { return yScaleB(d); })
                    .curve(d3.curveMonotoneX);

                g.append("path")
                    .datum(bandwidthData)
                    .attr("class", "bandwidth-area")
                    .attr("d", areaB);

                g.append("path")
                    .datum(bandwidthData)
                    .attr("class", "bandwidth-line")
                    .attr("d", lineB);

                const lastIdxB = bandwidthData.length - 1;
                if (lastIdxB >= 0) {
                    g.append("circle")
                        .attr("cx", xScale(lastIdxB))
                        .attr("cy", yScaleB(bandwidthData[lastIdxB]))
                        .attr("r", 5)
                        .attr("fill", "#00E5FF")
                        .attr("stroke", "#FFFFFF")
                        .attr("stroke-width", 2);
                }
            }
        };

        // Fallback default load render
        window.addEventListener('DOMContentLoaded', function() {
            window.renderD3Chart(
                [1420, 1480, 1550, 1620, 1700, 1750, 1810, 1840],
                [8.2, 8.5, 9.1, 8.8, 9.4, 9.2, 8.7, 9.5],
                1840,
                9.5,
                'dual_metrics',
                30
            );
        });
    </script>
</body>
</html>
    """.trimIndent()
}
