package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.memora.app.application.documents.DocumentTreeApprover
import com.memora.app.application.documents.PdfFolderConnectionFinder
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceId
import com.memora.app.work.SafPdfDiscoveryWorkScheduler
import com.memora.app.work.SafPdfDiscoveryWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DocumentTreeSetupUiState(
    val connection: DocumentTreeConnectionState = DocumentTreeConnectionState.READY,
    val indexing: PdfFolderIndexingState = PdfFolderIndexingState.NOT_STARTED,
)

sealed interface DocumentTreeConnectionState {
    data object LOADING : DocumentTreeConnectionState

    data object READY : DocumentTreeConnectionState

    data object SAVING : DocumentTreeConnectionState

    data class CONNECTED(val sourceId: SourceId) : DocumentTreeConnectionState

    data class FAILED(val message: String) : DocumentTreeConnectionState
}

sealed interface PdfFolderIndexingState {
    data object NOT_STARTED : PdfFolderIndexingState

    /**
     * Folder was indexed before; offer rescan without implying this is a first-time index.
     */
    data class READY_TO_CHECK(
        val totalAssetCount: Int,
    ) : PdfFolderIndexingState {
        init {
            require(totalAssetCount > 0) {
                "Ready-to-check needs a positive saved PDF count."
            }
        }
    }

    data object IN_PROGRESS : PdfFolderIndexingState

    /**
     * Work exists but Android is not running it: an unmet constraint, or a
     * backoff between retries.
     *
     * Collapsing this into [IN_PROGRESS] is how a scan that was making no
     * progress looked exactly like one that was. A person waiting on a spinner
     * deserves to know the difference, and to be able to stop either.
     */
    data class WAITING(val retrying: Boolean) : PdfFolderIndexingState

    data object STOPPED : PdfFolderIndexingState

    data class COMPLETED(
        val totalAssetCount: Int,
        val newlyDiscoveredAssetCount: Int,
        val hasMore: Boolean,
    ) : PdfFolderIndexingState {
        init {
            require(totalAssetCount >= 0) {
                "A PDF indexing result cannot contain a negative asset count."
            }
            require(newlyDiscoveredAssetCount >= 0) {
                "A PDF indexing result cannot contain a negative new-asset count."
            }
        }
    }

    data class FAILED(val message: String) : PdfFolderIndexingState

    /** This session has a scan under way, running or merely scheduled. */
    fun isActive(): Boolean = this is IN_PROGRESS || this is WAITING
}

/** Presentation boundary for explicitly approving one SAF document tree. */
@HiltViewModel
class DocumentTreeSetupViewModel @Inject constructor(
    private val approveDocumentTree: DocumentTreeApprover,
    private val findPdfFolderConnection: PdfFolderConnectionFinder,
    private val discoveryWorkScheduler: SafPdfDiscoveryWorkScheduler,
    private val assetRepository: AssetRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        DocumentTreeSetupUiState(connection = DocumentTreeConnectionState.LOADING),
    )

    val uiState: StateFlow<DocumentTreeSetupUiState> = mutableUiState.asStateFlow()

    private var workObservationJob: Job? = null

    private var assetCountBeforeIndexing: Int = 0

    init {
        restoreMostRecentConnection()
    }

    fun onDerivedDataCleared() {
        workObservationJob?.cancel()
        restoreMostRecentConnection()
    }

    private fun restoreMostRecentConnection() {
        viewModelScope.launch {
            mutableUiState.value = runCatching {
                findPdfFolderConnection()
            }.fold(
                onSuccess = { sourceId ->
                    DocumentTreeSetupUiState(
                        connection = sourceId?.let(DocumentTreeConnectionState::CONNECTED)
                            ?: DocumentTreeConnectionState.READY,
                    )
                },
                onFailure = {
                    DocumentTreeSetupUiState(
                        connection = DocumentTreeConnectionState.FAILED(
                            "UNFYND could not check saved folder connections. Please choose a folder again.",
                        ),
                    )
                },
            )
            val connectedId =
                (mutableUiState.value.connection as? DocumentTreeConnectionState.CONNECTED)?.sourceId
            if (connectedId != null) {
                observeDiscoveryWork(connectedId)
                refreshIndexingIdleState(connectedId)
            }
        }
    }

    fun onPersistedReadAccessReceived(treeUri: String) {
        if (mutableUiState.value.connection == DocumentTreeConnectionState.SAVING) return

        mutableUiState.value = DocumentTreeSetupUiState(DocumentTreeConnectionState.SAVING)
        viewModelScope.launch {
            mutableUiState.value = runCatching {
                approveDocumentTree(treeUri)
            }.fold(
                onSuccess = { approval ->
                    DocumentTreeSetupUiState(DocumentTreeConnectionState.CONNECTED(approval.sourceId))
                },
                onFailure = {
                    DocumentTreeSetupUiState(
                        DocumentTreeConnectionState.FAILED(
                            "UNFYND could not save this folder connection. Please choose it again.",
                        ),
                    )
                },
            )
            val connectedId =
                (mutableUiState.value.connection as? DocumentTreeConnectionState.CONNECTED)?.sourceId
            if (connectedId != null) {
                observeDiscoveryWork(connectedId)
                refreshIndexingIdleState(connectedId)
            }
        }
    }

    fun onPersistableReadAccessFailed() {
        mutableUiState.value = DocumentTreeSetupUiState(
            DocumentTreeConnectionState.FAILED(
                "Android could not retain read access to this folder. Please choose it again.",
            ),
        )
    }

    /**
     * Starts WorkManager-backed discovery drain for the connected folder after explicit consent.
     *
     * Metadata placeholders only; does not open PDF bytes or save searchable text.
     */
    fun onIndexRequested() {
        val sourceId = (mutableUiState.value.connection as? DocumentTreeConnectionState.CONNECTED)
            ?.sourceId
            ?: return
        if (mutableUiState.value.indexing.isActive()) return

        viewModelScope.launch {
            assetCountBeforeIndexing = runCatching {
                assetRepository.countBySourceAndType(sourceId, AssetType.PDF)
            }.getOrDefault(0)
            mutableUiState.value = mutableUiState.value.copy(
                indexing = PdfFolderIndexingState.IN_PROGRESS,
            )
            discoveryWorkScheduler.enqueueDrain(sourceId)
            observeDiscoveryWork(sourceId)
        }
    }

    /**
     * Ends the scan, including any continuation page already queued behind the
     * one running. Nothing already listed is discarded; the walk resumes from
     * its saved checkpoint the next time the person asks.
     */
    fun onStopIndexingRequested() {
        val sourceId = (mutableUiState.value.connection as? DocumentTreeConnectionState.CONNECTED)
            ?.sourceId
            ?: return
        if (!mutableUiState.value.indexing.isActive()) return

        discoveryWorkScheduler.cancelDrain(sourceId)
        mutableUiState.value = mutableUiState.value.copy(
            indexing = PdfFolderIndexingState.STOPPED,
        )
    }

    private fun observeDiscoveryWork(sourceId: SourceId) {
        workObservationJob?.cancel()
        workObservationJob = viewModelScope.launch {
            discoveryWorkScheduler.observeUniqueWork(sourceId).collect { infos ->
                applyWorkInfos(sourceId, infos)
            }
        }
    }

    private suspend fun applyWorkInfos(sourceId: SourceId, infos: List<WorkInfo>) {
        if (infos.isEmpty()) return

        val connection = mutableUiState.value.connection
        if (connection !is DocumentTreeConnectionState.CONNECTED || connection.sourceId != sourceId) {
            return
        }

        val indexing = mutableUiState.value.indexing
        val isRunning = infos.any { it.state == WorkInfo.State.RUNNING }
        val isWaiting = infos.any { info ->
            info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.BLOCKED
        }

        when {
            isRunning -> {
                mutableUiState.value = mutableUiState.value.copy(
                    indexing = PdfFolderIndexingState.IN_PROGRESS,
                )
            }

            isWaiting -> {
                // runAttemptCount > 0 means this page already ran and failed,
                // so Android is sitting out a backoff rather than queueing.
                val retrying = infos.any { info ->
                    (info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.BLOCKED) &&
                        info.runAttemptCount > 0
                }
                mutableUiState.value = mutableUiState.value.copy(
                    indexing = PdfFolderIndexingState.WAITING(retrying = retrying),
                )
            }

            infos.any { it.state == WorkInfo.State.CANCELLED } &&
                indexing.isActive() -> {
                mutableUiState.value = mutableUiState.value.copy(
                    indexing = PdfFolderIndexingState.STOPPED,
                )
            }

            infos.any { it.state == WorkInfo.State.FAILED } -> {
                // Ignore stale finished work from a prior session (e.g. after clear).
                if (!indexing.isActive()) return

                val failed = infos.lastOrNull { it.state == WorkInfo.State.FAILED }
                val reason = failed?.outputData?.getString(SafPdfDiscoveryWorker.KEY_FAILURE_REASON)
                when (reason) {
                    SafPdfDiscoveryWorker.REASON_ACCESS_STOPPED ->
                        mutableUiState.value = DocumentTreeSetupUiState(
                            connection = DocumentTreeConnectionState.FAILED(
                                "Android no longer allows UNFYND to read this folder. " +
                                    "Please choose it again.",
                            ),
                        )

                    SafPdfDiscoveryWorker.REASON_STOPPED ->
                        mutableUiState.value = mutableUiState.value.copy(
                            indexing = PdfFolderIndexingState.STOPPED,
                        )

                    SafPdfDiscoveryWorker.REASON_GAVE_UP ->
                        mutableUiState.value = mutableUiState.value.copy(
                            indexing = PdfFolderIndexingState.FAILED(
                                "UNFYND retried this folder scan and could not get past the same " +
                                    "point, so it stopped rather than keep trying. Nothing already " +
                                    "listed was lost.",
                            ),
                        )

                    else ->
                        mutableUiState.value = mutableUiState.value.copy(
                            indexing = PdfFolderIndexingState.FAILED(
                                "UNFYND could not finish reading PDF folder metadata. " +
                                    "You can try again.",
                            ),
                        )
                }
            }

            infos.all { it.state.isFinished } -> {
                // Ignore stale finished work unless this session started indexing
                // (or process death resumed into an active state above).
                if (!indexing.isActive()) return

                val totalAssets = runCatching {
                    assetRepository.countBySourceAndType(sourceId, AssetType.PDF)
                }.getOrDefault(0)
                val newlyDiscovered = (totalAssets - assetCountBeforeIndexing).coerceAtLeast(0)
                val lastSuccess = infos.lastOrNull { it.state == WorkInfo.State.SUCCEEDED }
                val hasMore = lastSuccess?.outputData?.getBoolean(
                    SafPdfDiscoveryWorker.KEY_HAS_MORE,
                    false,
                ) == true
                mutableUiState.value = mutableUiState.value.copy(
                    indexing = PdfFolderIndexingState.COMPLETED(
                        totalAssetCount = totalAssets,
                        newlyDiscoveredAssetCount = newlyDiscovered,
                        hasMore = hasMore,
                    ),
                )
            }
        }
    }

    private suspend fun refreshIndexingIdleState(sourceId: SourceId) {
        if (mutableUiState.value.indexing !is PdfFolderIndexingState.NOT_STARTED) return
        val count = runCatching {
            assetRepository.countBySourceAndType(sourceId, AssetType.PDF)
        }.getOrDefault(0)
        if (count > 0) {
            mutableUiState.value = mutableUiState.value.copy(
                indexing = PdfFolderIndexingState.READY_TO_CHECK(totalAssetCount = count),
            )
        }
    }
}
