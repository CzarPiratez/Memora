package com.memora.app.data.mediastore

import com.memora.app.domain.discovery.DiscoveryCursor
import java.util.Base64

/**
 * Opaque MediaStore progress retained by a later persistence layer.
 *
 * A store-version change invalidates its generation watermark and causes a safe
 * rescan. On Android versions before MediaStore generations, the watermark is the
 * source modified timestamp instead.
 */
data class MediaStoreDiscoveryCheckpoint(
    val storeVersion: String,
    val watermark: Long,
    val lastMediaId: Long,
) {
    init {
        require(storeVersion.isNotBlank()) { "A MediaStore version cannot be blank." }
        require(watermark >= 0) { "A MediaStore watermark cannot be negative." }
        require(lastMediaId >= 0) { "A MediaStore ID cannot be negative." }
    }

    fun toCursor(): DiscoveryCursor = DiscoveryCursor(
        sourceId = MediaStoreImageMapper.sourceId,
        value = listOf(
            ENCODED_VERSION_PREFIX + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(storeVersion.toByteArray(Charsets.UTF_8)),
            watermark.toString(),
            lastMediaId.toString(),
        ).joinToString(SEPARATOR),
    )

    companion object {
        private const val SEPARATOR = ":"
        private const val ENCODED_VERSION_PREFIX = "v1-"

        fun initial(storeVersion: String): MediaStoreDiscoveryCheckpoint =
            MediaStoreDiscoveryCheckpoint(storeVersion, watermark = 0, lastMediaId = 0)

        fun from(cursor: DiscoveryCursor): MediaStoreDiscoveryCheckpoint {
            require(cursor.sourceId == MediaStoreImageMapper.sourceId) {
                "A MediaStore checkpoint must belong to the MediaStore image source."
            }

            val parts = cursor.value.split(SEPARATOR)
            require(parts.size == 3 && parts[0].startsWith(ENCODED_VERSION_PREFIX)) {
                "The MediaStore checkpoint is malformed."
            }

            val version = runCatching {
                String(
                    Base64.getUrlDecoder().decode(parts[0].removePrefix(ENCODED_VERSION_PREFIX)),
                    Charsets.UTF_8,
                )
            }.getOrElse { throw IllegalArgumentException("The MediaStore checkpoint is malformed.") }

            return MediaStoreDiscoveryCheckpoint(
                storeVersion = version,
                watermark = parts[1].toLongOrNull()
                    ?: throw IllegalArgumentException("The MediaStore checkpoint is malformed."),
                lastMediaId = parts[2].toLongOrNull()
                    ?: throw IllegalArgumentException("The MediaStore checkpoint is malformed."),
            )
        }
    }
}
