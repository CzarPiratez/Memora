package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.documents.DocumentTreeApprover
import com.memora.app.application.documents.PdfFolderConnectionFinder
import com.memora.app.application.documents.SafPdfFolderIndexer
import com.memora.app.application.documents.SafPdfFolderIndexingOutcome
import com.memora.app.domain.asset.SourceId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

    data object IN_PROGRESS : PdfFolderIndexingState

    data class COMPLETED(
        val discoveredAssetCount: Int,
        val hasMore: Boolean,
    ) : PdfFolderIndexingState {
        init {
            require(discoveredAssetCount >= 0) {
                "A PDF indexing result cannot contain a negative asset count."
            }
        }
    }

    data class FAILED(val message: String) : PdfFolderIndexingState
}

/** Presentation boundary for explicitly approving one SAF document tree. */
@HiltViewModel
class DocumentTreeSetupViewModel @Inject constructor(
    private val approveDocumentTree: DocumentTreeApprover,
    private val findPdfFolderConnection: PdfFolderConnectionFinder,
    private val indexPdfFolder: SafPdfFolderIndexer,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        DocumentTreeSetupUiState(connection = DocumentTreeConnectionState.LOADING),
    )

    val uiState: StateFlow<DocumentTreeSetupUiState> = mutableUiState.asStateFlow()

    init {
        restoreMostRecentConnection()
    }

    fun onDerivedDataCleared() {
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
                            "Memora could not check saved folder connections. Please choose a folder again.",
                        ),
                    )
                },
            )
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
                            "Memora could not save this folder connection. Please choose it again.",
                        ),
                    )
                },
            )
        }
    }

    fun onPersistableReadAccessFailed() {
        mutableUiState.value = DocumentTreeSetupUiState(
            DocumentTreeConnectionState.FAILED(
                "Android could not retain read access to this folder. Please choose it again.",
            ),
        )
    }

    /** Starts one explicit, bounded, read-only metadata page for the connected folder. */
    fun onIndexRequested() {
        val sourceId = (mutableUiState.value.connection as? DocumentTreeConnectionState.CONNECTED)
            ?.sourceId
            ?: return
        if (mutableUiState.value.indexing == PdfFolderIndexingState.IN_PROGRESS) return

        mutableUiState.value = mutableUiState.value.copy(
            indexing = PdfFolderIndexingState.IN_PROGRESS,
        )
        viewModelScope.launch {
            mutableUiState.value = runCatching {
                indexPdfFolder(sourceId)
            }.fold(
                onSuccess = ::stateFor,
                onFailure = {
                    mutableUiState.value.copy(
                        indexing = PdfFolderIndexingState.FAILED(
                            "Memora could not complete this PDF indexing step. You can try again.",
                        ),
                    )
                },
            )
        }
    }

    private fun stateFor(outcome: SafPdfFolderIndexingOutcome): DocumentTreeSetupUiState = when (outcome) {
        is SafPdfFolderIndexingOutcome.Indexed -> mutableUiState.value.copy(
            indexing = PdfFolderIndexingState.COMPLETED(
                discoveredAssetCount = outcome.discoveredAssetCount,
                hasMore = outcome.hasMore,
            ),
        )

        SafPdfFolderIndexingOutcome.SourceNotConnected -> DocumentTreeSetupUiState(
            connection = DocumentTreeConnectionState.FAILED(
                "Memora can no longer find this folder connection. Please choose it again.",
            ),
        )

        SafPdfFolderIndexingOutcome.AccessRequired,
        SafPdfFolderIndexingOutcome.AccessRevoked -> DocumentTreeSetupUiState(
            connection = DocumentTreeConnectionState.FAILED(
                "Android no longer allows Memora to read this folder. Please choose it again.",
            ),
        )

        is SafPdfFolderIndexingOutcome.Failed -> mutableUiState.value.copy(
            indexing = PdfFolderIndexingState.FAILED(outcome.failure.message),
        )
    }
}
