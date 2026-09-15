package com.memora.app.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.memora.app.application.find.FindThumbnailRequest
import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.intelligence.RecallPrecision

@Composable
fun MeaningSearchScreen(
    uiState: MeaningSearchUiState,
    onQueryChanged: (String) -> Unit,
    onQueryCleared: () -> Unit,
    onSearch: () -> Unit,
    onSearchCancelled: () -> Unit,
    onOpenOriginal: (MeaningSearchHit) -> Unit,
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
            Text(MeaningSearchCopy.BACK_LABEL)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = MeaningSearchCopy.SCREEN_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = MeaningSearchCopy.SCOPE_BODY,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (val readiness = uiState.readiness) {
                MeaningSearchReadinessUi.Loading -> MeaningSearchCopy.READINESS_LOADING_BODY
                MeaningSearchReadinessUi.CouldNotLoad ->
                    MeaningSearchCopy.READINESS_COULD_NOT_LOAD_BODY
                is MeaningSearchReadinessUi.EngineUnavailable ->
                    MeaningSearchCopy.engineUnavailableBody(readiness.reason)
                is MeaningSearchReadinessUi.Ready ->
                    MeaningSearchCopy.readinessBody(readiness.snapshot)
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
            label = { Text(MeaningSearchCopy.QUERY_LABEL) },
            singleLine = true,
            enabled = uiState.phase !is MeaningSearchPhase.Searching,
            trailingIcon = {
                if (uiState.canClearQuery) {
                    TextButton(onClick = onQueryCleared) {
                        Text(MeaningSearchCopy.CLEAR_QUERY_LABEL)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (uiState.canSubmitSearch) onSearch()
                },
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSearch,
            enabled = uiState.canSubmitSearch,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(MeaningSearchCopy.SEARCH_LABEL)
        }
        if (uiState.canCancelSearch) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSearchCancelled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(MeaningSearchCopy.CANCEL_SEARCH_LABEL)
            }
        }
        // Open progress and open failures render on the card that was tapped;
        // see FindOpenOriginalButton.
        Spacer(modifier = Modifier.height(20.dp))
        when (val phase = uiState.phase) {
            MeaningSearchPhase.Idle -> Unit
            MeaningSearchPhase.EmptyQuery -> PhaseBody(MeaningSearchCopy.EMPTY_QUERY_BODY)
            MeaningSearchPhase.Searching -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(12.dp))
                PhaseBody(MeaningSearchCopy.SEARCHING_BODY)
            }
            is MeaningSearchPhase.EngineUnavailable ->
                PhaseBody(MeaningSearchCopy.engineUnavailableBody(phase.reason))
            is MeaningSearchPhase.NothingIndexed ->
                PhaseBody(MeaningSearchCopy.nothingIndexedBody(phase.query))
            is MeaningSearchPhase.NoMatches ->
                PhaseBody(MeaningSearchCopy.noMatchesBody(phase.query))
            MeaningSearchPhase.SearchCouldNotFinish ->
                PhaseBody(MeaningSearchCopy.SEARCH_COULD_NOT_FINISH_BODY)
            is MeaningSearchPhase.Results -> {
                val precision = phase.precision
                if (precision is RecallPrecision.Partial) {
                    // What is missing outranks how many were found: "add another
                    // word" is the wrong advice when a word already went unmatched.
                    PhaseBody(
                        MeaningSearchCopy.partialMatchBody(
                            matched = precision.matched,
                            missing = precision.missing,
                        ),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (precision is RecallPrecision.MeaningOnly) {
                    PhaseBody(MeaningSearchCopy.meaningOnlyBody(precision.missing))
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (phase.limitReached) {
                    PhaseBody(MeaningSearchCopy.limitReachedBody())
                    Spacer(modifier = Modifier.height(12.dp))
                }
                phase.hits.forEach { hit ->
                    MeaningHitCard(
                        hit = hit,
                        query = phase.query,
                        openState = uiState.openStateFor(hit.openTarget()),
                        onOpenOriginal = onOpenOriginal,
                        onDismissOpenFeedback = onDismissOpenFeedback,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PhaseBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun MeaningHitCard(
    hit: MeaningSearchHit,
    query: String,
    openState: FindCardOpenState,
    onOpenOriginal: (MeaningSearchHit) -> Unit,
    onDismissOpenFeedback: () -> Unit,
) {
    var showWhy by remember(hit.revisionId.value, query) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                FindResultThumbnail(request = FindThumbnailRequest.fromMeaning(hit))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = MeaningSearchCopy.friendlyHitLabel(hit.label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FindOpenOriginalButton(
                copy = MeaningSearchCopy.OPEN_ORIGINAL,
                state = openState,
                onOpen = { onOpenOriginal(hit) },
                onDismissFailure = onDismissOpenFeedback,
            )
            Spacer(modifier = Modifier.height(8.dp))
            WhyDisclosure(
                expanded = showWhy,
                onExpandedChange = { showWhy = it },
                presentation = { CanonicalRecallWhyCopy.present(hit, query) },
            )
        }
    }
}
