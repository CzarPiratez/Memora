package com.memora.app.domain.privacy

/**
 * Clears Memora-owned derived index state after explicit user confirmation.
 *
 * Does not revoke Android URI grants or alter original user sources.
 */
interface UserConfirmedDerivedDataClearer {
    fun clear()
}
