package com.memora.app.data.local

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceAvailabilityCause
import com.memora.app.domain.asset.SourceAvailabilityObservation
import com.memora.app.domain.asset.SourceAvailabilityStatus
import com.memora.app.domain.asset.SourceAvailabilityStore
import com.memora.app.domain.asset.SourceId
import java.time.Instant

class RoomSourceAvailabilityStore(
    private val dao: () -> SourceAvailabilityDao,
) : SourceAvailabilityStore {
    override suspend fun find(
        sourceId: SourceId,
        sourceAssetKey: SourceAssetKey,
    ): SourceAvailabilityObservation? =
        dao().find(sourceId.value, sourceAssetKey.value)?.toDomainOrNull()

    override suspend fun findAll(
        identities: Collection<AssetIdentity>,
    ): Map<AssetIdentity, SourceAvailabilityObservation> {
        if (identities.isEmpty()) return emptyMap()
        val wanted = identities.toSet()
        val sourceIds = wanted.map { it.sourceId.value }.distinct()
        return dao().listForSources(sourceIds)
            .mapNotNull { row -> row.toDomainOrNull()?.let { identityOf(row) to it } }
            .filter { (identity, _) -> identity in wanted }
            .toMap()
    }

    override suspend fun save(observation: SourceAvailabilityObservation) {
        dao().upsert(observation.toEntity())
    }
}

private fun identityOf(row: SourceAvailabilityEntity) =
    AssetIdentity(SourceId(row.sourceId), SourceAssetKey(row.sourceAssetKey))

private fun SourceAvailabilityEntity.toDomainOrNull(): SourceAvailabilityObservation? {
    val status = runCatching { SourceAvailabilityStatus.valueOf(status) }.getOrNull()
        ?: return null
    val parsedCause = runCatching { SourceAvailabilityCause.valueOf(cause) }.getOrNull()
        ?: return null
    if (status == SourceAvailabilityStatus.UNKNOWN) return null
    return runCatching {
        SourceAvailabilityObservation(
            sourceId = SourceId(sourceId),
            sourceAssetKey = SourceAssetKey(sourceAssetKey),
            status = status,
            cause = parsedCause,
            observedAt = Instant.ofEpochMilli(observedAtEpochMillis),
        )
    }.getOrNull()
}

private fun SourceAvailabilityObservation.toEntity() = SourceAvailabilityEntity(
    sourceId = sourceId.value,
    sourceAssetKey = sourceAssetKey.value,
    status = status.name,
    cause = cause.name,
    observedAtEpochMillis = observedAt.toEpochMilli(),
)
