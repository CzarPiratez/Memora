package com.memora.app.data.security

import android.content.Context

/**
 * Best-effort cleanup of production database identity files used by disposable
 * instrumentation proofs. Leaves the live app able to create a fresh encrypted DB.
 */
object ProductionDatabaseTestCleanup {
    fun clearAll(context: Context) {
        MemoraDerivedDataClearer.clearOwnedState(context)
    }
}
