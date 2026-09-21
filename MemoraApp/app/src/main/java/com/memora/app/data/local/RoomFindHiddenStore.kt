package com.memora.app.data.local

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.FindHiddenItem
import com.memora.app.domain.asset.FindHiddenStore
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import java.time.Instant

class RoomFindHiddenStore(
    private val dao: () -> FindHiddenDao,
) : FindHiddenStore {
    override suspend fun hide(item: FindHiddenItem) {
        dao().upsert(item.toEntity())
    }

    override suspend fun showAgain(identity: AssetIdentity) {
        dao().delete(identity.sourceId.value, identity.sourceAssetKey.value)
    }

    override suspend fun listAll(): List<FindHiddenItem> =
        dao().listAll().mapNotNull { it.toDomainOrNull() }

    override suspend fun hiddenIdentities(
        among: Collection<AssetIdentity>,
    ): Set<AssetIdentity> {
        if (among.isEmpty()) return emptySet()
        val wanted = among.toSet()
        val sourceIds = wanted.map { it.sourceId.value }.distinct()
        return dao().listForSources(sourceIds)
            .mapNotNull { row -> row.toDomainOrNull()?.asIdentity() }
            .filter { it in wanted }
            .toSet()
    }
}

private fun FindHiddenEntity.toDomainOrNull(): FindHiddenItem? = runCatching {
    FindHiddenItem(
        sourceId = SourceId(sourceId),
        sourceAssetKey = SourceAssetKey(sourceAssetKey),
        label = label,
        hiddenAt = Instant.ofEpochMilli(hiddenAtEpochMillis),
    )
}.getOrNull()

private fun FindHiddenItem.toEntity() = FindHiddenEntity(
    sourceId = sourceId.value,
    sourceAssetKey = sourceAssetKey.value,
    label = label,
    hiddenAtEpochMillis = hiddenAt.toEpochMilli(),
)
