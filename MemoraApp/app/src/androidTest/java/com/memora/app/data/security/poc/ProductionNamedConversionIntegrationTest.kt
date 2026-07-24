package com.memora.app.data.security.poc

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.di.PersistenceModule
import com.memora.app.data.local.AssetEntity
import com.memora.app.data.local.DiscoveryCheckpointEntity
import com.memora.app.data.local.DocumentTreeApprovalEntity
import com.memora.app.data.security.DatabaseEncryptionConversionJournal
import com.memora.app.data.security.DatabaseEncryptionConversionPhase
import com.memora.app.data.security.DatabaseSecretFailureCategory
import com.memora.app.data.security.KeystoreDatabasePassphraseStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Production-named disposable conversion corpus.
 * Uses memora.db naming inside instrumentation only, then deletes those files so the
 * live PersistenceModule plaintext path is not left encrypted.
 */
@RunWith(AndroidJUnit4::class)
class ProductionNamedConversionIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var passphraseStore: KeystoreDatabasePassphraseStore
    private lateinit var journal: DatabaseEncryptionConversionJournal
    private lateinit var harness: PlaintextToEncryptedConversionHarness

    @Before
    fun setUp() {
        passphraseStore = KeystoreDatabasePassphraseStore(
            context = context,
            keyAlias = PlaintextToEncryptedConversionHarness.PRODUCTION_KEY_ALIAS,
            wrapperFileName = PlaintextToEncryptedConversionHarness.PRODUCTION_WRAPPER_FILE,
        )
        journal = DatabaseEncryptionConversionJournal(
            context = context,
            journalFileName = PlaintextToEncryptedConversionHarness.PRODUCTION_JOURNAL_FILE,
        )
        harness = PlaintextToEncryptedConversionHarness.productionNamed(
            context = context,
            passphraseStore = passphraseStore,
            journal = journal,
        )
        clearDisposableProductionFiles()
    }

    @After
    fun tearDown() {
        clearDisposableProductionFiles()
    }

    @Test
    fun production_persistence_module_still_opens_plaintext_memora_db_name() {
        val field = PersistenceModule::class.java.getDeclaredField("DATABASE_NAME")
        field.isAccessible = true
        assertEquals(
            PlaintextToEncryptedConversionHarness.PRODUCTION_DATABASE_NAME,
            field.get(PersistenceModule),
        )
    }

    @Test
    fun converts_and_renames_encrypted_candidate_onto_memora_db() = runBlocking {
        seedPlaintextFixtures()

        val pending = harness.convertCopyAndValidate() as ConversionHarnessResult.ValidatedSwitchPending
        assertEquals(
            PlaintextToEncryptedConversionHarness.PRODUCTION_DATABASE_NAME,
            pending.plaintextDatabaseName,
        )
        assertEquals(
            PlaintextToEncryptedConversionHarness.PRODUCTION_ENCRYPTED_CANDIDATE_NAME,
            pending.encryptedDatabaseName,
        )
        assertTrue(pending.plaintextRetained)
        assertTrue(harness.productionDatabaseExists())
        assertTrue(
            context.getDatabasePath(
                PlaintextToEncryptedConversionHarness.PRODUCTION_ENCRYPTED_CANDIDATE_NAME,
            ).exists(),
        )

        val completed = harness.finalizeByRenamingEncryptedToProductionName()
            as ConversionHarnessResult.Completed
        assertEquals(
            PlaintextToEncryptedConversionHarness.PRODUCTION_DATABASE_NAME,
            completed.encryptedDatabaseName,
        )
        assertTrue(completed.plaintextDeleted)
        assertEquals(DatabaseEncryptionConversionPhase.COMPLETED, journal.read().phase)
        assertTrue(harness.productionDatabaseExists())
        assertFalse(
            context.getDatabasePath(
                PlaintextToEncryptedConversionHarness.PRODUCTION_ENCRYPTED_CANDIDATE_NAME,
            ).exists(),
        )
        assertFalse(
            context.getDatabasePath(
                PlaintextToEncryptedConversionHarness.PRODUCTION_PLAINTEXT_RETAINED_NAME,
            ).exists(),
        )

        val reopened = harness.reopenEncryptedAtProductionName()
        val database = (reopened as EncryptedPocOpenResult.Opened).database
        try {
            assertEquals(1, database.assetDao().count())
            assertEquals(
                PlaintextToEncryptedConversionHarness.PRODUCTION_FIXTURE_MARKER,
                database.assetDao().findAll().single().fingerprint,
            )
        } finally {
            database.close()
        }

        assertFalse(
            "Standard SQLite must not open production-named encrypted memora.db.",
            probeStandardSqlite(
                PlaintextToEncryptedConversionHarness.PRODUCTION_DATABASE_NAME,
            ),
        )
    }

    @Test
    fun validation_failure_keeps_production_named_plaintext() = runBlocking {
        seedPlaintextFixtures()
        val before = context.getDatabasePath(
            PlaintextToEncryptedConversionHarness.PRODUCTION_DATABASE_NAME,
        ).length()

        val denied = harness.convertCopyAndValidate(corruptEncryptedAfterCopyForTest = true)
        assertEquals(
            ConversionHarnessResult.Denied(
                DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED,
            ),
            denied,
        )
        assertTrue(harness.productionDatabaseExists())
        assertFalse(
            context.getDatabasePath(
                PlaintextToEncryptedConversionHarness.PRODUCTION_ENCRYPTED_CANDIDATE_NAME,
            ).exists(),
        )
        assertEquals(
            before,
            context.getDatabasePath(
                PlaintextToEncryptedConversionHarness.PRODUCTION_DATABASE_NAME,
            ).length(),
        )

        val plaintext = harness.openPlaintext()
        try {
            assertEquals(
                PlaintextToEncryptedConversionHarness.PRODUCTION_FIXTURE_MARKER,
                plaintext.assetDao().findAll().single().fingerprint,
            )
        } finally {
            plaintext.close()
        }
    }

    private fun clearDisposableProductionFiles() {
        passphraseStore.clearForTest()
        journal.clearForTest()
        harness.deleteAllHarnessFiles()
        EncryptedPocDatabaseFactory.deleteDatabaseFiles(context)
    }

    private suspend fun seedPlaintextFixtures() {
        val plaintext = harness.openPlaintext()
        try {
            plaintext.assetDao().upsert(
                AssetEntity(
                    sourceId = "production-named-source",
                    sourceAssetKey = "production-named-asset-1",
                    assetType = "PDF",
                    location = "content://com.memora.poc/production-named/1",
                    fingerprint = PlaintextToEncryptedConversionHarness.PRODUCTION_FIXTURE_MARKER,
                    discoveredAtEpochMillis = 1_700_000_200_000L,
                    displayName = "production-named-fixture.pdf",
                    sourceModifiedAtEpochMillis = 1_700_000_200_050L,
                    indexingStatus = "DISCOVERED",
                    indexingAttemptCount = 0,
                    failureCode = null,
                    failureMessage = null,
                ),
            )
            plaintext.discoveryCheckpointDao().upsert(
                DiscoveryCheckpointEntity(
                    sourceId = "production-named-source",
                    cursorValue = "production-named-cursor-1",
                    savedAtEpochMillis = 1_700_000_200_000L,
                ),
            )
            plaintext.documentTreeApprovalDao().upsert(
                DocumentTreeApprovalEntity(
                    sourceId = "production-named-source",
                    treeUri = "content://com.memora.poc/tree/production-named",
                    approvedAtEpochMillis = 1_700_000_200_100L,
                ),
            )
        } finally {
            plaintext.close()
        }
    }

    private fun probeStandardSqlite(databaseName: String): Boolean {
        val file = context.getDatabasePath(databaseName)
        if (!file.exists()) {
            return false
        }
        return try {
            SQLiteDatabase.openDatabase(
                file.path,
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
    }
}
