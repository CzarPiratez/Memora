package com.memora.app.domain.asset

/** The recoverable lifecycle of a single asset as it becomes a searchable memory. */
enum class IndexingStatus {
    DISCOVERED,
    EXTRACTION_QUEUED,
    EXTRACTING,
    UNDERSTANDING_QUEUED,
    UNDERSTANDING,
    INDEXED,
    FAILED_RETRYABLE,
    ACCESS_REVOKED,
}

/** A structured failure recorded so retries can be safe and visible. */
data class IndexingFailure(
    val code: String,
    val message: String,
) {
    init {
        require(code.isNotBlank()) { "An indexing failure code cannot be blank." }
        require(message.isNotBlank()) { "An indexing failure message cannot be blank." }
    }
}

/**
 * Immutable indexing state. State transitions are validated here so workers, future
 * repositories, and UI cannot accidentally skip extraction or claim an item is indexed.
 */
data class IndexingState(
    val status: IndexingStatus,
    val attemptCount: Int = 0,
    val failure: IndexingFailure? = null,
) {
    init {
        require(attemptCount >= 0) { "The indexing attempt count cannot be negative." }
        require((status == IndexingStatus.FAILED_RETRYABLE) == (failure != null)) {
            "Only a retryable failure state may include failure details."
        }
    }

    fun transitionTo(
        nextStatus: IndexingStatus,
        nextFailure: IndexingFailure? = null,
    ): IndexingState {
        require(nextStatus in allowedTransitions.getValue(status)) {
            "Cannot transition from $status to $nextStatus."
        }
        require((nextStatus == IndexingStatus.FAILED_RETRYABLE) == (nextFailure != null)) {
            "A retryable failure transition must include failure details, and no other transition may include them."
        }

        return IndexingState(
            status = nextStatus,
            attemptCount = if (nextStatus == IndexingStatus.EXTRACTING) attemptCount + 1 else attemptCount,
            failure = nextFailure,
        )
    }

    companion object {
        val discovered: IndexingState = IndexingState(status = IndexingStatus.DISCOVERED)

        private val allowedTransitions: Map<IndexingStatus, Set<IndexingStatus>> = mapOf(
            IndexingStatus.DISCOVERED to setOf(
                IndexingStatus.EXTRACTION_QUEUED,
                IndexingStatus.ACCESS_REVOKED,
            ),
            IndexingStatus.EXTRACTION_QUEUED to setOf(
                IndexingStatus.EXTRACTING,
                IndexingStatus.ACCESS_REVOKED,
            ),
            IndexingStatus.EXTRACTING to setOf(
                IndexingStatus.UNDERSTANDING_QUEUED,
                IndexingStatus.FAILED_RETRYABLE,
                IndexingStatus.ACCESS_REVOKED,
            ),
            IndexingStatus.UNDERSTANDING_QUEUED to setOf(
                IndexingStatus.UNDERSTANDING,
                IndexingStatus.ACCESS_REVOKED,
            ),
            IndexingStatus.UNDERSTANDING to setOf(
                IndexingStatus.INDEXED,
                IndexingStatus.FAILED_RETRYABLE,
                IndexingStatus.ACCESS_REVOKED,
            ),
            IndexingStatus.INDEXED to setOf(
                IndexingStatus.EXTRACTION_QUEUED,
                IndexingStatus.ACCESS_REVOKED,
            ),
            IndexingStatus.FAILED_RETRYABLE to setOf(
                IndexingStatus.EXTRACTION_QUEUED,
                IndexingStatus.ACCESS_REVOKED,
            ),
            IndexingStatus.ACCESS_REVOKED to setOf(IndexingStatus.EXTRACTION_QUEUED),
        )
    }
}
