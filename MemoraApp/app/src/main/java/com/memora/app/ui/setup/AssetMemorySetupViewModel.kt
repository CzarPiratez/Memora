package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.intelligence.LoadCorpusCompleteness
import com.memora.app.domain.memory.CorpusCompletenessCounts
import com.memora.app.domain.memory.CorpusCompletenessSnapshot
import com.memora.app.ui.search.CorpusHonestyCopy
import com.memora.app.work.AssetMemoryAssemblyWorkObservation
import com.memora.app.work.AssetMemoryAssemblyWorkPhase
import com.memora.app.work.AssetMemoryAssemblyWorkScheduler
import com.memora.app.work.AssetMemoryAssemblyWorkTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
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
    data class Building(
        val currentReadyCount: Int,
        val assembledSoFar: Int = 0,
        val skippedSoFar: Int = 0,
        val pendingAtStart: Int = 0,
        val progressFeedback: String = AssetMemorySetupCopy.BUILDING,
    ) : AssetMemorySetupState
    data class Failed(val currentReadyCount: Int) : AssetMemorySetupState
}

@HiltViewModel
class AssetMemorySetupViewModel(
    private val scheduler: AssetMemoryAssemblyWorkScheduler,
    private val loadSnapshot: suspend () -> CorpusCompletenessSnapshot,
) : ViewModel() {
    @Inject
    constructor(
        scheduler: AssetMemoryAssemblyWorkScheduler,
        loadCorpusCompleteness: LoadCorpusCompleteness,
    ) : this(scheduler, loadCorpusCompleteness::invoke)

    private val mutableUiState = MutableStateFlow<AssetMemorySetupState>(
        AssetMemorySetupState.Loading,
    )
    val uiState: StateFlow<AssetMemorySetupState> = mutableUiState.asStateFlow()

    private var observationJob: Job? = null
    private var drainRequested: Boolean = false
    private var pendingAtStart: Int = 0

    init {
        observeWork()
        refreshCount()
    }

    fun onBuildRequested() {
        if (mutableUiState.value is AssetMemorySetupState.Building) return
        viewModelScope.launch {
            val snapshot = runCatching { loadSnapshot() }.getOrNull()
            val ready = snapshot?.counts?.memoriesReady ?: countFrom(mutableUiState.value)
            pendingAtStart = snapshot?.counts?.memoriesPendingAssembly ?: 0
            drainRequested = true
            mutableUiState.value = buildingState(
                readyCount = ready,
                totals = AssetMemoryAssemblyWorkTotals(readyCount = ready),
            )
            scheduler.enqueueDrain()
        }
    }

    fun onStop() {
        if (mutableUiState.value !is AssetMemorySetupState.Building) return
        scheduler.cancel()
    }

    fun onDerivedDataCleared() {
        drainRequested = false
        pendingAtStart = 0
        scheduler.cancel()
        mutableUiState.value = AssetMemorySetupState.Ready(currentReadyCount = 0)
    }

    private fun observeWork() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            scheduler.observeUniqueWork().collect { infos ->
                applyWorkPhase(AssetMemoryAssemblyWorkObservation.phase(infos))
            }
        }
    }

    private suspend fun applyWorkPhase(phase: AssetMemoryAssemblyWorkPhase) {
        when (phase) {
            AssetMemoryAssemblyWorkPhase.Idle -> Unit
            is AssetMemoryAssemblyWorkPhase.Active -> {
                if (!drainRequested) {
                    drainRequested = true
                    if (pendingAtStart == 0) {
                        val pendingNow = runCatching {
                            loadSnapshot().counts.memoriesPendingAssembly
                        }.getOrDefault(0)
                        pendingAtStart = pendingNow + phase.totals.assembled
                    }
                }
                mutableUiState.value = buildingState(
                    readyCount = phase.totals.readyCount.takeIf { it > 0 }
                        ?: countFrom(mutableUiState.value),
                    totals = phase.totals,
                )
            }
            is AssetMemoryAssemblyWorkPhase.Completed -> {
                if (!isWatchingDrain()) return
                finishReady(phase.totals)
            }
            is AssetMemoryAssemblyWorkPhase.Cancelled -> {
                if (!isWatchingDrain()) return
                finishReady(phase.totals, stopped = true)
            }
            AssetMemoryAssemblyWorkPhase.Failed -> {
                if (!isWatchingDrain()) return
                drainRequested = false
                mutableUiState.value = AssetMemorySetupState.Failed(
                    countFrom(mutableUiState.value),
                )
            }
        }
    }

    private fun isWatchingDrain(): Boolean =
        drainRequested || mutableUiState.value is AssetMemorySetupState.Building

    private suspend fun finishReady(
        totals: AssetMemoryAssemblyWorkTotals,
        stopped: Boolean = false,
    ) {
        drainRequested = false
        val snapshot = runCatching { loadSnapshot() }.getOrNull()
        val pending = snapshot?.counts?.memoriesPendingAssembly ?: 0
        val ready = when {
            totals.readyCount > 0 -> totals.readyCount
            snapshot != null -> snapshot.counts.memoriesReady
            else -> countFrom(mutableUiState.value)
        }
        mutableUiState.value = AssetMemorySetupState.Ready(
            currentReadyCount = ready,
            pendingAssemblyCount = pending,
            assembledInLastRun = totals.assembled,
            skippedInLastRun = totals.skipped,
            hasMore = if (stopped) pending > 0 else totals.hasMore || pending > 0,
        )
        pendingAtStart = 0
    }

    private fun refreshCount() {
        viewModelScope.launch {
            val snapshot = runCatching { loadSnapshot() }
            if (drainRequested || mutableUiState.value is AssetMemorySetupState.Building) {
                return@launch
            }
            mutableUiState.value = snapshot.fold(
                onSuccess = { loaded ->
                    AssetMemorySetupState.Ready(
                        currentReadyCount = loaded.counts.memoriesReady,
                        pendingAssemblyCount = loaded.counts.memoriesPendingAssembly,
                    )
                },
                onFailure = { AssetMemorySetupState.Failed(0) },
            )
        }
    }

    private fun buildingState(
        readyCount: Int,
        totals: AssetMemoryAssemblyWorkTotals,
    ) = AssetMemorySetupState.Building(
        currentReadyCount = readyCount,
        assembledSoFar = totals.assembled,
        skippedSoFar = totals.skipped,
        pendingAtStart = pendingAtStart,
        progressFeedback = AssetMemorySetupCopy.progressFeedback(
            assembledSoFar = totals.assembled,
            skippedSoFar = totals.skipped,
            pendingAtStart = pendingAtStart,
        ),
    )

    private fun countFrom(state: AssetMemorySetupState): Int = when (state) {
        AssetMemorySetupState.Loading -> 0
        is AssetMemorySetupState.Ready -> state.currentReadyCount
        is AssetMemorySetupState.Building -> state.currentReadyCount
        is AssetMemorySetupState.Failed -> state.currentReadyCount
    }

    override fun onCleared() {
        observationJob?.cancel()
        super.onCleared()
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
    const val STOP_LABEL = "Stop"
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

    fun progressFeedback(
        assembledSoFar: Int,
        skippedSoFar: Int = 0,
        pendingAtStart: Int = 0,
    ): String {
        val assembled = assembledSoFar.coerceAtLeast(0)
        val skipped = skippedSoFar.coerceAtLeast(0)
        val pending = pendingAtStart.coerceAtLeast(0)
        val skippedNote = if (skipped > 0) {
            " Skipped $skipped that could not become a memory from the saved facts."
        } else {
            ""
        }
        return when {
            pending <= 0 && assembled <= 0 ->
                "Looking for saved facts that still need a memory…"
            pending > 0 && assembled <= 0 ->
                "Starting to build memories from up to $pending saved facts…"
            pending > 0 ->
                "Built $assembled of about $pending memories.$skippedNote"
            else ->
                "Built $assembled in this pass.$skippedNote"
        }
    }

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
