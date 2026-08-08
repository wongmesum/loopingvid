package com.example.core.presets

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val EMPTY_PRESET_JSON = "[]"

private val Context.presetDataStore: DataStore<Preferences> by preferencesDataStore(name = "export_presets")

/**
 * Repository for managing custom export presets.
 * Built-in presets are always available; custom presets are persisted via DataStore.
 *
 * The DataStore is injectable because [preferencesDataStore] caches a single
 * process-wide instance, which tests cannot isolate.
 */
class PresetRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.presetDataStore)

    private val customPresetsKey = stringPreferencesKey("custom_export_presets")

    private val presetListAdapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<ExportPreset>>(
            Types.newParameterizedType(List::class.java, ExportPreset::class.java)
        )

    /**
     * Returns all presets (built-in + custom).
     */
    fun getAllPresets(): Flow<List<ExportPreset>> =
        getCustomPresets().map { ExportPresets.ALL_PRESETS + it }

    /**
     * Returns only custom (user-created) presets.
     */
    fun getCustomPresets(): Flow<List<ExportPreset>> =
        dataStore.data.map { readPresets(it) }

    /**
     * Saves a new custom preset under a freshly generated ID.
     */
    suspend fun saveCustomPreset(preset: ExportPreset) {
        dataStore.edit { preferences ->
            val presets = readPresets(preferences).toMutableList()
            presets.add(
                preset.copy(
                    id = "custom_${System.currentTimeMillis()}",
                    isBuiltIn = false,
                    category = PresetCategory.CUSTOM,
                    createdAtTimestamp = System.currentTimeMillis()
                )
            )
            preferences[customPresetsKey] = presetListAdapter.toJson(presets)
        }
    }

    /**
     * Updates an existing custom preset. Built-in presets are immutable.
     */
    suspend fun updateCustomPreset(preset: ExportPreset) {
        if (preset.isBuiltIn) return

        dataStore.edit { preferences ->
            val presets = readPresets(preferences).toMutableList()
            val index = presets.indexOfFirst { it.id == preset.id }
            if (index != -1) {
                presets[index] = preset
                preferences[customPresetsKey] = presetListAdapter.toJson(presets)
            }
        }
    }

    /**
     * Deletes a custom preset by ID.
     */
    suspend fun deleteCustomPreset(presetId: String) {
        dataStore.edit { preferences ->
            val presets = readPresets(preferences).filterNot { it.id == presetId }
            preferences[customPresetsKey] = presetListAdapter.toJson(presets)
        }
    }

    /**
     * Finds a preset by ID, searching built-in presets before stored custom ones.
     */
    suspend fun findPresetById(id: String): ExportPreset? {
        ExportPresets.findById(id)?.let { return it }
        return readPresets(dataStore.data.first()).firstOrNull { it.id == id }
    }

    /**
     * Exports all custom presets as a JSON string for backup or sharing.
     */
    suspend fun exportCustomPresetsJson(): String =
        dataStore.data.first()[customPresetsKey] ?: EMPTY_PRESET_JSON

    /**
     * Imports custom presets from a JSON string, skipping IDs that already exist.
     *
     * @throws IllegalArgumentException when the JSON cannot be parsed as a preset list.
     */
    suspend fun importCustomPresetsJson(json: String) {
        val imported = parsePresetsOrThrow(json)

        dataStore.edit { preferences ->
            val presets = readPresets(preferences).toMutableList()
            imported.forEach { preset ->
                if (presets.none { it.id == preset.id }) {
                    presets.add(preset.copy(isBuiltIn = false, category = PresetCategory.CUSTOM))
                }
            }
            preferences[customPresetsKey] = presetListAdapter.toJson(presets)
        }
    }

    // Stored presets are read defensively: a corrupt payload degrades to "no custom presets"
    // instead of breaking every screen that observes the preset list.
    private fun readPresets(preferences: Preferences): List<ExportPreset> {
        val json = preferences[customPresetsKey] ?: return emptyList()
        return try {
            presetListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parsePresetsOrThrow(json: String): List<ExportPreset> {
        val presets = try {
            presetListAdapter.fromJson(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid preset JSON format", e)
        }
        return presets ?: throw IllegalArgumentException("Invalid preset JSON format")
    }
}
