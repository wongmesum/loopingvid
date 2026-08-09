package com.example.feature.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AssetEntity
import com.example.core.database.AssetRepository
import com.example.core.ui.SelectedMediaFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AssetManagerUiState(
    val recentAssets: List<AssetEntity> = emptyList(),
    val favoriteAssets: List<AssetEntity> = emptyList(),
    val selectedTab: Int = 0 // 0 = Terbaru, 1 = Favorit
)

/**
 * Holds no [android.content.Context] — permission checks and file resolution happen
 * in the picker call site before [recordAccess] is invoked, keeping this class
 * plain unit-testable against a fake [AssetRepository].
 */
class AssetManagerViewModel(private val repository: AssetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetManagerUiState())
    val uiState: StateFlow<AssetManagerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.recentAssets.collect { assets ->
                _uiState.value = _uiState.value.copy(recentAssets = assets)
            }
        }
        viewModelScope.launch {
            repository.favoriteAssets.collect { assets ->
                _uiState.value = _uiState.value.copy(favoriteAssets = assets)
            }
        }
    }

    fun setTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun recordAccess(file: SelectedMediaFile, permissionPersisted: Boolean) {
        viewModelScope.launch {
            repository.recordAccess(file, permissionPersisted)
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun togglePinned(id: Long) {
        viewModelScope.launch { repository.togglePinned(id) }
    }

    fun deleteAsset(id: Long) {
        viewModelScope.launch { repository.deleteAsset(id) }
    }
}
