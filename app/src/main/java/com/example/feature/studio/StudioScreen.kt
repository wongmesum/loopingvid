package com.example.feature.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ProPrimary
import com.example.ui.theme.ProSecondary
import com.example.ui.theme.ProSuccess

@Composable
fun StudioScreen(
    onOpenLoop: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenMastering: () -> Unit,
    onOpenVisualizer: () -> Unit,
    onOpenSlideshow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Studio Kreator", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Pilih alat produksi untuk memulai proyek baru.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        StudioToolCard("Video Loop", "Buat loop mulus dan presisi", Icons.Rounded.Loop, ProPrimary, onOpenLoop, "studio_open_loop")
        StudioToolCard("Video Editor", "Susun, potong, dan poles video", Icons.Rounded.Movie, ProSecondary, onOpenEditor, "studio_open_editor")
        StudioToolCard("Audio Mastering", "Seimbangkan level dan karakter audio", Icons.Rounded.Equalizer, ProSuccess, onOpenMastering, "studio_open_mastering")
        StudioToolCard("Visualizer Studio", "Preview visual audio real-time", Icons.Rounded.GraphicEq, ProPrimary, onOpenVisualizer, "studio_open_visualizer")
        StudioToolCard("Slideshow", "Gambar, transisi, musik, dan ekspor video", Icons.Rounded.Slideshow, ProSecondary, onOpenSlideshow, "studio_open_slideshow")
    }
}

@Composable
private fun StudioToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
