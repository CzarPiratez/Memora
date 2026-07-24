package com.memora.app.data.security

import android.content.Context
import java.io.File

/**
 * Deletes only Memora-owned encrypted database identity files and the Keystore wrap
 * alias. Never releases Android persistable URI grants or mutates user source files.
 */
object MemoraDerivedDataClearer {
    fun clearOwnedState(context: Context) {
        val appContext = context.applicationContext
        KeystoreDatabasePassphraseStore(
            context = appContext,
            keyAlias = ProductionDatabaseIdentity.KEY_ALIAS,
            wrapperFileName = ProductionDatabaseIdentity.WRAPPER_FILE,
        ).clearOwnedState()
        DatabaseEncryptionConversionJournal(
            context = appContext,
            journalFileName = ProductionDatabaseIdentity.JOURNAL_FILE,
        ).clearOwnedState()
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
