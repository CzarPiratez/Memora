package com.memora.app.application.notes

import com.memora.app.domain.asset.SourceAvailabilityStatus

/**
 * Local Open-class learning for notes. Not a search probe and not ranking.
 *
 * A stored OneNote URL is enough to hand off without a session. Missing the
 * Asset is a confirmed gone original. Missing both the URL and a vaulted
 * grant is what Open would return as SourceUnavailable — presentation only,
 * so Connect can resurrect without a tap.
 */
data class NoteOpenClassLearning(
    val status: SourceAvailabilityStatus,
    val persistUnreachable: Boolean,
) {
    init {
        require(status == SourceAvailabilityStatus.UNREACHABLE) {
            "Note Open-class only fills standing unreachable; it never invents REACHABLE."
        }
        require(!persistUnreachable || status == SourceAvailabilityStatus.UNREACHABLE)
    }
}

object NoteOpenClassPolicy {
    fun learn(
        assetPresent: Boolean,
        openTargetPresent: Boolean,
        vaultedGrantPresent: Boolean,
    ): NoteOpenClassLearning? {
        if (!assetPresent) {
            return NoteOpenClassLearning(
                status = SourceAvailabilityStatus.UNREACHABLE,
                persistUnreachable = true,
            )
        }
        if (openTargetPresent) return null
        if (vaultedGrantPresent) return null
        return NoteOpenClassLearning(
            status = SourceAvailabilityStatus.UNREACHABLE,
            persistUnreachable = false,
        )
    }
}
