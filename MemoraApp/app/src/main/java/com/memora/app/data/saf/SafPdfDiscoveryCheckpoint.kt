package com.memora.app.data.saf

import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCursor
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Source-owned continuation token for bounded, depth-first SAF metadata discovery.
 *
 * A frame represents one folder whose immediate children remain to be read. A null
 * [FolderFrame.parentDocumentId] is the selected tree root. The final frame is the
 * next folder to read, so the checkpoint grows with pending depth-first work rather
 * than causing an unbounded query in a single discovery invocation.
 */
data class SafPdfDiscoveryCheckpoint(
    val sourceId: SourceId,
    val frames: List<FolderFrame>,
) {
    data class FolderFrame(
        val parentDocumentId: String?,
        val afterDocumentId: String?,
    ) {
        init {
            require(parentDocumentId == null || parentDocumentId.isNotBlank()) {
                "A SAF discovery parent document ID cannot be blank."
            }
            require(afterDocumentId == null || afterDocumentId.isNotBlank()) {
                "A SAF discovery document ID cannot be blank."
            }
        }
    }

    fun toCursor(): DiscoveryCursor = DiscoveryCursor(sourceId, PREFIX_V2 + encodeFrames(frames))

    companion object {
        private const val PREFIX_V1 = "saf-pdf-v1:"
        private const val PREFIX_V2 = "saf-pdf-v2:"
        private const val INITIAL_TOKEN = "initial"
        private const val NULL_VALUE = "-"
        private const val FRAME_SEPARATOR = "."
        private const val VALUE_SEPARATOR = ":"

        fun initial(sourceId: SourceId): SafPdfDiscoveryCheckpoint = SafPdfDiscoveryCheckpoint(
            sourceId = sourceId,
            frames = listOf(FolderFrame(parentDocumentId = null, afterDocumentId = null)),
        )

        fun from(cursor: DiscoveryCursor): SafPdfDiscoveryCheckpoint {
            return when {
                cursor.value.startsWith(PREFIX_V2) -> SafPdfDiscoveryCheckpoint(
                    sourceId = cursor.sourceId,
                    frames = decodeFrames(cursor.value.removePrefix(PREFIX_V2)),
                )

                cursor.value.startsWith(PREFIX_V1) -> fromVersionOne(cursor)
                else -> throw IllegalArgumentException("Unsupported SAF discovery checkpoint.")
            }
        }

        private fun fromVersionOne(cursor: DiscoveryCursor): SafPdfDiscoveryCheckpoint {
            val encoded = cursor.value.removePrefix(PREFIX_V1)
            return SafPdfDiscoveryCheckpoint(
                sourceId = cursor.sourceId,
                frames = listOf(
                    FolderFrame(
                        parentDocumentId = null,
                        afterDocumentId = when (encoded) {
                            INITIAL_TOKEN -> null
                            else -> decode(encoded)
                        },
                    ),
                ),
            )
        }

        private fun encodeFrames(frames: List<FolderFrame>): String = frames.joinToString(FRAME_SEPARATOR) {
            frame -> listOf(frame.parentDocumentId, frame.afterDocumentId).joinToString(VALUE_SEPARATOR) {
                value -> value?.let(::encode) ?: NULL_VALUE
            }
        }

        private fun decodeFrames(value: String): List<FolderFrame> {
            if (value.isEmpty()) return emptyList()
            return value.split(FRAME_SEPARATOR).map { encodedFrame ->
                val values = encodedFrame.split(VALUE_SEPARATOR)
                require(values.size == 2) { "Malformed SAF traversal checkpoint frame." }
                FolderFrame(
                    parentDocumentId = values[0].decodeNullable(),
                    afterDocumentId = values[1].decodeNullable(),
                )
            }
        }

        private fun String.decodeNullable(): String? = when (this) {
            NULL_VALUE -> null
            else -> decode(this)
        }

        private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decode(value: String): String = String(
            Base64.getUrlDecoder().decode(value),
            StandardCharsets.UTF_8,
        ).also { require(it.isNotBlank()) { "A SAF discovery checkpoint document ID cannot be blank." } }
    }
}
