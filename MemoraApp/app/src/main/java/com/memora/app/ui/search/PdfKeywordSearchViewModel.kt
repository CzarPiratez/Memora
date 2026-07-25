package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.documents.PdfKeywordSearchHit
import com.memora.app.application.documents.PdfKeywordSearchOutcome
import com.memora.app.application.documents.SearchPersistedPdfPageText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PdfKeywordSearchUiState(
    val query: String = "",
    val phase: PdfKeywordSearchPhase = PdfKeywordSearchPhase.Idle,
)

sealed interface PdfKeywordSearchPhase {
    data object Idle : PdfKeywordSearchPhase

    data object EmptyQuery : PdfKeywordSearchPhase

    data object Searching : PdfKeywordSearchPhase

    data class Results(
        val query: String,
        val hits: List<PdfKeywordSearchHit>,
    ) : PdfKeywordSearchPhase {
        init {
            require(query.isNotBlank()) { "Results need the search query for Explain Mode." }
            require(hits.isNotEmpty()) { "Results need at least one hit." }
        }
    }
    data object NoMatches : PdfKeywordSearchPhase
}

@HiltViewModel
class PdfKeywordSearchViewModel @Inject constructor(
    private val searchPersistedPdfPageText: SearchPersistedPdfPageText,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PdfKeywordSearchUiState())
    val uiState: StateFlow<PdfKeywordSearchUiState> = mutableUiState.asStateFlow()

    fun onQueryChanged(value: String) {
        mutableUiState.value = mutableUiState.value.copy(query = value)
    }

    fun onSearch() {
        val query = mutableUiState.value.query
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(phase = PdfKeywordSearchPhase.Searching)
            mutableUiState.value = mutableUiState.value.copy(
                phase = when (val outcome = searchPersistedPdfPageText(query)) {
                    PdfKeywordSearchOutcome.BlankQuery -> PdfKeywordSearchPhase.EmptyQuery
                    is PdfKeywordSearchOutcome.Matches -> if (outcome.hits.isEmpty()) {
                        PdfKeywordSearchPhase.NoMatches
                    } else {
                        PdfKeywordSearchPhase.Results(
                            query = outcome.query,
                            hits = outcome.hits,
                        )
                    }
                },
            )
        }
    }
}
