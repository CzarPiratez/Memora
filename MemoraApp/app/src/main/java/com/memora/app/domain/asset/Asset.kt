package com.memora.app.domain.asset

import java.time.Instant

/** The content categories Memora can normalize into a searchable memory. */
enum class AssetType {
    PHOTO,
    SCREENSHOT,
    PDF,
    NOTE,
}

/** A stable identifier for a source integration, such as MediaStore or a note provider. */
@JvmInline
value class SourceId(val value: String) {
    init {
        require(value.isNotBlank()) { "A source ID cannot be blank." }
    }
}

/** A stable identifier supplied by a source for one individual asset. */
@JvmInline
value class SourceAssetKey(val value: String) {
    init {
        require(value.isNotBlank()) { "A source asset key cannot be blank." }
    }
}

/** A source location that can be re-opened only through the source adapter that owns it. */
@JvmInline
value class AssetLocation(val value: String) {
    init {
        require(value.isNotBlank()) { "An asset location cannot be blank." }
    }
}

/** A deterministic version marker used to avoid indexing an unchanged source asset twice. */
@JvmInline
value class AssetFingerprint(val value: String) {
    init {
        require(value.isNotBlank()) { "An asset fingerprint cannot be blank." }
    }
}

/** Source identity remains stable even when the display name or location changes. */
data class AssetIdentity(
    val sourceId: SourceId,
    val sourceAssetKey: SourceAssetKey,
)

/**
 * A read-only description of an item discovered in a permitted source.
 *
 * This model deliberately has no Android framework types. A platform adapter owns how
 * the [location] is opened and the domain layer owns the stable [identity].
 */
data class Asset(
    val identity: AssetIdentity,
    val type: AssetType,
    val location: AssetLocation,
    val fingerprint: AssetFingerprint,
    val discoveredAt: Instant,
    val displayName: String? = null,
    val sourceModifiedAt: Instant? = null,
) {
    init {
        require(displayName == null || displayName.isNotBlank()) {
            "An asset display name must be meaningful when provided."
        }
    }
}
