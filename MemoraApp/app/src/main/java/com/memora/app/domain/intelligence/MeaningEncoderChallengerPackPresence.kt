package com.memora.app.domain.intelligence

/** Presence check for the ADR-055 BGE challenger pack (probe-only). */
fun interface MeaningEncoderChallengerPackPresence {
    fun isInstalled(): Boolean
}
