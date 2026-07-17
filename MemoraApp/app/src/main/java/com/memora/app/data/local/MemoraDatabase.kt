package com.memora.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AssetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MemoraDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
}
