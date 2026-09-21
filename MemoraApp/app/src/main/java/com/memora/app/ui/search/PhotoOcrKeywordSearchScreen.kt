package com.memora.app.ui.search

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memora.app.application.find.FindHiddenPolicy
import com.memora.app.application.find.FindThumbnailRequest
import com.memora.app.application.find.FindThumbnailResult
import com.memora.app.application.images.PhotoOcrKeywordSearchHit
import com.memora.app.application.preview.OriginalPreviewReloadRequest
import com.memora.app.application.preview.PreviewZoomPolicy
import com.memora.app.domain.asset.SourceAvailabilityStatus

@Composable
fun PhotoOcrKeywordSearchScreen(
    uiState: PhotoOcrKeywordSearchUiState,
    onQueryChanged: (String) -> Unit,
    onQueryCleared: () -> Unit,
    onSearch: () -> Unit,
    onSearchCancelled: () -> Unit,
    onOpenOriginalPhoto: (PhotoOcrKeywordSearchHit) -> Unit,
    onDismissOpenFeedback: () -> Unit,
    onThumbnailLoaded: (FindOpenTarget, FindThumbnailResult) -> Unit,
    findHidden: FindHiddenHost,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Button(onClick = onBack) { Text(PhotoOcrKeywordSearchCopy.BACK_LABEL) }
        Spacer(Modifier.height(20.dp))
        Text(
            PhotoOcrKeywordSearchCopy.SCREEN_TITLE,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(PhotoOcrKeywordSearchCopy.SCOPE_BODY)
        Spacer(Modifier.height(8.dp))
        Text(
            when (val ready = uiState.readiness) {
                PhotoOcrKeywordSearchReadinessUi.Loading -> "Checking saved photo text…"
                PhotoOcrKeywordSearchReadinessUi.CouldNotLoad -> "Could not check saved photo text."
                is PhotoOcrKeywordSearchReadinessUi.Ready ->
                    PhotoOcrKeywordSearchCopy.readinessBody(ready.snapshot.photoCount)
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChanged,
            label = { Text(PhotoOcrKeywordSearchCopy.QUERY_LABEL) },
            trailingIcon = {
                if (uiState.query.isNotBlank()) {
                    TextButton(onClick = onQueryCleared) {
                        Text(PhotoOcrKeywordSearchCopy.CLEAR_QUERY_LABEL)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSearch, enabled = uiState.canSubmitSearch, modifier = Modifier.fillMaxWidth()) {
            Text(PhotoOcrKeywordSearchCopy.SEARCH_LABEL)
        }
        OutlinedButton(
            onClick = onSearchCancelled,
            enabled = uiState.canCancelSearch,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(PhotoOcrKeywordSearchCopy.CANCEL_SEARCH_LABEL)
        }
        Spacer(Modifier.height(16.dp))
        when (val phase = uiState.phase) {
            PhotoOcrKeywordSearchPhase.Idle -> if (uiState.query.isBlank()) {
                Text(PhotoOcrKeywordSearchCopy.EMPTY_QUERY_BODY)
            }
            PhotoOcrKeywordSearchPhase.Searching -> {
                CircularProgressIndicator()
                Text(PhotoOcrKeywordSearchCopy.SEARCHING_BODY)
            }
            is PhotoOcrKeywordSearchPhase.NoMatches ->
                Text(PhotoOcrKeywordSearchCopy.noMatchesBody(phase.query))
            is PhotoOcrKeywordSearchPhase.NothingSavedToSearch ->
                Text(PhotoOcrKeywordSearchCopy.NOTHING_SAVED_BODY)
            PhotoOcrKeywordSearchPhase.SearchCouldNotFinish ->
                Text(PhotoOcrKeywordSearchCopy.SEARCH_COULD_NOT_FINISH_BODY)
            is PhotoOcrKeywordSearchPhase.Results -> {
                val hiddenIds = findHidden.hiddenIdentities()
                val visible = FindHiddenPolicy.visible(phase.hits, hiddenIds) {
                    it.recall.openTarget().asIdentity()
                }
                if (FindHiddenPolicy.rankedHitsAreAllHidden(phase.hits.size, visible.size)) {
                    Text(FindHiddenCopy.ALL_HIDDEN_BODY)
                } else {
                    Text(
                        PhotoOcrKeywordSearchCopy.resultsSummary(
                            phase.query,
                            visible.size,
                            phase.limitReached,
                        ),
                    )
                    when (uiState.openFeedback) {
                        PhotoOpenFeedbackUi.Opening,
                        PhotoOpenFeedbackUi.SourceUnavailable,
                        PhotoOpenFeedbackUi.CouldNotOpen,
                        PhotoOpenFeedbackUi.None -> Unit
                    }
                    visible.forEach { hit ->
                        PhotoHitCard(
                            hit = hit,
                            query = phase.query,
                            openState = uiState.openStateFor(hit.recall.openTarget()),
                            availability = uiState.availabilityFor(hit.recall.openTarget()),
                            onOpen = { onOpenOriginalPhoto(hit) },
                            onDismissOpenFeedback = onDismissOpenFeedback,
                            onThumbnailLoaded = onThumbnailLoaded,
                            onHideFromFind = {
                                findHidden.onHide(hit.recall.openTarget(), hit.label)
                            },
                        )
                    }
                }
            }
        }
        val hideRanked = (uiState.phase as? PhotoOcrKeywordSearchPhase.Results)
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
private fun PhotoHitCard(
    hit: PhotoOcrKeywordSearchHit,
    query: String,
    openState: FindCardOpenState,
    availability: SourceAvailabilityStatus,
    onOpen: () -> Unit,
    onDismissOpenFeedback: () -> Unit,
    onThumbnailLoaded: (FindOpenTarget, FindThumbnailResult) -> Unit,
    onHideFromFind: () -> Unit,
) {
    var why by remember(hit.sourceAssetKey, query) { mutableStateOf(false) }
    Spacer(Modifier.height(12.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                FindResultThumbnail(
                    request = FindThumbnailRequest.fromRecall(hit.recall),
                    onLoaded = { onThumbnailLoaded(hit.recall.openTarget(), it) },
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    FindHitLabel(text = hit.label)
                    Text(hit.excerpt)
                }
            }
            Spacer(Modifier.height(8.dp))
            FindOpenOriginalButton(
                copy = PhotoOcrKeywordSearchCopy.OPEN_ORIGINAL,
                state = openState,
                availability = availability,
                onOpen = onOpen,
                onDismissFailure = onDismissOpenFeedback,
                onHideFromFind = onHideFromFind,
            )
            Spacer(Modifier.height(8.dp))
            WhyDisclosure(
                expanded = why,
                onExpandedChange = { why = it },
                presentation = { CanonicalRecallWhyCopy.present(hit.recall, query) },
            )
        }
    }
}

@Composable
fun PhotoOriginalPreviewScreen(
    preview: PhotoOriginalPreviewUi,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    OriginalPreviewScaffold(
        onClose = onClose,
        closeLabel = PhotoOcrKeywordSearchCopy.CLOSE_PREVIEW_LABEL,
        title = PhotoOcrKeywordSearchCopy.PREVIEW_TITLE,
        subtitle = preview.photoLabel,
        scopeBody = PhotoOcrKeywordSearchCopy.PREVIEW_SCOPE_BODY,
        contentDescription = PhotoOcrKeywordSearchCopy.previewImageContentDescription(
            preview.photoLabel,
        ),
        reloadRequest = OriginalPreviewReloadRequest.Photo(
            sourceId = preview.sourceId,
            sourceAssetKey = preview.sourceAssetKey,
            photoLabel = preview.photoLabel,
        ),
        initialEdgePx = PreviewZoomPolicy.IMAGE_INITIAL_EDGE_PX,
        widthPx = preview.widthPx,
        heightPx = preview.heightPx,
        argb8888 = preview.argb8888,
        modifier = modifier,
    )
}
