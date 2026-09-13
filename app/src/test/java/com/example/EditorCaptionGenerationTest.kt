package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.database.LoopingVidRepository
import com.example.core.ffmpeg.MediaProcessor
import com.example.feature.editor.EditorViewModel
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Bug condition exploration test for Area 1 (Editor - Generate Auto Captions) of the
 * action-failure-error-popup bugfix.
 *
 * Property 1 (Bug Condition): Editor Caption Generation Uncaught Exception.
 *
 * GOAL: prove that on the CURRENT (unfixed) [EditorViewModel.generateAutoCaptions], an uncaught
 * exception thrown inside the `viewModelScope.launch` body (here: the settings DAO access via
 * `AppDatabase.getDatabase(context).appSettingDao().getValueByKey(...)`) leaves
 * `jobProgress.isProcessing == true` forever, with no field anywhere in [EditorUiState] to signal
 * the failure to the UI.
 *
 * Scoped/deterministic reproduction: the singleton Room [AppDatabase] is closed *before*
 * `generateAutoCaptions()` runs, so `appSettingDao().getValueByKey(...)` throws
 * `IllegalStateException` ("attempt to re-open an already-closed object" / similar SQLite
 * exception) on the closed database - mirroring the design's DB-locked example.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EditorCaptionGenerationTest {

    private lateinit var context: Context
    private lateinit var mediaProcessor: MediaProcessor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // MediaProcessor needs *a* repository to construct, but generateAutoCaptions() talks
        // directly to the AppDatabase singleton (via AppDatabase.getDatabase(context)), not to
        // this injected repository - so an in-memory DB here is fine and unrelated to the bug
        // reproduction below.
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = LoopingVidRepository(
            inMemoryDb.renderJobDao(),
            inMemoryDb.liveSessionDao(),
            inMemoryDb.appSettingDao()
        )
        mediaProcessor = MediaProcessor(context, repository)
    }

    /**
     * Robolectric's main looper is PAUSED by default, and Room's suspend DAO calls hop onto a
     * real background query-executor thread before resuming the coroutine back on the main
     * dispatcher. This helper gives that real background work a chance to finish and then flushes
     * the (paused) main looper so any posted coroutine resumption actually runs.
     */
    private fun awaitBackgroundWork(iterations: Int = 40, stepMs: Long = 50) {
        repeat(iterations) {
            Thread.sleep(stepMs)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `generateAutoCaptions never resets isProcessing when DAO throws on closed DB`() {
        val viewModel = EditorViewModel(mediaProcessor, context)

        // Let EditorViewModel's init-time coroutines (auto-save session check, which also touches
        // the AppDatabase singleton) settle against the still-OPEN database first, so the closed-DB
        // failure below is isolated to generateAutoCaptions() itself.
        awaitBackgroundWork(iterations = 30)

        viewModel.onMediaSelected("content://media/fake_video.mp4", "fake_video.mp4")

        // Force the concrete failing case: close the singleton AppDatabase BEFORE
        // generateAutoCaptions() runs, so appSettingDao().getValueByKey("gemini_api_key") throws
        // an IllegalStateException/SQLiteException on the closed DB.
        AppDatabase.getDatabase(context).close()

        viewModel.generateAutoCaptions()
        awaitBackgroundWork()

        val state = viewModel.uiState.value

        // EXPECTED POST-FIX SHAPE: once task 3 wraps generateAutoCaptions() in try/catch, the
        // caught exception resets jobProgress.isProcessing to false (in addition to populating
        // the not-yet-existing captionGenerationError field, checked separately in task 3.2).
        //
        // On UNFIXED code this assertion FAILS: isProcessing was flipped to true at the very
        // start of the coroutine ("Analyzing audio with Gemini 1.5 Pro...") and nothing ever
        // resets it, because the exception thrown by the closed-DB DAO call is never caught -
        // it is only visible via the default CoroutineExceptionHandler / stderr, never through
        // EditorUiState.
        //
        // COUNTEREXAMPLE: closing the Room AppDatabase before calling generateAutoCaptions()
        // leaves jobProgress.isProcessing == true forever, and no field in EditorUiState
        // indicates that caption generation failed.
        assertFalse(
            "BUG CONFIRMED: jobProgress.isProcessing is stuck at true forever after an " +
                "uncaught exception in generateAutoCaptions() (closed-DB DAO access). It should " +
                "be reset to false once the fix (task 3) wraps the coroutine body in try/catch.",
            state.jobProgress.isProcessing
        )
    }
}
