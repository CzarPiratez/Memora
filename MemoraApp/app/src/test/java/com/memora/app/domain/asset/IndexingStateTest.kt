package com.memora.app.domain.asset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class IndexingStateTest {
    @Test
    fun `asset follows the valid indexing lifecycle`() {
        val indexed = IndexingState.discovered
            .transitionTo(IndexingStatus.EXTRACTION_QUEUED)
            .transitionTo(IndexingStatus.EXTRACTING)
            .transitionTo(IndexingStatus.UNDERSTANDING_QUEUED)
            .transitionTo(IndexingStatus.UNDERSTANDING)
            .transitionTo(IndexingStatus.INDEXED)

        assertEquals(IndexingStatus.INDEXED, indexed.status)
        assertEquals(1, indexed.attemptCount)
        assertNull(indexed.failure)
    }

    @Test
    fun `retryable failure can be requeued and increments a new attempt`() {
        val failure = IndexingFailure(
            code = "EXTRACTION_UNAVAILABLE",
            message = "The source could not be opened.",
        )

        val retrying = IndexingState.discovered
            .transitionTo(IndexingStatus.EXTRACTION_QUEUED)
            .transitionTo(IndexingStatus.EXTRACTING)
            .transitionTo(IndexingStatus.FAILED_RETRYABLE, failure)
            .transitionTo(IndexingStatus.EXTRACTION_QUEUED)
            .transitionTo(IndexingStatus.EXTRACTING)

        assertEquals(IndexingStatus.EXTRACTING, retrying.status)
        assertEquals(2, retrying.attemptCount)
        assertNull(retrying.failure)
    }

    @Test
    fun `invalid transitions and invalid failure details are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            IndexingState.discovered.transitionTo(IndexingStatus.INDEXED)
        }
        assertThrows(IllegalArgumentException::class.java) {
            IndexingState.discovered.transitionTo(IndexingStatus.FAILED_RETRYABLE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            IndexingState.discovered.transitionTo(
                nextStatus = IndexingStatus.EXTRACTION_QUEUED,
                nextFailure = IndexingFailure("UNEXPECTED", "Not valid for this state."),
            )
        }
    }
}
