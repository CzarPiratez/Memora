package com.memora.app.application.privacy

import com.memora.app.data.security.MemoraDatabaseHandle
import javax.inject.Inject

/**
 * User-confirmed clearing of Memora-owned derived index state only.
 * Recovery copy never mentions SQLCipher, keys, or encryption failures (ADR-021).
 */
class ClearMemoraDerivedData @Inject constructor(
    private val databaseHandle: MemoraDatabaseHandle,
) {
    operator fun invoke(): ClearMemoraDerivedDataResult = try {
        databaseHandle.clearUserConfirmedDerivedData()
        ClearMemoraDerivedDataResult.Cleared(APPROVED_REBUILD_MESSAGE)
    } catch (_: Exception) {
        ClearMemoraDerivedDataResult.Failed
    }

    companion object {
        const val APPROVED_REBUILD_MESSAGE =
            "Your private Memora index needs to be rebuilt. Your original photos, documents, and notes are unchanged."
    }
}

sealed interface ClearMemoraDerivedDataResult {
    data class Cleared(val message: String) : ClearMemoraDerivedDataResult

    data object Failed : ClearMemoraDerivedDataResult
}
