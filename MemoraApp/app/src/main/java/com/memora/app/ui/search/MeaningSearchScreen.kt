package com.memora.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.asset.AssetType
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
            enabled = uiState.phase !is MeaningSearchPhase.Searching &&
                uiState.openFeedback !is MeaningOpenFeedbackUi.Opening,
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
        when (val feedback = uiState.openFeedback) {
            MeaningOpenFeedbackUi.None -> Unit
            MeaningOpenFeedbackUi.Opening -> {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(8.dp))
                PhaseBody(MeaningSearchCopy.OPEN_FEEDBACK_OPENING_BODY)
            }
            MeaningOpenFeedbackUi.SourceUnavailable -> {
                Spacer(modifier = Modifier.height(12.dp))
                PhaseBody(MeaningSearchCopy.OPEN_FEEDBACK_SOURCE_UNAVAILABLE_BODY)
                TextButton(onClick = onDismissOpenFeedback) {
                    Text(MeaningSearchCopy.DISMISS_OPEN_FEEDBACK_LABEL)
                }
            }
            MeaningOpenFeedbackUi.CouldNotOpen -> {
                Spacer(modifier = Modifier.height(12.dp))
                PhaseBody(MeaningSearchCopy.OPEN_FEEDBACK_COULD_NOT_OPEN_BODY)
                TextButton(onClick = onDismissOpenFeedback) {
                    Text(MeaningSearchCopy.DISMISS_OPEN_FEEDBACK_LABEL)
                }
            }
        }
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
                } else if (phase.limitReached) {
                    PhaseBody(MeaningSearchCopy.limitReachedBody())
                    Spacer(modifier = Modifier.height(12.dp))
                }
                phase.hits.forEach { hit ->
                    MeaningHitCard(
                        hit = hit,
                        query = phase.query,
                        canOpen = uiState.canOpenOriginal,
                        onOpenOriginal = onOpenOriginal,
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
    canOpen: Boolean,
    onOpenOriginal: (MeaningSearchHit) -> Unit,
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
            Text(
                text = MeaningSearchCopy.hitTypeLabel(hit.assetType),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MeaningSearchCopy.friendlyHitLabel(hit.label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = hit.summaryText,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (hit.assetType == AssetType.PDF) {
                val pageLabel = hit.rankedPdfPageNumber?.let { MeaningSearchCopy.rankedPdfPageLabel(it) }
                    ?: hit.citedPdfPageNumber?.let { MeaningSearchCopy.citedPdfPageLabel(it) }
                pageLabel?.let { label ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showWhy = !showWhy }) {
                Text(if (showWhy) "Hide why" else "Why this result?")
            }
            if (showWhy) {
                MeaningWhyPanel(explanation = MeaningWhy.explain(hit, query))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { onOpenOriginal(hit) },
                enabled = canOpen,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(MeaningSearchCopy.OPEN_ORIGINAL_LABEL)
            }
            if (hit.assetType == AssetType.PDF) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = MeaningSearchCopy.openOriginalPdfHint(
                        citedPdfPageNumber = hit.citedPdfPageNumber,
                        rankedPdfPageNumber = hit.rankedPdfPageNumber,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Why is a coloured panel, not another muted paragraph. Matched words are
 * bold; missing words are named in the error colour so a partial tier cannot
 * be mistaken for an exact one. Colour is not the only signal — the sentence
 * still says "Has" / "Does not have".
 */
@Composable
private fun MeaningWhyPanel(explanation: MeaningWhy.Explanation) {
    val colors = MaterialTheme.colorScheme
    val spoken = MeaningWhy.plainText(explanation)
    Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = spoken },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(colors.primary),
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = coverageAnnotated(explanation),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val cited = explanation.citedLine
                if (cited != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "On this line: \"$cited\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.82f),
                    )
                }
            }
        }
    }
}

@Composable
private fun coverageAnnotated(explanation: MeaningWhy.Explanation) =
    buildAnnotatedString {
        val matchedStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val missingStyle = SpanStyle(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
        )
        if (explanation.matched.isEmpty()) {
            append("None of those words are in this file.")
        } else {
            append("Has ")
            appendQuoted(explanation.matched, matchedStyle)
            append(".")
        }
        if (explanation.missing.isNotEmpty()) {
            append(" Does not have ")
            appendQuoted(explanation.missing, missingStyle)
            append(".")
        }
    }

private fun AnnotatedString.Builder.appendQuoted(
    words: List<String>,
    style: SpanStyle,
) {
    words.forEachIndexed { index, word ->
        if (index > 0) {
            append(if (index == words.lastIndex) " and " else ", ")
        }
        withStyle(style) { append("\"$word\"") }
    }
}
