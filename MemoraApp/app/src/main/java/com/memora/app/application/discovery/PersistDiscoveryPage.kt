package com.memora.app.application.discovery

import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryPageStore
import javax.inject.Inject

/** Application boundary for committing a completed, bounded source-discovery page. */
class PersistDiscoveryPage @Inject constructor(
    private val discoveryPageStore: DiscoveryPageStore,
) {
    suspend operator fun invoke(page: DiscoveryPage) {
        discoveryPageStore.save(page)
    }
}
