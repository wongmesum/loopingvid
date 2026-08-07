package com.example.feature.slideshow

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.ui.FfmpegCircularProgressIndicator
import com.example.ui.theme.ProLive
import com.example.ui.theme.ProPrimary
import com.example.ui.theme.ProSuccess

/**
 * Slideshow Studio: pick images, order them, set timing/transition/ratio,
 * optionally add a music bed, then render a real video via FFmpeg.
 */
@Composable
fun SlideshowScreen(
    viewModel: SlideshowViewModel,
    onNavigateToGoLive: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        viewModel.addImages(
            uris.map { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    // Ignore
                }
                SlideshowImage(uri = uri.toString(), displayName = uri.lastPathSegment ?: "gambar")
            }
        )
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Ignore
            }
            viewModel.setAudio(it.toString(), it.lastPathSegment ?: "audio")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SlideshowHeader()

        SlideshowImagePicker(
            images = uiState.images,
            onPickImages = { imagePickerLauncher.launch(arrayOf("image/*")) },
            onRemove = viewModel::removeImage,
            onMoveLeft = { index -> viewModel.moveImage(index, index - 1) },
            onMoveRight = { index -> viewModel.moveImage(index, index + 1) }
        )

        SlideshowAudioPicker(
            audioName = uiState.audioName,
            onPickAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
            onClearAudio = { viewModel.setAudio(null, "") }
        )

        SlideshowTimingControls(
            perImageDurationSec = uiState.perImageDurationSec,
            transitionDurationSec = uiState.transitionDurationSec,
            estimatedDurationSec = uiState.estimatedDurationSec,
            isTransitionEnabled = uiState.transition != "none",
            onDurationChange = viewModel::setPerImageDuration,
            onTransitionDurationChange = viewModel::setTransitionDuration
        )

        SlideshowStyleControls(
            transition = uiState.transition,
            aspectRatio = uiState.aspectRatio,
            resolution = uiState.resolution,
            onTransitionChange = viewModel::setTransition,
            onAspectRatioChange = viewModel::setAspectRatio,
            onResolutionChange = viewModel::setResolution
        )

        OutlinedTextField(
            value = uiState.outputName,
            onValueChange = viewModel::setOutputName,
            label = { Text("Nama file keluaran (opsional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("slideshow_output_name")
        )

        uiState.validationMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = ProLive,
                modifier = Modifier.testTag("slideshow_validation_message")
            )
        }

        SlideshowRenderSection(
            uiState = uiState,
            onRender = viewModel::renderSlideshow,
            onCancel = viewModel::cancelRender,
            onNavigateToGoLive = onNavigateToGoLive
        )
    }
}

@Composable
private fun SlideshowHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Slideshow, contentDescription = null, tint = ProPrimary)
            Text("Slideshow", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "Susun gambar menjadi video dengan transisi dan musik.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SlideshowImagePicker(
    images: List<SlideshowImage>,
    onPickImages: () -> Unit,
    onRemove: (String) -> Unit,
    onMoveLeft: (Int) -> Unit,
    onMoveRight: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("1. Pilih Gambar", style = MaterialTheme.typography.titleMedium)

            if (images.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ProPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onPickImages)
                        .testTag("slideshow_pick_images_empty"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = ProPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("Ketuk untuk memilih gambar", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(images, key = { _, image -> image.uri }) { index, image ->
                        SlideshowThumbnail(
                            image = image,
                            index = index,
                            isFirst = index == 0,
                            isLast = index == images.lastIndex,
                            onRemove = { onRemove(image.uri) },
                            onMoveLeft = { onMoveLeft(index) },
                            onMoveRight = { onMoveRight(index) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPickImages,
                        modifier = Modifier.testTag("slideshow_add_more_images")
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Tambah gambar")
                    }
                    Text(
                        "${images.size} gambar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideshowThumbnail(
    image: SlideshowImage,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onRemove: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            AsyncImage(
                model = image.uri,
                contentDescription = image.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("slideshow_remove_image_$index")
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Hapus gambar", tint = ProLive)
            }
        }
        Row {
            IconButton(
                onClick = onMoveLeft,
                enabled = !isFirst,
                modifier = Modifier.testTag("slideshow_move_left_$index")
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Geser ke kiri")
            }
            IconButton(
                onClick = onMoveRight,
                enabled = !isLast,
                modifier = Modifier.testTag("slideshow_move_right_$index")
            ) {
                Icon(Icons.Rounded.ArrowForward, contentDescription = "Geser ke kanan")
            }
        }
    }
}

@Composable
private fun SlideshowAudioPicker(
    audioName: String,
    onPickAudio: () -> Unit,
    onClearAudio: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("2. Musik Latar (opsional)", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onPickAudio, modifier = Modifier.testTag("slideshow_pick_audio")) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (audioName.isBlank()) "Pilih audio" else "Ganti audio")
                }
                if (audioName.isNotBlank()) {
                    Text(audioName, style = MaterialTheme.typography.bodySmall, color = ProSuccess)
                    IconButton(onClick = onClearAudio, modifier = Modifier.testTag("slideshow_clear_audio")) {
                        Icon(Icons.Rounded.Close, contentDescription = "Hapus audio", tint = ProLive)
                    }
                }
            }
        }
    }
}

@Composable
private fun SlideshowTimingControls(
    perImageDurationSec: Double,
    transitionDurationSec: Double,
    estimatedDurationSec: Double,
    isTransitionEnabled: Boolean,
    onDurationChange: (Double) -> Unit,
    onTransitionDurationChange: (Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("3. Durasi", style = MaterialTheme.typography.titleMedium)

            Text(
                "Durasi per gambar: %.1f detik".format(perImageDurationSec),
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = perImageDurationSec.toFloat(),
                onValueChange = { onDurationChange(it.toDouble()) },
                valueRange = 1f..15f,
                modifier = Modifier.testTag("slideshow_duration_slider")
            )

            if (isTransitionEnabled) {
                Text(
                    "Durasi transisi: %.1f detik".format(transitionDurationSec),
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = transitionDurationSec.toFloat(),
                    onValueChange = { onTransitionDurationChange(it.toDouble()) },
                    valueRange = 0.1f..(perImageDurationSec.toFloat() - 0.2f).coerceAtLeast(0.2f),
                    modifier = Modifier.testTag("slideshow_transition_slider")
                )
            }

            Text(
                "Estimasi durasi video: %.1f detik".format(estimatedDurationSec),
                style = MaterialTheme.typography.labelMedium,
                color = ProSuccess,
                modifier = Modifier.testTag("slideshow_estimated_duration")
            )
        }
    }
}

@Composable
private fun SlideshowStyleControls(
    transition: String,
    aspectRatio: String,
    resolution: String,
    onTransitionChange: (String) -> Unit,
    onAspectRatioChange: (String) -> Unit,
    onResolutionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("4. Gaya & Format", style = MaterialTheme.typography.titleMedium)

            ChipGroup(
                label = "Transisi",
                options = SlideshowViewModel.TRANSITIONS,
                selected = transition,
                onSelect = onTransitionChange,
                tagPrefix = "slideshow_transition"
            )
            ChipGroup(
                label = "Rasio",
                options = SlideshowViewModel.ASPECT_RATIOS,
                selected = aspectRatio,
                onSelect = onAspectRatioChange,
                tagPrefix = "slideshow_ratio"
            )
            ChipGroup(
                label = "Resolusi",
                options = SlideshowViewModel.RESOLUTIONS,
                selected = resolution,
                onSelect = onResolutionChange,
                tagPrefix = "slideshow_resolution"
            )
        }
    }
}

@Composable
private fun ChipGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    tagPrefix: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(options) { _, option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(if (option == "none") "tanpa transisi" else option) },
                    modifier = Modifier.testTag("${tagPrefix}_$option")
                )
            }
        }
    }
}

@Composable
private fun SlideshowRenderSection(
    uiState: SlideshowUiState,
    onRender: () -> Unit,
    onCancel: () -> Unit,
    onNavigateToGoLive: (String) -> Unit
) {
    val progress = uiState.jobProgress

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("5. Render", style = MaterialTheme.typography.titleMedium)

            if (progress.isProcessing) {
                FfmpegCircularProgressIndicator(
                    progress = progress.progress,
                    statusText = progress.statusText,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().testTag("slideshow_cancel_render")
                ) {
                    Text("Batalkan render")
                }
            } else {
                Button(
                    onClick = onRender,
                    enabled = uiState.canRender,
                    modifier = Modifier.fillMaxWidth().testTag("slideshow_render_button")
                ) {
                    Text("Render slideshow")
                }
            }

            progress.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = ProLive,
                    modifier = Modifier.testTag("slideshow_error_message")
                )
            }

            if (!progress.isProcessing && progress.outputFilePath.isNotBlank()) {
                Text(
                    text = progress.statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ProSuccess
                )
                OutlinedButton(
                    onClick = { onNavigateToGoLive(progress.outputFilePath) },
                    modifier = Modifier.fillMaxWidth().testTag("slideshow_go_live_button")
                ) {
                    Text("Siarkan hasil ini")
                }
            }
        }
    }
}
