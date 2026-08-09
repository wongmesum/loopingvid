package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.audio.AudioAnalysisRepository
import com.example.core.database.AppDatabase
import com.example.core.database.FirestoreJobHistorySyncManager
import com.example.core.database.LoopingVidRepository
import com.example.core.ffmpeg.MediaProcessor
import com.example.core.ffmpeg.VisualizerProcessor
import com.example.feature.editor.EditorViewModel
import com.example.feature.history.HistoryViewModel
import com.example.feature.live.LiveViewModel
import com.example.feature.loop.LoopViewModel
import com.example.feature.mastering.MasteringViewModel
import com.example.feature.project.ProjectManagerViewModel
import com.example.feature.settings.SettingsViewModel
import com.example.feature.slideshow.SlideshowViewModel
import com.example.core.ffmpeg.SlideshowProcessor
import com.example.feature.visualizer.VisualizerViewModel
import com.example.ui.navigation.MainScreen
import com.example.ui.theme.LoopingVidTheme
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
        val repository = LoopingVidRepository(db.renderJobDao(), db.liveSessionDao(), db.appSettingDao(), db.jobHistoryDao(), db.editorAutoSaveDao(), db.projectDao())
        val firestoreSyncManager = FirestoreJobHistorySyncManager(applicationContext, repository)
        repository.firestoreSyncManager = firestoreSyncManager

        val mediaProcessor = MediaProcessor(applicationContext, repository)
        val audioAnalysisRepository = AudioAnalysisRepository(applicationContext)

        val loopViewModel = LoopViewModel(mediaProcessor)
        val masteringViewModel = MasteringViewModel(mediaProcessor, applicationContext, audioAnalysisRepository)
        val editorViewModel = EditorViewModel(mediaProcessor, applicationContext)
        val liveViewModel = LiveViewModel(repository)
        val historyViewModel = HistoryViewModel(repository, firestoreSyncManager)
        val projectManagerViewModel = ProjectManagerViewModel(repository)
        val settingsViewModel = SettingsViewModel(repository)
        val slideshowProcessor = SlideshowProcessor(applicationContext, repository)
        val slideshowViewModel = SlideshowViewModel(slideshowProcessor)
        val visualizerProcessor = VisualizerProcessor(applicationContext, repository)
        val visualizerViewModel = VisualizerViewModel(applicationContext, visualizerProcessor, audioAnalysisRepository)
        val exportViewModel = com.example.core.ui.ExportViewModel(mediaProcessor)
        val exportQueueViewModel = com.example.core.work.ExportQueueViewModel(application)

        setContent {
            LoopingVidTheme {
                MainScreen(
                    loopViewModel = loopViewModel,
                    masteringViewModel = masteringViewModel,
                    editorViewModel = editorViewModel,
                    liveViewModel = liveViewModel,
                    historyViewModel = historyViewModel,
                    projectManagerViewModel = projectManagerViewModel,
                    settingsViewModel = settingsViewModel,
                    slideshowViewModel = slideshowViewModel,
                    visualizerViewModel = visualizerViewModel,
                    exportViewModel = exportViewModel,
                    exportQueueViewModel = exportQueueViewModel,
                    audioAnalysisRepository = audioAnalysisRepository
                )
            }
        }
    }
}


