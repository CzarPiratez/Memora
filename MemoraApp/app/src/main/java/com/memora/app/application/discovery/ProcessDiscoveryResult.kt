package com.memora.app.application.discovery

import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryResult
import javax.inject.Inject

/**
 * Commits only a successful source page. Access and source failures remain explicit
 * outcomes so a caller can present the right recovery action without advancing work.
 */
class ProcessDiscoveryResult @Inject constructor(
    private val persistDiscoveryPage: PersistDiscoveryPage,
) {
    suspend operator fun invoke(result: DiscoveryResult): DiscoveryResult = when (result) {
        is DiscoveryResult.Page -> persistPage(result)
        DiscoveryResult.AccessRequired,
        DiscoveryResult.AccessRevoked,
        is DiscoveryResult.Failed,
        -> result
    }

    private suspend fun persistPage(result: DiscoveryResult.Page): DiscoveryResult = try {
        persistDiscoveryPage(result.value)
        result
    } catch (_: Exception) {
        DiscoveryResult.Failed(
            DiscoveryFailure(
                code = PAGE_PERSISTENCE_FAILED,
                message = "UNFYND could not safely save this discovery page. Please try again.",
            ),
        )
    }

    private companion object {
        const val PAGE_PERSISTENCE_FAILED = "discovery_page_persistence_failed"
    }
}
