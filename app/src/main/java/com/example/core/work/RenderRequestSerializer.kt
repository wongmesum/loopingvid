package com.example.core.work

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.core.ffmpeg.SlideshowRenderRequest
import com.example.core.ffmpeg.VisualizerRenderRequest
import com.example.feature.visualizer.VisualizerBackground
import com.example.feature.visualizer.VisualizerMode
import com.example.feature.visualizer.VisualizerRenderConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializes complex render requests into JSON strings for WorkManager Data,
 * because WorkManager limits payload size and depth.
 */
object RenderRequestSerializer {

    // --- Slideshow ---

    fun serializeSlideshow(request: SlideshowRenderRequest): String {
        val root = JSONObject()
        val imagesArray = JSONArray()
        request.imageUris.forEach { imagesArray.put(it) }
        root.put("imageUris", imagesArray)
        root.put("audioUri", request.audioUri ?: JSONObject.NULL)
        root.put("perImageDurationSec", request.perImageDurationSec)
        root.put("transition", request.transition)
        root.put("transitionDurationSec", request.transitionDurationSec)
        root.put("resolution", request.resolution)
        root.put("aspectRatio", request.aspectRatio)
        root.put("frameRate", request.frameRate)
        root.put("outputName", request.outputName)
        root.put("kenBurnsEnabled", request.kenBurnsEnabled)
        root.put("overlayText", request.overlayText)
        return root.toString()
    }

    fun deserializeSlideshow(json: String): SlideshowRenderRequest {
        val root = JSONObject(json)
        val imagesArray = root.getJSONArray("imageUris")
        val imageUris = mutableListOf<String>()
        for (i in 0 until imagesArray.length()) {
            imageUris.add(imagesArray.getString(i))
        }
        return SlideshowRenderRequest(
            imageUris = imageUris,
            audioUri = if (root.isNull("audioUri")) null else root.getString("audioUri"),
            perImageDurationSec = root.getDouble("perImageDurationSec"),
            transition = root.getString("transition"),
            transitionDurationSec = root.getDouble("transitionDurationSec"),
            resolution = root.getString("resolution"),
            aspectRatio = root.getString("aspectRatio"),
            frameRate = root.getString("frameRate"),
            outputName = root.getString("outputName"),
            kenBurnsEnabled = root.optBoolean("kenBurnsEnabled", false),
            overlayText = root.optString("overlayText", "")
        )
    }

    // --- Visualizer ---

    fun serializeVisualizer(request: VisualizerRenderRequest): String {
        val root = JSONObject()
        root.put("audioUri", request.audioUri)

        val markersArray = JSONArray()
        request.beatMarkersMs.forEach { markersArray.put(it) }
        root.put("beatMarkersMs", markersArray)
        root.put("beatEffectExpression", request.beatEffectExpression ?: JSONObject.NULL)
        root.put("durationMs", request.durationMs)
        root.put("outputName", request.outputName)

        val config = request.config
        val configObj = JSONObject()
        configObj.put("mode", config.mode.name)
        configObj.put("bandCount", config.bandCount)
        configObj.put("sizeScale", config.sizeScale.toDouble())
        configObj.put("gapScale", config.gapScale.toDouble())
        configObj.put("thickness", config.thickness.toDouble())
        configObj.put("cornerRadius", config.cornerRadius.toDouble())
        configObj.put("sensitivityGain", config.sensitivityGain.toDouble())
        configObj.put("smoothing", config.smoothing.toDouble())
        configObj.put("offsetX", config.offsetX.toDouble())
        configObj.put("offsetY", config.offsetY.toDouble())
        configObj.put("rotationDegrees", config.rotationDegrees.toDouble())
        configObj.put("primaryColor", config.primaryColor.toArgb())
        configObj.put("secondaryColor", config.secondaryColor.toArgb())
        configObj.put("opacity", config.opacity.toDouble())
        configObj.put("glowRadius", config.glowRadius.toDouble())
        configObj.put("shadowRadius", config.shadowRadius.toDouble())
        configObj.put("trailFade", config.trailFade.toDouble())
        configObj.put("aspectRatio", config.aspectRatio)
        configObj.put("safeAreaOverlay", config.safeAreaOverlay ?: JSONObject.NULL)

        val bgObj = JSONObject()
        when (val bg = config.background) {
            is VisualizerBackground.Transparent -> {
                bgObj.put("type", "transparent")
            }
            is VisualizerBackground.SolidColor -> {
                bgObj.put("type", "solid")
                bgObj.put("color", bg.color.toArgb())
            }
            is VisualizerBackground.Gradient -> {
                bgObj.put("type", "gradient")
                bgObj.put("topColor", bg.topColor.toArgb())
                bgObj.put("bottomColor", bg.bottomColor.toArgb())
            }
            is VisualizerBackground.Image -> {
                bgObj.put("type", "image")
                bgObj.put("uri", bg.uri)
                bgObj.put("blurRadius", bg.blurRadius.toDouble())
            }
        }
        configObj.put("background", bgObj)
        root.put("config", configObj)

        return root.toString()
    }

    fun deserializeVisualizer(json: String): VisualizerRenderRequest {
        val root = JSONObject(json)
        val markersArray = root.getJSONArray("beatMarkersMs")
        val beatMarkersMs = mutableListOf<Long>()
        for (i in 0 until markersArray.length()) {
            beatMarkersMs.add(markersArray.getLong(i))
        }

        val configObj = root.getJSONObject("config")
        val bgObj = configObj.getJSONObject("background")
        val background = when (bgObj.getString("type")) {
            "solid" -> VisualizerBackground.SolidColor(Color(bgObj.getInt("color")))
            "gradient" -> VisualizerBackground.Gradient(
                Color(bgObj.getInt("topColor")),
                Color(bgObj.getInt("bottomColor"))
            )
            "image" -> VisualizerBackground.Image(
                bgObj.getString("uri"),
                bgObj.getDouble("blurRadius").toFloat()
            )
            else -> VisualizerBackground.Transparent
        }

        val config = VisualizerRenderConfig(
            mode = VisualizerMode.valueOf(configObj.getString("mode")),
            bandCount = configObj.getInt("bandCount"),
            sizeScale = configObj.getDouble("sizeScale").toFloat(),
            gapScale = configObj.getDouble("gapScale").toFloat(),
            thickness = configObj.getDouble("thickness").toFloat(),
            cornerRadius = configObj.getDouble("cornerRadius").toFloat(),
            sensitivityGain = configObj.getDouble("sensitivityGain").toFloat(),
            smoothing = configObj.getDouble("smoothing").toFloat(),
            offsetX = configObj.getDouble("offsetX").toFloat(),
            offsetY = configObj.getDouble("offsetY").toFloat(),
            rotationDegrees = configObj.getDouble("rotationDegrees").toFloat(),
            primaryColor = Color(configObj.getInt("primaryColor")),
            secondaryColor = Color(configObj.getInt("secondaryColor")),
            opacity = configObj.getDouble("opacity").toFloat(),
            glowRadius = configObj.getDouble("glowRadius").toFloat(),
            shadowRadius = configObj.getDouble("shadowRadius").toFloat(),
            trailFade = configObj.getDouble("trailFade").toFloat(),
            aspectRatio = configObj.getString("aspectRatio"),
            safeAreaOverlay = if (configObj.isNull("safeAreaOverlay")) null else configObj.getString("safeAreaOverlay"),
            background = background
        )

        return VisualizerRenderRequest(
            audioUri = root.getString("audioUri"),
            config = config,
            beatMarkersMs = beatMarkersMs,
            beatEffectExpression = if (root.isNull("beatEffectExpression")) null else root.getString("beatEffectExpression"),
            durationMs = root.getLong("durationMs"),
            outputName = root.getString("outputName")
        )
    }
}
