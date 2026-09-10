package com.example.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LoopingVidRepository(
    private val renderJobDao: RenderJobDao,
    private val liveSessionDao: LiveSessionDao,
    private val appSettingDao: AppSettingDao,
    private val jobHistoryDao: JobHistoryDao? = null,
    private val editorAutoSaveDao: EditorAutoSaveDao? = null,
    var firestoreSyncManager: FirestoreJobHistorySyncManager? = null
) {
    // Editor Auto-Save Session
    val observeAutoSaveSession: Flow<EditorAutoSaveEntity?> =
        editorAutoSaveDao?.observeAutoSaveSession() ?: kotlinx.coroutines.flow.flowOf(null)

    suspend fun getAutoSaveSession(): EditorAutoSaveEntity? =
        editorAutoSaveDao?.getAutoSaveSession()

    suspend fun saveAutoSaveSession(session: EditorAutoSaveEntity) {
        editorAutoSaveDao?.saveAutoSaveSession(session)
    }

    suspend fun clearAutoSaveSession() {
        editorAutoSaveDao?.clearAutoSaveSession()
    }
    // Job History & Logs
    val allHistory: Flow<List<JobHistoryEntity>> = jobHistoryDao?.getAllHistory() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getHistoryByTaskType(taskType: String): Flow<List<JobHistoryEntity>> =
        jobHistoryDao?.getHistoryByTaskType(taskType) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun saveJobHistory(history: JobHistoryEntity): Long {
        val id = jobHistoryDao?.insertHistory(history) ?: -1L
        if (id > 0) {
            val savedEntity = history.copy(id = id)
            firestoreSyncManager?.syncJobHistoryToFirestore(savedEntity)
        }
        return id
    }

    suspend fun updateJobHistory(history: JobHistoryEntity) {
        jobHistoryDao?.updateHistory(history)
        firestoreSyncManager?.syncJobHistoryToFirestore(history)
    }

    suspend fun deleteJobHistoryById(id: Long) =
        jobHistoryDao?.deleteHistoryById(id)

    suspend fun clearAllHistory() =
        jobHistoryDao?.clearAllHistory()

    // Render Jobs
    val allJobs: Flow<List<RenderJobEntity>> = renderJobDao.getAllJobs()

    fun getJobsByType(type: String): Flow<List<RenderJobEntity>> = renderJobDao.getJobsByType(type)

    suspend fun getJobById(id: Long): RenderJobEntity? = renderJobDao.getJobById(id)

    // Background scope for the fire-and-forget "mark job as synced" write triggered from
    // Firestore's success callback, which runs outside of any caller's coroutine (Firestore's
    // listener callbacks execute on their own internal thread, not inside a suspend context).
    private val syncFlagScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    suspend fun saveJob(job: RenderJobEntity): Long {
        val id = renderJobDao.insertJob(job)
        if (id > 0) {
            val savedJob = job.copy(id = id)
            firestoreSyncManager?.syncRenderJobToFirestore(savedJob) { syncedJobId ->
                syncFlagScope.launch { markJobSyncedToCloud(syncedJobId) }
            }
        }
        return id
    }

    suspend fun updateJob(job: RenderJobEntity) {
        renderJobDao.updateJob(job)
        firestoreSyncManager?.syncRenderJobToFirestore(job) { syncedJobId ->
            syncFlagScope.launch { markJobSyncedToCloud(syncedJobId) }
        }
    }

    /**
     * Marks a single job as successfully uploaded to Firestore, called only from the real
     * upload success callback in [FirestoreJobHistorySyncManager.syncRenderJobToFirestore] -
     * never speculatively - so [RenderJobEntity.isSyncedToCloud] reflects that specific job's
     * actual cloud status instead of the app-wide sync indicator.
     */
    private suspend fun markJobSyncedToCloud(jobId: Long) {
        try {
            val current = renderJobDao.getJobById(jobId) ?: return
            if (!current.isSyncedToCloud) {
                // Direct DAO write (not updateJob()) - re-triggering another Firestore sync here
                // for a flag-only local change would be pointless and could recurse.
                renderJobDao.updateJob(current.copy(isSyncedToCloud = true))
            }
        } catch (_: Exception) {
            // Best-effort: a failure to persist the flag doesn't affect the actual cloud upload.
        }
    }

    suspend fun deleteJobById(id: Long) = renderJobDao.deleteJobById(id)

    suspend fun clearAllJobs() = renderJobDao.clearAllJobs()

    // Live Sessions
    val allLiveSessions: Flow<List<LiveSessionEntity>> = liveSessionDao.getAllSessions()

    suspend fun saveLiveSession(session: LiveSessionEntity): Long = liveSessionDao.insertSession(session)

    suspend fun updateLiveSession(session: LiveSessionEntity) = liveSessionDao.updateSession(session)

    suspend fun deleteLiveSessionById(id: Long) = liveSessionDao.deleteSessionById(id)

    // Settings
    val allSettings: Flow<List<AppSettingEntity>> = appSettingDao.getAllSettings()

    suspend fun getSettingValue(key: String): String? = appSettingDao.getValueByKey(key)

    fun observeSettingValue(key: String): Flow<String?> = appSettingDao.observeValueByKey(key)

    suspend fun setSetting(key: String, value: String) {
        appSettingDao.setSetting(AppSettingEntity(key, value))
    }
}
