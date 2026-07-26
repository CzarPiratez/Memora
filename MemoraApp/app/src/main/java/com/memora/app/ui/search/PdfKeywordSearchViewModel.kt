package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.documents.PdfKeywordSearchHit
import com.memora.app.application.documents.PdfKeywordSearchOutcome
import com.memora.app.application.documents.SearchPersistedPdfPageText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicInteger
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
        val limitReached: Boolean,
    ) : PdfKeywordSearchPhase {
        init {
            require(query.isNotBlank()) { "Results need the search query for Explain Mode." }
            require(hits.isNotEmpty()) { "Results need at least one hit." }
        }
    }

    /** No listed hits for the submitted [query]. */
    data class NoMatches(
        val query: String,
    ) : PdfKeywordSearchPhase {
        init {
            require(query.isNotBlank()) { "No-matches needs the submitted search query." }
        }
    }

    /** Search ran, but no current saved PDF page text exists to search. */
    data class NothingSavedToSearch(
        val query: String,
    ) : PdfKeywordSearchPhase {
        init {
            require(query.isNotBlank()) {
                "Nothing-saved needs the submitted search query."
            }
        }
    }

    /** Search could not finish; never leave the UI spinning. */
    data object SearchCouldNotFinish : PdfKeywordSearchPhase
}

@HiltViewModel
class PdfKeywordSearchViewModel(
    private val searchPersistedPdfPageText: suspend (String) -> PdfKeywordSearchOutcome,
) : ViewModel() {
    @Inject
    constructor(
        searchPersistedPdfPageText: SearchPersistedPdfPageText,
    ) : this(
        searchPersistedPdfPageText = { rawQuery -> searchPersistedPdfPageText(rawQuery) },
    )

    private val mutableUiState = MutableStateFlow(PdfKeywordSearchUiState())
    val uiState: StateFlow<PdfKeywordSearchUiState> = mutableUiState.asStateFlow()

    private val searchGeneration = AtomicInteger(0)

    fun onQueryChanged(value: String) {
        val current = mutableUiState.value
        if (value == current.query) return

        // Field and phase must stay coherent: never leave Why/results for a prior query.
        searchGeneration.incrementAndGet()
        mutableUiState.value = current.copy(
            query = value,
            phase = PdfKeywordSearchPhase.Idle,
        )
    }

    fun onSearch() {
        val query = mutableUiState.value.query
        val generation = searchGeneration.incrementAndGet()
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(phase = PdfKeywordSearchPhase.Searching)
            val outcome = try {
                searchPersistedPdfPageText(query)
            } catch (_: Exception) {
                if (generation != searchGeneration.get()) return@launch
                mutableUiState.value = mutableUiState.value.copy(
                    phase = PdfKeywordSearchPhase.SearchCouldNotFinish,
                )
                return@launch
            }
            if (generation != searchGeneration.get()) return@launch

            mutableUiState.value = mutableUiState.value.copy(
                phase = when (outcome) {
                    PdfKeywordSearchOutcome.BlankQuery -> PdfKeywordSearchPhase.EmptyQuery
                    is PdfKeywordSearchOutcome.NothingSavedToSearch ->
                        PdfKeywordSearchPhase.NothingSavedToSearch(query = outcome.query)
                    is PdfKeywordSearchOutcome.Matches -> if (outcome.hits.isEmpty()) {
                        PdfKeywordSearchPhase.NoMatches(query = outcome.query)
                    } else {
                        PdfKeywordSearchPhase.Results(
                            query = outcome.query,
                            hits = outcome.hits,
                            limitReached = outcome.limitReached,
                        )
                    }
                },
            )
        }
    }

    /**
     * Drops Results/Why after user-confirmed index clear so Explain Mode cannot
     * cite excerpts that no longer exist in Memora's private store.
     */
    fun onDerivedDataCleared() {
        searchGeneration.incrementAndGet()
        mutableUiState.value = mutableUiState.value.copy(
            phase = PdfKeywordSearchPhase.Idle,
        )
    }
}
