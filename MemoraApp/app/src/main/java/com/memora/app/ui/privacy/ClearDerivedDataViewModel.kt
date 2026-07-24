package com.memora.app.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.privacy.ClearMemoraDerivedData
import com.memora.app.application.privacy.ClearMemoraDerivedDataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ClearDerivedDataUiState(
    val phase: ClearDerivedDataPhase = ClearDerivedDataPhase.Idle,
)

sealed interface ClearDerivedDataPhase {
    data object Idle : ClearDerivedDataPhase

    data object Confirming : ClearDerivedDataPhase

    data object InProgress : ClearDerivedDataPhase

    data class Cleared(val message: String) : ClearDerivedDataPhase

    data object Failed : ClearDerivedDataPhase
}

@HiltViewModel
class ClearDerivedDataViewModel @Inject constructor(
    private val clearMemoraDerivedData: ClearMemoraDerivedData,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ClearDerivedDataUiState())
    val uiState: StateFlow<ClearDerivedDataUiState> = mutableUiState.asStateFlow()

    fun onClearRequested() {
        if (mutableUiState.value.phase is ClearDerivedDataPhase.InProgress) return
        mutableUiState.value = ClearDerivedDataUiState(ClearDerivedDataPhase.Confirming)
    }

    fun onConfirmDismissed() {
        if (mutableUiState.value.phase is ClearDerivedDataPhase.InProgress) return
        mutableUiState.value = ClearDerivedDataUiState(ClearDerivedDataPhase.Idle)
    }

    fun onClearConfirmed() {
        if (mutableUiState.value.phase is ClearDerivedDataPhase.InProgress) return
        mutableUiState.value = ClearDerivedDataUiState(ClearDerivedDataPhase.InProgress)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                clearMemoraDerivedData()
            }
            mutableUiState.value = when (result) {
                is ClearMemoraDerivedDataResult.Cleared -> ClearDerivedDataUiState(
                    ClearDerivedDataPhase.Cleared(result.message),
                )
                ClearMemoraDerivedDataResult.Failed -> ClearDerivedDataUiState(
                    ClearDerivedDataPhase.Failed,
                )
            }
        }
    }

    fun onClearedAcknowledged() {
        mutableUiState.value = ClearDerivedDataUiState(ClearDerivedDataPhase.Idle)
    }
}
