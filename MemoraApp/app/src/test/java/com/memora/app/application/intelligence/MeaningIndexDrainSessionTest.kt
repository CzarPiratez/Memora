package com.memora.app.application.intelligence

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningIndexDrainSessionTest {
    @Test
    fun empty_queue_finishes_without_indexing() = runBlocking {
        val session = MeaningIndexDrainSession(
            runPending = { RunPendingMeaningIndexResult.NothingPending },
            nowMs = { 0L },
        )
        val result = session.run(isStopped = { false })
        assertTrue(result is MeaningIndexDrainSessionResult.Finished)
        assertEquals(0, result.indexedMemories)
        assertEquals(0, result.remainingPending)
    }

    @Test
    fun continues_until_the_queue_is_empty() = runBlocking {
        val remaining = ArrayDeque(listOf(50, 25, 0))
        val session = MeaningIndexDrainSession(
            runPending = {
                val left = remaining.removeFirst()
                completed(indexed = 25, remainingPending = left, hasMore = left > 0)
            },
            nowMs = { 0L },
            budgetMs = 60_000L,
        )
        val result = session.run(isStopped = { false })
        assertTrue(result is MeaningIndexDrainSessionResult.Finished)
        assertEquals(75, result.indexedMemories)
        assertEquals(0, result.remainingPending)
    }

    @Test
    fun budget_exhaustion_returns_after_the_batch_that_fit() = runBlocking {
        var now = 0L
        val session = MeaningIndexDrainSession(
            runPending = {
                now += 2_000L
                completed(indexed = 25, remainingPending = 100, hasMore = true)
            },
            nowMs = { now },
            budgetMs = 1_000L,
        )
        val result = session.run(isStopped = { false })
        assertTrue(result is MeaningIndexDrainSessionResult.BudgetExhausted)
        assertEquals(25, result.indexedMemories)
        assertEquals(100, result.remainingPending)
    }

    @Test
    fun engine_unavailable_stops_and_does_not_look_like_empty() = runBlocking {
        val session = MeaningIndexDrainSession(
            runPending = { RunPendingMeaningIndexResult.EngineUnavailable("model missing") },
            nowMs = { 0L },
        )
        val result = session.run(isStopped = { false })
        assertTrue(result is MeaningIndexDrainSessionResult.EngineUnavailable)
        assertEquals("model missing", (result as MeaningIndexDrainSessionResult.EngineUnavailable).reason)
    }

    @Test
    fun selection_disagreement_stops_and_does_not_retry() = runBlocking {
        val session = MeaningIndexDrainSession(
            runPending = { RunPendingMeaningIndexResult.SelectionDisagreed(4) },
            nowMs = { 0L },
        )
        val result = session.run(isStopped = { false })
        assertTrue(result is MeaningIndexDrainSessionResult.SelectionDisagreed)
        assertEquals(4, (result as MeaningIndexDrainSessionResult.SelectionDisagreed).pendingCount)
    }

    @Test
    fun coroutine_cancellation_is_not_swallowed_as_retry() = runBlocking {
        val session = MeaningIndexDrainSession(
            runPending = { throw kotlin.coroutines.cancellation.CancellationException() },
            nowMs = { 0L },
        )
        try {
            session.run(isStopped = { false })
            throw AssertionError("expected CancellationException")
        } catch (_: kotlin.coroutines.cancellation.CancellationException) {
            // WorkManager must see cancellation, not a retryable failure.
        }
    }

    @Test
    fun unexpected_exception_is_retryable() = runBlocking {
        val session = MeaningIndexDrainSession(
            runPending = { error("disk") },
            nowMs = { 0L },
        )
        val result = session.run(isStopped = { false })
        assertEquals(MeaningIndexDrainSessionResult.RetryableFailure, result)
    }

    @Test
    fun stop_between_batches_preserves_progress() = runBlocking {
        var calls = 0
        val session = MeaningIndexDrainSession(
            runPending = {
                calls += 1
                completed(indexed = 25, remainingPending = 40, hasMore = true)
            },
            nowMs = { 0L },
            budgetMs = 60_000L,
        )
        val result = session.run(isStopped = { calls >= 1 })
        assertTrue(result is MeaningIndexDrainSessionResult.Stopped)
        assertEquals(25, result.indexedMemories)
        assertEquals(40, result.remainingPending)
    }

    private fun completed(
        indexed: Int,
        remainingPending: Int,
        hasMore: Boolean,
    ) = RunPendingMeaningIndexResult.Completed(
        memories = IndexMemoryEmbeddingsResult.Completed(indexed, 0, 0),
        pdf = IndexPdfPageEmbeddingsResult.Completed(0, 0, 0),
        ocr = IndexOcrEvidenceEmbeddingsResult.Completed(0, 0, 0),
        note = IndexOcrEvidenceEmbeddingsResult.Completed(0, 0, 0),
        remainingPending = remainingPending,
        hasMore = hasMore,
    )
}
