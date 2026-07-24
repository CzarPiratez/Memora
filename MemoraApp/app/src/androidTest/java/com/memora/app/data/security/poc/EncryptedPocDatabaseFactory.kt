package com.memora.app.data.security.poc

import android.content.Context
import androidx.room.Room
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.security.DatabaseSecretFailureCategory
import com.memora.app.data.security.KeystoreDatabasePassphraseStore
import com.memora.app.data.security.PassphraseUnwrapResult
import com.memora.app.data.security.StandardSqliteDatabaseProbe
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * Opens a separately named synthetic encrypted Room database for PoC tests only.
 * It must never be wired into production [com.memora.app.data.di.PersistenceModule].
 */
object EncryptedPocDatabaseFactory {
    const val DATABASE_NAME = "memora_encrypted_poc.db"
    const val FIXTURE_MARKER = "MEMORA_POC_FIXTURE_MARKER_v1"

    fun loadNativeLibrary() {
        System.loadLibrary("sqlcipher")
    }

    fun createNew(
        context: Context,
        passphraseStore: KeystoreDatabasePassphraseStore,
    ): EncryptedPocOpenResult {
        deleteDatabaseFiles(context)
        val created = passphraseStore.createAndWrapNewPassphrase()
        val passphrase = when (created) {
            is PassphraseUnwrapResult.Unwrapped -> created.passphrase
            is PassphraseUnwrapResult.Denied -> {
                return EncryptedPocOpenResult.Denied(created.category)
            }
        }
        return openWithPassphrase(context, passphraseStore, passphrase)
    }

    fun reopenExisting(
        context: Context,
        passphraseStore: KeystoreDatabasePassphraseStore,
    ): EncryptedPocOpenResult {
        val unwrapped = passphraseStore.unwrapExistingPassphrase()
        val passphrase = when (unwrapped) {
            is PassphraseUnwrapResult.Unwrapped -> unwrapped.passphrase
            is PassphraseUnwrapResult.Denied -> {
                return EncryptedPocOpenResult.Denied(unwrapped.category)
            }
        }
        return openWithPassphrase(context, passphraseStore, passphrase)
    }

    fun openWithRawPassphrase(
        context: Context,
        passphrase: ByteArray,
    ): EncryptedPocOpenResult {
        return try {
            loadNativeLibrary()
            val factory = SupportOpenHelperFactory(passphrase.copyOf())
            val database = Room.databaseBuilder(
                context.applicationContext,
                MemoraDatabase::class.java,
                DATABASE_NAME,
            )
                .openHelperFactory(factory)
                .build()
            database.openHelper.writableDatabase
            EncryptedPocOpenResult.Opened(database)
        } catch (_: Exception) {
            EncryptedPocOpenResult.Denied(DatabaseSecretFailureCategory.DATABASE_AUTH_FAILED)
        } finally {
            passphrase.fill(0)
        }
    }

    fun databaseFile(context: Context): File =
        context.applicationContext.getDatabasePath(DATABASE_NAME)

    fun deleteDatabaseFiles(context: Context) {
        val base = databaseFile(context)
        base.delete()
        File(base.path + "-wal").delete()
        File(base.path + "-shm").delete()
        File(base.path + "-journal").delete()
    }

    fun containsFixtureMarkerInCleartextArtifacts(context: Context): Boolean {
        val base = databaseFile(context)
        val candidates = listOf(
            base,
            File(base.path + "-wal"),
            File(base.path + "-shm"),
            File(base.path + "-journal"),
        )
        val needle = FIXTURE_MARKER.toByteArray(Charsets.UTF_8)
        return candidates.any { file ->
            file.exists() && file.readBytes().containsSequence(needle)
        }
    }

    fun probeWithStandardSqlite(context: Context): StandardSqliteProbeResult {
        val file = databaseFile(context)
        if (!file.exists()) {
            return StandardSqliteProbeResult(
                openedWithoutPassphrase = false,
                fileExistedBeforeProbe = false,
                fileExistsAfterProbe = false,
                fileLengthBeforeProbe = 0L,
                fileLengthAfterProbe = 0L,
            )
        }
        val lengthBefore = file.length()
        val opened = StandardSqliteDatabaseProbe.canOpenWithoutPassphrase(file)
        val after = databaseFile(context)
        return StandardSqliteProbeResult(
            openedWithoutPassphrase = opened,
            fileExistedBeforeProbe = true,
            fileExistsAfterProbe = after.exists(),
            fileLengthBeforeProbe = lengthBefore,
            fileLengthAfterProbe = if (after.exists()) after.length() else 0L,
        )
    }

    private fun openWithPassphrase(
        context: Context,
        passphraseStore: KeystoreDatabasePassphraseStore,
        passphrase: ByteArray,
    ): EncryptedPocOpenResult {
        return try {
            loadNativeLibrary()
            val factory = SupportOpenHelperFactory(passphrase.copyOf())
            val database = Room.databaseBuilder(
                context.applicationContext,
                MemoraDatabase::class.java,
                DATABASE_NAME,
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

    private fun ByteArray.containsSequence(needle: ByteArray): Boolean {
        if (needle.isEmpty() || size < needle.size) {
            return false
        }
        outer@ for (start in 0..(size - needle.size)) {
            for (offset in needle.indices) {
                if (this[start + offset] != needle[offset]) {
                    continue@outer
                }
            }
            return true
        }
        return false
    }
}

sealed class EncryptedPocOpenResult {
    data class Opened(val database: MemoraDatabase) : EncryptedPocOpenResult()
    data class Denied(val category: DatabaseSecretFailureCategory) : EncryptedPocOpenResult()
}

data class StandardSqliteProbeResult(
    val openedWithoutPassphrase: Boolean,
    val fileExistedBeforeProbe: Boolean,
    val fileExistsAfterProbe: Boolean,
    val fileLengthBeforeProbe: Long,
    val fileLengthAfterProbe: Long,
)
