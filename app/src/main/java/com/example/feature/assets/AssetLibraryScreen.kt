package com.example.feature.assets

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.database.AssetEntity
import com.example.feature.history.MediaThumbnailImage

@Composable
fun AssetLibraryScreen(viewModel: AssetManagerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val displayedAssets = if (uiState.selectedTab == TAB_RECENT) {
        uiState.recentAssets
    } else {
        uiState.favoriteAssets
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Pustaka Media",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Akses cepat ke media yang pernah digunakan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AssetTabs(uiState.selectedTab, viewModel::setTab)
        if (displayedAssets.isEmpty()) {
            EmptyAssetState(uiState.selectedTab)
        } else {
            AssetList(displayedAssets, viewModel)
        }
    }
}

@Composable
private fun AssetTabs(selectedTab: Int, onSelectTab: (Int) -> Unit) {
    TabRow(selectedTabIndex = selectedTab) {
        listOf("Terbaru", "Favorit").forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onSelectTab(index) },
                text = { Text(title) },
                modifier = Modifier.testTag("asset_tab_${title.lowercase()}")
            )
        }
    }
}

@Composable
private fun EmptyAssetState(selectedTab: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (selectedTab == TAB_RECENT) {
                "Belum ada media. Pilih media dari Loop, Editor, atau Mastering."
            } else {
                "Belum ada media favorit."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AssetList(assets: List<AssetEntity>, viewModel: AssetManagerViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(assets, key = { it.id }) { asset ->
            AssetRow(
                asset = asset,
                onToggleFavorite = { viewModel.toggleFavorite(asset.id) },
                onTogglePinned = { viewModel.togglePinned(asset.id) },
                onDelete = { viewModel.deleteAsset(asset.id) }
            )
        }
    }
}

@Composable
private fun AssetRow(
    asset: AssetEntity,
    onToggleFavorite: () -> Unit,
    onTogglePinned: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("asset_item_${asset.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetThumbnail(asset)
            Spacer(Modifier.width(12.dp))
            AssetDetails(asset, Modifier.weight(1f))
            AssetActions(asset, onToggleFavorite, onTogglePinned, onDelete)
        }
    }
}

@Composable
private fun AssetThumbnail(asset: AssetEntity) {
    if (asset.mediaType == "VIDEO") {
        MediaThumbnailImage(
            uriString = asset.uriString,
            modifier = Modifier.size(64.dp),
            placeholderIcon = Icons.Default.Movie
        )
    } else {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun AssetDetails(asset: AssetEntity, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = asset.fileName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${formatFileSize(asset.fileSize)} • Dipakai ${asset.usageCount}×",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!asset.permissionPersisted) {
            AssistChip(
                onClick = {},
                label = { Text("Sesi ini") },
                modifier = Modifier.testTag("asset_session_only_${asset.id}")
            )
        }
    }
}

@Composable
private fun AssetActions(
    asset: AssetEntity,
    onToggleFavorite: () -> Unit,
    onTogglePinned: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        IconButton(onClick = onToggleFavorite, modifier = Modifier.testTag("asset_favorite_${asset.id}")) {
            Icon(
                imageVector = if (asset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (asset.isFavorite) "Hapus dari favorit" else "Tambah ke favorit"
            )
        }
        IconButton(onClick = onTogglePinned, modifier = Modifier.testTag("asset_pin_${asset.id}")) {
            Icon(
                imageVector = if (asset.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (asset.isPinned) "Lepas sematan" else "Sematkan"
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.testTag("asset_delete_${asset.id}")) {
            Icon(Icons.Default.Delete, contentDescription = "Hapus dari pustaka")
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= GIBIBYTE -> "%.1f GB".format(bytes.toDouble() / GIBIBYTE)
    bytes >= MEBIBYTE -> "%.1f MB".format(bytes.toDouble() / MEBIBYTE)
    bytes >= KIBIBYTE -> "%.1f KB".format(bytes.toDouble() / KIBIBYTE)
    else -> "$bytes B"
}

private const val TAB_RECENT = 0
private const val KIBIBYTE = 1024L
private const val MEBIBYTE = KIBIBYTE * 1024L
private const val GIBIBYTE = MEBIBYTE * 1024L
