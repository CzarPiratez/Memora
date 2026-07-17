package com.memora.app.domain.discovery

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.SourceCapability
import com.memora.app.domain.asset.SourceId

/**
 * An opaque, source-owned checkpoint for incremental discovery. It must only ever be
 * given back to the source that produced it.
 */
data class DiscoveryCursor(
    val sourceId: SourceId,
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "A discovery cursor cannot be blank." }
    }
}

/** A bounded discovery request that prevents a source adapter from doing unbounded work. */
data class DiscoveryRequest(
    val cursor: DiscoveryCursor? = null,
    val batchSize: Int = DEFAULT_BATCH_SIZE,
) {
    init {
        require(batchSize in MIN_BATCH_SIZE..MAX_BATCH_SIZE) {
            "A discovery batch must contain between $MIN_BATCH_SIZE and $MAX_BATCH_SIZE assets."
        }
    }

    companion object {
        const val DEFAULT_BATCH_SIZE: Int = 50
        const val MIN_BATCH_SIZE: Int = 1
        const val MAX_BATCH_SIZE: Int = 100
    }
}

/** A stable page of source-neutral Asset descriptions returned by a discovery source. */
data class DiscoveryPage(
    val sourceId: SourceId,
    val assets: List<Asset>,
    val checkpoint: DiscoveryCursor,
    val hasMore: Boolean,
) {
    init {
        require(assets.all { it.identity.sourceId == sourceId }) {
            "A discovery page may only contain assets from its declared source."
        }
        require(checkpoint.sourceId == sourceId) {
            "A discovery page checkpoint must belong to the same source."
        }
        require(assets.map(Asset::identity).distinct().size == assets.size) {
            "A discovery page cannot contain duplicate asset identities."
        }
    }

    val isComplete: Boolean
        get() = !hasMore
}

/** Structured source failure that is safe to record, surface, and retry later. */
data class DiscoveryFailure(
    val code: String,
    val message: String,
) {
    init {
        require(code.isNotBlank()) { "A discovery failure code cannot be blank." }
        require(message.isNotBlank()) { "A discovery failure message cannot be blank." }
    }
}

/** The outcome of one source-discovery attempt. */
sealed interface DiscoveryResult {
    data class Page(val value: DiscoveryPage) : DiscoveryResult

    data object AccessRequired : DiscoveryResult

    data object AccessRevoked : DiscoveryResult

    data class Failed(val failure: DiscoveryFailure) : DiscoveryResult
}

/** The known source-access status used by setup UI and background work. */
enum class SourceAccessState {
    GRANTED,
    ACCESS_REQUIRED,
    ACCESS_REVOKED,
    UNAVAILABLE,
}

/**
 * Read-only discovery boundary implemented by MediaStore, SAF, and approved providers.
 *
 * Implementations return asset descriptions and source-owned cursors only. They must
 * not modify originals, interpret semantic content, or assume that an access failure
 * is equivalent to an empty source.
 */
interface AssetDiscoverySource {
    val capability: SourceCapability

    suspend fun accessState(): SourceAccessState

    suspend fun discover(request: DiscoveryRequest): DiscoveryResult
}
