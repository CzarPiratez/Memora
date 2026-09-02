package com.memora.app.domain.grounding

/**
 * Selects packaging and answer shape (`GROUNDING_ARCHITECTURE.md` §5).
 *
 * v1 implementation may hard-code [ANSWER_QUESTION] only.
 */
enum class ReasoningTask {
    ANSWER_QUESTION,
    LIST,
    EXTRACT,
    COMPARE,
    SUMMARIZE,
    TIMELINE,
    CLASSIFY,
}
