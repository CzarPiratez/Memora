package com.memora.app.data.security.poc

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.memora.app.data.local.AssetEntity
import com.memora.app.data.local.DiscoveryCheckpointEntity
import com.memora.app.data.local.DocumentTreeApprovalEntity
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.security.DatabaseEncryptionConversionJournal
import com.memora.app.data.security.DatabaseEncryptionConversionPhase
import com.memora.app.data.security.DatabaseEncryptionConversionRecord
import com.memora.app.data.security.DatabaseSecretFailureCategory
import com.memora.app.data.security.KeystoreDatabasePassphraseStore
import com.memora.app.data.security.PassphraseUnwrapResult
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * Synthetic plaintext-to-encrypted copy-and-validate harness.
 * Uses separately named PoC databases only. Never touches production `memora.db`.
 */
class PlaintextToEncryptedConversionHarness(
    context: Context,
    private val passphraseStore: KeystoreDatabasePassphraseStore,
    private val journal: DatabaseEncryptionConversionJournal,
    private val plaintextDatabaseName: String = PLAINTEXT_DATABASE_NAME,
    private val encryptedDatabaseName: String = ENCRYPTED_CANDIDATE_DATABASE_NAME,
) {
    private val appContext = context.applicationContext

    suspend fun convertCopyAndValidate(
        corruptEncryptedAfterCopyForTest: Boolean = false,
    ): ConversionHarnessResult {
        cleanupIncompleteEncryptedCandidateIfNeeded()

        val plaintext = openPlaintext()
        try {
            if (plaintext.openHelper.readableDatabase.version != EXPECTED_SCHEMA_VERSION) {
                return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
            }
            journal.write(
                DatabaseEncryptionConversionRecord(
                    phase = DatabaseEncryptionConversionPhase.PLAINTEXT_VALIDATED,
                    schemaVersion = EXPECTED_SCHEMA_VERSION,
                ),
            )

            val snapshot = Snapshot.from(plaintext)
            deleteDatabaseFiles(encryptedDatabaseName)

            val encryptedOpen = createEncryptedCandidate()
            val encrypted = when (encryptedOpen) {
                is EncryptedPocOpenResult.Opened -> encryptedOpen.database
                is EncryptedPocOpenResult.Denied -> {
                    return failSafe(encryptedOpen.category)
                }
            }

            try {
                journal.write(
                    DatabaseEncryptionConversionRecord(
                        phase = DatabaseEncryptionConversionPhase.CANDIDATE_CREATED,
                        schemaVersion = EXPECTED_SCHEMA_VERSION,
                    ),
                )
                copyRows(snapshot, encrypted)
                journal.write(
                    DatabaseEncryptionConversionRecord(
                        phase = DatabaseEncryptionConversionPhase.ROWS_COPIED,
                        schemaVersion = EXPECTED_SCHEMA_VERSION,
                    ),
                )
                if (corruptEncryptedAfterCopyForTest) {
                    encrypted.openHelper.writableDatabase.execSQL("DELETE FROM assets")
                }

                if (!validateAgainstSnapshot(encrypted, snapshot)) {
                    encrypted.close()
                    deleteDatabaseFiles(encryptedDatabaseName)
                    return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
                }
                journal.write(
                    DatabaseEncryptionConversionRecord(
                        phase = DatabaseEncryptionConversionPhase.CANDIDATE_VALIDATED,
                        schemaVersion = EXPECTED_SCHEMA_VERSION,
                    ),
                )
            } catch (_: Exception) {
                encrypted.close()
                deleteDatabaseFiles(encryptedDatabaseName)
                return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
            }

            encrypted.close()
        } finally {
            plaintext.close()
        }

        // Simulated process boundary: reopen encrypted only, prove switch readiness.
        val reopened = reopenEncrypted()
        val verified = when (reopened) {
            is EncryptedPocOpenResult.Opened -> reopened.database
            is EncryptedPocOpenResult.Denied -> {
                deleteDatabaseFiles(encryptedDatabaseName)
                return failSafe(reopened.category)
            }
        }
        try {
            val plaintextStillOpenable = openPlaintext()
            val snapshot = try {
                Snapshot.from(plaintextStillOpenable)
            } finally {
                plaintextStillOpenable.close()
            }
            if (!validateAgainstSnapshot(verified, snapshot)) {
                deleteDatabaseFiles(encryptedDatabaseName)
                return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
            }
            journal.write(
                DatabaseEncryptionConversionRecord(
                    phase = DatabaseEncryptionConversionPhase.SWITCH_PENDING,
                    schemaVersion = EXPECTED_SCHEMA_VERSION,
                ),
            )
        } finally {
            verified.close()
        }

        return ConversionHarnessResult.ValidatedSwitchPending(
            plaintextDatabaseName = plaintextDatabaseName,
            encryptedDatabaseName = encryptedDatabaseName,
            plaintextRetained = databaseFile(plaintextDatabaseName).exists(),
        )
    }

    fun finalizeAfterValidatedSwitch(): ConversionHarnessResult {
        val record = journal.read()
        if (record.phase != DatabaseEncryptionConversionPhase.SWITCH_PENDING) {
            return ConversionHarnessResult.Denied(
                DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED,
            )
        }
        if (!databaseFile(encryptedDatabaseName).exists()) {
            return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
        }
        if (!databaseFile(plaintextDatabaseName).exists()) {
            return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
        }

        deleteDatabaseFiles(plaintextDatabaseName)
        if (databaseFile(plaintextDatabaseName).exists()) {
            return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
        }

        journal.write(
            DatabaseEncryptionConversionRecord(
                phase = DatabaseEncryptionConversionPhase.COMPLETED,
                schemaVersion = EXPECTED_SCHEMA_VERSION,
            ),
        )
        return ConversionHarnessResult.Completed(
            encryptedDatabaseName = encryptedDatabaseName,
            plaintextDeleted = !databaseFile(plaintextDatabaseName).exists(),
        )
    }

    /**
     * Production naming finalize: keep [PRODUCTION_DATABASE_NAME] as the durable file
     * name by renaming the encrypted candidate onto it after moving plaintext aside.
     * Instrumentation must delete these disposable files in tearDown so the live
     * live encrypted [PersistenceModule] identity is not left with harness residue.
     */
    fun finalizeByRenamingEncryptedToProductionName(): ConversionHarnessResult {
        val record = journal.read()
        if (record.phase != DatabaseEncryptionConversionPhase.SWITCH_PENDING) {
            return ConversionHarnessResult.Denied(
                DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED,
            )
        }
        if (plaintextDatabaseName != PRODUCTION_DATABASE_NAME) {
            return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
        }
        if (!databaseFile(encryptedDatabaseName).exists() ||
            !databaseFile(plaintextDatabaseName).exists()
        ) {
            return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
        }

        deleteDatabaseFiles(PRODUCTION_PLAINTEXT_RETAINED_NAME)
        if (!renameDatabaseFiles(plaintextDatabaseName, PRODUCTION_PLAINTEXT_RETAINED_NAME)) {
            return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
        }
        if (!renameDatabaseFiles(encryptedDatabaseName, PRODUCTION_DATABASE_NAME)) {
            // Best-effort restore of plaintext identity.
            renameDatabaseFiles(PRODUCTION_PLAINTEXT_RETAINED_NAME, PRODUCTION_DATABASE_NAME)
            return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
        }

        val reopened = openEncryptedDatabase(PRODUCTION_DATABASE_NAME)
        when (reopened) {
            is EncryptedPocOpenResult.Opened -> reopened.database.close()
            is EncryptedPocOpenResult.Denied -> {
                renameDatabaseFiles(PRODUCTION_DATABASE_NAME, encryptedDatabaseName)
                renameDatabaseFiles(PRODUCTION_PLAINTEXT_RETAINED_NAME, PRODUCTION_DATABASE_NAME)
                return failSafe(reopened.category)
            }
        }

        deleteDatabaseFiles(PRODUCTION_PLAINTEXT_RETAINED_NAME)
        if (databaseFile(PRODUCTION_PLAINTEXT_RETAINED_NAME).exists() ||
            databaseFile(encryptedDatabaseName).exists()
        ) {
            return failSafe(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
        }

        journal.write(
            DatabaseEncryptionConversionRecord(
                phase = DatabaseEncryptionConversionPhase.COMPLETED,
                schemaVersion = EXPECTED_SCHEMA_VERSION,
            ),
        )
        return ConversionHarnessResult.Completed(
            encryptedDatabaseName = PRODUCTION_DATABASE_NAME,
            plaintextDeleted = !databaseFile(PRODUCTION_PLAINTEXT_RETAINED_NAME).exists(),
        )
    }

    fun markInterruptedAfterRowsCopiedForTest() {
        journal.write(
            DatabaseEncryptionConversionRecord(
                phase = DatabaseEncryptionConversionPhase.ROWS_COPIED,
                schemaVersion = EXPECTED_SCHEMA_VERSION,
            ),
        )
    }

    fun openPlaintext(): MemoraDatabase =
        Room.databaseBuilder(
            appContext,
            MemoraDatabase::class.java,
            plaintextDatabaseName,
        ).build().also { it.openHelper.writableDatabase }

    fun reopenEncrypted(): EncryptedPocOpenResult = openEncryptedWithStore()

    fun reopenEncryptedAtProductionName(): EncryptedPocOpenResult =
        openEncryptedDatabase(PRODUCTION_DATABASE_NAME)

    fun plaintextExists(): Boolean = databaseFile(plaintextDatabaseName).exists()

    fun encryptedExists(): Boolean = databaseFile(encryptedDatabaseName).exists()

    fun productionDatabaseExists(): Boolean =
        databaseFile(PRODUCTION_DATABASE_NAME).exists()

    fun deleteAllHarnessFiles() {
        deleteDatabaseFiles(plaintextDatabaseName)
        deleteDatabaseFiles(encryptedDatabaseName)
        deleteDatabaseFiles(PRODUCTION_DATABASE_NAME)
        deleteDatabaseFiles(PRODUCTION_PLAINTEXT_RETAINED_NAME)
        deleteDatabaseFiles(PRODUCTION_ENCRYPTED_CANDIDATE_NAME)
    }

    private fun cleanupIncompleteEncryptedCandidateIfNeeded() {
        when (journal.read().phase) {
            DatabaseEncryptionConversionPhase.CANDIDATE_CREATED,
            DatabaseEncryptionConversionPhase.ROWS_COPIED,
            DatabaseEncryptionConversionPhase.FAILED_SAFE,
            -> {
                deleteDatabaseFiles(encryptedDatabaseName)
                journal.write(
                    DatabaseEncryptionConversionRecord(
                        phase = DatabaseEncryptionConversionPhase.NOT_STARTED,
                        schemaVersion = null,
                    ),
                )
            }
            DatabaseEncryptionConversionPhase.NOT_STARTED,
            DatabaseEncryptionConversionPhase.PLAINTEXT_VALIDATED,
            DatabaseEncryptionConversionPhase.CANDIDATE_VALIDATED,
            DatabaseEncryptionConversionPhase.SWITCH_PENDING,
            DatabaseEncryptionConversionPhase.COMPLETED,
            -> Unit
        }
    }

    private fun createEncryptedCandidate(): EncryptedPocOpenResult {
        passphraseStore.clearForTest()
        val created = passphraseStore.createAndWrapNewPassphrase()
        val passphrase = when (created) {
            is PassphraseUnwrapResult.Unwrapped -> created.passphrase
            is PassphraseUnwrapResult.Denied -> return EncryptedPocOpenResult.Denied(created.category)
        }
        return openEncrypted(passphrase)
    }

    private fun openEncryptedWithStore(): EncryptedPocOpenResult {
        val unwrapped = passphraseStore.unwrapExistingPassphrase()
        val passphrase = when (unwrapped) {
            is PassphraseUnwrapResult.Unwrapped -> unwrapped.passphrase
            is PassphraseUnwrapResult.Denied -> return EncryptedPocOpenResult.Denied(unwrapped.category)
        }
        return openEncryptedDatabase(encryptedDatabaseName, passphrase)
    }

    private fun openEncrypted(passphrase: ByteArray): EncryptedPocOpenResult =
        openEncryptedDatabase(encryptedDatabaseName, passphrase)

    private fun openEncryptedDatabase(databaseName: String): EncryptedPocOpenResult {
        val unwrapped = passphraseStore.unwrapExistingPassphrase()
        val passphrase = when (unwrapped) {
            is PassphraseUnwrapResult.Unwrapped -> unwrapped.passphrase
            is PassphraseUnwrapResult.Denied -> return EncryptedPocOpenResult.Denied(unwrapped.category)
        }
        return openEncryptedDatabase(databaseName, passphrase)
    }

    private fun openEncryptedDatabase(
        databaseName: String,
        passphrase: ByteArray,
    ): EncryptedPocOpenResult {
        return try {
            EncryptedPocDatabaseFactory.loadNativeLibrary()
            val factory = SupportOpenHelperFactory(passphrase.copyOf())
            val database = Room.databaseBuilder(
                appContext,
                MemoraDatabase::class.java,
                databaseName,
            )
                .openHelperFactory(factory)
                .build()
            database.openHelper.writableDatabase
            EncryptedPocOpenResult.Opened(database)
        } catch (_: Exception) {
            EncryptedPocOpenResult.Denied(DatabaseSecretFailureCategory.DATABASE_AUTH_FAILED)
        } finally {
            passphraseStore.clearPassphrase(passphrase)
        }
    }

    private suspend fun copyRows(snapshot: Snapshot, encrypted: MemoraDatabase) {
        encrypted.withTransaction {
            for (asset in snapshot.assets) {
                encrypted.assetDao().upsert(asset)
            }
            for (checkpoint in snapshot.checkpoints) {
                encrypted.discoveryCheckpointDao().upsert(checkpoint)
            }
            for (approval in snapshot.approvals) {
                encrypted.documentTreeApprovalDao().upsert(approval)
            }
        }
    }

    private suspend fun validateAgainstSnapshot(
        encrypted: MemoraDatabase,
        snapshot: Snapshot,
    ): Boolean {
        if (encrypted.openHelper.readableDatabase.version != EXPECTED_SCHEMA_VERSION) {
            return false
        }
        if (encrypted.assetDao().count() != snapshot.assets.size) return false
        if (encrypted.discoveryCheckpointDao().count() != snapshot.checkpoints.size) return false
        if (encrypted.documentTreeApprovalDao().count() != snapshot.approvals.size) return false
        if (encrypted.assetDao().findAll() != snapshot.assets) return false
        if (encrypted.discoveryCheckpointDao().findAll() != snapshot.checkpoints) return false
        if (encrypted.documentTreeApprovalDao().findAll() != snapshot.approvals) return false
        return true
    }

    private fun failSafe(category: DatabaseSecretFailureCategory): ConversionHarnessResult {
        journal.write(
            DatabaseEncryptionConversionRecord(
                phase = DatabaseEncryptionConversionPhase.FAILED_SAFE,
                schemaVersion = EXPECTED_SCHEMA_VERSION,
            ),
        )
        return ConversionHarnessResult.Denied(category)
    }

    private fun databaseFile(name: String): File = appContext.getDatabasePath(name)

    private fun deleteDatabaseFiles(name: String) {
        val base = databaseFile(name)
        base.delete()
        File(base.path + "-wal").delete()
        File(base.path + "-shm").delete()
        File(base.path + "-journal").delete()
    }

    private fun renameDatabaseFiles(fromName: String, toName: String): Boolean {
        val from = databaseFile(fromName)
        val to = databaseFile(toName)
        if (!from.exists()) {
            return false
        }
        deleteDatabaseFiles(toName)
        to.parentFile?.mkdirs()
        val mainRenamed = from.renameTo(to)
        if (!mainRenamed) {
            return false
        }
        listOf("-wal", "-shm", "-journal").forEach { suffix ->
            val source = File(from.path + suffix)
            if (source.exists()) {
                val target = File(to.path + suffix)
                target.delete()
                source.renameTo(target)
            }
        }
        return to.exists()
    }

    private data class Snapshot(
        val assets: List<AssetEntity>,
        val checkpoints: List<DiscoveryCheckpointEntity>,
        val approvals: List<DocumentTreeApprovalEntity>,
    ) {
        companion object {
            suspend fun from(database: MemoraDatabase): Snapshot = Snapshot(
                assets = database.assetDao().findAll(),
                checkpoints = database.discoveryCheckpointDao().findAll(),
                approvals = database.documentTreeApprovalDao().findAll(),
            )
        }
    }

    companion object {
        const val PLAINTEXT_DATABASE_NAME = "memora_plaintext_conversion_poc.db"
        const val ENCRYPTED_CANDIDATE_DATABASE_NAME = "memora_encrypted_conversion_poc.db"
        const val PRODUCTION_DATABASE_NAME = "memora.db"
        const val PRODUCTION_ENCRYPTED_CANDIDATE_NAME = "memora.db.encrypted_candidate"
        const val PRODUCTION_PLAINTEXT_RETAINED_NAME = "memora.db.plaintext_retained"
        const val EXPECTED_SCHEMA_VERSION = 4
        const val CONVERSION_KEY_ALIAS = "memora.poc.conversion.wrap.v1"
        const val CONVERSION_WRAPPER_FILE = "memora_poc_conversion_wrap_v1.bin"
        const val PRODUCTION_KEY_ALIAS = "memora.db.wrap.v1"
        const val PRODUCTION_WRAPPER_FILE = "memora_db_wrap_v1.bin"
        const val PRODUCTION_JOURNAL_FILE = "memora_db_conversion_v1.journal"
        const val FIXTURE_MARKER = "MEMORA_CONVERSION_FIXTURE_MARKER_v1"
        const val PRODUCTION_FIXTURE_MARKER = "MEMORA_PRODUCTION_NAMED_CONVERSION_MARKER_v1"

        fun productionNamed(
            context: Context,
            passphraseStore: KeystoreDatabasePassphraseStore,
            journal: DatabaseEncryptionConversionJournal,
        ): PlaintextToEncryptedConversionHarness = PlaintextToEncryptedConversionHarness(
            context = context,
            passphraseStore = passphraseStore,
            journal = journal,
            plaintextDatabaseName = PRODUCTION_DATABASE_NAME,
            encryptedDatabaseName = PRODUCTION_ENCRYPTED_CANDIDATE_NAME,
        )
    }
}

sealed class ConversionHarnessResult {
    data class ValidatedSwitchPending(
        val plaintextDatabaseName: String,
        val encryptedDatabaseName: String,
        val plaintextRetained: Boolean,
    ) : ConversionHarnessResult()

    data class Completed(
        val encryptedDatabaseName: String,
        val plaintextDeleted: Boolean,
    ) : ConversionHarnessResult()

    data class Denied(
        val category: DatabaseSecretFailureCategory,
    ) : ConversionHarnessResult()
}
