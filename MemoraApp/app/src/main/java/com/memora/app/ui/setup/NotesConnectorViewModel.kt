package com.memora.app.ui.setup

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.notes.OneNoteAuthOutcome
import com.memora.app.application.notes.OneNoteDiscoveryOutcome
import com.memora.app.application.notes.OneNoteInteractiveAuth
import com.memora.app.application.notes.OneNotePagesIndexer
import com.memora.app.data.notes.OneNotePagesDiscoverySource
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.notes.OneNoteAuthConfiguration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NotesConnectorUiState(
    val registrationConfigured: Boolean,
    val connectedAccountLabel: String?,
    val notePlaceholderCount: Int,
    val statusBody: String,
    val showConnect: Boolean,
    val showDisconnect: Boolean,
    val showDiscover: Boolean,
    val discoverLabel: String,
    val hasMoreToDiscover: Boolean,
    val isBusy: Boolean = false,
    val feedbackMessage: String? = null,
)

@HiltViewModel
class NotesConnectorViewModel @Inject constructor(
    private val authConfiguration: OneNoteAuthConfiguration,
    private val oneNoteAuth: OneNoteInteractiveAuth,
    private val oneNotePagesIndexer: OneNotePagesIndexer,
    private val assetRepository: AssetRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        buildState(connectedAccountLabel = null, notePlaceholderCount = 0),
    )
    val uiState: StateFlow<NotesConnectorUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val label = oneNoteAuth.restoreAccountLabel()
            val count = noteCount()
            mutableUiState.value = buildState(label, count)
        }
    }

    fun onConnectRequested(activity: Activity) {
        if (mutableUiState.value.isBusy) return
        if (!authConfiguration.isRegistrationConfigured) {
            mutableUiState.value = buildState(
                connectedAccountLabel = null,
                notePlaceholderCount = mutableUiState.value.notePlaceholderCount,
                feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_REGISTRATION_REQUIRED,
            )
            return
        }
        mutableUiState.value = mutableUiState.value.copy(
            isBusy = true,
            feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_CONNECTING,
            showConnect = false,
        )
        viewModelScope.launch {
            when (val outcome = oneNoteAuth.connect(activity)) {
                is OneNoteAuthOutcome.Connected -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = outcome.session.accountDisplayLabel,
                        notePlaceholderCount = noteCount(),
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_CONNECTED,
                    )
                }
                OneNoteAuthOutcome.Cancelled -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = null,
                        notePlaceholderCount = noteCount(),
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_CANCELLED,
                    )
                }
                OneNoteAuthOutcome.RegistrationRequired -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = null,
                        notePlaceholderCount = noteCount(),
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_REGISTRATION_REQUIRED,
                    )
                }
                is OneNoteAuthOutcome.Failed -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = null,
                        notePlaceholderCount = noteCount(),
                        feedbackMessage = outcome.userMessage,
                    )
                }
            }
        }
    }

    fun onDisconnectRequested() {
        if (mutableUiState.value.isBusy) return
        mutableUiState.value = mutableUiState.value.copy(
            isBusy = true,
            feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_DISCONNECTING,
            showDisconnect = false,
            showDiscover = false,
        )
        viewModelScope.launch {
            oneNoteAuth.disconnect()
            mutableUiState.value = buildState(
                connectedAccountLabel = null,
                notePlaceholderCount = noteCount(),
                feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_DISCONNECTED,
            )
        }
    }

    fun onDiscoverRequested() {
        if (mutableUiState.value.isBusy) return
        if (mutableUiState.value.connectedAccountLabel == null) {
            mutableUiState.value = mutableUiState.value.copy(
                feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_ACCESS_REQUIRED,
            )
            return
        }
        mutableUiState.value = mutableUiState.value.copy(
            isBusy = true,
            feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_DISCOVERING,
            showDiscover = false,
        )
        viewModelScope.launch {
            val label = mutableUiState.value.connectedAccountLabel
            when (val outcome = oneNotePagesIndexer()) {
                is OneNoteDiscoveryOutcome.Discovered -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = label,
                        notePlaceholderCount = outcome.totalNoteAssets,
                        hasMoreToDiscover = outcome.hasMore,
                        feedbackMessage = NotesConnectorHonestyCopy.discoveredFeedback(
                            pageCount = outcome.pageAssetCount,
                            totalCount = outcome.totalNoteAssets,
                            hasMore = outcome.hasMore,
                        ),
                    )
                }
                OneNoteDiscoveryOutcome.AccessRequired -> {
                    // Vault has no usable token; Connect again (do not pretend Connected).
                    mutableUiState.value = buildState(
                        connectedAccountLabel = oneNoteAuth.restoreAccountLabel(),
                        notePlaceholderCount = noteCount(),
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_ACCESS_REQUIRED,
                    )
                }
                OneNoteDiscoveryOutcome.AccessRevoked -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = label,
                        notePlaceholderCount = noteCount(),
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_ACCESS_REVOKED,
                    )
                }
                is OneNoteDiscoveryOutcome.Failed -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = label,
                        notePlaceholderCount = noteCount(),
                        feedbackMessage = outcome.failure.message,
                    )
                }
            }
        }
    }

    fun onDerivedDataCleared() {
        mutableUiState.value = buildState(
            connectedAccountLabel = null,
            notePlaceholderCount = 0,
        )
    }

    private suspend fun noteCount(): Int = withContext(Dispatchers.IO) {
        runCatching {
            assetRepository.countBySourceAndType(
                sourceId = OneNotePagesDiscoverySource.SOURCE_ID,
                type = AssetType.NOTE,
            )
        }.getOrDefault(0)
    }

    private fun buildState(
        connectedAccountLabel: String?,
        notePlaceholderCount: Int,
        hasMoreToDiscover: Boolean = false,
        feedbackMessage: String? = null,
    ): NotesConnectorUiState {
        val configured = authConfiguration.isRegistrationConfigured
        val connected = connectedAccountLabel != null
        return NotesConnectorUiState(
            registrationConfigured = configured,
            connectedAccountLabel = connectedAccountLabel,
            notePlaceholderCount = notePlaceholderCount,
            statusBody = NotesConnectorHonestyCopy.statusBody(
                registrationConfigured = configured,
                connectedAccountLabel = connectedAccountLabel,
                notePlaceholderCount = notePlaceholderCount,
            ),
            showConnect = configured && !connected,
            showDisconnect = connected,
            showDiscover = connected,
            discoverLabel = if (hasMoreToDiscover) {
                NotesConnectorHonestyCopy.DISCOVER_CONTINUE_LABEL
            } else {
                NotesConnectorHonestyCopy.DISCOVER_LABEL
            },
            hasMoreToDiscover = hasMoreToDiscover,
            isBusy = false,
            feedbackMessage = feedbackMessage,
        )
    }
}
