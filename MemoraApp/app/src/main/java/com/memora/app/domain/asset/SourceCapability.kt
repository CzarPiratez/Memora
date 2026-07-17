package com.memora.app.domain.asset

/** The approval mechanism required before a source adapter may read source content. */
enum class SourceAccessModel {
    ANDROID_MEDIA_PERMISSION,
    PERSISTED_DOCUMENT_ACCESS,
    PROVIDER_AUTHORIZATION,
}

/**
 * The contract advertised by a source adapter.
 *
 * This allows discovery orchestration to reason about a source without knowing whether
 * it is backed by MediaStore, a document tree, or an approved external provider.
 */
data class SourceCapability(
    val sourceId: SourceId,
    val supportedAssetTypes: Set<AssetType>,
    val accessModel: SourceAccessModel,
    val supportsIncrementalDiscovery: Boolean,
    val supportsBackgroundIndexing: Boolean,
    val isReadOnly: Boolean = true,
) {
    init {
        require(supportedAssetTypes.isNotEmpty()) {
            "A source must support at least one asset type."
        }
        require(isReadOnly) {
            "Memora source adapters must be read-only."
        }
    }
}
