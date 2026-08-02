package com.memora.app.data.intelligence

import android.content.Context
import com.memora.app.domain.intelligence.AiPackPayloadStore
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores AI Pack payloads under no-backup private storage (not world-readable).
 */
@Singleton
class NoBackupAiPackPayloadStore @Inject constructor(
    @ApplicationContext context: Context,
) : AiPackPayloadStore {
    private val rootDir = File(context.applicationContext.noBackupFilesDir, ROOT_DIR_NAME)

    override fun writePayload(packId: String, payload: ByteArray) {
        require(packId.isNotBlank())
        require(payload.isNotEmpty())
        rootDir.mkdirs()
        val target = fileFor(packId)
        val temp = File(rootDir, "${sanitize(packId)}.tmp")
        temp.writeBytes(payload)
        if (target.exists()) {
            target.delete()
        }
        check(temp.renameTo(target)) { "Could not finalize AI Pack payload file." }
    }

    override fun readPayload(packId: String): ByteArray? {
        require(packId.isNotBlank())
        val file = fileFor(packId)
        if (!file.exists()) return null
        return file.readBytes()
    }

    override fun deletePayload(packId: String) {
        require(packId.isNotBlank())
        fileFor(packId).delete()
        File(rootDir, "${sanitize(packId)}.tmp").delete()
    }

    override fun clearAll() {
        if (!rootDir.exists()) return
        rootDir.listFiles()?.forEach { it.delete() }
        rootDir.delete()
    }

    private fun fileFor(packId: String): File = File(rootDir, "${sanitize(packId)}.bin")

    private fun sanitize(packId: String): String =
        packId.replace(Regex("[^A-Za-z0-9._-]"), "_")

    companion object {
        const val ROOT_DIR_NAME = "memora_ai_packs"
    }
}
