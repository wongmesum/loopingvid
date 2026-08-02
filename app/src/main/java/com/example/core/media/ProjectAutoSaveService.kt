package com.example.core.media

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.feature.editor.EditorUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Background Service that automatically serializes and saves the active project state
 * to the local Room SQLite Database every 30 seconds for crash and unexpected closure recovery.
 */
class ProjectAutoSaveService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var autoSaveLoopJob: Job? = null

    private val binder = LocalBinder()
    private lateinit var autoSaveManager: EditorAutoSaveManager

    inner class LocalBinder : Binder() {
        fun getService(): ProjectAutoSaveService = this@ProjectAutoSaveService
    }

    override fun onCreate() {
        super.onCreate()
        autoSaveManager = EditorAutoSaveManager(applicationContext)
        Timber.d("ProjectAutoSaveService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startThirtySecondAutoSaveLoop()
        return START_STICKY
    }

    private fun startThirtySecondAutoSaveLoop() {
        autoSaveLoopJob?.cancel()
        autoSaveLoopJob = serviceScope.launch {
            while (true) {
                delay(AUTO_SAVE_INTERVAL_MS)
                val currentState = currentActiveProjectState.value
                if (currentState != null && (currentState.selectedMediaUri != null || currentState.titleText.isNotBlank())) {
                    val saved = autoSaveManager.saveSession(currentState, wasCleanExit = false)
                    if (saved) {
                        Timber.d("ProjectAutoSaveService: Background 30s auto-save to Room DB succeeded.")
                    } else {
                        Timber.w("ProjectAutoSaveService: Background 30s auto-save failed.")
                    }
                }
            }
        }
    }

    fun saveCurrentProjectNow(state: EditorUiState, wasCleanExit: Boolean = false) {
        serviceScope.launch {
            autoSaveManager.saveSession(state, wasCleanExit = wasCleanExit)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        autoSaveLoopJob?.cancel()
        serviceJob.cancel()
        Timber.d("ProjectAutoSaveService destroyed")
    }

    companion object {
        const val AUTO_SAVE_INTERVAL_MS = 30_000L // 30 seconds

        val currentActiveProjectState = MutableStateFlow<EditorUiState?>(null)

        fun updateActiveProjectState(state: EditorUiState?) {
            currentActiveProjectState.value = state
        }

        fun startService(context: Context) {
            val intent = Intent(context, ProjectAutoSaveService::class.java)
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start ProjectAutoSaveService")
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ProjectAutoSaveService::class.java)
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop ProjectAutoSaveService")
            }
        }
    }
}
