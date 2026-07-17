package com.memora.app.data.di

import android.content.Context
import androidx.room.Room
import com.memora.app.data.local.AssetDao
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.RoomAssetRepository
import com.memora.app.domain.asset.AssetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    @Provides
    @Singleton
    fun provideMemoraDatabase(
        @ApplicationContext context: Context
    ): MemoraDatabase = Room.databaseBuilder(
        context,
        MemoraDatabase::class.java,
        DATABASE_NAME
    ).build()

    @Provides
    fun provideAssetDao(database: MemoraDatabase): AssetDao = database.assetDao()

    @Provides
    @Singleton
    fun provideAssetRepository(assetDao: AssetDao): AssetRepository =
        RoomAssetRepository(assetDao)

    private const val DATABASE_NAME = "memora.db"
}
