package com.memora.app.data.security

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.MemoraDatabaseMigrations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * Opens production [MemoraDatabase] through SQLCipher with a Keystore-wrapped passphrase.
 * Fresh installs create an encrypted database. Existing plaintext `memora.db` is converted
 * with copy-and-validate rename finalize before repository traffic is served.
 */
object MemoraEncryptedDatabaseOpener {
    @Volatile
    private var testStopAfterPhase: DatabaseEncryptionConversionPhase? = null

    @Volatile
    private var storageGuard: ConversionStorageGuard = StatFsConversionStorageGuard

    @Volatile
    private var unlockGate: UserCredentialUnlockGate = AndroidUserCredentialUnlockGate

    @Volatile
    private var testForceIoFailureDuringConversion: Boolean = false

    @Volatile
    var lastFailureCategory: DatabaseSecretFailureCategory? = null
        private set

    fun setStorageGuardForTest(guard: ConversionStorageGuard?) {
        storageGuard = guard ?: StatFsConversionStorageGuard
    }

    fun setUnlockGateForTest(gate: UserCredentialUnlockGate?) {
        unlockGate = gate ?: AndroidUserCredentialUnlockGate
    }

    fun isUserUnlocked(context: Context): Boolean =
        unlockGate.isUserUnlocked(context.applicationContext)

    fun setForceIoFailureDuringConversionForTest(force: Boolean) {
        testForceIoFailureDuringConversion = force
    }

    fun clearLastFailureCategoryForTest() {
        lastFailureCategory = null
    }

    fun open(context: Context): MemoraDatabase {
        val appContext = context.applicationContext
        if (!unlockGate.isUserUnlocked(appContext)) {
            lastFailureCategory = DatabaseSecretFailureCategory.WAITING_FOR_USER_UNLOCK
            throw DeviceLockedException()
        }
        System.loadLibrary("sqlcipher")
        val passphraseStore = KeystoreDatabasePassphraseStore(
            context = appContext,
            keyAlias = ProductionDatabaseIdentity.KEY_ALIAS,
            wrapperFileName = ProductionDatabaseIdentity.WRAPPER_FILE,
        )
        val journal = DatabaseEncryptionConversionJournal(
            context = appContext,
            journalFileName = ProductionDatabaseIdentity.JOURNAL_FILE,
        )
        return runBlocking(Dispatchers.IO) {
            try {
                prepareEncryptedDatabase(appContext, passphraseStore, journal)
            } catch (denied: ConversionDeniedException) {
                lastFailureCategory = denied.category
                val productionFile = databaseFile(appContext, ProductionDatabaseIdentity.DATABASE_NAME)
                if (isPlaintextReadable(productionFile)) {
                    return@runBlocking openPlaintext(appContext)
                }
                throw denied
            }
            val productionFile = databaseFile(appContext, ProductionDatabaseIdentity.DATABASE_NAME)
            if (journal.read().phase == DatabaseEncryptionConversionPhase.FAILED_SAFE &&
                isPlaintextReadable(productionFile)
            ) {
                return@runBlocking openPlaintext(appContext)
            }
            openEncrypted(
                context = appContext,
                passphraseStore = passphraseStore,
                databaseName = ProductionDatabaseIdentity.DATABASE_NAME,
            )
        }
    }

    /**
     * Runs conversion prepare only, stopping after [phase] is persisted.
     * Simulates process death before a later open() resume. Instrumentation only.
     */
    fun prepareConversionStoppingAfterPhaseForTest(
        context: Context,
        phase: DatabaseEncryptionConversionPhase,
    ) {
        testStopAfterPhase = phase
        try {
            val appContext = context.applicationContext
            System.loadLibrary("sqlcipher")
            val passphraseStore = KeystoreDatabasePassphraseStore(
                context = appContext,
                keyAlias = ProductionDatabaseIdentity.KEY_ALIAS,
                wrapperFileName = ProductionDatabaseIdentity.WRAPPER_FILE,
            )
            val journal = DatabaseEncryptionConversionJournal(
                context = appContext,
                journalFileName = ProductionDatabaseIdentity.JOURNAL_FILE,
            )
            runBlocking(Dispatchers.IO) {
                prepareEncryptedDatabase(appContext, passphraseStore, journal)
            }
        } finally {
            testStopAfterPhase = null
        }
    }

    private fun shouldStopAfter(phase: DatabaseEncryptionConversionPhase): Boolean {
        if (testStopAfterPhase != phase) {
            return false
        }
        testStopAfterPhase = null
        return true
    }

    private suspend fun prepareEncryptedDatabase(
        context: Context,
        passphraseStore: KeystoreDatabasePassphraseStore,
        journal: DatabaseEncryptionConversionJournal,
    ) {
        cleanupIncompleteCandidate(context, journal)

        val phase = journal.read().phase
        val productionFile = databaseFile(context, ProductionDatabaseIdentity.DATABASE_NAME)

        when (phase) {
            DatabaseEncryptionConversionPhase.COMPLETED -> {
                if (!productionFile.exists()) {
                    createFreshEncryptedDatabase(context, passphraseStore, journal)
                }
            }

            DatabaseEncryptionConversionPhase.SWITCH_PENDING -> {
                finalizeRename(context, passphraseStore, journal)
            }

            DatabaseEncryptionConversionPhase.CANDIDATE_VALIDATED -> {
                // Closed before SWITCH_PENDING persisted; re-validate by converting again
                // only if plaintext is still authoritative.
                if (productionFile.exists() && isPlaintextReadable(productionFile)) {
                    convertPlaintextToEncrypted(context, passphraseStore, journal)
                } else if (productionFile.exists()) {
                    journal.write(
                        DatabaseEncryptionConversionRecord(
                            phase = DatabaseEncryptionConversionPhase.COMPLETED,
                            schemaVersion = ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION,
                        ),
                    )
                } else {
                    createFreshEncryptedDatabase(context, passphraseStore, journal)
                }
            }

            DatabaseEncryptionConversionPhase.NOT_STARTED,
            DatabaseEncryptionConversionPhase.PLAINTEXT_VALIDATED,
            DatabaseEncryptionConversionPhase.FAILED_SAFE,
            DatabaseEncryptionConversionPhase.CANDIDATE_CREATED,
            DatabaseEncryptionConversionPhase.ROWS_COPIED,
            -> {
                when {
                    productionFile.exists() && isPlaintextReadable(productionFile) -> {
                        convertPlaintextToEncrypted(context, passphraseStore, journal)
                    }

                    productionFile.exists() -> {
                        // Encrypted file already present; treat as complete.
                        journal.write(
                            DatabaseEncryptionConversionRecord(
                                phase = DatabaseEncryptionConversionPhase.COMPLETED,
                                schemaVersion = ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION,
                            ),
                        )
                    }

                    else -> createFreshEncryptedDatabase(context, passphraseStore, journal)
                }
            }
        }
    }

    private suspend fun convertPlaintextToEncrypted(
        context: Context,
        passphraseStore: KeystoreDatabasePassphraseStore,
        journal: DatabaseEncryptionConversionJournal,
    ) {
        val productionFile = databaseFile(context, ProductionDatabaseIdentity.DATABASE_NAME)
        if (!storageGuard.hasRoomForConversion(context, productionFile.length())) {
            denyConversion(context, journal)
        }

        val plaintext = openPlaintext(context)
        try {
            if (plaintext.openHelper.readableDatabase.version !=
                ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION
            ) {
                denyConversion(context, journal)
            }
            journal.write(
                DatabaseEncryptionConversionRecord(
                    phase = DatabaseEncryptionConversionPhase.PLAINTEXT_VALIDATED,
                    schemaVersion = ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION,
                ),
            )
            val snapshot = Snapshot.from(plaintext)
            deleteDatabaseFiles(context, ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME)

            val passphrase = requirePassphrase(passphraseStore, createIfMissing = true)
            val encrypted = openEncryptedWithPassphrase(
                context = context,
                databaseName = ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME,
                passphrase = passphrase,
            )
            try {
                journal.write(
                    DatabaseEncryptionConversionRecord(
                        phase = DatabaseEncryptionConversionPhase.CANDIDATE_CREATED,
                        schemaVersion = ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION,
                    ),
                )
                if (testForceIoFailureDuringConversion) {
                    testForceIoFailureDuringConversion = false
                    encrypted.close()
                    deleteDatabaseFiles(context, ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME)
                    denyConversion(context, journal)
                }
                copyRows(snapshot, encrypted)
                journal.write(
                    DatabaseEncryptionConversionRecord(
                        phase = DatabaseEncryptionConversionPhase.ROWS_COPIED,
                        schemaVersion = ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION,
                    ),
                )
                if (shouldStopAfter(DatabaseEncryptionConversionPhase.ROWS_COPIED)) {
                    return
                }
                if (!validateAgainstSnapshot(encrypted, snapshot)) {
                    encrypted.close()
                    deleteDatabaseFiles(context, ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME)
                    denyConversion(context, journal)
                }
                journal.write(
                    DatabaseEncryptionConversionRecord(
                        phase = DatabaseEncryptionConversionPhase.CANDIDATE_VALIDATED,
                        schemaVersion = ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION,
                    ),
                )
            } catch (denied: ConversionDeniedException) {
                runCatching { encrypted.close() }
                throw denied
            } catch (error: Exception) {
                runCatching { encrypted.close() }
                deleteDatabaseFiles(context, ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME)
                denyConversion(context, journal)
            } finally {
                runCatching { encrypted.close() }
            }
        } finally {
            plaintext.close()
        }

        if (journal.read().phase == DatabaseEncryptionConversionPhase.ROWS_COPIED) {
            return
        }

        // Process-boundary style reopen before rename.
        val reopened = openEncrypted(
            context,
            passphraseStore,
            ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME,
        )
        val plaintextForSnapshot = openPlaintext(context)
        val verifiedSnapshot = try {
            Snapshot.from(plaintextForSnapshot)
        } finally {
            plaintextForSnapshot.close()
        }
        try {
            if (!validateAgainstSnapshot(reopened, verifiedSnapshot)) {
                reopened.close()
                deleteDatabaseFiles(context, ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME)
                denyConversion(context, journal)
            }
            journal.write(
                DatabaseEncryptionConversionRecord(
                    phase = DatabaseEncryptionConversionPhase.SWITCH_PENDING,
                    schemaVersion = ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION,
                ),
            )
            if (shouldStopAfter(DatabaseEncryptionConversionPhase.SWITCH_PENDING)) {
                return
            }
        } finally {
            reopened.close()
        }

        finalizeRename(context, passphraseStore, journal)
    }

    private fun denyConversion(
        context: Context,
        journal: DatabaseEncryptionConversionJournal,
    ): Nothing {
        deleteDatabaseFiles(context, ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME)
        markFailed(journal)
        lastFailureCategory = DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED
        throw ConversionDeniedException(DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED)
    }

    private fun finalizeRename(
        context: Context,
        passphraseStore: KeystoreDatabasePassphraseStore,
        journal: DatabaseEncryptionConversionJournal,
    ) {
        val production = ProductionDatabaseIdentity.DATABASE_NAME
        val candidate = ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME
        val retained = ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME
        val productionFile = databaseFile(context, production)
        val candidateExists = databaseFile(context, candidate).exists()
        val retainedExists = databaseFile(context, retained).exists()
        val productionExists = productionFile.exists()

        when {
            // Clean SWITCH_PENDING: plaintext still authoritative, candidate ready.
            candidateExists && productionExists && isPlaintextReadable(productionFile) -> {
                deleteDatabaseFiles(context, retained)
                if (!renameDatabaseFiles(context, production, retained)) {
                    markFailed(journal)
                    error("Unable to retain plaintext during encrypted finalize.")
                }
                if (!renameDatabaseFiles(context, candidate, production)) {
                    renameDatabaseFiles(context, retained, production)
                    markFailed(journal)
                    error("Unable to promote encrypted candidate to memora.db.")
                }
                completeAfterEncryptedPromotion(context, passphraseStore, journal)
            }

            // Death after moving plaintext aside: retained + candidate, production gone.
            candidateExists && retainedExists && !productionExists -> {
                if (!renameDatabaseFiles(context, candidate, production)) {
                    markFailed(journal)
                    error("Unable to promote encrypted candidate after interrupted finalize.")
                }
                completeAfterEncryptedPromotion(context, passphraseStore, journal)
            }

            // Death after promoting encrypted candidate onto memora.db.
            productionExists && !candidateExists && !isPlaintextReadable(productionFile) -> {
                completeAfterEncryptedPromotion(context, passphraseStore, journal)
            }

            else -> {
                markFailed(journal)
                error("Conversion finalize is missing plaintext or encrypted candidate files.")
            }
        }
    }

    private fun completeAfterEncryptedPromotion(
        context: Context,
        passphraseStore: KeystoreDatabasePassphraseStore,
        journal: DatabaseEncryptionConversionJournal,
    ) {
        val verify = openEncrypted(
            context,
            passphraseStore,
            ProductionDatabaseIdentity.DATABASE_NAME,
        )
        verify.close()
        deleteDatabaseFiles(context, ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME)
        journal.write(
            DatabaseEncryptionConversionRecord(
                phase = DatabaseEncryptionConversionPhase.COMPLETED,
                schemaVersion = ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION,
            ),
        )
    }

    private fun createFreshEncryptedDatabase(
        context: Context,
        passphraseStore: KeystoreDatabasePassphraseStore,
        journal: DatabaseEncryptionConversionJournal,
    ) {
        deleteDatabaseFiles(context, ProductionDatabaseIdentity.DATABASE_NAME)
        deleteDatabaseFiles(context, ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME)
        deleteDatabaseFiles(context, ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME)
        val passphrase = requirePassphrase(passphraseStore, createIfMissing = true)
        val database = openEncryptedWithPassphrase(
            context = context,
            databaseName = ProductionDatabaseIdentity.DATABASE_NAME,
            passphrase = passphrase,
        )
        database.close()
        journal.write(
            DatabaseEncryptionConversionRecord(
                phase = DatabaseEncryptionConversionPhase.COMPLETED,
                schemaVersion = ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION,
            ),
        )
    }

    private fun cleanupIncompleteCandidate(
        context: Context,
        journal: DatabaseEncryptionConversionJournal,
    ) {
        when (journal.read().phase) {
            DatabaseEncryptionConversionPhase.CANDIDATE_CREATED,
            DatabaseEncryptionConversionPhase.ROWS_COPIED,
            DatabaseEncryptionConversionPhase.FAILED_SAFE,
            -> {
                deleteDatabaseFiles(context, ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME)
                if (journal.read().phase != DatabaseEncryptionConversionPhase.FAILED_SAFE) {
                    journal.write(
                        DatabaseEncryptionConversionRecord(
                            phase = DatabaseEncryptionConversionPhase.NOT_STARTED,
                            schemaVersion = null,
                        ),
                    )
                }
            }

            else -> Unit
        }
    }

    private fun requirePassphrase(
        passphraseStore: KeystoreDatabasePassphraseStore,
        createIfMissing: Boolean,
    ): ByteArray {
        when (val existing = passphraseStore.unwrapExistingPassphrase()) {
            is PassphraseUnwrapResult.Unwrapped -> return existing.passphrase
            is PassphraseUnwrapResult.Denied -> Unit
        }
        if (!createIfMissing) {
            error("Database passphrase is unavailable.")
        }
        return when (val created = passphraseStore.createAndWrapNewPassphrase()) {
            is PassphraseUnwrapResult.Unwrapped -> created.passphrase
            is PassphraseUnwrapResult.Denied -> error("Unable to create database passphrase.")
        }
    }

    private fun openEncrypted(
        context: Context,
        passphraseStore: KeystoreDatabasePassphraseStore,
        databaseName: String,
    ): MemoraDatabase {
        val passphrase = requirePassphrase(passphraseStore, createIfMissing = false)
        return openEncryptedWithPassphrase(context, databaseName, passphrase)
    }

    private fun openEncryptedWithPassphrase(
        context: Context,
        databaseName: String,
        passphrase: ByteArray,
    ): MemoraDatabase {
        return try {
            val factory = SupportOpenHelperFactory(passphrase.copyOf())
            Room.databaseBuilder(
                context.applicationContext,
                MemoraDatabase::class.java,
                databaseName,
            )
                .openHelperFactory(factory)
                .addMigrations(
                    MemoraDatabaseMigrations.MIGRATION_1_2,
                    MemoraDatabaseMigrations.MIGRATION_2_3,
                    MemoraDatabaseMigrations.MIGRATION_3_4,
                    MemoraDatabaseMigrations.MIGRATION_4_5,
                    MemoraDatabaseMigrations.MIGRATION_5_6,
                    MemoraDatabaseMigrations.MIGRATION_6_7,
                )
                .build()
                .also { it.openHelper.writableDatabase }
        } finally {
            passphrase.fill(0)
        }
    }

    private fun openPlaintext(context: Context): MemoraDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            MemoraDatabase::class.java,
            ProductionDatabaseIdentity.DATABASE_NAME,
        )
            .addMigrations(
                MemoraDatabaseMigrations.MIGRATION_1_2,
                MemoraDatabaseMigrations.MIGRATION_2_3,
                MemoraDatabaseMigrations.MIGRATION_3_4,
                MemoraDatabaseMigrations.MIGRATION_4_5,
                MemoraDatabaseMigrations.MIGRATION_5_6,
                MemoraDatabaseMigrations.MIGRATION_6_7,
            )
            .build()
            .also { it.openHelper.writableDatabase }

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
        if (encrypted.openHelper.readableDatabase.version !=
            ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION
        ) {
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

    private fun isPlaintextReadable(file: File): Boolean {
        if (!file.exists()) return false
        return try {
            SQLiteDatabase.openDatabase(
                file.path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { database ->
                database.rawQuery("SELECT 1", null).use { cursor -> cursor.moveToFirst() }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun markFailed(journal: DatabaseEncryptionConversionJournal) {
        journal.write(
            DatabaseEncryptionConversionRecord(
                phase = DatabaseEncryptionConversionPhase.FAILED_SAFE,
                schemaVersion = ProductionDatabaseIdentity.EXPECTED_SCHEMA_VERSION,
            ),
        )
    }

    private fun databaseFile(context: Context, name: String): File =
        context.applicationContext.getDatabasePath(name)

    private fun deleteDatabaseFiles(context: Context, name: String) {
        val base = databaseFile(context, name)
        base.delete()
        File(base.path + "-wal").delete()
        File(base.path + "-shm").delete()
        File(base.path + "-journal").delete()
    }

    private fun renameDatabaseFiles(context: Context, fromName: String, toName: String): Boolean {
        val from = databaseFile(context, fromName)
        val to = databaseFile(context, toName)
        if (!from.exists()) return false
        deleteDatabaseFiles(context, toName)
        to.parentFile?.mkdirs()
        if (!from.renameTo(to)) return false
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
        val assets: List<com.memora.app.data.local.AssetEntity>,
        val checkpoints: List<com.memora.app.data.local.DiscoveryCheckpointEntity>,
        val approvals: List<com.memora.app.data.local.DocumentTreeApprovalEntity>,
    ) {
        companion object {
            suspend fun from(database: MemoraDatabase): Snapshot = Snapshot(
                assets = database.assetDao().findAll(),
                checkpoints = database.discoveryCheckpointDao().findAll(),
                approvals = database.documentTreeApprovalDao().findAll(),
            )
        }
    }
}
