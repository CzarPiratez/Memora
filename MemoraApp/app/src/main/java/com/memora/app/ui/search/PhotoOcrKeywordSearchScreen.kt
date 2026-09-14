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
import com.memora.app.application.find.FindThumbnailRequest
import com.memora.app.application.images.PhotoOcrKeywordSearchHit
import com.memora.app.application.preview.OriginalPreviewReloadRequest
import com.memora.app.application.preview.PreviewZoomPolicy

@Composable
fun PhotoOcrKeywordSearchScreen(
    uiState: PhotoOcrKeywordSearchUiState,
    onQueryChanged: (String) -> Unit,
    onQueryCleared: () -> Unit,
    onSearch: () -> Unit,
    onSearchCancelled: () -> Unit,
    onOpenOriginalPhoto: (PhotoOcrKeywordSearchHit) -> Unit,
    onDismissOpenFeedback: () -> Unit,
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
                Text(
                    PhotoOcrKeywordSearchCopy.resultsSummary(
                        phase.query,
                        phase.hits.size,
                        phase.limitReached,
                    ),
                )
                when (uiState.openFeedback) {
                    PhotoOpenFeedbackUi.Opening -> Text(PhotoOcrKeywordSearchCopy.OPENING_BODY)
                    PhotoOpenFeedbackUi.SourceUnavailable ->
                        OpenError(PhotoOcrKeywordSearchCopy.SOURCE_UNAVAILABLE_BODY, onDismissOpenFeedback)
                    PhotoOpenFeedbackUi.CouldNotOpen ->
                        OpenError(PhotoOcrKeywordSearchCopy.COULD_NOT_OPEN_BODY, onDismissOpenFeedback)
                    PhotoOpenFeedbackUi.None -> Unit
                }
                phase.hits.forEach { hit ->
                    PhotoHitCard(hit, phase.query) { onOpenOriginalPhoto(hit) }
                }
            }
        }
    }
}

@Composable
private fun PhotoHitCard(hit: PhotoOcrKeywordSearchHit, query: String, onOpen: () -> Unit) {
    var why by remember(hit.sourceAssetKey, query) { mutableStateOf(false) }
    Spacer(Modifier.height(12.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                FindResultThumbnail(request = FindThumbnailRequest.fromRecall(hit.recall))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(hit.label, fontWeight = FontWeight.SemiBold)
                    Text(hit.excerpt)
                }
            }
            Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Text(PhotoOcrKeywordSearchCopy.OPEN_ORIGINAL_LABEL)
            }
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
private fun OpenError(body: String, dismiss: () -> Unit) {
    Text(body, color = MaterialTheme.colorScheme.error)
    TextButton(onClick = dismiss) { Text(PhotoOcrKeywordSearchCopy.DISMISS_LABEL) }
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
