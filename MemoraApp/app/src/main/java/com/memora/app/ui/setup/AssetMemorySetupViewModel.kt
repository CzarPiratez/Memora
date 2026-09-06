package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.intelligence.LoadCorpusCompleteness
import com.memora.app.application.memory.AssetMemoryDrainResult
import com.memora.app.application.memory.RunPendingAssetMemoryAssembly
import com.memora.app.domain.memory.CorpusCompletenessCounts
import com.memora.app.ui.search.CorpusHonestyCopy
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
        val pendingAssemblyCount: Int = 0,
        val assembledInLastRun: Int = 0,
        val skippedInLastRun: Int = 0,
        val hasMore: Boolean = false,
    ) : AssetMemorySetupState
    data class Building(val currentReadyCount: Int) : AssetMemorySetupState
    data class Failed(val currentReadyCount: Int) : AssetMemorySetupState
}

@HiltViewModel
class AssetMemorySetupViewModel @Inject constructor(
    private val runPendingAssembly: RunPendingAssetMemoryAssembly,
    private val loadCorpusCompleteness: LoadCorpusCompleteness,
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
                    pendingAssemblyCount = pendingAssemblyCount(),
                    assembledInLastRun = result.assembledCount,
                    skippedInLastRun = result.skippedCount,
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
            mutableUiState.value = runCatching { loadCorpusCompleteness() }
                .fold(
                    onSuccess = { snapshot ->
                        AssetMemorySetupState.Ready(
                            currentReadyCount = snapshot.counts.memoriesReady,
                            pendingAssemblyCount = snapshot.counts.memoriesPendingAssembly,
                        )
                    },
                    onFailure = { AssetMemorySetupState.Failed(0) },
                )
        }
    }

    private suspend fun pendingAssemblyCount(): Int =
        loadCorpusCompleteness().counts.memoriesPendingAssembly

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
            "Everything runs on-device. Find by meaning uses the on-device model and index."
    const val BUILD_LABEL = "Build memories from saved facts"
    const val CONTINUE_LABEL = "Continue building saved fact memories"
    const val BUILDING = "Building memories from saved facts on this phone…"
    const val FAILED = "UNFYND could not finish building saved fact memories. You can try again."

    fun readiness(counts: CorpusCompletenessCounts): String =
        CorpusHonestyCopy.assetMemoryReadiness(counts)

    fun readiness(readyCount: Int, pendingAssemblyCount: Int = 0): String =
        CorpusHonestyCopy.assetMemoryReadiness(
            CorpusCompletenessCounts(
                memoriesReady = readyCount,
                memoriesPendingAssembly = pendingAssemblyCount,
                meaningSummaryIndexed = 0,
                meaningEvidenceIndexed = 0,
                meaningIndexPending = 0,
            ),
        )

    fun completed(
        assembledCount: Int,
        currentReadyCount: Int,
        skippedCount: Int = 0,
    ): String {
        val built = "Built $assembledCount in this step."
        val skipped = if (skippedCount > 0) {
            " Skipped $skippedCount that could not become a memory from the saved facts."
        } else {
            ""
        }
        return "$built$skipped ${readiness(currentReadyCount)} " +
            "Use keyword search for exact words; meaning search needs the on-device model " +
            "and meaning index."
    }
}
