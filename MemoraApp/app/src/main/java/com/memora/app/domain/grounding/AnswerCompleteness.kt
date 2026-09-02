package com.memora.app.domain.grounding

/**
 * Completeness signal from evidence packaging (`GROUNDING_ARCHITECTURE.md` §7.3).
 */
enum class AnswerCompleteness {
    COMPLETE,
    PARTIAL,
    UNKNOWN,
}
