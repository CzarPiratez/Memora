package com.memora.app.domain.discovery

/**
 * Persists one discovered page and its durable checkpoint as a single all-or-nothing
 * operation. A caller must never advance a cursor separately from its Asset records.
 */
interface DiscoveryPageStore {
    suspend fun save(page: DiscoveryPage)
}
