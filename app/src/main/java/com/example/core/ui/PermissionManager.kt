package com.example.core.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Represents permission categories required across feature modules.
 */
enum class AppPermissionGroup(
    val title: String,
    val description: String,
    val iconName: String
) {
    CAMERA(
        title = "Camera Access",
        description = "Required to capture live video stream feeds and preview video in real time",
        iconName = "Videocam"
    ),
    MICROPHONE(
        title = "Microphone Access",
        description = "Required to capture live stream audio, ambient sound, and mix audio tracks",
        iconName = "Mic"
    ),
    FOREGROUND_SERVICE(
        title = "Foreground Service & Notifications",
        description = "Required to run background stream encoding and display persistent status notifications",
        iconName = "Notifications"
    ),
    STORAGE(
        title = "Media Storage Access",
        description = "Required to import local video clips and export recorded stream files",
        iconName = "Folder"
    );

    /**
     * Returns string permissions needed for this permission group depending on Android API level.
     */
    fun getPermissions(): Array<String> {
        return when (this) {
            CAMERA -> arrayOf(Manifest.permission.CAMERA)
            MICROPHONE -> arrayOf(Manifest.permission.RECORD_AUDIO)
            FOREGROUND_SERVICE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.FOREGROUND_SERVICE)
                } else {
                    arrayOf(Manifest.permission.FOREGROUND_SERVICE)
                }
            }
            STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
}

/**
 * Status snapshot for a permission group.
 */
data class PermissionStatusInfo(
    val group: AppPermissionGroup,
    val isGranted: Boolean,
    val missingPermissions: List<String>
)

/**
 * Utility functions for central permission state checking.
 */
object PermissionManager {

    /**
     * Checks whether all permissions in a specific permission group are granted.
     */
    fun isGroupGranted(context: Context, group: AppPermissionGroup): Boolean {
        return group.getPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks permission status for a group.
     */
    fun getGroupStatus(context: Context, group: AppPermissionGroup): PermissionStatusInfo {
        val missing = group.getPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        return PermissionStatusInfo(
            group = group,
            isGranted = missing.isEmpty(),
            missingPermissions = missing
        )
    }

    /**
     * Obtains statuses for all app permission groups.
     */
    fun getAllStatuses(context: Context): Map<AppPermissionGroup, PermissionStatusInfo> {
        return AppPermissionGroup.entries.associateWith { getGroupStatus(context, it) }
    }

    /**
     * Opens system app details settings page for the current app.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * Compose state holder and controller for PermissionManager.
 */
class PermissionManagerState(
    val context: Context,
    private val onPermissionsUpdated: () -> Unit
) {
    var statuses by mutableStateOf(PermissionManager.getAllStatuses(context))
        private set

    var showRationaleDialog by mutableStateOf(false)
        private set

    var pendingGroupRequest by mutableStateOf<AppPermissionGroup?>(null)
        private set

    fun refreshStatuses() {
        statuses = PermissionManager.getAllStatuses(context)
        onPermissionsUpdated()
    }

    fun isAllGranted(): Boolean {
        return statuses.values.all { it.isGranted }
    }

    fun isGranted(group: AppPermissionGroup): Boolean {
        return statuses[group]?.isGranted == true
    }

    fun dismissRationaleDialog() {
        showRationaleDialog = false
        pendingGroupRequest = null
    }

    fun requestPermissionGroup(group: AppPermissionGroup, launcher: (Array<String>) -> Unit) {
        if (isGranted(group)) return
        pendingGroupRequest = group
        val permissions = group.getPermissions()
        launcher(permissions)
    }

    fun openAppSettings() {
        PermissionManager.openAppSettings(context)
    }
}

/**
 * Creates and remembers a PermissionManagerState across lifecycle resumptions.
 */
@Composable
fun rememberPermissionManager(
    onPermissionsUpdated: () -> Unit = {}
): Pair<PermissionManagerState, (Array<String>) -> Unit> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val state = remember(context) {
        PermissionManagerState(context, onPermissionsUpdated)
    }

    // Multiple permission launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        state.refreshStatuses()
    }

    // Refresh permission statuses on resume lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.refreshStatuses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return Pair(state, launcher::launch)
}
