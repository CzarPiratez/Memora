package com.memora.app.data.security

import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Probes whether a database file opens with platform SQLite (no SQLCipher passphrase).
 *
 * Always probes a temporary copy. On some OEM/API builds, opening an encrypted SQLCipher
 * file with standard SQLite can delete or truncate the path under test; the live Memora
 * database must never be risked by a read-only probe.
 */
object StandardSqliteDatabaseProbe {
    fun canOpenWithoutPassphrase(databaseFile: File): Boolean {
        if (!databaseFile.exists()) {
            return false
        }
        val probeDirectory = databaseFile.parentFile ?: return false
        val probeCopy = File.createTempFile("memora-sqlite-probe-", ".db", probeDirectory)
        try {
            databaseFile.copyTo(probeCopy, overwrite = true)
            return try {
                SQLiteDatabase.openDatabase(
                    probeCopy.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY,
                ).use { database ->
                    database.rawQuery("SELECT 1", null).use { cursor ->
                        cursor.moveToFirst()
                    }
                }
                true
            } catch (_: Exception) {
                false
            }
        } finally {
            deleteSidecars(probeCopy)
            probeCopy.delete()
        }
    }

    private fun deleteSidecars(base: File) {
        File(base.path + "-wal").delete()
        File(base.path + "-shm").delete()
        File(base.path + "-journal").delete()
    }
}
