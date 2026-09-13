package com.example.core.database

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

enum class CloudSyncState {
    DISCONNECTED,
    SYNCING,
    SYNCED,
    ERROR
}

data class FirestoreSyncInfo(
    val syncState: CloudSyncState = CloudSyncState.DISCONNECTED,
    val isAutoSyncEnabled: Boolean = true,
    val totalSyncedCount: Int = 0,
    val remoteDeviceCount: Int = 0,
    val lastSyncedTimeMs: Long = 0L,
    val currentDeviceId: String = "",
    val errorMessage: String? = null
)

/**
 * FirestoreJobHistorySyncManager handles real-time cross-device synchronization
 * of job history logs using Firebase Firestore.
 */
class FirestoreJobHistorySyncManager(
    private val context: Context,
    private val repository: LoopingVidRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("HardwareIds")
    val currentDeviceId: String = try {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        "${Build.MODEL.replace(" ", "_")}_${androidId?.takeLast(4) ?: "DEV"}"
    } catch (e: Exception) {
        "Device_${Build.MODEL.replace(" ", "_")}"
    }

    private val _syncInfo = MutableStateFlow(
        FirestoreSyncInfo(
            currentDeviceId = currentDeviceId
        )
    )
    val syncInfo: StateFlow<FirestoreSyncInfo> = _syncInfo.asStateFlow()

    private var snapshotListener: ListenerRegistration? = null
    private var isFirestoreAvailable = true

    private fun getFirestoreInstance(): FirebaseFirestore? {
        if (!isFirestoreAvailable) return null
        return try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }
            if (app == null) {
                isFirestoreAvailable = false
                null
            } else {
                FirebaseFirestore.getInstance(app)
            }
        } catch (e: Throwable) {
            Timber.i("Firebase Firestore is not initialized or available: ${e.localizedMessage}")
            isFirestoreAvailable = false
            null
        }
    }

    init {
        scope.launch {
            val enabled = repository.getSettingValue("firestore_sync_enabled")?.toBooleanStrictOrNull() ?: true
            _syncInfo.value = _syncInfo.value.copy(isAutoSyncEnabled = enabled)
            if (enabled) {
                startRealtimeSync()
            } else {
                _syncInfo.value = _syncInfo.value.copy(syncState = CloudSyncState.DISCONNECTED)
            }
        }
    }

    fun startRealtimeSync() {
        scope.launch {
            try {
                if (!isFirestoreAvailable) return@launch
                
                val db = getFirestoreInstance()
                if (db == null) {
                    _syncInfo.value = _syncInfo.value.copy(
                        syncState = CloudSyncState.DISCONNECTED,
                        errorMessage = "Cloud sync unavailable (Firebase not configured)"
                    )
                    return@launch
                }
                
                _syncInfo.value = _syncInfo.value.copy(syncState = CloudSyncState.SYNCING)

                snapshotListener?.remove()
                snapshotListener = db.collection("job_history_logs")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Timber.w(error, "Firestore snapshot listener error")
                            _syncInfo.value = _syncInfo.value.copy(
                                syncState = CloudSyncState.ERROR,
                                errorMessage = error.localizedMessage ?: "Sync error"
                            )
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val documents = snapshot.documents
                            val deviceSet = mutableSetOf<String>()
                            var remoteCount = 0

                            for (doc in documents) {
                                val devId = doc.getString("deviceId") ?: ""
                                if (devId.isNotEmpty()) deviceSet.add(devId)
                                if (devId != currentDeviceId && devId.isNotEmpty()) {
                                    remoteCount++
                                }

                                // Extract remote job fields and merge into Room
                                val title = doc.getString("title") ?: continue
                                val jobType = doc.getString("jobType") ?: "EDITOR"
                                val status = doc.getString("status") ?: "COMPLETED"
                                val inputUri = doc.getString("inputUri") ?: ""
                                val outputUri = doc.getString("outputUri") ?: ""
                                val paramsSummary = doc.getString("paramsSummary") ?: ""
                                val fileSizeMb = doc.getDouble("fileSizeMb") ?: 0.0
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                val style = doc.getString("style") ?: "NORMAL"
                                val durationSec = doc.getDouble("durationSec") ?: 0.0
                                val progress = (doc.getLong("progress") ?: 100L).toInt()

                                scope.launch(Dispatchers.IO) {
                                    // Also save into JobHistoryDao if present
                                    repository.saveJobHistory(
                                        JobHistoryEntity(
                                            taskType = jobType,
                                            title = if (devId != currentDeviceId) "$title [Synced from $devId]" else title,
                                            status = status,
                                            inputUri = inputUri,
                                            outputUri = outputUri,
                                            executionLogs = paramsSummary,
                                            errorMessage = if (status == "FAILED") "Remote execution failure" else null,
                                            durationMs = (durationSec * 1000).toLong(),
                                            fileSizeBytes = (fileSizeMb * 1024 * 1024).toLong(),
                                            timestamp = createdAt
                                        )
                                    )
                                }
                            }

                            _syncInfo.value = _syncInfo.value.copy(
                                syncState = CloudSyncState.SYNCED,
                                totalSyncedCount = documents.size,
                                remoteDeviceCount = deviceSet.size.coerceAtLeast(1),
                                lastSyncedTimeMs = System.currentTimeMillis(),
                                errorMessage = null
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Firebase Firestore initialization failed")
                isFirestoreAvailable = false
                _syncInfo.value = _syncInfo.value.copy(
                    syncState = CloudSyncState.DISCONNECTED,
                    errorMessage = "Firestore Offline Mode (${e.localizedMessage})"
                )
            }
        }
    }

    /**
     * Uploads [job] to Firestore. [onJobSynced] fires only once this SPECIFIC job's document
     * write actually succeeds, so the caller (repository) can persist a per-job synced flag
     * instead of relying on the app-wide [FirestoreSyncInfo.syncState] - previously the History
     * screen's per-job "Synced to Firestore" badge read that global state, so every job in the
     * list showed the same badge regardless of whether it individually reached the cloud.
     */
    suspend fun syncRenderJobToFirestore(job: RenderJobEntity, onJobSynced: ((Long) -> Unit)? = null) = withContext(Dispatchers.IO) {
        if (!_syncInfo.value.isAutoSyncEnabled || !isFirestoreAvailable) return@withContext
        try {
            _syncInfo.value = _syncInfo.value.copy(syncState = CloudSyncState.SYNCING)
            val db = getFirestoreInstance() ?: return@withContext

            val docData = hashMapOf(
                "jobId" to job.id,
                "jobType" to job.jobType,
                "title" to job.title,
                "inputUri" to job.inputUri,
                "outputUri" to job.outputUri,
                "style" to job.style,
                "status" to job.status,
                "progress" to job.progress,
                "durationSec" to job.durationSec,
                "fileSizeMb" to job.fileSizeMb,
                "createdAt" to job.createdAt,
                "paramsSummary" to job.paramsSummary,
                "deviceId" to currentDeviceId,
                "deviceModel" to Build.MODEL,
                "lastUpdated" to System.currentTimeMillis()
            )

            val docId = "job_${currentDeviceId}_${job.id}"
            db.collection("job_history_logs")
                .document(docId)
                .set(docData, SetOptions.merge())
                .addOnSuccessListener {
                    _syncInfo.value = _syncInfo.value.copy(
                        syncState = CloudSyncState.SYNCED,
                        lastSyncedTimeMs = System.currentTimeMillis(),
                        errorMessage = null
                    )
                    onJobSynced?.invoke(job.id)
                }
                .addOnFailureListener { err ->
                    _syncInfo.value = _syncInfo.value.copy(
                        syncState = CloudSyncState.ERROR,
                        errorMessage = err.localizedMessage
                    )
                }
        } catch (e: Exception) {
            Timber.w(e, "Failed to upload render job to Firestore")
            _syncInfo.value = _syncInfo.value.copy(
                syncState = CloudSyncState.DISCONNECTED,
                errorMessage = e.localizedMessage
            )
        }
    }

    suspend fun syncJobHistoryToFirestore(history: JobHistoryEntity) = withContext(Dispatchers.IO) {
        if (!_syncInfo.value.isAutoSyncEnabled || !isFirestoreAvailable) return@withContext
        try {
            _syncInfo.value = _syncInfo.value.copy(syncState = CloudSyncState.SYNCING)
            val db = getFirestoreInstance() ?: return@withContext

            val docData = hashMapOf(
                "historyId" to history.id,
                "jobType" to history.taskType,
                "title" to history.title,
                "status" to history.status,
                "inputUri" to history.inputUri,
                "outputUri" to history.outputUri,
                "paramsSummary" to history.executionLogs,
                "errorMessage" to history.errorMessage,
                "durationSec" to (history.durationMs / 1000.0),
                "fileSizeMb" to (history.fileSizeBytes.toDouble() / (1024 * 1024)),
                "createdAt" to history.timestamp,
                "deviceId" to currentDeviceId,
                "deviceModel" to Build.MODEL,
                "lastUpdated" to System.currentTimeMillis()
            )

            val docId = "history_${currentDeviceId}_${history.id}_${history.timestamp}"
            db.collection("job_history_logs")
                .document(docId)
                .set(docData, SetOptions.merge())
                .addOnSuccessListener {
                    _syncInfo.value = _syncInfo.value.copy(
                        syncState = CloudSyncState.SYNCED,
                        lastSyncedTimeMs = System.currentTimeMillis()
                    )
                }
        } catch (e: Exception) {
            Timber.w(e, "Failed to upload job history to Firestore")
        }
    }

    fun triggerManualSync(jobsList: List<RenderJobEntity>) {
        scope.launch(Dispatchers.IO) {
            _syncInfo.value = _syncInfo.value.copy(syncState = CloudSyncState.SYNCING)
            for (job in jobsList) {
                syncRenderJobToFirestore(job)
            }
            startRealtimeSync()
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        _syncInfo.value = _syncInfo.value.copy(isAutoSyncEnabled = enabled)
        if (enabled) {
            startRealtimeSync()
        } else {
            snapshotListener?.remove()
            snapshotListener = null
            _syncInfo.value = _syncInfo.value.copy(syncState = CloudSyncState.DISCONNECTED)
        }
    }

    fun toggleAutoSync() {
        val newEnabled = !_syncInfo.value.isAutoSyncEnabled
        setAutoSyncEnabled(newEnabled)
        scope.launch {
            repository.setSetting("firestore_sync_enabled", newEnabled.toString())
        }
    }

    fun stopSync() {
        snapshotListener?.remove()
    }
}
