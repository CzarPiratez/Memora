package com.memora.app.domain.memory

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Digest of the fact set a Memory assembly attempt saw.
 *
 * A skip is valid only for this digest. New OCR / PDF / EXIF / note text
 * changes the digest, the skip is cleared, and the Asset is pending again.
 */
object AssemblyFactsDigest {
    const val NONE = "none"

    fun of(facts: List<AssetMemoryFact>): String {
        if (facts.isEmpty()) return NONE
        val digest = MessageDigest.getInstance("SHA-256")
        facts
            .sortedWith(
                compareBy(
                    { it.kind.name },
                    { it.locator },
                    { it.excerpt },
                    { it.extractionSchemaVersion },
                ),
            )
            .forEachIndexed { index, fact ->
                if (index > 0) digest.update(UNIT_SEPARATOR)
                digest.update(fact.kind.name.toByteArray(StandardCharsets.UTF_8))
                digest.update(UNIT_SEPARATOR)
                digest.update(fact.locator.toByteArray(StandardCharsets.UTF_8))
                digest.update(UNIT_SEPARATOR)
                digest.update(fact.excerpt.toByteArray(StandardCharsets.UTF_8))
                digest.update(UNIT_SEPARATOR)
                digest.update(fact.extractionSchemaVersion.toByteArray(StandardCharsets.UTF_8))
            }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private val UNIT_SEPARATOR = byteArrayOf(0x1f)
}
