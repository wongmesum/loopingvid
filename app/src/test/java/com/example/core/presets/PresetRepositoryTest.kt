package com.example.core.presets

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PresetRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `catalog covers required creator aspect ratios`() {
        val aspectRatios = ExportPresets.getVideoPresets().map { it.aspectRatio }.toSet()

        assertTrue("Missing vertical preset", "9:16" in aspectRatios)
        assertTrue("Missing landscape preset", "16:9" in aspectRatios)
        assertTrue("Missing square preset", "1:1" in aspectRatios)
        assertTrue("Missing portrait feed preset", "4:5" in aspectRatios)
    }

    @Test
    fun `saved preset can be found and exported as JSON`() = runTest {
        val repository = newRepository()
        repository.saveCustomPreset(ExportPresets.TIKTOK.copy(name = "Client Vertical"))

        val savedPreset = repository.getCustomPresets().first().single()
        val foundPreset = repository.findPresetById(savedPreset.id)
        val exportedJson = repository.exportCustomPresetsJson()

        assertNotNull(foundPreset)
        assertEquals("Client Vertical", foundPreset?.name)
        assertTrue(exportedJson.startsWith("["))
        assertTrue(exportedJson.contains("Client Vertical"))
        assertFalse(exportedJson.contains("SafeFlow"))
    }

    @Test
    fun `import marks presets custom and ignores duplicate ids`() = runTest {
        val repository = newRepository()
        val json = """
            [{
              "id":"shared_vertical",
              "name":"Shared Vertical",
              "description":"Shared creator preset",
              "category":"PLATFORM",
              "resolution":"1080p",
              "frameRate":"30fps",
              "bitrate":"High",
              "aspectRatio":"9:16",
              "format":"mp4",
              "isBuiltIn":true,
              "createdAtTimestamp":1
            }]
        """.trimIndent()

        repository.importCustomPresetsJson(json)
        repository.importCustomPresetsJson(json)

        val presets = repository.getCustomPresets().first()
        assertEquals(1, presets.size)
        assertEquals(PresetCategory.CUSTOM, presets.single().category)
        assertFalse(presets.single().isBuiltIn)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid import is rejected`() = runTest {
        newRepository().importCustomPresetsJson("not-json")
    }

    private fun newRepository(): PresetRepository {
        val file = context.preferencesDataStoreFile("test_export_presets_${System.nanoTime()}")
        val dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { file }
        )
        return PresetRepository(dataStore)
    }
}
