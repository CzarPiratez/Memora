package com.memora.app.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.local.AssetEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Device-lock deferral must not create a second database or mutate existing files.
 */
@RunWith(AndroidJUnit4::class)
class DeviceUnlockDeferredOpenIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        ProductionDatabaseTestCleanup.clearAll(context)
        MemoraEncryptedDatabaseOpener.setUnlockGateForTest(null)
        MemoraEncryptedDatabaseOpener.clearLastFailureCategoryForTest()
    }

    @After
    fun tearDown() {
        MemoraEncryptedDatabaseOpener.setUnlockGateForTest(null)
        MemoraEncryptedDatabaseOpener.clearLastFailureCategoryForTest()
        ProductionDatabaseTestCleanup.clearAll(context)
    }

    @Test
    fun locked_gate_defers_open_without_mutating_existing_encrypted_database() = runBlocking {
        val seeded = MemoraEncryptedDatabaseOpener.open(context)
        try {
            seeded.assetDao().upsert(fixtureAsset())
            assertEquals(1, seeded.assetDao().count())
        } finally {
            seeded.close()
        }

        val production = context.getDatabasePath(ProductionDatabaseIdentity.DATABASE_NAME)
        val lengthBefore = production.length()
        val wrapperBefore = File(
            context.noBackupFilesDir,
            ProductionDatabaseIdentity.WRAPPER_FILE,
        ).readBytes()

        MemoraEncryptedDatabaseOpener.setUnlockGateForTest(
            UserCredentialUnlockGate { false },
        )
        MemoraEncryptedDatabaseOpener.clearLastFailureCategoryForTest()

        assertThrows(DeviceLockedException::class.java) {
            MemoraEncryptedDatabaseOpener.open(context)
        }
        assertEquals(
            DatabaseSecretFailureCategory.WAITING_FOR_USER_UNLOCK,
            MemoraEncryptedDatabaseOpener.lastFailureCategory,
        )
        assertTrue(production.exists())
        assertEquals(lengthBefore, production.length())
        assertTrue(
            wrapperBefore.contentEquals(
                File(context.noBackupFilesDir, ProductionDatabaseIdentity.WRAPPER_FILE).readBytes(),
            ),
        )
        assertFalse(
            context.getDatabasePath(ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME).exists(),
        )
        assertFalse(
            context.getDatabasePath("memora_second.db").exists(),
        )

        val handle = MemoraDatabaseHandle(context)
        assertEquals(DatabaseOpenAvailability.WaitingForUnlock, handle.availability())

        MemoraEncryptedDatabaseOpener.setUnlockGateForTest(null)
        assertEquals(DatabaseOpenAvailability.Ready, handle.availability())
        try {
            assertEquals(1, handle.database().assetDao().count())
            assertEquals(FIXTURE_MARKER, handle.database().assetDao().findAll().single().fingerprint)
        } finally {
            handle.database().close()
        }
    }

    @Test
    fun locked_gate_does_not_create_database_on_fresh_install() {
        MemoraEncryptedDatabaseOpener.setUnlockGateForTest(
            UserCredentialUnlockGate { false },
        )

        assertThrows(DeviceLockedException::class.java) {
            MemoraEncryptedDatabaseOpener.open(context)
        }
        assertEquals(
            DatabaseSecretFailureCategory.WAITING_FOR_USER_UNLOCK,
            MemoraEncryptedDatabaseOpener.lastFailureCategory,
        )
        assertFalse(context.getDatabasePath(ProductionDatabaseIdentity.DATABASE_NAME).exists())
        assertFalse(
            File(context.noBackupFilesDir, ProductionDatabaseIdentity.WRAPPER_FILE).exists(),
        )
        assertFalse(
            context.getDatabasePath(ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME).exists(),
        )
    }

    private fun fixtureAsset(): AssetEntity = AssetEntity(
        sourceId = "unlock-source",
        sourceAssetKey = "unlock-asset-1",
        assetType = "PDF",
        location = "content://com.memora.unlock/document/1",
        fingerprint = FIXTURE_MARKER,
        discoveredAtEpochMillis = 1_700_000_000_000L,
        displayName = "unlock-fixture.pdf",
        sourceModifiedAtEpochMillis = 1_700_000_000_050L,
        indexingStatus = "DISCOVERED",
        indexingAttemptCount = 0,
        failureCode = null,
        failureMessage = null,
    )

    companion object {
        private const val FIXTURE_MARKER = "MEMORA_UNLOCK_FIXTURE_v1"
    }
}
