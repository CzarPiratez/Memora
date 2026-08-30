package com.memora.app.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.memora.app.application.notes.NotePageKeywordSearchHit

@Composable
fun NotePageKeywordSearchScreen(
    uiState: NotePageKeywordSearchUiState,
    onQueryChanged: (String) -> Unit,
    onQueryCleared: () -> Unit,
    onSearch: () -> Unit,
    onSearchCancelled: () -> Unit,
    onOpenOriginalNote: (NotePageKeywordSearchHit) -> Unit,
    onDismissOpenFeedback: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
            .padding(vertical = 24.dp),
    ) {
        Button(onClick = onBack) {
            Text(NotePageKeywordSearchCopy.BACK_LABEL)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = NotePageKeywordSearchCopy.SCREEN_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = NotePageKeywordSearchCopy.SCOPE_BODY,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (val readiness = uiState.readiness) {
                NotePageKeywordSearchReadinessUi.Loading ->
                    NotePageKeywordSearchCopy.READINESS_LOADING_BODY
                NotePageKeywordSearchReadinessUi.CouldNotLoad ->
                    NotePageKeywordSearchCopy.READINESS_COULD_NOT_LOAD_BODY
                is NotePageKeywordSearchReadinessUi.Ready ->
                    NotePageKeywordSearchCopy.readinessBody(
                        noteCount = readiness.snapshot.noteCount,
                    )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(NotePageKeywordSearchCopy.QUERY_LABEL) },
            singleLine = true,
            enabled = uiState.phase !is NotePageKeywordSearchPhase.Searching &&
                uiState.openFeedback !is NotePageOpenFeedbackUi.Opening,
            trailingIcon = {
                if (uiState.canClearQuery) {
                    TextButton(onClick = onQueryCleared) {
                        Text(NotePageKeywordSearchCopy.CLEAR_QUERY_LABEL)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (uiState.canSubmitSearch) {
                        onSearch()
                    }
                },
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canSubmitSearch,
        ) {
            Text(NotePageKeywordSearchCopy.SEARCH_LABEL)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSearchCancelled,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canCancelSearch,
        ) {
            Text(NotePageKeywordSearchCopy.CANCEL_SEARCH_LABEL)
        }
        Spacer(modifier = Modifier.height(20.dp))
        when (val phase = uiState.phase) {
            NotePageKeywordSearchPhase.Idle -> {
                if (uiState.query.isBlank()) {
                    Text(
                        text = NotePageKeywordSearchCopy.EMPTY_QUERY_BODY,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            NotePageKeywordSearchPhase.EmptyQuery -> Text(
                text = NotePageKeywordSearchCopy.EMPTY_QUERY_BODY,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            NotePageKeywordSearchPhase.Searching -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = NotePageKeywordSearchCopy.SEARCHING_BODY
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(modifier = Modifier.clearAndSetSemantics { })
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = NotePageKeywordSearchCopy.SEARCHING_BODY,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            is NotePageKeywordSearchPhase.NoMatches -> Text(
                text = NotePageKeywordSearchCopy.noMatchesBody(phase.query),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            is NotePageKeywordSearchPhase.NothingSavedToSearch -> Text(
                text = NotePageKeywordSearchCopy.NOTHING_SAVED_BODY,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            NotePageKeywordSearchPhase.SearchCouldNotFinish -> Text(
                text = NotePageKeywordSearchCopy.SEARCH_COULD_NOT_FINISH_BODY,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            is NotePageKeywordSearchPhase.Results -> {
                Text(
                    text = NotePageKeywordSearchCopy.resultsSummary(
                        query = phase.query,
                        matchCount = phase.hits.size,
                        limitReached = phase.limitReached,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                when (val feedback = uiState.openFeedback) {
                    NotePageOpenFeedbackUi.None -> Unit
                    NotePageOpenFeedbackUi.Opening -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    liveRegion = LiveRegionMode.Polite
                                    contentDescription =
                                        NotePageKeywordSearchCopy.OPEN_FEEDBACK_OPENING_BODY
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.clearAndSetSemantics { })
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = NotePageKeywordSearchCopy.OPEN_FEEDBACK_OPENING_BODY,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    NotePageOpenFeedbackUi.SourceUnavailable,
                    NotePageOpenFeedbackUi.CouldNotOpen,
                    -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (feedback is NotePageOpenFeedbackUi.SourceUnavailable) {
                                NotePageKeywordSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY
                            } else {
                                NotePageKeywordSearchCopy.OPEN_FEEDBACK_COULD_NOT_OPEN_BODY
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        TextButton(onClick = onDismissOpenFeedback) {
                            Text(NotePageKeywordSearchCopy.DISMISS_OPEN_FEEDBACK_LABEL)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                phase.hits.forEachIndexed { index, hit ->
                    NotePageKeywordHitCard(
                        hit = hit,
                        query = phase.query,
                        index = index,
                        openEnabled = uiState.openFeedback !is NotePageOpenFeedbackUi.Opening,
                        onOpenOriginal = { onOpenOriginalNote(hit) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun NotePageKeywordHitCard(
    hit: NotePageKeywordSearchHit,
    query: String,
    index: Int,
    openEnabled: Boolean,
    onOpenOriginal: () -> Unit,
) {
    var whyExpanded by remember(query, index, hit.sourceId, hit.sourceAssetKey) {
        mutableStateOf(false)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = hit.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = PdfKeywordSearchHighlight.annotatedExcerpt(
                    excerpt = hit.excerpt,
                    query = query,
                    highlightColor = MaterialTheme.colorScheme.primary,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = NotePageKeywordSearchCopy.OPEN_ORIGINAL_NOTE_HINT,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenOriginal,
                modifier = Modifier.fillMaxWidth(),
                enabled = openEnabled,
            ) {
                Text(NotePageKeywordSearchCopy.OPEN_ORIGINAL_NOTE_LABEL)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { whyExpanded = !whyExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (whyExpanded) {
                        NotePageKeywordSearchCopy.HIDE_WHY_LABEL
                    } else {
                        NotePageKeywordSearchCopy.WHY_THIS_RESULT_LABEL
                    },
                )
            }
            if (whyExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = NotePageKeywordSearchCopy.whyThisResultBody(
                        hit = hit,
                        query = query,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
