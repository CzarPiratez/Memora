package com.memora.app.ui.setup

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.notes.OneNoteAuthOutcome
import com.memora.app.application.notes.OneNoteInteractiveAuth
import com.memora.app.domain.notes.OneNoteAuthConfiguration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotesConnectorUiState(
    val registrationConfigured: Boolean,
    val connectedAccountLabel: String?,
    val statusBody: String,
    val showConnect: Boolean,
    val showDisconnect: Boolean,
    val isBusy: Boolean = false,
    val feedbackMessage: String? = null,
)

@HiltViewModel
class NotesConnectorViewModel @Inject constructor(
    private val authConfiguration: OneNoteAuthConfiguration,
    private val oneNoteAuth: OneNoteInteractiveAuth,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(buildState(connectedAccountLabel = null))
    val uiState: StateFlow<NotesConnectorUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val label = oneNoteAuth.restoreAccountLabel()
            mutableUiState.value = buildState(label)
        }
    }

    fun onConnectRequested(activity: Activity) {
        if (mutableUiState.value.isBusy) return
        if (!authConfiguration.isRegistrationConfigured) {
            mutableUiState.value = buildState(
                connectedAccountLabel = null,
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
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_CONNECTED,
                    )
                }
                OneNoteAuthOutcome.Cancelled -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = null,
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_CANCELLED,
                    )
                }
                OneNoteAuthOutcome.RegistrationRequired -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = null,
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_REGISTRATION_REQUIRED,
                    )
                }
                is OneNoteAuthOutcome.Failed -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = null,
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
        )
        viewModelScope.launch {
            oneNoteAuth.disconnect()
            mutableUiState.value = buildState(
                connectedAccountLabel = null,
                feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_DISCONNECTED,
            )
        }
    }

    fun onDerivedDataCleared() {
        mutableUiState.value = buildState(connectedAccountLabel = null)
    }

    private fun buildState(
        connectedAccountLabel: String?,
        feedbackMessage: String? = null,
    ): NotesConnectorUiState {
        val configured = authConfiguration.isRegistrationConfigured
        val connected = connectedAccountLabel != null
        return NotesConnectorUiState(
            registrationConfigured = configured,
            connectedAccountLabel = connectedAccountLabel,
            statusBody = NotesConnectorHonestyCopy.statusBody(
                registrationConfigured = configured,
                connectedAccountLabel = connectedAccountLabel,
            ),
            showConnect = configured && !connected,
            showDisconnect = connected,
            isBusy = false,
            feedbackMessage = feedbackMessage,
        )
    }
}
