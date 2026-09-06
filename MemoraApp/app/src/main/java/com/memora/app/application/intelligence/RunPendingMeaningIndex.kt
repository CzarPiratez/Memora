package com.memora.app.application.intelligence

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.extraction.SavedPdfPageTextSource
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.memory.MemoryEmbeddingSummary
import com.memora.app.domain.memory.MemoryRepository
import javax.inject.Inject

/**
 * One bounded meaning-index drain over READY / STALE-reindex memories.
 *
 * Selects the next batch, indexes summaries then evidence (PDF pages, OCR,
 * notes), applies the MIG-05 evidence cutover, and reports whether more work
 * remains. UI and a future I3 worker must both call this — not a second
 * indexing path. Does not schedule WorkManager (I3). Does not change Find.
 */
class RunPendingMeaningIndex @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val memoryRepository: MemoryRepository,
    private val indexMemoryEmbeddings: IndexMemoryEmbeddings,
    private val indexPdfPageEmbeddings: IndexPdfPageEmbeddings,
    private val indexOcrEvidenceEmbeddings: IndexOcrEvidenceEmbeddings,
    private val savedPdfPages: SavedPdfPageTextSource,
    private val applyMig05EvidenceSearchCutover: ApplyMig05EvidenceSearchCutover,
) {
    suspend operator fun invoke(
        nowEpochMs: Long,
        limit: Int = MeaningIndexBatchLimits.MAX_MEMORIES_PER_TAP,
        onProgress: ((MeaningIndexDrainProgress) -> Unit)? = null,
    ): RunPendingMeaningIndexResult {
        require(nowEpochMs >= 0)
        require(limit in 1..MeaningIndexBatchLimits.MAX_MEMORIES_PER_TAP)

        val model = when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Available -> availability.model
            is CapabilityAvailability.Unavailable ->
                return RunPendingMeaningIndexResult.EngineUnavailable(availability.reason)
        }

        val pendingTotal = memoryRepository.countMeaningIndexPending(model)
        val summaries = memoryRepository.listMeaningIndexSummaries(model = model, limit = limit)
        if (summaries.isEmpty()) {
            return if (pendingTotal > 0) {
                RunPendingMeaningIndexResult.SelectionDisagreed(pendingCount = pendingTotal)
            } else {
                RunPendingMeaningIndexResult.NothingPending
            }
        }

        val candidates = summaries.map { summary ->
            MemoryEmbeddingCandidate(
                revisionId = summary.revisionId,
                memoryId = summary.memoryId,
                summaryText = summary.summaryText,
            )
        }
        onProgress?.invoke(
            MeaningIndexDrainProgress(MeaningIndexDrainPhase.SUMMARIES, processed = 0, total = candidates.size),
        )
        val summaryResult = indexMemoryEmbeddings(
            candidates = candidates,
            nowEpochMs = nowEpochMs,
            onProgress = { processed, total ->
                onProgress?.invoke(
                    MeaningIndexDrainProgress(MeaningIndexDrainPhase.SUMMARIES, processed, total),
                )
            },
        )
        val evidence = when (summaryResult) {
            is IndexMemoryEmbeddingsResult.EngineUnavailable ->
                return RunPendingMeaningIndexResult.EngineUnavailable(summaryResult.reason)
            is IndexMemoryEmbeddingsResult.Completed -> indexEvidence(
                summaries = summaries,
                nowEpochMs = nowEpochMs,
                onProgress = onProgress,
            )
        }

        when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Available ->
                applyMig05EvidenceSearchCutover.ensureApplied(
                    model = availability.model,
                    nowEpochMs = nowEpochMs,
                )
            is CapabilityAvailability.Unavailable -> Unit
        }

        val remaining = memoryRepository.countMeaningIndexPending(model)
        return RunPendingMeaningIndexResult.Completed(
            memories = summaryResult,
            pdf = evidence.pdf,
            ocr = evidence.ocr,
            note = evidence.note,
            remainingPending = remaining,
            hasMore = remaining > 0,
        )
    }

    private suspend fun indexEvidence(
        summaries: List<MemoryEmbeddingSummary>,
        nowEpochMs: Long,
        onProgress: ((MeaningIndexDrainProgress) -> Unit)?,
    ): MeaningEvidenceIndexBatchResults {
        val revisionIds = summaries.map { it.revisionId }
        val lookups = memoryRepository.findMeaningIndexLookups(revisionIds)
        val evidenceIdsByRevision = memoryRepository.findPdfPageEvidenceIds(revisionIds)
        val ocrEvidenceByRevision = memoryRepository.findOcrTextEvidenceForEmbedding(revisionIds)
        val noteEvidenceByRevision = memoryRepository.findNoteTextEvidenceForEmbedding(revisionIds)

        val pageCandidates = summaries.flatMap { summary ->
            val lookup = lookups[summary.revisionId] ?: return@flatMap emptyList()
            if (lookup.assetType != AssetType.PDF) return@flatMap emptyList()
            val pageEvidenceIds = evidenceIdsByRevision[summary.revisionId].orEmpty()
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
            onProgress?.invoke(
                MeaningIndexDrainProgress(MeaningIndexDrainPhase.PDF_PAGES, processed = 0, total = pageCandidates.size),
            )
            indexPdfPageEmbeddings(
                candidates = pageCandidates,
                nowEpochMs = nowEpochMs,
                onProgress = { processed, total ->
                    onProgress?.invoke(
                        MeaningIndexDrainProgress(MeaningIndexDrainPhase.PDF_PAGES, processed, total),
                    )
                },
            )
        }

        val ocrCandidates = summaries.flatMap { summary ->
            val lookup = lookups[summary.revisionId] ?: return@flatMap emptyList()
            if (lookup.assetType != AssetType.PHOTO && lookup.assetType != AssetType.SCREENSHOT) {
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
            onProgress?.invoke(
                MeaningIndexDrainProgress(
                    MeaningIndexDrainPhase.OCR_EVIDENCE,
                    processed = 0,
                    total = ocrCandidates.size,
                ),
            )
            indexOcrEvidenceEmbeddings(
                candidates = ocrCandidates,
                nowEpochMs = nowEpochMs,
                onProgress = { processed, total ->
                    onProgress?.invoke(
                        MeaningIndexDrainProgress(MeaningIndexDrainPhase.OCR_EVIDENCE, processed, total),
                    )
                },
            )
        }

        val noteCandidates = summaries.flatMap { summary ->
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
            onProgress?.invoke(
                MeaningIndexDrainProgress(
                    MeaningIndexDrainPhase.NOTE_EVIDENCE,
                    processed = 0,
                    total = noteCandidates.size,
                ),
            )
            indexOcrEvidenceEmbeddings(
                candidates = noteCandidates,
                nowEpochMs = nowEpochMs,
                onProgress = { processed, total ->
                    onProgress?.invoke(
                        MeaningIndexDrainProgress(MeaningIndexDrainPhase.NOTE_EVIDENCE, processed, total),
                    )
                },
            )
        }

        return MeaningEvidenceIndexBatchResults(
            pdf = pdfResult,
            ocr = ocrResult,
            note = noteResult,
        )
    }
}

enum class MeaningIndexDrainPhase {
    SUMMARIES,
    PDF_PAGES,
    OCR_EVIDENCE,
    NOTE_EVIDENCE,
}

data class MeaningIndexDrainProgress(
    val phase: MeaningIndexDrainPhase,
    val processed: Int,
    val total: Int,
) {
    init {
        require(processed >= 0)
        require(total >= 0)
        require(processed <= total)
    }
}

sealed interface RunPendingMeaningIndexResult {
    data class EngineUnavailable(val reason: String) : RunPendingMeaningIndexResult {
        init {
            require(reason.isNotBlank())
        }
    }

    data object NothingPending : RunPendingMeaningIndexResult

    data class SelectionDisagreed(
        val pendingCount: Int,
    ) : RunPendingMeaningIndexResult {
        init {
            require(pendingCount > 0)
        }
    }

    data class Completed(
        val memories: IndexMemoryEmbeddingsResult.Completed,
        val pdf: IndexPdfPageEmbeddingsResult,
        val ocr: IndexOcrEvidenceEmbeddingsResult,
        val note: IndexOcrEvidenceEmbeddingsResult,
        val remainingPending: Int,
        val hasMore: Boolean,
    ) : RunPendingMeaningIndexResult {
        init {
            require(remainingPending >= 0)
            require(hasMore == remainingPending > 0) {
                "hasMore must match remainingPending so UI and a future worker cannot drift."
            }
        }
    }
}

private data class MeaningEvidenceIndexBatchResults(
    val pdf: IndexPdfPageEmbeddingsResult,
    val ocr: IndexOcrEvidenceEmbeddingsResult,
    val note: IndexOcrEvidenceEmbeddingsResult,
)
