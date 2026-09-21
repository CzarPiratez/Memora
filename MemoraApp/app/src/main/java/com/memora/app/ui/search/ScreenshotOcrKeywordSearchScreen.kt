package com.memora.app.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.memora.app.application.find.FindHiddenPolicy
import com.memora.app.application.find.FindThumbnailRequest
import com.memora.app.application.find.FindThumbnailResult
import com.memora.app.application.images.ScreenshotOcrKeywordSearchHit
import com.memora.app.application.preview.OriginalPreviewReloadRequest
import com.memora.app.application.preview.PreviewZoomPolicy

@Composable
fun ScreenshotOcrKeywordSearchScreen(
    uiState: ScreenshotOcrKeywordSearchUiState,
    onQueryChanged: (String) -> Unit,
    onQueryCleared: () -> Unit,
    onSearch: () -> Unit,
    onSearchCancelled: () -> Unit,
    onOpenOriginalScreenshot: (ScreenshotOcrKeywordSearchHit) -> Unit,
    onDismissOpenFeedback: () -> Unit,
    onThumbnailLoaded: (FindOpenTarget, FindThumbnailResult) -> Unit,
    findHidden: FindHiddenHost,
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
            Text(ScreenshotOcrKeywordSearchCopy.BACK_LABEL)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = ScreenshotOcrKeywordSearchCopy.SCREEN_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = ScreenshotOcrKeywordSearchCopy.SCOPE_BODY,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (val readiness = uiState.readiness) {
                ScreenshotOcrKeywordSearchReadinessUi.Loading ->
                    ScreenshotOcrKeywordSearchCopy.READINESS_LOADING_BODY
                ScreenshotOcrKeywordSearchReadinessUi.CouldNotLoad ->
                    ScreenshotOcrKeywordSearchCopy.READINESS_COULD_NOT_LOAD_BODY
                is ScreenshotOcrKeywordSearchReadinessUi.Ready ->
                    ScreenshotOcrKeywordSearchCopy.readinessBody(
                        screenshotCount = readiness.snapshot.screenshotCount,
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
            label = { Text(ScreenshotOcrKeywordSearchCopy.QUERY_LABEL) },
            singleLine = true,
            enabled = uiState.phase !is ScreenshotOcrKeywordSearchPhase.Searching &&
                uiState.openFeedback !is ScreenshotOpenFeedbackUi.Opening,
            trailingIcon = {
                if (uiState.canClearQuery) {
                    TextButton(onClick = onQueryCleared) {
                        Text(ScreenshotOcrKeywordSearchCopy.CLEAR_QUERY_LABEL)
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
            Text(ScreenshotOcrKeywordSearchCopy.SEARCH_LABEL)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSearchCancelled,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canCancelSearch,
        ) {
            Text(ScreenshotOcrKeywordSearchCopy.CANCEL_SEARCH_LABEL)
        }
        Spacer(modifier = Modifier.height(20.dp))
        when (val phase = uiState.phase) {
            ScreenshotOcrKeywordSearchPhase.Idle -> {
                if (uiState.query.isBlank()) {
                    Text(
                        text = ScreenshotOcrKeywordSearchCopy.EMPTY_QUERY_BODY,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            ScreenshotOcrKeywordSearchPhase.EmptyQuery -> Text(
                text = ScreenshotOcrKeywordSearchCopy.EMPTY_QUERY_BODY,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            ScreenshotOcrKeywordSearchPhase.Searching -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = ScreenshotOcrKeywordSearchCopy.SEARCHING_BODY
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(modifier = Modifier.clearAndSetSemantics { })
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = ScreenshotOcrKeywordSearchCopy.SEARCHING_BODY,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            is ScreenshotOcrKeywordSearchPhase.NoMatches -> Text(
                text = ScreenshotOcrKeywordSearchCopy.noMatchesBody(phase.query),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            is ScreenshotOcrKeywordSearchPhase.NothingSavedToSearch -> Text(
                text = ScreenshotOcrKeywordSearchCopy.NOTHING_SAVED_BODY,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            ScreenshotOcrKeywordSearchPhase.SearchCouldNotFinish -> Text(
                text = ScreenshotOcrKeywordSearchCopy.SEARCH_COULD_NOT_FINISH_BODY,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            is ScreenshotOcrKeywordSearchPhase.Results -> {
                val hiddenIds = findHidden.hiddenIdentities()
                val visible = FindHiddenPolicy.visible(phase.hits, hiddenIds) {
                    it.recall.openTarget().asIdentity()
                }
                if (FindHiddenPolicy.rankedHitsAreAllHidden(phase.hits.size, visible.size)) {
                    Text(
                        text = FindHiddenCopy.ALL_HIDDEN_BODY,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                } else {
                    Text(
                        text = ScreenshotOcrKeywordSearchCopy.resultsSummary(
                            query = phase.query,
                            matchCount = visible.size,
                            limitReached = phase.limitReached,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    visible.forEachIndexed { index, hit ->
                        ScreenshotOcrKeywordHitCard(
                            hit = hit,
                            query = phase.query,
                            index = index,
                            openState = uiState.openStateFor(hit.recall.openTarget()),
                            availability = uiState.availabilityFor(hit.recall.openTarget()),
                            onOpenOriginal = { onOpenOriginalScreenshot(hit) },
                            onDismissOpenFeedback = onDismissOpenFeedback,
                            onThumbnailLoaded = onThumbnailLoaded,
                            onHideFromFind = {
                                findHidden.onHide(hit.recall.openTarget(), hit.label)
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
        val hideRanked = (uiState.phase as? ScreenshotOcrKeywordSearchPhase.Results)
            ?.hits
            ?.map { it.recall.openTarget().asIdentity() }
            .orEmpty()
        FindHiddenResultsFooter(
            rankedIdentities = hideRanked,
            findHidden = findHidden,
        )
    }
}

@Composable
fun ScreenshotOriginalPreviewScreen(
    preview: ScreenshotOriginalPreviewUi,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    OriginalPreviewScaffold(
        onClose = onClose,
        closeLabel = ScreenshotOcrKeywordSearchCopy.CLOSE_PREVIEW_LABEL,
        title = ScreenshotOcrKeywordSearchCopy.PREVIEW_TITLE,
        subtitle = preview.screenshotLabel,
        scopeBody = ScreenshotOcrKeywordSearchCopy.PREVIEW_SCOPE_BODY,
        contentDescription = ScreenshotOcrKeywordSearchCopy.previewImageContentDescription(
            screenshotLabel = preview.screenshotLabel,
        ),
        reloadRequest = OriginalPreviewReloadRequest.Screenshot(
            sourceId = preview.sourceId,
            sourceAssetKey = preview.sourceAssetKey,
            screenshotLabel = preview.screenshotLabel,
        ),
        initialEdgePx = PreviewZoomPolicy.IMAGE_INITIAL_EDGE_PX,
        widthPx = preview.widthPx,
        heightPx = preview.heightPx,
        argb8888 = preview.argb8888,
        modifier = modifier,
    )
}

@Composable
private fun ScreenshotOcrKeywordHitCard(
    hit: ScreenshotOcrKeywordSearchHit,
    query: String,
    index: Int,
    openState: FindCardOpenState,
    availability: com.memora.app.domain.asset.SourceAvailabilityStatus,
    onOpenOriginal: () -> Unit,
    onDismissOpenFeedback: () -> Unit,
    onThumbnailLoaded: (FindOpenTarget, FindThumbnailResult) -> Unit,
    onHideFromFind: () -> Unit,
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
            Row {
                FindResultThumbnail(
                    request = FindThumbnailRequest.fromRecall(hit.recall),
                    onLoaded = { onThumbnailLoaded(hit.recall.openTarget(), it) },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    FindHitLabel(text = hit.label)
                }
            }
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
                text = ScreenshotOcrKeywordSearchCopy.OPEN_ORIGINAL_SCREENSHOT_HINT,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FindOpenOriginalButton(
                copy = ScreenshotOcrKeywordSearchCopy.OPEN_ORIGINAL,
                state = openState,
                availability = availability,
                onOpen = onOpenOriginal,
                onDismissFailure = onDismissOpenFeedback,
                onHideFromFind = onHideFromFind,
            )
            Spacer(modifier = Modifier.height(8.dp))
            WhyDisclosure(
                expanded = whyExpanded,
                onExpandedChange = { whyExpanded = it },
                presentation = { CanonicalRecallWhyCopy.present(hit.recall, query) },
            )
        }
    }
}
