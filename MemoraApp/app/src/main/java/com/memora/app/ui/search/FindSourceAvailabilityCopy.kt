package com.memora.app.ui.search

/**
 * Standing Open-honesty on Find cards (ADR-056 / I16 / R1).
 *
 * List density: one or two short sentences. Does not claim a single cause
 * (delete vs move vs revoked grant). Does not hide the hit or the Why.
 */
object FindSourceAvailabilityCopy {
    const val FILE_ORIGINAL_UNREACHABLE =
        "Original cannot be opened. Memory and Why are still here."

    const val NOTE_ORIGINAL_UNREACHABLE =
        "That note cannot be opened. Memory and Why are still here."
}
