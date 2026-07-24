package com.memora.app.data.security

import android.content.Context
import java.io.File

/**
 * Persists only content-free conversion phase + schema version under no-backup storage.
 * It never stores passphrases, SQL, URIs, or row content.
 */
class DatabaseEncryptionConversionJournal(
    context: Context,
    private val journalFileName: String = DEFAULT_POC_JOURNAL_FILE,
) {
    private val appContext = context.applicationContext

    fun read(): DatabaseEncryptionConversionRecord {
        val file = journalFile()
        if (!file.exists()) {
            return DatabaseEncryptionConversionRecord(
                phase = DatabaseEncryptionConversionPhase.NOT_STARTED,
                schemaVersion = null,
            )
        }
        return try {
            val lines = file.readText(Charsets.UTF_8).lines()
            val phase = DatabaseEncryptionConversionPhase.valueOf(lines[0])
            val schemaVersion = lines.getOrNull(1)?.toIntOrNull()
            DatabaseEncryptionConversionRecord(phase = phase, schemaVersion = schemaVersion)
        } catch (_: Exception) {
            DatabaseEncryptionConversionRecord(
                phase = DatabaseEncryptionConversionPhase.FAILED_SAFE,
                schemaVersion = null,
            )
        }
    }

    fun write(record: DatabaseEncryptionConversionRecord) {
        val target = journalFile()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        val payload = buildString {
            append(record.phase.name)
            append('\n')
            append(record.schemaVersion?.toString().orEmpty())
            append('\n')
        }
        temporary.writeText(payload, Charsets.UTF_8)
        check(temporary.renameTo(target) || (target.delete() && temporary.renameTo(target))) {
            "Unable to persist the conversion journal."
        }
    }

    fun clearOwnedState() {
        journalFile().delete()
        File(journalFile().parentFile, "${journalFile().name}.tmp").delete()
    }

    fun clearForTest() {
        clearOwnedState()
    }

    private fun journalFile(): File = File(appContext.noBackupFilesDir, journalFileName)

    companion object {
        const val DEFAULT_POC_JOURNAL_FILE = "memora_poc_db_conversion_v1.journal"
    }
}

data class DatabaseEncryptionConversionRecord(
    val phase: DatabaseEncryptionConversionPhase,
    val schemaVersion: Int?,
)
