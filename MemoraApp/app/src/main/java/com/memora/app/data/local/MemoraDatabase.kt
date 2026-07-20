package com.memora.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AssetEntity::class, DiscoveryCheckpointEntity::class, DocumentTreeApprovalEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class MemoraDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao

    abstract fun discoveryCheckpointDao(): DiscoveryCheckpointDao

    abstract fun documentTreeApprovalDao(): DocumentTreeApprovalDao
}
