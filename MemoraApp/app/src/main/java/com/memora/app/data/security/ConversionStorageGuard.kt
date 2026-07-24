package com.memora.app.data.security

import android.content.Context
import android.os.StatFs

/**
 * Preflight free-space check before creating an encrypted conversion candidate.
 * Denial must leave plaintext intact and surface [DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED].
 */
fun interface ConversionStorageGuard {
    fun hasRoomForConversion(context: Context, plaintextBytes: Long): Boolean
}

object StatFsConversionStorageGuard : ConversionStorageGuard {
    override fun hasRoomForConversion(context: Context, plaintextBytes: Long): Boolean {
        val databaseDir = context.applicationContext
            .getDatabasePath(ProductionDatabaseIdentity.DATABASE_NAME)
            .parentFile
            ?: return false
        val availableBytes = StatFs(databaseDir.path).availableBytes
        val requiredBytes = (plaintextBytes * 2) + HEADROOM_BYTES
        return availableBytes >= requiredBytes
    }

    private const val HEADROOM_BYTES = 1_048_576L
}

class ConversionDeniedException(
    val category: DatabaseSecretFailureCategory,
) : Exception("Memora database conversion was denied: $category")
