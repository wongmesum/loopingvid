package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.core.database.AppDatabase
import com.example.core.database.FirestoreJobHistorySyncManager
import com.example.core.database.LoopingVidRepository
import com.example.core.ffmpeg.MediaProcessor
import com.example.core.media.EditorAutoSaveManager
import com.example.feature.editor.EditorViewModel
import com.example.feature.history.HistoryViewModel
import com.example.feature.live.LiveViewModel
import com.example.feature.loop.LoopViewModel
import com.example.feature.mastering.MasteringViewModel
import com.example.feature.settings.SettingsViewModel
import com.example.ui.navigation.MainScreen
import com.example.ui.theme.LoopingVidTheme
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for previous crashes
        val prefs = getSharedPreferences("crash_prefs", MODE_PRIVATE)
        val lastCrash = prefs.getString("last_crash", null)
        if (lastCrash != null) {
            Toast.makeText(this, "Previous crash: \n$lastCrash", Toast.LENGTH_LONG).show()
            android.util.Log.e("CrashDetector", "Previous crash detected: $lastCrash")
            prefs.edit().remove("last_crash").apply()
        }

        // Global crash handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            val trace = android.util.Log.getStackTraceString(exception)
            getSharedPreferences("crash_prefs", MODE_PRIVATE)
                .edit()
                .putString("last_crash", trace)
                .commit()
            defaultHandler?.uncaughtException(thread, exception)
        }

        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = LoopingVidRepository(db.renderJobDao(), db.liveSessionDao(), db.appSettingDao(), db.jobHistoryDao())
        val firestoreSyncManager = FirestoreJobHistorySyncManager(applicationContext, repository)
        repository.firestoreSyncManager = firestoreSyncManager

        val mediaProcessor = MediaProcessor(applicationContext, repository)

        val loopViewModel = LoopViewModel(mediaProcessor)
        val masteringViewModel = MasteringViewModel(mediaProcessor, applicationContext)
        val editorViewModel = EditorViewModel(mediaProcessor, applicationContext)
        val liveViewModel = LiveViewModel(repository)
        val historyViewModel = HistoryViewModel(repository, firestoreSyncManager, mediaProcessor)
        val settingsViewModel = SettingsViewModel(repository)
        val exportViewModel = com.example.core.ui.ExportViewModel(mediaProcessor, repository)
        val exportQueueViewModel = com.example.core.work.ExportQueueViewModel(application)

        setContent {
            LoopingVidTheme {
                MainScreen(
                    loopViewModel = loopViewModel,
                    masteringViewModel = masteringViewModel,
                    editorViewModel = editorViewModel,
                    liveViewModel = liveViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel,
                    exportViewModel = exportViewModel,
                    exportQueueViewModel = exportQueueViewModel
                )
            }
        }
    }

    /**
     * Marks the Editor auto-save session as a clean exit whenever the user intentionally leaves
     * or closes the app (finishing this Activity). This is what [EditorAutoSaveManager.markCleanExit]
     * was written for but previously had no caller anywhere in the app, so the recovery banner in
     * [MainScreen] would show up on every normal restart too (any saved session always looked
     * "unexpected" because `wasCleanExit` could never become true). A force-kill/crash skips
     * onStop entirely, so the flag correctly stays false in that case and the recovery prompt still
     * appears where it's actually needed.
     */
    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            lifecycleScope.launch {
                EditorAutoSaveManager(applicationContext).markCleanExit()
            }
        }
    }
}


