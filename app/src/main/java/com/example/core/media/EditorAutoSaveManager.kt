package com.example.core.media

import android.content.Context
import com.example.core.database.AppDatabase
import com.example.core.database.EditorAutoSaveEntity
import com.example.feature.editor.EditorUiState
import com.example.feature.editor.VisualizerMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AutoSaveSessionInfo(
    val exists: Boolean = false,
    val lastSavedTimestamp: Long = 0L,
    val mediaName: String = "",
    val mediaUri: String? = null,
    val titleText: String = "",
    val fileSizeFormatted: String = "0 B",
    val formattedTime: String = "",
    val wasCleanExit: Boolean = false
)

class EditorAutoSaveManager(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).editorAutoSaveDao()

    val sessionInfoFlow: Flow<AutoSaveSessionInfo> = dao.observeAutoSaveSession().map { entity ->
        if (entity == null) {
            AutoSaveSessionInfo(exists = false)
        } else {
            val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            val formattedTime = sdf.format(Date(entity.lastSavedTimestamp))
            val mediaName = if (entity.selectedMediaName.isBlank()) entity.titleText else entity.selectedMediaName
            AutoSaveSessionInfo(
                exists = true,
                lastSavedTimestamp = entity.lastSavedTimestamp,
                mediaName = if (mediaName.isBlank()) "Untitled Video Project" else mediaName,
                mediaUri = entity.selectedMediaUri,
                titleText = entity.titleText,
                fileSizeFormatted = "SQLite Room DB",
                formattedTime = formattedTime,
                wasCleanExit = entity.wasCleanExit
            )
        }
    }

    suspend fun saveSession(state: EditorUiState, wasCleanExit: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            // Serialize ColorGradingConfig
            val colorObj = JSONObject().apply {
                put("preset", state.colorGradingConfig.preset.name)
                put("brightness", state.colorGradingConfig.brightness.toDouble())
                put("contrast", state.colorGradingConfig.contrast.toDouble())
                put("saturation", state.colorGradingConfig.saturation.toDouble())
                put("hue", state.colorGradingConfig.hue.toDouble())
            }

            // Serialize SegmentTransitionConfig
            val transObj = JSONObject().apply {
                put("globalTransitionEffect", state.transitionConfig.globalTransitionEffect.name)
                put("globalTransitionDurationSec", state.transitionConfig.globalTransitionDurationSec)
                val segmentsArray = JSONArray()
                state.transitionConfig.segments.forEach { seg ->
                    val segObj = JSONObject().apply {
                        put("id", seg.id)
                        put("segmentName", seg.segmentName)
                        put("durationSec", seg.durationSec)
                        put("loopRepeatCount", seg.loopRepeatCount)
                        put("transitionToNext", seg.transitionToNext.name)
                        put("transitionDurationSec", seg.transitionDurationSec)
                    }
                    segmentsArray.put(segObj)
                }
                put("segments", segmentsArray)
            }

            val entity = EditorAutoSaveEntity(
                id = 1,
                lastSavedTimestamp = System.currentTimeMillis(),
                selectedMediaUri = state.selectedMediaUri,
                selectedMediaName = state.selectedMediaName,
                selectedAudioUri = state.selectedAudioUri,
                selectedAudioName = state.selectedAudioName,
                titleText = state.titleText,
                watermarkText = state.watermarkText,
                showTimerOverlay = state.showTimerOverlay,
                spectrumStyle = state.spectrumStyle.name,
                presetQuality = state.presetQuality,
                visualizerMode = state.visualizerMode.name,
                playbackSpeed = state.playbackSpeed,
                selectedTemplateId = state.selectedTemplateId,
                trimStartSec = state.trimStartSec,
                trimEndSec = state.trimEndSec,
                colorGradingJson = colorObj.toString(),
                transitionConfigJson = transObj.toString(),
                version = 1,
                wasCleanExit = wasCleanExit
            )

            dao.saveAutoSaveSession(entity)
            Timber.d("Auto-saved editor session to Room SQLite Database (Timestamp: ${entity.lastSavedTimestamp})")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to auto-save editor session state to Room")
            false
        }
    }

    suspend fun loadSavedSession(): EditorUiState? = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getAutoSaveSession() ?: return@withContext null

            val spectrumStyle = try {
                SpectrumStyle.valueOf(entity.spectrumStyle)
            } catch (_: Exception) {
                SpectrumStyle.BARS
            }

            val visualizerMode = try {
                VisualizerMode.valueOf(entity.visualizerMode)
            } catch (_: Exception) {
                VisualizerMode.FFT_BARS
            }

            // Deserialize ColorGradingConfig
            var colorGradingConfig = ColorGradingConfig()
            if (entity.colorGradingJson.isNotBlank()) {
                try {
                    val colorObj = JSONObject(entity.colorGradingJson)
                    val presetStr = colorObj.optString("preset", ColorFilterPreset.NONE.name)
                    val preset = try { ColorFilterPreset.valueOf(presetStr) } catch (_: Exception) { ColorFilterPreset.NONE }
                    colorGradingConfig = ColorGradingConfig(
                        preset = preset,
                        brightness = colorObj.optDouble("brightness", 0.0).toFloat(),
                        contrast = colorObj.optDouble("contrast", 1.0).toFloat(),
                        saturation = colorObj.optDouble("saturation", 1.0).toFloat(),
                        hue = colorObj.optDouble("hue", 0.0).toFloat()
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing color grading JSON from Room auto-save entity")
                }
            }

            // Deserialize SegmentTransitionConfig
            var transitionConfig = SegmentTransitionConfig()
            if (entity.transitionConfigJson.isNotBlank()) {
                try {
                    val transObj = JSONObject(entity.transitionConfigJson)
                    val globalEffectStr = transObj.optString("globalTransitionEffect", TransitionEffect.CROSSFADE.name)
                    val globalEffect = try { TransitionEffect.valueOf(globalEffectStr) } catch (_: Exception) { TransitionEffect.CROSSFADE }
                    val globalDuration = transObj.optDouble("globalTransitionDurationSec", 1.0)

                    val segmentsList = mutableListOf<LoopedSegmentConfig>()
                    if (transObj.has("segments")) {
                        val segArray = transObj.getJSONArray("segments")
                        for (i in 0 until segArray.length()) {
                            val segObj = segArray.getJSONObject(i)
                            val id = segObj.optString("id", "seg_$i")
                            val segName = segObj.optString("segmentName", "Segment ${i + 1}")
                            val dur = segObj.optDouble("durationSec", 5.0)
                            val loops = segObj.optInt("loopRepeatCount", 2)
                            val transNextStr = segObj.optString("transitionToNext", TransitionEffect.CROSSFADE.name)
                            val transNext = try { TransitionEffect.valueOf(transNextStr) } catch (_: Exception) { TransitionEffect.CROSSFADE }
                            val transDur = segObj.optDouble("transitionDurationSec", 1.0)

                            segmentsList.add(
                                LoopedSegmentConfig(
                                    id = id,
                                    segmentName = segName,
                                    durationSec = dur,
                                    loopRepeatCount = loops,
                                    transitionToNext = transNext,
                                    transitionDurationSec = transDur
                                )
                            )
                        }
                    }

                    transitionConfig = SegmentTransitionConfig(
                        globalTransitionEffect = globalEffect,
                        globalTransitionDurationSec = globalDuration,
                        segments = if (segmentsList.isNotEmpty()) segmentsList else transitionConfig.segments
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing transition config JSON from Room auto-save entity")
                }
            }

            EditorUiState(
                selectedMediaUri = entity.selectedMediaUri,
                selectedMediaName = entity.selectedMediaName,
                selectedAudioUri = entity.selectedAudioUri,
                selectedAudioName = entity.selectedAudioName,
                titleText = entity.titleText,
                watermarkText = entity.watermarkText,
                showTimerOverlay = entity.showTimerOverlay,
                spectrumStyle = spectrumStyle,
                presetQuality = entity.presetQuality,
                visualizerMode = visualizerMode,
                colorGradingConfig = colorGradingConfig,
                transitionConfig = transitionConfig,
                playbackSpeed = entity.playbackSpeed,
                selectedTemplateId = entity.selectedTemplateId,
                trimStartSec = entity.trimStartSec,
                trimEndSec = entity.trimEndSec
            )
        } catch (e: Exception) {
            Timber.e(e, "Error loading Room auto-saved session")
            null
        }
    }

    suspend fun getSessionInfo(): AutoSaveSessionInfo = withContext(Dispatchers.IO) {
        val entity = dao.getAutoSaveSession()
        if (entity == null) {
            AutoSaveSessionInfo(exists = false)
        } else {
            val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            val formattedTime = sdf.format(Date(entity.lastSavedTimestamp))
            val mediaName = if (entity.selectedMediaName.isBlank()) entity.titleText else entity.selectedMediaName

            AutoSaveSessionInfo(
                exists = true,
                lastSavedTimestamp = entity.lastSavedTimestamp,
                mediaName = if (mediaName.isBlank()) "Untitled Video Project" else mediaName,
                mediaUri = entity.selectedMediaUri,
                titleText = entity.titleText,
                fileSizeFormatted = "Room SQLite",
                formattedTime = formattedTime,
                wasCleanExit = entity.wasCleanExit
            )
        }
    }

    suspend fun markCleanExit(): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getAutoSaveSession()
            if (entity != null) {
                dao.saveAutoSaveSession(entity.copy(wasCleanExit = true))
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark clean exit")
            false
        }
    }

    suspend fun isUnexpectedClosureDetected(): Boolean = withContext(Dispatchers.IO) {
        val entity = dao.getAutoSaveSession()
        entity != null && !entity.wasCleanExit && (entity.selectedMediaUri != null || entity.titleText.isNotBlank())
    }

    suspend fun discardSession(): Boolean = withContext(Dispatchers.IO) {
        try {
            dao.clearAutoSaveSession()
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear Room auto-save session")
            false
        }
    }
}
