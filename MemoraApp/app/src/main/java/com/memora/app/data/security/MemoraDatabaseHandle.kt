package com.memora.app.data.security

import android.content.Context
import com.memora.app.data.local.MemoraDatabase
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Owns the live production [MemoraDatabase] so user-confirmed derived-data clearing can
 * close, delete Memora-owned files, and reopen without requiring a process kill.
 *
 * Open is deferred while the user profile is locked (credential-encrypted storage).
 */
class MemoraDatabaseHandle(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val lock = ReentrantLock()
    private var database: MemoraDatabase? = null

    fun availability(): DatabaseOpenAvailability = lock.withLock { resolveAvailabilityLocked() }

    fun database(): MemoraDatabase = lock.withLock {
        when (val state = resolveAvailabilityLocked()) {
            DatabaseOpenAvailability.Ready -> checkNotNull(database)
            DatabaseOpenAvailability.WaitingForUnlock -> throw DeviceLockedException()
        }
    }

    /**
     * Closes the open helper, deletes Memora-owned DB/wrapper/journal/Keystore state,
     * then opens a fresh encrypted empty database. Does not touch Android URI grants.
     */
    fun clearUserConfirmedDerivedData() {
        lock.withLock {
            if (!MemoraEncryptedDatabaseOpener.isUserUnlocked(appContext)) {
                throw DeviceLockedException()
            }
            database?.close()
            database = null
            MemoraDerivedDataClearer.clearOwnedState(appContext)
            database = MemoraEncryptedDatabaseOpener.open(appContext)
        }
    }

    private fun resolveAvailabilityLocked(): DatabaseOpenAvailability {
        database?.let { return DatabaseOpenAvailability.Ready }
        if (!MemoraEncryptedDatabaseOpener.isUserUnlocked(appContext)) {
            return DatabaseOpenAvailability.WaitingForUnlock
        }
        return try {
            database = MemoraEncryptedDatabaseOpener.open(appContext)
            DatabaseOpenAvailability.Ready
        } catch (_: DeviceLockedException) {
            DatabaseOpenAvailability.WaitingForUnlock
        }
    }
}
