package com.memora.app.ui.setup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.BuildConfig
import com.memora.app.application.intelligence.ActivateOfflineEmbeddingPackContainer
import com.memora.app.application.intelligence.ActivateOfflineEmbeddingPackResult
import com.memora.app.application.intelligence.DownloadOnDeviceEmbeddingModel
import com.memora.app.application.intelligence.DownloadOnDeviceEmbeddingModelResult
import com.memora.app.application.intelligence.LoadCorpusCompleteness
import com.memora.app.application.intelligence.MeaningEncoderProbeReport
import com.memora.app.application.intelligence.ProbeMeaningEncoderRanks
import com.memora.app.data.intelligence.MediaPipeEmbeddingEngine
import com.memora.app.domain.intelligence.AiPackInstallLedger
import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.AiPackManager
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelStore
import com.memora.app.domain.memory.CorpusCompletenessSnapshot
import com.memora.app.ui.search.CorpusHonestyCopy
import com.memora.app.work.MeaningIndexWorkObservation
import com.memora.app.work.MeaningIndexWorkPhase
import com.memora.app.work.MeaningIndexWorkScheduler
import com.memora.app.work.MeaningIndexWorkTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AiPackDisclosureUiState(
    val statusBody: String,
    val corpusCompletenessBody: String? = null,
    val showAcknowledge: Boolean,
    val showActivate: Boolean,
    val showDownloadModel: Boolean,
    val showBuildIndex: Boolean,
    val showStopIndex: Boolean = false,
    val showEncoderProbe: Boolean = false,
    val isBusy: Boolean = false,
    val isIndexing: Boolean = false,
    val progressFeedback: String? = null,
    val feedbackMessage: String? = null,
) {
    val blockOtherActions: Boolean get() = isBusy || isIndexing
}

@HiltViewModel
class AiPackDisclosureViewModel @Inject constructor(
    private val ledger: AiPackInstallLedger,
    private val aiPackManager: AiPackManager,
    private val activateOfflinePack: ActivateOfflineEmbeddingPackContainer,
    private val downloadModel: DownloadOnDeviceEmbeddingModel,
    private val modelStore: OnDeviceEmbeddingModelStore,
    private val embeddingEngine: EmbeddingEngine,
    private val mediaPipeEmbeddingEngine: MediaPipeEmbeddingEngine,
    private val loadCorpusCompleteness: LoadCorpusCompleteness,
    private val meaningIndexScheduler: MeaningIndexWorkScheduler,
    private val probeMeaningEncoderRanks: ProbeMeaningEncoderRanks,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(buildState())
    val uiState: StateFlow<AiPackDisclosureUiState> = mutableUiState.asStateFlow()

    private var observationJob: Job? = null
    private var drainRequested: Boolean = false

    init {
        observeWork()
        refresh()
    }

    fun refresh() {
        if (drainRequested || mutableUiState.value.isIndexing) return
        viewModelScope.launch {
            mutableUiState.value = withContext(Dispatchers.IO) { buildStateWithCorpus() }
        }
    }

    fun onAcknowledgeRequested() {
        if (mutableUiState.value.blockOtherActions || !mutableUiState.value.showAcknowledge) return
        setBusy()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ledger.acknowledgeDisclosure(
                    EmbeddingFirstAiPackTrack.plannedDisclosure(
                        atEpochMs = System.currentTimeMillis(),
                    ),
                )
            }
            mutableUiState.value = withContext(Dispatchers.IO) {
                buildStateWithCorpus(feedbackMessage = AiPackDisclosureCopy.FEEDBACK_ACKNOWLEDGED)
            }
        }
    }

    fun onActivateRequested() {
        if (mutableUiState.value.blockOtherActions || !mutableUiState.value.showActivate) return
        setBusy()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                activateOfflinePack(nowEpochMs = System.currentTimeMillis())
            }
            val feedback = when (result) {
                ActivateOfflineEmbeddingPackResult.Activated ->
                    AiPackDisclosureCopy.FEEDBACK_ACTIVATED
                ActivateOfflineEmbeddingPackResult.AlreadyActive ->
                    AiPackDisclosureCopy.FEEDBACK_ALREADY_ACTIVE
                ActivateOfflineEmbeddingPackResult.DisclosureRequired ->
                    AiPackDisclosureCopy.FEEDBACK_DISCLOSURE_REQUIRED
                is ActivateOfflineEmbeddingPackResult.Failed -> result.reason
            }
            mutableUiState.value = withContext(Dispatchers.IO) {
                buildStateWithCorpus(feedbackMessage = feedback)
            }
        }
    }

    fun onDownloadModelRequested() {
        if (mutableUiState.value.blockOtherActions || !mutableUiState.value.showDownloadModel) {
            return
        }
        setBusy()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { downloadModel() }
            withContext(Dispatchers.IO) { mediaPipeEmbeddingEngine.reset() }
            val feedback = when (result) {
                DownloadOnDeviceEmbeddingModelResult.Installed ->
                    AiPackDisclosureCopy.FEEDBACK_MODEL_INSTALLED
                DownloadOnDeviceEmbeddingModelResult.AlreadyInstalled ->
                    AiPackDisclosureCopy.FEEDBACK_MODEL_ALREADY
                DownloadOnDeviceEmbeddingModelResult.DisclosureRequired ->
                    AiPackDisclosureCopy.FEEDBACK_DISCLOSURE_REQUIRED
                is DownloadOnDeviceEmbeddingModelResult.Failed -> result.reason
            }
            mutableUiState.value = withContext(Dispatchers.IO) {
                buildStateWithCorpus(feedbackMessage = feedback)
            }
        }
    }

    fun onBuildIndexRequested() {
        if (mutableUiState.value.isIndexing || !mutableUiState.value.showBuildIndex) return
        drainRequested = true
        mutableUiState.value = mutableUiState.value.copy(
            isIndexing = true,
            showStopIndex = true,
            showBuildIndex = false,
            showAcknowledge = false,
            showActivate = false,
            showDownloadModel = false,
            progressFeedback = AiPackDisclosureCopy.PROGRESS_PREPARING,
            feedbackMessage = null,
        )
        meaningIndexScheduler.enqueueDrain()
    }

    fun onStopIndexRequested() {
        if (!mutableUiState.value.isIndexing) return
        meaningIndexScheduler.cancel()
    }

    fun onEncoderProbeRequested() {
        if (mutableUiState.value.blockOtherActions || !mutableUiState.value.showEncoderProbe) {
            return
        }
        setBusy(AiPackDisclosureCopy.FEEDBACK_PROBE_RUNNING)
        viewModelScope.launch {
            val report = try {
                withContext(Dispatchers.IO) { probeMeaningEncoderRanks() }
            } catch (_: Exception) {
                mutableUiState.value = withContext(Dispatchers.IO) {
                    buildStateWithCorpus(
                        feedbackMessage = AiPackDisclosureCopy.FEEDBACK_PROBE_FAILED,
                    )
                }
                return@launch
            }
            if (report is MeaningEncoderProbeReport.Completed) {
                report.rows.forEach { row ->
                    Log.i(ProbeMeaningEncoderRanks.LOG_TAG, row.logLine())
                }
            }
            mutableUiState.value = withContext(Dispatchers.IO) {
                buildStateWithCorpus(feedbackMessage = AiPackDisclosureCopy.encoderProbeFinished(report))
            }
        }
    }

    fun onDerivedDataCleared() {
        drainRequested = false
        meaningIndexScheduler.cancel()
        mediaPipeEmbeddingEngine.reset()
        viewModelScope.launch {
            mutableUiState.value = withContext(Dispatchers.IO) { buildStateWithCorpus() }
        }
    }

    private fun observeWork() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            meaningIndexScheduler.observeUniqueWork().collect { infos ->
                applyWorkPhase(MeaningIndexWorkObservation.phase(infos))
            }
        }
    }

    private suspend fun applyWorkPhase(phase: MeaningIndexWorkPhase) {
        when (phase) {
            MeaningIndexWorkPhase.Idle -> Unit
            is MeaningIndexWorkPhase.Active -> {
                drainRequested = true
                mutableUiState.value = mutableUiState.value.copy(
                    isIndexing = true,
                    showStopIndex = true,
                    showBuildIndex = false,
                    showAcknowledge = false,
                    showActivate = false,
                    showDownloadModel = false,
                    isBusy = false,
                    progressFeedback = AiPackDisclosureCopy.indexingProgress(
                        indexedSoFar = phase.totals.indexedMemories,
                        remainingPending = phase.totals.remainingPending,
                    ),
                    feedbackMessage = null,
                )
            }
            is MeaningIndexWorkPhase.Completed -> {
                if (!isWatchingDrain()) return
                finishIndexing(phase.totals, stopped = false)
            }
            is MeaningIndexWorkPhase.Cancelled -> {
                if (!isWatchingDrain()) return
                finishIndexing(phase.totals, stopped = true)
            }
            MeaningIndexWorkPhase.Failed -> {
                if (!isWatchingDrain()) return
                drainRequested = false
                mutableUiState.value = withContext(Dispatchers.IO) {
                    buildStateWithCorpus(feedbackMessage = AiPackDisclosureCopy.FEEDBACK_INDEX_FAILED)
                }
            }
        }
    }

    private fun isWatchingDrain(): Boolean =
        drainRequested || mutableUiState.value.isIndexing

    private suspend fun finishIndexing(totals: MeaningIndexWorkTotals, stopped: Boolean) {
        drainRequested = false
        val feedback = when {
            totals.engineUnavailable -> AiPackDisclosureCopy.FEEDBACK_INDEX_UNAVAILABLE
            totals.disagreedPending > 0 ->
                AiPackDisclosureCopy.indexSelectionDisagreed(totals.disagreedPending)
            stopped -> AiPackDisclosureCopy.indexStopped(totals.remainingPending)
            totals.indexedMemories == 0 && totals.remainingPending == 0 ->
                AiPackDisclosureCopy.FEEDBACK_INDEX_EMPTY
            else -> AiPackDisclosureCopy.indexDrainComplete(
                indexedMemories = totals.indexedMemories,
                remainingPending = totals.remainingPending,
            )
        }
        mutableUiState.value = withContext(Dispatchers.IO) {
            buildStateWithCorpus(feedbackMessage = feedback)
        }
    }

    private fun setBusy(progressFeedback: String? = null) {
        mutableUiState.value = mutableUiState.value.copy(
            isBusy = true,
            progressFeedback = progressFeedback,
            feedbackMessage = null,
            showAcknowledge = false,
            showActivate = false,
            showDownloadModel = false,
            showBuildIndex = false,
            showStopIndex = false,
        )
    }

    private suspend fun buildStateWithCorpus(
        feedbackMessage: String? = null,
    ): AiPackDisclosureUiState {
        val embeddingAvailable =
            embeddingEngine.availability() is CapabilityAvailability.Available
        val snapshot = if (embeddingAvailable) loadCorpusCompleteness() else null
        return buildState(feedbackMessage = feedbackMessage, corpusSnapshot = snapshot)
    }

    private fun buildState(
        feedbackMessage: String? = null,
        corpusSnapshot: CorpusCompletenessSnapshot? = null,
    ): AiPackDisclosureUiState {
        val packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID
        val entry = ledger.entry(packId)
        val installationState = aiPackManager.installationState(packId)
        val disclosed = entry?.disclosureAcknowledgedAtEpochMs != null
        val modelInstalled = modelStore.isInstalled()
        val embeddingAvailable =
            embeddingEngine.availability() is CapabilityAvailability.Available
        val canActivate = disclosed &&
            installationState != AiPackInstallState.ACTIVE &&
            installationState != AiPackInstallState.VERIFYING
        return AiPackDisclosureUiState(
            statusBody = AiPackDisclosureCopy.statusBody(
                installationState = installationState,
                disclosureAcknowledged = disclosed,
                modelInstalled = modelInstalled,
                embeddingAvailable = embeddingAvailable,
            ),
            showAcknowledge = !disclosed &&
                installationState == AiPackInstallState.NOT_INSTALLED,
            showActivate = canActivate,
            showDownloadModel = disclosed && !modelInstalled,
            showBuildIndex = embeddingAvailable,
            showStopIndex = false,
            showEncoderProbe = BuildConfig.DEBUG && embeddingAvailable,
            corpusCompletenessBody = corpusSnapshot?.let { CorpusHonestyCopy.aiPackCorpusLine(it) },
            isBusy = false,
            isIndexing = false,
            progressFeedback = null,
            feedbackMessage = feedbackMessage,
        )
    }

    override fun onCleared() {
        observationJob?.cancel()
        super.onCleared()
    }
}
