package com.memora.app.ui.setup

import com.memora.app.application.intelligence.IndexMemoryEmbeddingsResult
import com.memora.app.application.intelligence.IndexOcrEvidenceEmbeddingsResult
import com.memora.app.application.intelligence.IndexPdfPageEmbeddingsResult
import com.memora.app.application.intelligence.MeaningIndexDrainPhase
import com.memora.app.application.intelligence.MeaningIndexDrainProgress
import com.memora.app.application.intelligence.RunPendingMeaningIndexResult
import com.memora.app.domain.intelligence.AiPackInstallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPackDisclosureCopyTest {
    @Test
    fun honesty_copy_names_model_download_and_keeps_keyword_path() {
        val all = listOf(
            AiPackDisclosureCopy.ENTRY_LABEL,
            AiPackDisclosureCopy.LEAD_BODY,
            AiPackDisclosureCopy.SCOPE_BODY,
            AiPackDisclosureCopy.NETWORK_BODY,
            AiPackDisclosureCopy.STATUS_NEED_MODEL,
            AiPackDisclosureCopy.STATUS_MODEL_READY,
            AiPackDisclosureCopy.DOWNLOAD_MODEL_LABEL,
            AiPackDisclosureCopy.BUILD_INDEX_LABEL,
            AiPackDisclosureCopy.FEEDBACK_MODEL_INSTALLED,
        ).joinToString(" ").lowercase()

        assertTrue(all.contains("on-device"))
        assertTrue(all.contains("model"))
        assertTrue(AiPackDisclosureCopy.SCOPE_BODY.contains("Universal Sentence Encoder"))
        assertTrue(AiPackDisclosureCopy.SIZE_BODY.contains("rebuild the meaning index"))
        assertTrue(AiPackDisclosureCopy.INDEX_BATCH_BODY.contains("25"))
        assertTrue(AiPackDisclosureCopy.INDEX_BATCH_BODY.contains("Stop"))
        assertFalse(AiPackDisclosureCopy.INDEX_BATCH_BODY.contains("tap Build again"))
        assertTrue(AiPackDisclosureCopy.NETWORK_BODY.contains("model bytes only"))
        assertTrue(AiPackDisclosureCopy.remainingBatchHint(12).contains("12 READY still waiting"))
        assertFalse(all.contains("available now"))
        assertFalse(all.contains("uploads your memories"))
    }

    @Test
    fun status_body_tracks_model_install() {
        assertTrue(
            AiPackDisclosureCopy.statusBody(
                installationState = AiPackInstallState.NOT_INSTALLED,
                disclosureAcknowledged = true,
                modelInstalled = false,
                embeddingAvailable = false,
            ).contains("Download"),
        )
        assertTrue(
            AiPackDisclosureCopy.statusBody(
                installationState = AiPackInstallState.ACTIVE,
                disclosureAcknowledged = true,
                modelInstalled = true,
                embeddingAvailable = true,
            ).contains("Meaning model is installed"),
        )
    }

    @Test
    fun empty_queue_uses_empty_copy_not_mismatch() {
        val copy = AiPackDisclosureCopy.indexDrainFeedback(
            RunPendingMeaningIndexResult.NothingPending,
        )
        assertEquals(AiPackDisclosureCopy.FEEDBACK_INDEX_EMPTY, copy)
        assertTrue(copy.contains("No memories are waiting for a meaning-index batch"))
        assertFalse(copy.contains("No READY memories to index yet"))
        assertFalse(copy.contains("queue mismatch"))
    }

    @Test
    fun selection_disagreement_is_not_an_empty_library() {
        val copy = AiPackDisclosureCopy.indexDrainFeedback(
            RunPendingMeaningIndexResult.SelectionDisagreed(4),
        )
        assertTrue(copy.contains("4 memories"))
        assertTrue(copy.contains("queue mismatch"))
        assertFalse(copy.contains("No READY memories"))
    }

    @Test
    fun remaining_hint_uses_after_batch_count() {
        val remaining = AiPackDisclosureCopy.indexDrainFeedback(
            completed(remainingPending = 12, hasMore = true),
        )
        assertTrue(remaining.contains(AiPackDisclosureCopy.remainingBatchHint(12)))

        val lastBatch = AiPackDisclosureCopy.indexDrainFeedback(
            completed(remainingPending = 0, hasMore = false),
        )
        assertFalse(lastBatch.contains("still waiting"))
    }

    @Test
    fun drain_progress_and_stop_copy_are_honest() {
        assertTrue(AiPackDisclosureCopy.indexingProgress(0, 0).contains("Preparing"))
        assertTrue(AiPackDisclosureCopy.indexingProgress(25, 100).contains("25"))
        assertTrue(AiPackDisclosureCopy.indexingProgress(25, 100).contains("100"))
        assertTrue(AiPackDisclosureCopy.indexStopped(12).contains("Stopped"))
        assertTrue(AiPackDisclosureCopy.indexStopped(12).contains("12"))
        assertTrue(AiPackDisclosureCopy.indexDrainComplete(40, 0).contains("40"))
        assertFalse(AiPackDisclosureCopy.indexDrainComplete(40, 0).contains("still waiting"))
        assertEquals("Stop", AiPackDisclosureCopy.STOP_INDEX_LABEL)
    }

    @Test
    fun engine_unavailable_stays_canned() {
        assertEquals(
            AiPackDisclosureCopy.FEEDBACK_INDEX_UNAVAILABLE,
            AiPackDisclosureCopy.indexDrainFeedback(
                RunPendingMeaningIndexResult.EngineUnavailable("missing pack"),
            ),
        )
    }

    @Test
    fun progress_maps_each_phase_without_application_copy() {
        assertEquals(
            AiPackDisclosureCopy.progressSummaries(1, 3),
            AiPackDisclosureCopy.progressFor(
                MeaningIndexDrainProgress(MeaningIndexDrainPhase.SUMMARIES, 1, 3),
            ),
        )
        assertEquals(
            AiPackDisclosureCopy.progressPages(2, 4),
            AiPackDisclosureCopy.progressFor(
                MeaningIndexDrainProgress(MeaningIndexDrainPhase.PDF_PAGES, 2, 4),
            ),
        )
        assertEquals(
            AiPackDisclosureCopy.progressOcrEvidence(0, 1),
            AiPackDisclosureCopy.progressFor(
                MeaningIndexDrainProgress(MeaningIndexDrainPhase.OCR_EVIDENCE, 0, 1),
            ),
        )
        assertEquals(
            AiPackDisclosureCopy.progressNoteEvidence(3, 3),
            AiPackDisclosureCopy.progressFor(
                MeaningIndexDrainProgress(MeaningIndexDrainPhase.NOTE_EVIDENCE, 3, 3),
            ),
        )
    }

    private fun completed(
        remainingPending: Int,
        hasMore: Boolean,
    ) = RunPendingMeaningIndexResult.Completed(
        memories = IndexMemoryEmbeddingsResult.Completed(1, 0, 0),
        pdf = IndexPdfPageEmbeddingsResult.Completed(0, 0, 0),
        ocr = IndexOcrEvidenceEmbeddingsResult.Completed(0, 0, 0),
        note = IndexOcrEvidenceEmbeddingsResult.Completed(0, 0, 0),
        remainingPending = remainingPending,
        hasMore = hasMore,
    )
}
