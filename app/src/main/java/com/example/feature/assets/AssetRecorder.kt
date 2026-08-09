package com.example.feature.assets

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.core.ui.SelectedMediaFile
import timber.log.Timber

/**
 * Records a real user media pick into the asset library.
 *
 * Resolving metadata touches the ContentResolver, so this stays at the picker call
 * site instead of inside [AssetManagerViewModel], which holds no Context.
 */
fun recordPickedMedia(
    context: Context,
    uri: Uri,
    viewModel: AssetManagerViewModel?
) {
    if (viewModel == null) return
    try {
        val file = SelectedMediaFile.fromUri(context, uri)
        file.takePersistablePermission(context)
        // takePersistablePermission() swallows failures, so the grant is re-read
        // instead of trusted. GetContent() URIs are usually not persistable.
        viewModel.recordAccess(file, file.hasPersistedReadPermission(context))
    } catch (e: Exception) {
        Timber.e(e, "Failed to record picked media into asset library")
    }
}

/**
 * Returns a callback that records a picked URI, bound to the current Context.
 */
@Composable
fun rememberAssetRecorder(viewModel: AssetManagerViewModel?): (Uri) -> Unit {
    val context = LocalContext.current
    return remember(context, viewModel) {
        { uri: Uri -> recordPickedMedia(context, uri, viewModel) }
    }
}
