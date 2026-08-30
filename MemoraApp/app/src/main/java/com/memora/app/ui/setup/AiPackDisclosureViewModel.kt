package com.memora.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.intelligence.ActivateOfflineEmbeddingPackContainer
import com.memora.app.application.intelligence.ActivateOfflineEmbeddingPackResult
import com.memora.app.application.intelligence.ApplyMig05EvidenceSearchCutover
import com.memora.app.application.intelligence.DownloadOnDeviceEmbeddingModel
import com.memora.app.application.intelligence.DownloadOnDeviceEmbeddingModelResult
import com.memora.app.application.intelligence.IndexMemoryEmbeddings
import com.memora.app.application.intelligence.IndexMemoryEmbeddingsResult
import com.memora.app.application.intelligence.IndexOcrEvidenceEmbeddings
import com.memora.app.application.intelligence.IndexOcrEvidenceEmbeddingsResult
import com.memora.app.application.intelligence.IndexPdfPageEmbeddings
import com.memora.app.application.intelligence.IndexPdfPageEmbeddingsResult
import com.memora.app.application.intelligence.LoadCorpusCompleteness
import com.memora.app.application.intelligence.MeaningIndexBatchLimits
import com.memora.app.application.intelligence.MemoryEmbeddingCandidate
import com.memora.app.application.intelligence.OcrEvidenceEmbeddingCandidate
import com.memora.app.application.intelligence.PdfPageEmbeddingCandidate
import com.memora.app.application.intelligence.ResolveMeaningPdfOpenPage
import com.memora.app.data.intelligence.MediaPipeEmbeddingEngine
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.extraction.SavedPdfPageTextSource
import com.memora.app.domain.intelligence.AiPackInstallLedger
import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.AiPackManager
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelStore
import com.memora.app.domain.memory.CorpusCompletenessSnapshot
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.ui.search.CorpusHonestyCopy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AiPackDisclosureUiState(
    val statusBody: String,
    val corpusCompletenessBody: String? = null,
    val showAcknowledge: Boolean,
    val showActivate: Boolean,
    val showDownloadModel: Boolean,
    val showBuildIndex: Boolean,
    val isBusy: Boolean = false,
    val progressFeedback: String? = null,
    val feedbackMessage: String? = null,
)

@HiltViewModel
class AiPackDisclosureViewModel @Inject constructor(
    private val ledger: AiPackInstallLedger,
    private val aiPackManager: AiPackManager,
    private val activateOfflinePack: ActivateOfflineEmbeddingPackContainer,
    private val downloadModel: DownloadOnDeviceEmbeddingModel,
    private val modelStore: OnDeviceEmbeddingModelStore,
    private val embeddingEngine: EmbeddingEngine,
    private val mediaPipeEmbeddingEngine: MediaPipeEmbeddingEngine,
    private val indexMemoryEmbeddings: IndexMemoryEmbeddings,
    private val indexPdfPageEmbeddings: IndexPdfPageEmbeddings,
    private val indexOcrEvidenceEmbeddings: IndexOcrEvidenceEmbeddings,
    private val savedPdfPages: SavedPdfPageTextSource,
    private val memoryRepository: MemoryRepository,
    private val loadCorpusCompleteness: LoadCorpusCompleteness,
    private val applyMig05EvidenceSearchCutover: ApplyMig05EvidenceSearchCutover,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(buildState())
    val uiState: StateFlow<AiPackDisclosureUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableUiState.value = withContext(Dispatchers.IO) { buildStateWithCorpus() }
        }
    }

    fun onAcknowledgeRequested() {
        if (mutableUiState.value.isBusy || !mutableUiState.value.showAcknowledge) return
        setBusy()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ledger.acknowledgeDisclosure(
                    EmbeddingFirstAiPackTrack.plannedDisclosure(
                        atEpochMs = System.currentTimeMillis(),
                    ),
                )
            }
            mutableUiState.value = withContext(Dispatchers.IO) {
                buildStateWithCorpus(feedbackMessage = AiPackDisclosureCopy.FEEDBACK_ACKNOWLEDGED)
            }
        }
    }

    fun onActivateRequested() {
        if (mutableUiState.value.isBusy || !mutableUiState.value.showActivate) return
        setBusy()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                activateOfflinePack(nowEpochMs = System.currentTimeMillis())
            }
            val feedback = when (result) {
                ActivateOfflineEmbeddingPackResult.Activated ->
                    AiPackDisclosureCopy.FEEDBACK_ACTIVATED
                ActivateOfflineEmbeddingPackResult.AlreadyActive ->
                    AiPackDisclosureCopy.FEEDBACK_ALREADY_ACTIVE
                ActivateOfflineEmbeddingPackResult.DisclosureRequired ->
                    AiPackDisclosureCopy.FEEDBACK_DISCLOSURE_REQUIRED
                is ActivateOfflineEmbeddingPackResult.Failed -> result.reason
            }
            mutableUiState.value = withContext(Dispatchers.IO) {
                buildStateWithCorpus(feedbackMessage = feedback)
            }
        }
    }

    fun onDownloadModelRequested() {
        if (mutableUiState.value.isBusy || !mutableUiState.value.showDownloadModel) return
        setBusy()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { downloadModel() }
            withContext(Dispatchers.IO) { mediaPipeEmbeddingEngine.reset() }
            val feedback = when (result) {
                DownloadOnDeviceEmbeddingModelResult.Installed ->
                    AiPackDisclosureCopy.FEEDBACK_MODEL_INSTALLED
                DownloadOnDeviceEmbeddingModelResult.AlreadyInstalled ->
                    AiPackDisclosureCopy.FEEDBACK_MODEL_ALREADY
                DownloadOnDeviceEmbeddingModelResult.DisclosureRequired ->
                    AiPackDisclosureCopy.FEEDBACK_DISCLOSURE_REQUIRED
                is DownloadOnDeviceEmbeddingModelResult.Failed -> result.reason
            }
            mutableUiState.value = withContext(Dispatchers.IO) {
                buildStateWithCorpus(feedbackMessage = feedback)
            }
        }
    }

    fun onBuildIndexRequested() {
        if (mutableUiState.value.isBusy || !mutableUiState.value.showBuildIndex) return
        setBusy(progressFeedback = AiPackDisclosureCopy.PROGRESS_PREPARING)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val limit = MeaningIndexBatchLimits.MAX_MEMORIES_PER_TAP
            val (readyTotal, summaries) = withContext(Dispatchers.IO) {
                memoryRepository.countMeaningIndexCandidates() to
                    memoryRepository.listMeaningIndexSummaries(limit = limit)
            }
            if (summaries.isEmpty()) {
                mutableUiState.value = withContext(Dispatchers.IO) {
                    buildStateWithCorpus(feedbackMessage = AiPackDisclosureCopy.FEEDBACK_INDEX_EMPTY)
                }
                return@launch
            }
            val remainingAfterBatch = (readyTotal - summaries.size).coerceAtLeast(0)
            val candidates = summaries.map { summary ->
                MemoryEmbeddingCandidate(
                    revisionId = summary.revisionId,
                    memoryId = summary.memoryId,
                    summaryText = summary.summaryText,
                )
            }
            mutableUiState.value = mutableUiState.value.copy(
                progressFeedback = AiPackDisclosureCopy.progressSummaries(
                    processed = 0,
                    total = candidates.size,
                ),
            )
            val result = withContext(Dispatchers.IO) {
                indexMemoryEmbeddings(
                    candidates = candidates,
                    nowEpochMs = now,
                    onProgress = { processed, total ->
                        mutableUiState.value = mutableUiState.value.copy(
                            progressFeedback = AiPackDisclosureCopy.progressSummaries(
                                processed = processed,
                                total = total,
                            ),
                        )
                    },
                )
            }
            val evidenceIndexResult = when (result) {
                is IndexMemoryEmbeddingsResult.EngineUnavailable -> null
                is IndexMemoryEmbeddingsResult.Completed -> withContext(Dispatchers.IO) {
                    val revisionIds = candidates.map { it.revisionId }
                    val lookups = memoryRepository.findCurrentReadyMeaningLookups(revisionIds)
                    val evidenceIdsByRevision =
                        memoryRepository.findPdfPageEvidenceIds(revisionIds)
                    val ocrEvidenceByRevision =
                        memoryRepository.findOcrTextEvidenceForEmbedding(revisionIds)
                    val noteEvidenceByRevision =
                        memoryRepository.findNoteTextEvidenceForEmbedding(revisionIds)
                    val pageCandidates = candidates.flatMap { summary ->
                        val lookup = lookups[summary.revisionId] ?: return@flatMap emptyList()
                        if (lookup.assetType != AssetType.PDF) return@flatMap emptyList()
                        val pageEvidenceIds =
                            evidenceIdsByRevision[summary.revisionId].orEmpty()
                        savedPdfPages.listCurrentVerifiedPages(
                            sourceId = lookup.sourceId.value,
                            sourceAssetKey = lookup.sourceAssetKey.value,
                        )
                            .take(ResolveMeaningPdfOpenPage.MAX_PAGES_TO_SCORE)
                            .map { page ->
                                PdfPageEmbeddingCandidate(
                                    revisionId = summary.revisionId,
                                    memoryId = summary.memoryId,
                                    pageNumber = page.pageNumber,
                                    pageText = page.text,
                                    evidenceId = pageEvidenceIds[page.pageNumber],
                                )
                            }
                    }
                    val pdfResult = if (pageCandidates.isEmpty()) {
                        IndexPdfPageEmbeddingsResult.Completed(0, 0, 0)
                    } else {
                        mutableUiState.value = mutableUiState.value.copy(
                            progressFeedback = AiPackDisclosureCopy.progressPages(
                                processed = 0,
                                total = pageCandidates.size,
                            ),
                        )
                        indexPdfPageEmbeddings(
                            candidates = pageCandidates,
                            nowEpochMs = now,
                            onProgress = { processed, total ->
                                mutableUiState.value = mutableUiState.value.copy(
                                    progressFeedback = AiPackDisclosureCopy.progressPages(
                                        processed = processed,
                                        total = total,
                                    ),
                                )
                            },
                        )
                    }
                    val ocrCandidates = candidates.flatMap { summary ->
                        val lookup = lookups[summary.revisionId] ?: return@flatMap emptyList()
                        if (lookup.assetType != AssetType.PHOTO &&
                            lookup.assetType != AssetType.SCREENSHOT
                        ) {
                            return@flatMap emptyList()
                        }
                        ocrEvidenceByRevision[summary.revisionId].orEmpty().map { row ->
                            OcrEvidenceEmbeddingCandidate(
                                revisionId = summary.revisionId,
                                memoryId = summary.memoryId,
                                excerpt = row.excerpt,
                                evidenceId = row.evidenceId,
                            )
                        }
                    }
                    val ocrResult = if (ocrCandidates.isEmpty()) {
                        IndexOcrEvidenceEmbeddingsResult.Completed(0, 0, 0)
                    } else {
                        mutableUiState.value = mutableUiState.value.copy(
                            progressFeedback = AiPackDisclosureCopy.progressOcrEvidence(
                                processed = 0,
                                total = ocrCandidates.size,
                            ),
                        )
                        indexOcrEvidenceEmbeddings(
                            candidates = ocrCandidates,
                            nowEpochMs = now,
                            onProgress = { processed, total ->
                                mutableUiState.value = mutableUiState.value.copy(
                                    progressFeedback = AiPackDisclosureCopy.progressOcrEvidence(
                                        processed = processed,
                                        total = total,
                                    ),
                                )
                            },
                        )
                    }
                    val noteCandidates = candidates.flatMap { summary ->
                        val lookup = lookups[summary.revisionId] ?: return@flatMap emptyList()
                        if (lookup.assetType != AssetType.NOTE) return@flatMap emptyList()
                        noteEvidenceByRevision[summary.revisionId].orEmpty().map { row ->
                            OcrEvidenceEmbeddingCandidate(
                                revisionId = summary.revisionId,
                                memoryId = summary.memoryId,
                                excerpt = row.excerpt,
                                evidenceId = row.evidenceId,
                            )
                        }
                    }
                    val noteResult = if (noteCandidates.isEmpty()) {
                        IndexOcrEvidenceEmbeddingsResult.Completed(0, 0, 0)
                    } else {
                        mutableUiState.value = mutableUiState.value.copy(
                            progressFeedback = AiPackDisclosureCopy.progressNoteEvidence(
                                processed = 0,
                                total = noteCandidates.size,
                            ),
                        )
                        indexOcrEvidenceEmbeddings(
                            candidates = noteCandidates,
                            nowEpochMs = now,
                            onProgress = { processed, total ->
                                mutableUiState.value = mutableUiState.value.copy(
                                    progressFeedback = AiPackDisclosureCopy.progressNoteEvidence(
                                        processed = processed,
                                        total = total,
                                    ),
                                )
                            },
                        )
                    }
                    MeaningEvidenceIndexBatchResults(
                        pdf = pdfResult,
                        ocr = ocrResult,
                        note = noteResult,
                    )
                }
            }
            when (val availability = embeddingEngine.availability()) {
                is CapabilityAvailability.Available ->
                    withContext(Dispatchers.IO) {
                        applyMig05EvidenceSearchCutover.ensureApplied(
                            model = availability.model,
                            nowEpochMs = now,
                        )
                    }
                is CapabilityAvailability.Unavailable -> Unit
            }
            val feedback = when (result) {
                is IndexMemoryEmbeddingsResult.EngineUnavailable ->
                    AiPackDisclosureCopy.FEEDBACK_INDEX_UNAVAILABLE
                is IndexMemoryEmbeddingsResult.Completed -> {
                    val pagePart = when (val batch = evidenceIndexResult) {
                        is MeaningEvidenceIndexBatchResults -> when (val pdfPart = batch.pdf) {
                            is IndexPdfPageEmbeddingsResult.Completed ->
                                " Pages indexed ${pdfPart.indexed} " +
                                    "(skipped ${pdfPart.skippedUnchanged}, failed ${pdfPart.failed})."
                            is IndexPdfPageEmbeddingsResult.EngineUnavailable ->
                                " PDF page index unavailable."
                        }
                        null -> ""
                    }
                    val ocrEvidencePart = when (val batch = evidenceIndexResult) {
                        is MeaningEvidenceIndexBatchResults -> when (val ocrPart = batch.ocr) {
                            is IndexOcrEvidenceEmbeddingsResult.Completed ->
                                " OCR evidence indexed ${ocrPart.indexed} " +
                                    "(skipped ${ocrPart.skippedUnchanged}, failed ${ocrPart.failed})."
                            is IndexOcrEvidenceEmbeddingsResult.EngineUnavailable ->
                                " OCR evidence index unavailable."
                        }
                        null -> ""
                    }
                    val noteEvidencePart = when (val batch = evidenceIndexResult) {
                        is MeaningEvidenceIndexBatchResults -> when (val notePart = batch.note) {
                            is IndexOcrEvidenceEmbeddingsResult.Completed ->
                                " Note evidence indexed ${notePart.indexed} " +
                                    "(skipped ${notePart.skippedUnchanged}, failed ${notePart.failed})."
                            is IndexOcrEvidenceEmbeddingsResult.EngineUnavailable ->
                                " Note evidence index unavailable."
                        }
                        null -> ""
                    }
                    val remainingHint = if (remainingAfterBatch > 0) {
                        " ${AiPackDisclosureCopy.remainingBatchHint(remainingAfterBatch)}"
                    } else {
                        ""
                    }
                    AiPackDisclosureCopy.FEEDBACK_INDEX_BUILT_PREFIX +
                        "${result.indexed} memories (skipped ${result.skippedUnchanged}, " +
                        "failed ${result.failed}).$pagePart$ocrEvidencePart$noteEvidencePart$remainingHint " +
                        "Use Find by meaning on Welcome next."
                }
            }
            mutableUiState.value = withContext(Dispatchers.IO) {
                buildStateWithCorpus(feedbackMessage = feedback)
            }
        }
    }

    fun onDerivedDataCleared() {
        mediaPipeEmbeddingEngine.reset()
        refresh()
    }

    private fun setBusy(progressFeedback: String? = null) {
        mutableUiState.value = mutableUiState.value.copy(
            isBusy = true,
            progressFeedback = progressFeedback,
            feedbackMessage = null,
            showAcknowledge = false,
            showActivate = false,
            showDownloadModel = false,
            showBuildIndex = false,
        )
    }

    private suspend fun buildStateWithCorpus(
        feedbackMessage: String? = null,
    ): AiPackDisclosureUiState {
        val embeddingAvailable =
            embeddingEngine.availability() is CapabilityAvailability.Available
        val snapshot = if (embeddingAvailable) loadCorpusCompleteness() else null
        return buildState(feedbackMessage = feedbackMessage, corpusSnapshot = snapshot)
    }

    private fun buildState(
        feedbackMessage: String? = null,
        corpusSnapshot: CorpusCompletenessSnapshot? = null,
    ): AiPackDisclosureUiState {
        val packId = EmbeddingFirstAiPackTrack.PLANNED_PACK_ID
        val entry = ledger.entry(packId)
        val installationState = aiPackManager.installationState(packId)
        val disclosed = entry?.disclosureAcknowledgedAtEpochMs != null
        val modelInstalled = modelStore.isInstalled()
        val embeddingAvailable =
            embeddingEngine.availability() is CapabilityAvailability.Available
        val canActivate = disclosed &&
            installationState != AiPackInstallState.ACTIVE &&
            installationState != AiPackInstallState.VERIFYING
        return AiPackDisclosureUiState(
            statusBody = AiPackDisclosureCopy.statusBody(
                installationState = installationState,
                disclosureAcknowledged = disclosed,
                modelInstalled = modelInstalled,
                embeddingAvailable = embeddingAvailable,
            ),
            showAcknowledge = !disclosed &&
                installationState == AiPackInstallState.NOT_INSTALLED,
            showActivate = canActivate,
            showDownloadModel = disclosed && !modelInstalled,
            showBuildIndex = embeddingAvailable,
            corpusCompletenessBody = corpusSnapshot?.let { CorpusHonestyCopy.aiPackCorpusLine(it) },
            isBusy = false,
            progressFeedback = null,
            feedbackMessage = feedbackMessage,
        )
    }
}

private data class MeaningEvidenceIndexBatchResults(
    val pdf: IndexPdfPageEmbeddingsResult,
    val ocr: IndexOcrEvidenceEmbeddingsResult,
    val note: IndexOcrEvidenceEmbeddingsResult,
)
