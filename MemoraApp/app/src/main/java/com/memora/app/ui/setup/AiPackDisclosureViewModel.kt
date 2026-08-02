package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.intelligence.ActivateOfflineEmbeddingPackContainer
import com.memora.app.application.intelligence.ActivateOfflineEmbeddingPackResult
import com.memora.app.domain.intelligence.AiPackInstallLedger
import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.AiPackManager
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiPackDisclosureUiState(
    val statusBody: String,
    val showAcknowledge: Boolean,
    val showActivate: Boolean,
    val isBusy: Boolean = false,
    val feedbackMessage: String? = null,
)

@HiltViewModel
class AiPackDisclosureViewModel @Inject constructor(
    private val ledger: AiPackInstallLedger,
    private val aiPackManager: AiPackManager,
    private val activateOfflinePack: ActivateOfflineEmbeddingPackContainer,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(buildStateFromLedger())
    val uiState: StateFlow<AiPackDisclosureUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableUiState.value = buildStateFromLedger()
        }
    }

    fun onAcknowledgeRequested() {
        if (mutableUiState.value.isBusy || !mutableUiState.value.showAcknowledge) return
        mutableUiState.value = mutableUiState.value.copy(
            isBusy = true,
            feedbackMessage = null,
            showAcknowledge = false,
            showActivate = false,
        )
        viewModelScope.launch {
            ledger.acknowledgeDisclosure(
                EmbeddingFirstAiPackTrack.plannedDisclosure(
                    atEpochMs = System.currentTimeMillis(),
                ),
            )
            mutableUiState.value = buildStateFromLedger(
                feedbackMessage = AiPackDisclosureCopy.FEEDBACK_ACKNOWLEDGED,
            )
        }
    }

    fun onActivateRequested() {
        if (mutableUiState.value.isBusy || !mutableUiState.value.showActivate) return
        mutableUiState.value = mutableUiState.value.copy(
            isBusy = true,
            feedbackMessage = null,
            showAcknowledge = false,
            showActivate = false,
        )
        viewModelScope.launch {
            val result = activateOfflinePack(nowEpochMs = System.currentTimeMillis())
            val feedback = when (result) {
                ActivateOfflineEmbeddingPackResult.Activated ->
                    AiPackDisclosureCopy.FEEDBACK_ACTIVATED
                ActivateOfflineEmbeddingPackResult.AlreadyActive ->
                    AiPackDisclosureCopy.FEEDBACK_ALREADY_ACTIVE
                ActivateOfflineEmbeddingPackResult.DisclosureRequired ->
                    AiPackDisclosureCopy.FEEDBACK_DISCLOSURE_REQUIRED
                is ActivateOfflineEmbeddingPackResult.Failed ->
                    result.reason
            }
            mutableUiState.value = buildStateFromLedger(feedbackMessage = feedback)
        }
    }

    fun onDerivedDataCleared() {
        refresh()
    }

    private fun buildStateFromLedger(
        feedbackMessage: String? = null,
    ): AiPackDisclosureUiState {
        val packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID
        val entry = ledger.entry(packId)
        val installationState = aiPackManager.installationState(packId)
        val disclosed = entry?.disclosureAcknowledgedAtEpochMs != null
        val canActivate = disclosed &&
            installationState != AiPackInstallState.ACTIVE &&
            installationState != AiPackInstallState.VERIFYING
        return AiPackDisclosureUiState(
            statusBody = AiPackDisclosureCopy.statusBody(
                installationState = installationState,
                disclosureAcknowledged = disclosed,
            ),
            showAcknowledge = !disclosed &&
                installationState == AiPackInstallState.NOT_INSTALLED,
            showActivate = canActivate,
            isBusy = false,
            feedbackMessage = feedbackMessage,
        )
    }
}
