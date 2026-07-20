package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.documents.DocumentTreeApprover
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DocumentTreeSetupUiState(
    val connection: DocumentTreeConnectionState = DocumentTreeConnectionState.READY,
)

sealed interface DocumentTreeConnectionState {
    data object READY : DocumentTreeConnectionState

    data object SAVING : DocumentTreeConnectionState

    data object CONNECTED : DocumentTreeConnectionState

    data class FAILED(val message: String) : DocumentTreeConnectionState
}

/** Presentation boundary for explicitly approving one SAF document tree. */
@HiltViewModel
class DocumentTreeSetupViewModel @Inject constructor(
    private val approveDocumentTree: DocumentTreeApprover,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DocumentTreeSetupUiState())

    val uiState: StateFlow<DocumentTreeSetupUiState> = mutableUiState.asStateFlow()

    fun onPersistedReadAccessReceived(treeUri: String) {
        if (mutableUiState.value.connection == DocumentTreeConnectionState.SAVING) return

        mutableUiState.value = DocumentTreeSetupUiState(DocumentTreeConnectionState.SAVING)
        viewModelScope.launch {
            mutableUiState.value = runCatching {
                approveDocumentTree(treeUri)
            }.fold(
                onSuccess = { DocumentTreeSetupUiState(DocumentTreeConnectionState.CONNECTED) },
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
}
