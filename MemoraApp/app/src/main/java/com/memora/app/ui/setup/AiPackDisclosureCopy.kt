package com.memora.app.ui.setup

import com.memora.app.application.intelligence.IndexOcrEvidenceEmbeddingsResult
import com.memora.app.application.intelligence.IndexPdfPageEmbeddingsResult
import com.memora.app.application.intelligence.MeaningEncoderBakeOffScorecard
import com.memora.app.application.intelligence.MeaningEncoderBakeOffVerdict
import com.memora.app.application.intelligence.MeaningEncoderChallengerProbeReport
import com.memora.app.application.intelligence.MeaningEncoderProbeReport
import com.memora.app.application.intelligence.MeaningIndexBatchLimits
import com.memora.app.application.intelligence.MeaningIndexDrainPhase
import com.memora.app.application.intelligence.MeaningIndexDrainProgress
import com.memora.app.application.intelligence.ProbeMeaningEncoderChallenger
import com.memora.app.application.intelligence.ProbeMeaningEncoderRanks
import com.memora.app.application.intelligence.RunPendingMeaningIndexResult
import com.memora.app.domain.intelligence.AiPackInstallState
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import com.memora.app.domain.intelligence.MediaPipeUniversalSentenceEncoderSpec
import com.memora.app.domain.intelligence.OnnxBgeSmallEnV15Spec

/**
 * Honesty copy for embedding-first disclosure / model install / index (E3–E4b).
 */
object AiPackDisclosureCopy {
    const val ENTRY_LABEL = "About on-device meaning search"

    const val SCREEN_TITLE = "On-device meaning search"

    const val LEAD_BODY =
        "UNFYND can understand what you already saved — PDF text, OCR, and notes — " +
            "so you can search by meaning, not only exact words. That needs an optional " +
            "on-device meaning model installed on this phone."

    const val SCOPE_TITLE = "What this is"

    const val SCOPE_BODY =
        "The meaning model is stored privately in UNFYND. It is not your photos, PDFs, " +
            "or notes. This model is MediaPipe’s Universal Sentence Encoder — a stronger " +
            "on-device semantic embedder than the earlier compact average-word model. " +
            "Meaning search stays a candidate path until measured on your device class. " +
            "Keyword Find saved text buttons remain available."

    const val NETWORK_TITLE = "Network"

    const val NETWORK_BODY =
        "Downloading the meaning model uses the network for model bytes only — never to " +
            "upload your memories. After install, embedding runs on this phone offline."

    const val SIZE_TITLE = "Size on this phone"

    val SIZE_BODY: String =
        "Meaning model download is under about " +
            "${MediaPipeUniversalSentenceEncoderSpec.DISCLOSED_SIZE_MB_CEILING} MB. " +
            "Pack-container verify (optional pipeline check) uses about " +
            "${EmbeddingFirstAiPackTrack.PLANNED_DOWNLOAD_SIZE_BYTES} bytes. " +
            "After upgrading the model, rebuild the meaning index."

    const val LICENSE_TITLE = "License"

    val LICENSE_BODY: String =
        EmbeddingFirstAiPackTrack.PLANNED_LICENSE +
            " The MediaPipe Universal Sentence Encoder model is subject to Google’s " +
            "published MediaPipe model terms."

    const val STATUS_TITLE = "Status right now"

    const val CORPUS_COMPLETENESS_TITLE = "Corpus on this phone"

    const val STATUS_NOT_ACKNOWLEDGED =
        "No disclosure recorded yet. Meaning search is off. " +
            "Keyword search on Welcome still works for text UNFYND has already saved."

    const val STATUS_NEED_MODEL =
        "Disclosure recorded. Download the on-device meaning model next. " +
            "Meaning search stays off until the model is installed."

    val STATUS_MODEL_READY: String =
        "Meaning model is installed on this phone. Build a meaning index from saved " +
            "Asset Memories (one tap continues until nothing is waiting), then use " +
            "Find by meaning on Welcome for candidate recall."

    const val STATUS_VERIFYING =
        "Pack verification is in progress. Meaning search stays off until verification finishes."

    const val STATUS_FAILED =
        "Pack verification failed earlier. You can still download the meaning model. " +
            "Your keyword search and original files are unchanged."

    const val STATUS_PACK_ACTIVE_NEED_MODEL =
        "Pack container verified. Download the on-device meaning model to turn on embedding."

    const val INDEX_BATCH_TITLE = "Building the meaning index"

    val INDEX_BATCH_BODY: String =
        "One tap builds the meaning index until nothing is waiting. Work runs in " +
            "batches of ${MeaningIndexBatchLimits.MAX_MEMORIES_PER_TAP} memories, " +
            "plus PDF pages and OCR or note evidence for those memories. You can Stop " +
            "and continue later. This is not a permanent library ceiling and not a " +
            "measured AVAILABLE claim."

    const val ACKNOWLEDGE_LABEL = "I understand these details"

    const val ACTIVATE_LABEL = "Verify pack container (optional)"

    const val DOWNLOAD_MODEL_LABEL = "Download on-device meaning model"

    const val BUILD_INDEX_LABEL = "Build meaning index from memories"

    const val STOP_INDEX_LABEL = "Stop"

    const val ENCODER_PROBE_LABEL = "Run encoder probe"

    const val ENCODER_PROBE_HINT =
        "Diagnostic for D-20. Does not change Find. Writes ranks to Logcat " +
            "(MeaningEncoderProbe). Never uploads your files."

    val CHALLENGER_PROBE_HINT: String =
        "ADR-055 bake-off only. Downloads BGE-small (~" +
            "${OnnxBgeSmallEnV15Spec.DISCLOSED_SIZE_MB_CEILING} MB) " +
            "into private storage, re-embeds the same USE-indexed Memories in memory, " +
            "and scores gold@60 against the frozen USE card. Does not change Find, " +
            "does not swap the product meaning model, does not write the meaning index. " +
            "Model bytes only — never uploads your files. Can take several minutes."

    const val DOWNLOAD_CHALLENGER_LABEL = "Download BGE challenger (probe only)"

    const val RUN_CHALLENGER_PROBE_LABEL = "Run BGE challenger probe"

    const val MEANING_LIVE_TRACE_HINT =
        "Debug meaning Find also writes a live trace to Logcat " +
            "(MeaningSearchTrace): tokens, pool, tier, droppedByTier, " +
            "gold collapse/admitted/shown ranks. " +
            "Does not change Find. Never uploads your files."

    const val BACK_LABEL = "Back"

    const val FEEDBACK_ACKNOWLEDGED =
        "Saved. Next download the on-device meaning model."

    const val FEEDBACK_ACTIVATED =
        "Pack container verified. Download the meaning model to enable embedding."

    const val FEEDBACK_ALREADY_ACTIVE =
        "Pack container already verified. Download the meaning model if needed."

    const val FEEDBACK_DISCLOSURE_REQUIRED =
        "Acknowledge the details above before continuing."

    const val FEEDBACK_MODEL_INSTALLED =
        "Meaning model installed. Rebuild the meaning index so rankings use this model."

    const val FEEDBACK_MODEL_ALREADY =
        "Meaning model is already installed on this phone."

    const val FEEDBACK_INDEX_BUILT_PREFIX = "Meaning index updated. Indexed "

    const val FEEDBACK_INDEX_UNAVAILABLE =
        "Meaning model is not ready yet. Download it before building an index."

    const val FEEDBACK_INDEX_EMPTY =
        "No memories are waiting for a meaning-index batch. " +
            "If none are saved yet, build Asset Memory first."

    const val FEEDBACK_INDEX_FAILED =
        "UNFYND could not finish the meaning index. You can try Build again. " +
            "Your original files are unchanged."

    const val FEEDBACK_INDEX_SELECTION_DISAGREED_SUFFIX =
        "That is a queue mismatch, not an empty library. Try Build again."

    const val PROGRESS_PREPARING = "Preparing meaning index…"

    fun progressSummaries(processed: Int, total: Int): String =
        "Indexing summaries $processed of $total…"

    fun progressPages(processed: Int, total: Int): String =
        "Indexing PDF pages $processed of $total…"

    fun progressOcrEvidence(processed: Int, total: Int): String =
        "Indexing photo/screenshot OCR evidence $processed of $total…"

    fun progressNoteEvidence(processed: Int, total: Int): String =
        "Indexing note text evidence $processed of $total…"

    fun progressFor(progress: MeaningIndexDrainProgress): String = when (progress.phase) {
        MeaningIndexDrainPhase.SUMMARIES ->
            progressSummaries(progress.processed, progress.total)
        MeaningIndexDrainPhase.PDF_PAGES ->
            progressPages(progress.processed, progress.total)
        MeaningIndexDrainPhase.OCR_EVIDENCE ->
            progressOcrEvidence(progress.processed, progress.total)
        MeaningIndexDrainPhase.NOTE_EVIDENCE ->
            progressNoteEvidence(progress.processed, progress.total)
    }

    fun remainingBatchHint(remaining: Int): String =
        "$remaining READY still waiting. Tap Build to continue."

    fun indexingProgress(indexedSoFar: Int, remainingPending: Int): String {
        val indexed = indexedSoFar.coerceAtLeast(0)
        val remaining = remainingPending.coerceAtLeast(0)
        return when {
            indexed <= 0 && remaining <= 0 -> PROGRESS_PREPARING
            remaining > 0 ->
                "Indexed $indexed memories. About $remaining still waiting…"
            else -> "Indexed $indexed memories. Finishing this pass…"
        }
    }

    fun indexStopped(remainingPending: Int): String {
        val remaining = remainingPending.coerceAtLeast(0)
        return if (remaining > 0) {
            "Stopped. $remaining memories still waiting. Tap Build to continue."
        } else {
            "Stopped. Tap Build if more memories still need a meaning index."
        }
    }

    fun indexDrainComplete(indexedMemories: Int, remainingPending: Int): String {
        val indexed = indexedMemories.coerceAtLeast(0)
        val remaining = remainingPending.coerceAtLeast(0)
        val remainingHint = if (remaining > 0) {
            " ${remainingBatchHint(remaining)}"
        } else {
            ""
        }
        return "Meaning index updated. Indexed $indexed memories in this pass.$remainingHint " +
            "Use Find by meaning on Welcome next."
    }

    fun indexSelectionDisagreed(pendingCount: Int): String {
        require(pendingCount > 0)
        val noun = if (pendingCount == 1) "memory" else "memories"
        return "This phone still lists $pendingCount $noun that need a meaning " +
            "index, but this tap could not select one. " +
            FEEDBACK_INDEX_SELECTION_DISAGREED_SUFFIX
    }

    fun indexDrainFeedback(result: RunPendingMeaningIndexResult): String = when (result) {
        is RunPendingMeaningIndexResult.EngineUnavailable -> FEEDBACK_INDEX_UNAVAILABLE
        RunPendingMeaningIndexResult.NothingPending -> FEEDBACK_INDEX_EMPTY
        is RunPendingMeaningIndexResult.SelectionDisagreed ->
            indexSelectionDisagreed(result.pendingCount)
        is RunPendingMeaningIndexResult.Completed -> indexBatchCompleted(result)
    }

    private fun indexBatchCompleted(result: RunPendingMeaningIndexResult.Completed): String {
        val pagePart = when (val pdfPart = result.pdf) {
            is IndexPdfPageEmbeddingsResult.Completed ->
                " Pages indexed ${pdfPart.indexed} " +
                    "(skipped ${pdfPart.skippedUnchanged}, failed ${pdfPart.failed})."
            is IndexPdfPageEmbeddingsResult.EngineUnavailable ->
                " PDF page index unavailable."
        }
        val ocrEvidencePart = when (val ocrPart = result.ocr) {
            is IndexOcrEvidenceEmbeddingsResult.Completed ->
                " OCR evidence indexed ${ocrPart.indexed} " +
                    "(skipped ${ocrPart.skippedUnchanged}, failed ${ocrPart.failed})."
            is IndexOcrEvidenceEmbeddingsResult.EngineUnavailable ->
                " OCR evidence index unavailable."
        }
        val noteEvidencePart = when (val notePart = result.note) {
            is IndexOcrEvidenceEmbeddingsResult.Completed ->
                " Note evidence indexed ${notePart.indexed} " +
                    "(skipped ${notePart.skippedUnchanged}, failed ${notePart.failed})."
            is IndexOcrEvidenceEmbeddingsResult.EngineUnavailable ->
                " Note evidence index unavailable."
        }
        val remainingHint = if (result.hasMore) {
            " ${remainingBatchHint(result.remainingPending)}"
        } else {
            ""
        }
        return FEEDBACK_INDEX_BUILT_PREFIX +
            "${result.memories.indexed} memories (skipped ${result.memories.skippedUnchanged}, " +
            "failed ${result.memories.failed}).$pagePart$ocrEvidencePart$noteEvidencePart$remainingHint " +
            "Use Find by meaning on Welcome next."
    }

    fun statusBody(
        installationState: AiPackInstallState,
        disclosureAcknowledged: Boolean,
        modelInstalled: Boolean,
        embeddingAvailable: Boolean,
    ): String = when {
        embeddingAvailable || modelInstalled -> STATUS_MODEL_READY
        installationState == AiPackInstallState.VERIFYING -> STATUS_VERIFYING
        installationState == AiPackInstallState.FAILED_VERIFICATION -> STATUS_FAILED
        installationState == AiPackInstallState.ACTIVE && disclosureAcknowledged ->
            STATUS_PACK_ACTIVE_NEED_MODEL
        disclosureAcknowledged -> STATUS_NEED_MODEL
        else -> STATUS_NOT_ACKNOWLEDGED
    }

    const val FEEDBACK_PROBE_RUNNING = "Running encoder probe…"

    const val FEEDBACK_PROBE_NOTHING_INDEXED =
        "Nothing is meaning-indexed yet. Build the meaning index first."

    const val FEEDBACK_PROBE_FAILED =
        "Encoder probe could not finish on this phone."

    const val FEEDBACK_CHALLENGER_DOWNLOADING = "Downloading BGE challenger…"

    const val FEEDBACK_CHALLENGER_INSTALLED =
        "BGE challenger installed. Run the challenger probe next. Find is unchanged."

    const val FEEDBACK_CHALLENGER_ALREADY =
        "BGE challenger is already installed on this phone."

    const val FEEDBACK_CHALLENGER_PROBE_RUNNING =
        "Running BGE challenger probe… This re-embeds the USE-indexed corpus and can take minutes."

    const val FEEDBACK_CHALLENGER_PACK_MISSING =
        "BGE challenger is not installed yet. Download it first."

    const val FEEDBACK_CHALLENGER_PROBE_FAILED =
        "BGE challenger probe could not finish on this phone."

    fun encoderProbeFinished(report: MeaningEncoderProbeReport): String =
        when (report) {
            is MeaningEncoderProbeReport.EngineUnavailable -> report.reason
            MeaningEncoderProbeReport.NothingIndexed -> FEEDBACK_PROBE_NOTHING_INDEXED
            is MeaningEncoderProbeReport.Completed -> {
                val card = MeaningEncoderBakeOffScorecard.of(report.rows)
                val verdict = MeaningEncoderBakeOffVerdict.decide(live = card)
                "Probe finished (${report.rows.size} cues). ${card.displayLine()} " +
                    "${verdict.displayLine()} See Logcat ${ProbeMeaningEncoderRanks.LOG_TAG}."
            }
        }

    fun challengerProbeFinished(report: MeaningEncoderChallengerProbeReport): String =
        when (report) {
            MeaningEncoderChallengerProbeReport.PackMissing -> FEEDBACK_CHALLENGER_PACK_MISSING
            MeaningEncoderChallengerProbeReport.NothingIndexed -> FEEDBACK_PROBE_NOTHING_INDEXED
            is MeaningEncoderChallengerProbeReport.ProductEngineUnavailable -> report.reason
            is MeaningEncoderChallengerProbeReport.Failed -> report.reason
            is MeaningEncoderChallengerProbeReport.Completed ->
                "Challenger probe finished (${report.rows.size} cues). " +
                    "${report.scorecard.displayLine()} ${report.verdict.displayLine()} " +
                    "See Logcat ${ProbeMeaningEncoderChallenger.LOG_TAG}."
        }
}
