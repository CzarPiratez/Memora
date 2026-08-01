package com.memora.app.ui.setup

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.memora.app.application.notes.OneNoteAuthOutcome
import com.memora.app.application.notes.OneNoteDiscoveryOutcome
import com.memora.app.application.notes.OneNoteInteractiveAuth
import com.memora.app.application.notes.OneNotePagesIndexer
import com.memora.app.application.notes.RunPendingOneNotePageExtract
import com.memora.app.data.notes.OneNotePagesDiscoverySource
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.extraction.NotePageExtractionPersistence
import com.memora.app.domain.notes.OneNoteAuthConfiguration
import com.memora.app.work.OneNotePageExtractWorkScheduler
import com.memora.app.work.OneNotePageExtractWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NotesConnectorUiState(
    val registrationConfigured: Boolean,
    val connectedAccountLabel: String?,
    val notePlaceholderCount: Int,
    val noteExtractCount: Int,
    val statusBody: String,
    val showConnect: Boolean,
    val showDisconnect: Boolean,
    val showDiscover: Boolean,
    val showExtract: Boolean,
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
    private val notePagePersistence: NotePageExtractionPersistence,
    private val extractWorkScheduler: OneNotePageExtractWorkScheduler,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        buildState(
            connectedAccountLabel = null,
            notePlaceholderCount = 0,
            noteExtractCount = 0,
        ),
    )
    val uiState: StateFlow<NotesConnectorUiState> = mutableUiState.asStateFlow()
    private var extractObservationJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val label = oneNoteAuth.restoreAccountLabel()
            mutableUiState.value = buildState(
                connectedAccountLabel = label,
                notePlaceholderCount = noteCount(),
                noteExtractCount = extractCount(),
            )
        }
    }

    fun onConnectRequested(activity: Activity) {
        if (mutableUiState.value.isBusy) return
        if (!authConfiguration.isRegistrationConfigured) {
            mutableUiState.value = buildState(
                connectedAccountLabel = null,
                notePlaceholderCount = mutableUiState.value.notePlaceholderCount,
                noteExtractCount = mutableUiState.value.noteExtractCount,
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
                        noteExtractCount = extractCount(),
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_CONNECTED,
                    )
                }
                OneNoteAuthOutcome.Cancelled -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = null,
                        notePlaceholderCount = noteCount(),
                        noteExtractCount = extractCount(),
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_CANCELLED,
                    )
                }
                OneNoteAuthOutcome.RegistrationRequired -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = null,
                        notePlaceholderCount = noteCount(),
                        noteExtractCount = extractCount(),
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_REGISTRATION_REQUIRED,
                    )
                }
                is OneNoteAuthOutcome.Failed -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = null,
                        notePlaceholderCount = noteCount(),
                        noteExtractCount = extractCount(),
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
            showExtract = false,
        )
        viewModelScope.launch {
            extractWorkScheduler.cancel()
            oneNoteAuth.disconnect()
            mutableUiState.value = buildState(
                connectedAccountLabel = null,
                notePlaceholderCount = noteCount(),
                noteExtractCount = extractCount(),
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
            showExtract = false,
        )
        viewModelScope.launch {
            val label = mutableUiState.value.connectedAccountLabel
            when (val outcome = oneNotePagesIndexer()) {
                is OneNoteDiscoveryOutcome.Discovered -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = label,
                        notePlaceholderCount = outcome.totalNoteAssets,
                        noteExtractCount = extractCount(),
                        hasMoreToDiscover = outcome.hasMore,
                        feedbackMessage = NotesConnectorHonestyCopy.discoveredFeedback(
                            pageCount = outcome.pageAssetCount,
                            totalCount = outcome.totalNoteAssets,
                            hasMore = outcome.hasMore,
                        ),
                    )
                }
                OneNoteDiscoveryOutcome.AccessRequired -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = oneNoteAuth.restoreAccountLabel(),
                        notePlaceholderCount = noteCount(),
                        noteExtractCount = extractCount(),
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_ACCESS_REQUIRED,
                    )
                }
                OneNoteDiscoveryOutcome.AccessRevoked -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = label,
                        notePlaceholderCount = noteCount(),
                        noteExtractCount = extractCount(),
                        feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_ACCESS_REVOKED,
                    )
                }
                is OneNoteDiscoveryOutcome.Failed -> {
                    mutableUiState.value = buildState(
                        connectedAccountLabel = label,
                        notePlaceholderCount = noteCount(),
                        noteExtractCount = extractCount(),
                        feedbackMessage = outcome.failure.message,
                    )
                }
            }
        }
    }

    fun onExtractRequested() {
        if (mutableUiState.value.isBusy) return
        if (mutableUiState.value.connectedAccountLabel == null) {
            mutableUiState.value = mutableUiState.value.copy(
                feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_ACCESS_REQUIRED,
            )
            return
        }
        if (mutableUiState.value.notePlaceholderCount <= 0) {
            mutableUiState.value = mutableUiState.value.copy(
                feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_EXTRACT_NO_PLACEHOLDERS,
            )
            return
        }
        mutableUiState.value = mutableUiState.value.copy(
            isBusy = true,
            feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_EXTRACTING,
            showDiscover = false,
            showExtract = false,
        )
        extractWorkScheduler.enqueueDrain()
        observeExtractWork()
    }

    fun onDerivedDataCleared() {
        extractWorkScheduler.cancel()
        mutableUiState.value = buildState(
            connectedAccountLabel = null,
            notePlaceholderCount = 0,
            noteExtractCount = 0,
        )
    }

    private fun observeExtractWork() {
        extractObservationJob?.cancel()
        extractObservationJob = viewModelScope.launch {
            extractWorkScheduler.observeUniqueWork().collect { infos ->
                applyExtractWorkInfos(infos)
            }
        }
    }

    private suspend fun applyExtractWorkInfos(infos: List<WorkInfo>) {
        if (infos.isEmpty()) return
        val label = mutableUiState.value.connectedAccountLabel
        when {
            infos.any { info ->
                info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.BLOCKED
            } -> {
                mutableUiState.value = mutableUiState.value.copy(
                    isBusy = true,
                    feedbackMessage = NotesConnectorHonestyCopy.FEEDBACK_EXTRACTING,
                    showExtract = false,
                    showDiscover = false,
                )
            }
            infos.any { it.state == WorkInfo.State.FAILED } -> {
                val failed = infos.lastOrNull { it.state == WorkInfo.State.FAILED }
                val reason = failed?.outputData
                    ?.getString(OneNotePageExtractWorker.KEY_FAILURE_REASON)
                val message = if (reason == OneNotePageExtractWorker.REASON_ACCESS_STOPPED) {
                    NotesConnectorHonestyCopy.FEEDBACK_ACCESS_REVOKED
                } else {
                    NotesConnectorHonestyCopy.FEEDBACK_EXTRACT_FAILED
                }
                mutableUiState.value = buildState(
                    connectedAccountLabel = label,
                    notePlaceholderCount = noteCount(),
                    noteExtractCount = extractCount(),
                    feedbackMessage = message,
                )
            }
            infos.all { it.state.isFinished } -> {
                val placeholders = noteCount()
                val extracted = extractCount()
                mutableUiState.value = buildState(
                    connectedAccountLabel = label,
                    notePlaceholderCount = placeholders,
                    noteExtractCount = extracted,
                    feedbackMessage = NotesConnectorHonestyCopy.extractedFeedback(
                        extractCount = extracted,
                        placeholderCount = placeholders,
                    ),
                )
            }
        }
    }

    private suspend fun noteCount(): Int = withContext(Dispatchers.IO) {
        runCatching {
            assetRepository.countBySourceAndType(
                sourceId = OneNotePagesDiscoverySource.SOURCE_ID,
                type = AssetType.NOTE,
            )
        }.getOrDefault(0)
    }

    private suspend fun extractCount(): Int = withContext(Dispatchers.IO) {
        runCatching {
            notePagePersistence.countCurrentForSource(
                sourceId = OneNotePagesDiscoverySource.SOURCE_ID.value,
                schemaVersion = RunPendingOneNotePageExtract.SCHEMA.value,
            )
        }.getOrDefault(0)
    }

    private fun buildState(
        connectedAccountLabel: String?,
        notePlaceholderCount: Int,
        noteExtractCount: Int,
        hasMoreToDiscover: Boolean = false,
        feedbackMessage: String? = null,
    ): NotesConnectorUiState {
        val configured = authConfiguration.isRegistrationConfigured
        val connected = connectedAccountLabel != null
        val pendingExtract = notePlaceholderCount > noteExtractCount
        return NotesConnectorUiState(
            registrationConfigured = configured,
            connectedAccountLabel = connectedAccountLabel,
            notePlaceholderCount = notePlaceholderCount,
            noteExtractCount = noteExtractCount,
            statusBody = NotesConnectorHonestyCopy.statusBody(
                registrationConfigured = configured,
                connectedAccountLabel = connectedAccountLabel,
                notePlaceholderCount = notePlaceholderCount,
                noteExtractCount = noteExtractCount,
            ),
            showConnect = configured && !connected,
            showDisconnect = connected,
            showDiscover = connected,
            showExtract = connected && pendingExtract,
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
