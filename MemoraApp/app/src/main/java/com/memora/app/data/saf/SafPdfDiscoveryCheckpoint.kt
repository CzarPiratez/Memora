package com.memora.app.data.saf

import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCursor
import java.nio.charset.StandardCharsets
import java.util.Base64

/** Source-owned continuation token for immediate-child SAF metadata discovery. */
data class SafPdfDiscoveryCheckpoint(
    val sourceId: SourceId,
    val afterDocumentId: String?,
) {
    init {
        require(afterDocumentId == null || afterDocumentId.isNotBlank()) {
            "A SAF discovery document ID cannot be blank."
        }
    }

    fun toCursor(): DiscoveryCursor = DiscoveryCursor(
        sourceId = sourceId,
        value = PREFIX + (afterDocumentId?.let(::encode) ?: INITIAL_TOKEN),
    )

    companion object {
        private const val PREFIX = "saf-pdf-v1:"
        private const val INITIAL_TOKEN = "initial"

        fun initial(sourceId: SourceId): SafPdfDiscoveryCheckpoint = SafPdfDiscoveryCheckpoint(
            sourceId = sourceId,
            afterDocumentId = null,
        )

        fun from(cursor: DiscoveryCursor): SafPdfDiscoveryCheckpoint {
            require(cursor.value.startsWith(PREFIX)) { "Unsupported SAF discovery checkpoint." }
            val encoded = cursor.value.removePrefix(PREFIX)
            return SafPdfDiscoveryCheckpoint(
                sourceId = cursor.sourceId,
                afterDocumentId = when (encoded) {
                    INITIAL_TOKEN -> null
                    else -> decode(encoded)
                },
            )
        }

        private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decode(value: String): String = String(
            Base64.getUrlDecoder().decode(value),
            StandardCharsets.UTF_8,
        ).also { require(it.isNotBlank()) { "A SAF discovery checkpoint document ID cannot be blank." } }
    }
}
