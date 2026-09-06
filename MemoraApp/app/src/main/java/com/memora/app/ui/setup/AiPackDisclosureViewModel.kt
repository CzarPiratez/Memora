package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.intelligence.ActivateOfflineEmbeddingPackContainer
import com.memora.app.application.intelligence.ActivateOfflineEmbeddingPackResult
import com.memora.app.application.intelligence.DownloadOnDeviceEmbeddingModel
import com.memora.app.application.intelligence.DownloadOnDeviceEmbeddingModelResult
import com.memora.app.application.intelligence.LoadCorpusCompleteness
import com.memora.app.application.intelligence.RunPendingMeaningIndex
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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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
    val isBusy: Boolean = false,
    val progressFeedback: String? = null,
    val feedbackMessage: String? = null,
)

@HiltViewModel
class AiPackDisclosureViewModel @Inject constructor(
    private val ledger: AiPackInstallLedger,
    private val aiPackManager: AiPackManager,
    private val activateOfflinePack: ActivateOfflineEmbeddingPackContainer,
    private val downloadModel: DownloadOnDeviceEmbeddingModel,
    private val modelStore: OnDeviceEmbeddingModelStore,
    private val embeddingEngine: EmbeddingEngine,
    private val mediaPipeEmbeddingEngine: MediaPipeEmbeddingEngine,
    private val runPendingMeaningIndex: RunPendingMeaningIndex,
    private val loadCorpusCompleteness: LoadCorpusCompleteness,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(buildState())
    val uiState: StateFlow<AiPackDisclosureUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableUiState.value = withContext(Dispatchers.IO) { buildStateWithCorpus() }
        }
    }

    fun onAcknowledgeRequested() {
        if (mutableUiState.value.isBusy || !mutableUiState.value.showAcknowledge) return
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
        if (mutableUiState.value.isBusy || !mutableUiState.value.showActivate) return
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
        if (mutableUiState.value.isBusy || !mutableUiState.value.showDownloadModel) return
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
        if (mutableUiState.value.isBusy || !mutableUiState.value.showBuildIndex) return
        setBusy(progressFeedback = AiPackDisclosureCopy.PROGRESS_PREPARING)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runPendingMeaningIndex(
                    nowEpochMs = System.currentTimeMillis(),
                    onProgress = { progress ->
                        mutableUiState.value = mutableUiState.value.copy(
                            progressFeedback = AiPackDisclosureCopy.progressFor(progress),
                        )
                    },
                )
            }
            mutableUiState.value = withContext(Dispatchers.IO) {
                buildStateWithCorpus(
                    feedbackMessage = AiPackDisclosureCopy.indexDrainFeedback(result),
                )
            }
        }
    }

    fun onDerivedDataCleared() {
        mediaPipeEmbeddingEngine.reset()
        refresh()
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
            corpusCompletenessBody = corpusSnapshot?.let { CorpusHonestyCopy.aiPackCorpusLine(it) },
            isBusy = false,
            progressFeedback = null,
            feedbackMessage = feedbackMessage,
        )
    }
}
