package com.memora.app.data.security.poc

import android.content.Context
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
 * Synthetic plaintext-to-encrypted conversion corpus.
 * Never opens production memora.db, user sources, UI, WorkManager, AI, or network.
 */
@RunWith(AndroidJUnit4::class)
class PlaintextToEncryptedConversionIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var passphraseStore: KeystoreDatabasePassphraseStore
    private lateinit var journal: DatabaseEncryptionConversionJournal
    private lateinit var harness: PlaintextToEncryptedConversionHarness

    @Before
    fun setUp() {
        passphraseStore = KeystoreDatabasePassphraseStore(
            context = context,
            keyAlias = PlaintextToEncryptedConversionHarness.CONVERSION_KEY_ALIAS,
            wrapperFileName = PlaintextToEncryptedConversionHarness.CONVERSION_WRAPPER_FILE,
        )
        journal = DatabaseEncryptionConversionJournal(context)
        harness = PlaintextToEncryptedConversionHarness(
            context = context,
            passphraseStore = passphraseStore,
            journal = journal,
        )
        passphraseStore.clearForTest()
        journal.clearForTest()
        harness.deleteAllHarnessFiles()
        EncryptedPocDatabaseFactory.deleteDatabaseFiles(context)
    }

    @After
    fun tearDown() {
        passphraseStore.clearForTest()
        journal.clearForTest()
        harness.deleteAllHarnessFiles()
    }

    @Test
    fun production_persistence_module_targets_memora_db_name() {
        assertEquals("memora.db", PersistenceModule.DATABASE_NAME)
        assertFalse(
            PlaintextToEncryptedConversionHarness.PLAINTEXT_DATABASE_NAME == "memora.db",
        )
        assertFalse(
            PlaintextToEncryptedConversionHarness.ENCRYPTED_CANDIDATE_DATABASE_NAME == "memora.db",
        )
    }

    @Test
    fun converts_schema_v3_fixture_rows_retains_plaintext_until_finalize() = runBlocking {
        seedPlaintextFixtures()

        val pending = harness.convertCopyAndValidate()
        val validated = pending as ConversionHarnessResult.ValidatedSwitchPending
        assertTrue(validated.plaintextRetained)
        assertTrue(harness.plaintextExists())
        assertTrue(harness.encryptedExists())
        assertEquals(
            DatabaseEncryptionConversionPhase.SWITCH_PENDING,
            journal.read().phase,
        )

        val reopened = harness.reopenEncrypted()
        val encrypted = (reopened as EncryptedPocOpenResult.Opened).database
        try {
            assertEquals(1, encrypted.assetDao().count())
            assertEquals(1, encrypted.discoveryCheckpointDao().count())
            assertEquals(1, encrypted.documentTreeApprovalDao().count())
            assertEquals(
                PlaintextToEncryptedConversionHarness.FIXTURE_MARKER,
                encrypted.assetDao().findAll().single().fingerprint,
            )
        } finally {
            encrypted.close()
        }

        val completed = harness.finalizeAfterValidatedSwitch() as ConversionHarnessResult.Completed
        assertTrue(completed.plaintextDeleted)
        assertFalse(harness.plaintextExists())
        assertTrue(harness.encryptedExists())
        assertEquals(DatabaseEncryptionConversionPhase.COMPLETED, journal.read().phase)
    }

    @Test
    fun empty_plaintext_converts_to_empty_encrypted_candidate() = runBlocking {
        harness.openPlaintext().close()

        val pending = harness.convertCopyAndValidate() as ConversionHarnessResult.ValidatedSwitchPending
        assertTrue(pending.plaintextRetained)

        val reopened = harness.reopenEncrypted()
        val encrypted = (reopened as EncryptedPocOpenResult.Opened).database
        try {
            assertEquals(0, encrypted.assetDao().count())
            assertEquals(0, encrypted.discoveryCheckpointDao().count())
            assertEquals(0, encrypted.documentTreeApprovalDao().count())
        } finally {
            encrypted.close()
        }
    }

    @Test
    fun validation_failure_keeps_plaintext_and_drops_bad_candidate() = runBlocking {
        seedPlaintextFixtures()
        val plaintextLength = context.getDatabasePath(
            PlaintextToEncryptedConversionHarness.PLAINTEXT_DATABASE_NAME,
        ).length()

        val denied = harness.convertCopyAndValidate(corruptEncryptedAfterCopyForTest = true)

        assertEquals(
            ConversionHarnessResult.Denied(
                DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED,
            ),
            denied,
        )
        assertTrue(harness.plaintextExists())
        assertFalse(harness.encryptedExists())
        assertEquals(plaintextLength, context.getDatabasePath(
            PlaintextToEncryptedConversionHarness.PLAINTEXT_DATABASE_NAME,
        ).length())
        assertEquals(DatabaseEncryptionConversionPhase.FAILED_SAFE, journal.read().phase)

        val plaintext = harness.openPlaintext()
        try {
            assertEquals(1, plaintext.assetDao().count())
            assertEquals(
                PlaintextToEncryptedConversionHarness.FIXTURE_MARKER,
                plaintext.assetDao().findAll().single().fingerprint,
            )
        } finally {
            plaintext.close()
        }
    }

    @Test
    fun interrupted_rows_copied_state_retries_without_destroying_plaintext() = runBlocking {
        seedPlaintextFixtures()
        val first = harness.convertCopyAndValidate() as ConversionHarnessResult.ValidatedSwitchPending
        assertTrue(first.plaintextRetained)

        // Simulate crash after a later retry left an incomplete copied candidate.
        harness.markInterruptedAfterRowsCopiedForTest()
        assertEquals(DatabaseEncryptionConversionPhase.ROWS_COPIED, journal.read().phase)

        val retry = harness.convertCopyAndValidate() as ConversionHarnessResult.ValidatedSwitchPending
        assertTrue(retry.plaintextRetained)
        assertTrue(harness.plaintextExists())
        assertTrue(harness.encryptedExists())

        val reopened = harness.reopenEncrypted()
        val encrypted = (reopened as EncryptedPocOpenResult.Opened).database
        try {
            assertEquals(1, encrypted.assetDao().count())
            assertEquals(1, encrypted.discoveryCheckpointDao().count())
            assertEquals(1, encrypted.documentTreeApprovalDao().count())
        } finally {
            encrypted.close()
        }
    }

    private suspend fun seedPlaintextFixtures() {
        val plaintext = harness.openPlaintext()
        try {
            plaintext.assetDao().upsert(
                AssetEntity(
                    sourceId = "conversion-source",
                    sourceAssetKey = "conversion-asset-1",
                    assetType = "PDF",
                    location = "content://com.memora.poc/conversion/1",
                    fingerprint = PlaintextToEncryptedConversionHarness.FIXTURE_MARKER,
                    discoveredAtEpochMillis = 1_700_000_100_000L,
                    displayName = "conversion-fixture.pdf",
                    sourceModifiedAtEpochMillis = 1_700_000_100_050L,
                    indexingStatus = "DISCOVERED",
                    indexingAttemptCount = 0,
                    failureCode = null,
                    failureMessage = null,
                ),
            )
            plaintext.discoveryCheckpointDao().upsert(
                DiscoveryCheckpointEntity(
                    sourceId = "conversion-source",
                    cursorValue = "conversion-cursor-1",
                    savedAtEpochMillis = 1_700_000_100_000L,
                ),
            )
            plaintext.documentTreeApprovalDao().upsert(
                DocumentTreeApprovalEntity(
                    sourceId = "conversion-source",
                    treeUri = "content://com.memora.poc/tree/conversion",
                    approvedAtEpochMillis = 1_700_000_100_100L,
                ),
            )
        } finally {
            plaintext.close()
        }
    }
}
