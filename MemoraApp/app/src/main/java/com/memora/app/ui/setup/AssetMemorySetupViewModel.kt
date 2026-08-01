package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.memory.AssetMemoryDrainResult
import com.memora.app.application.memory.RunPendingAssetMemoryAssembly
import com.memora.app.domain.memory.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AssetMemorySetupState {
    data object Loading : AssetMemorySetupState
    data class Ready(
        val currentReadyCount: Int,
        val assembledInLastRun: Int = 0,
        val hasMore: Boolean = false,
    ) : AssetMemorySetupState
    data class Building(val currentReadyCount: Int) : AssetMemorySetupState
    data class Failed(val currentReadyCount: Int) : AssetMemorySetupState
}

@HiltViewModel
class AssetMemorySetupViewModel @Inject constructor(
    private val runPendingAssembly: RunPendingAssetMemoryAssembly,
    private val memoryRepository: MemoryRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<AssetMemorySetupState>(
        AssetMemorySetupState.Loading,
    )
    val uiState: StateFlow<AssetMemorySetupState> = mutableUiState.asStateFlow()

    init {
        refreshCount()
    }

    fun onBuildRequested() {
        val current = mutableUiState.value
        if (current is AssetMemorySetupState.Building) return
        val count = countFrom(current)
        mutableUiState.value = AssetMemorySetupState.Building(count)
        viewModelScope.launch {
            mutableUiState.value = when (val result = runCatching {
                runPendingAssembly()
            }.getOrNull()) {
                is AssetMemoryDrainResult.Completed -> AssetMemorySetupState.Ready(
                    currentReadyCount = result.currentReadyCount,
                    assembledInLastRun = result.assembledCount,
                    hasMore = result.hasMore,
                )
                is AssetMemoryDrainResult.FailedSafely -> AssetMemorySetupState.Failed(
                    result.currentReadyCount,
                )
                null -> AssetMemorySetupState.Failed(count)
            }
        }
    }

    fun onDerivedDataCleared() {
        mutableUiState.value = AssetMemorySetupState.Ready(currentReadyCount = 0)
    }

    private fun refreshCount() {
        viewModelScope.launch {
            mutableUiState.value = runCatching { memoryRepository.countCurrentReady() }
                .fold(
                    onSuccess = { AssetMemorySetupState.Ready(it) },
                    onFailure = { AssetMemorySetupState.Failed(0) },
                )
        }
    }

    private fun countFrom(state: AssetMemorySetupState): Int = when (state) {
        AssetMemorySetupState.Loading -> 0
        is AssetMemorySetupState.Ready -> state.currentReadyCount
        is AssetMemorySetupState.Building -> state.currentReadyCount
        is AssetMemorySetupState.Failed -> state.currentReadyCount
    }
}

object AssetMemorySetupCopy {
    const val TITLE = "Saved fact memories"
    const val BODY =
        "Build evidence-backed memories from saved PDF text, image OCR, " +
            "useful photo facts, and saved OneNote page text. " +
            "This runs on-device without an AI pack. It does not provide meaning-based ranking yet."
    const val BUILD_LABEL = "Build memories from saved facts"
    const val CONTINUE_LABEL = "Continue building saved fact memories"
    const val BUILDING = "Building memories from saved facts on this phone…"
    const val FAILED = "Memora could not finish building saved fact memories. You can try again."

    fun readiness(count: Int): String =
        if (count == 1) "1 current evidence-backed Asset Memory is saved."
        else "$count current evidence-backed Asset Memories are saved."

    fun completed(assembledCount: Int, currentReadyCount: Int): String =
        "Built $assembledCount in this step. ${readiness(currentReadyCount)} " +
            "PDF, image, and note keyword search remain the interim recall path."
}
