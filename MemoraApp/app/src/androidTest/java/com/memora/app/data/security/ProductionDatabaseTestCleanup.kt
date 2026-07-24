package com.memora.app.data.security

import android.content.Context
import java.io.File

/**
 * Best-effort cleanup of production database identity files used by disposable
 * instrumentation proofs. Leaves the live app able to create a fresh encrypted DB.
 */
object ProductionDatabaseTestCleanup {
    fun clearAll(context: Context) {
        val appContext = context.applicationContext
        KeystoreDatabasePassphraseStore(
            context = appContext,
            keyAlias = ProductionDatabaseIdentity.KEY_ALIAS,
            wrapperFileName = ProductionDatabaseIdentity.WRAPPER_FILE,
        ).clearForTest()
        DatabaseEncryptionConversionJournal(
            context = appContext,
            journalFileName = ProductionDatabaseIdentity.JOURNAL_FILE,
        ).clearForTest()
        listOf(
            ProductionDatabaseIdentity.DATABASE_NAME,
            ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME,
            ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME,
        ).forEach { name ->
            deleteDatabaseFiles(appContext, name)
        }
    }

    private fun deleteDatabaseFiles(context: Context, name: String) {
        val base = context.getDatabasePath(name)
        base.delete()
        File(base.path + "-wal").delete()
        File(base.path + "-shm").delete()
        File(base.path + "-journal").delete()
    }
}
