package com.memora.app.data.local

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetRepository

/** Room-backed implementation of the Asset persistence boundary. */
class RoomAssetRepository(
    private val assetDao: AssetDao,
) : AssetRepository {
    override suspend fun save(record: AssetIndexRecord) {
        assetDao.upsert(record.toEntity())
    }

    override suspend fun find(identity: AssetIdentity): AssetIndexRecord? = assetDao
        .find(identity.sourceId.value, identity.sourceAssetKey.value)
        ?.toDomain()
}
