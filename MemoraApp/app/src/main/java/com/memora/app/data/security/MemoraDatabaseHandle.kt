package com.memora.app.data.security

import android.content.Context
import com.memora.app.data.local.MemoraDatabase
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Owns the live production [MemoraDatabase] so user-confirmed derived-data clearing can
 * close, delete Memora-owned files, and reopen without requiring a process kill.
 */
class MemoraDatabaseHandle(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val lock = ReentrantLock()
    private var database: MemoraDatabase = MemoraEncryptedDatabaseOpener.open(appContext)

    fun database(): MemoraDatabase = lock.withLock { database }

    /**
     * Closes the open helper, deletes Memora-owned DB/wrapper/journal/Keystore state,
     * then opens a fresh encrypted empty database. Does not touch Android URI grants.
     */
    fun clearUserConfirmedDerivedData() {
        lock.withLock {
            database.close()
            MemoraDerivedDataClearer.clearOwnedState(appContext)
            database = MemoraEncryptedDatabaseOpener.open(appContext)
        }
    }
}
