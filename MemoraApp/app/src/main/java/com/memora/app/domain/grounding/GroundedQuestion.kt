package com.memora.app.domain.grounding

/**
 * User question for Grounded Answers — distinct from Find browse queries.
 */
@JvmInline
value class GroundedQuestion(val value: String) {
    init {
        require(value.isNotBlank()) { "A grounded question cannot be blank." }
    }
}
