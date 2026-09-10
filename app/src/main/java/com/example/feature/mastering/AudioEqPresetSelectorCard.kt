package com.example.feature.mastering

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core.media.EqBandConfig
import com.example.core.media.MasteringPreset

/**
 * Preset selector card used from [MasteringScreen]. The current EQ / gain / LUFS state is captured
 * by the ViewModel when [onSavePreset] is invoked (it snapshots the live UI state), so those values
 * are accepted here for API completeness but the actual save is delegated to the ViewModel.
 *
 * This is a thin adapter over [AudioMasteringPresetSelector], which owns the full preset UI.
 */
@Composable
fun AudioEqPresetSelectorCard(
    selectedPreset: MasteringPreset,
    customPresets: List<MasteringPreset>,
    eqConfig: EqBandConfig,
    inputGainDb: Float,
    outputGainDb: Float,
    targetLufs: Double,
    onPresetSelected: (MasteringPreset) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (MasteringPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    AudioMasteringPresetSelector(
        selectedPreset = selectedPreset,
        onPresetSelected = onPresetSelected,
        modifier = modifier,
        customPresets = customPresets,
        onSaveCustomPreset = onSavePreset,
        onDeleteCustomPreset = onDeletePreset
    )
}
