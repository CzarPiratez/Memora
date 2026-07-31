package com.memora.app.data.di

import android.content.Context
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.RoomAssetRepository
import com.memora.app.data.local.RoomDiscoveryCheckpointRepository
import com.memora.app.data.local.RoomDocumentTreeApprovalRepository
import com.memora.app.data.local.RoomDiscoveryPageStore
import com.memora.app.data.local.RoomImageExifExtractionPersistencePort
import com.memora.app.data.local.RoomAssetMemoryFactSource
import com.memora.app.data.local.RoomMemoryRepository
import com.memora.app.data.local.RoomPhotoOcrExtractionPersistencePort
import com.memora.app.data.local.RoomScreenshotOcrExtractionPersistencePort
import com.memora.app.data.security.MemoraDatabaseHandle
import com.memora.app.data.security.ProductionDatabaseIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.discovery.DiscoveryCheckpointRepository
import com.memora.app.domain.discovery.DiscoveryPageStore
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import com.memora.app.domain.extraction.ImageExifExtractionPersistence
import com.memora.app.domain.extraction.PhotoOcrExtractionPersistence
import com.memora.app.domain.extraction.ScreenshotOcrExtractionPersistence
import com.memora.app.domain.memory.AssetMemoryFactSource
import com.memora.app.domain.memory.MemoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    const val DATABASE_NAME = ProductionDatabaseIdentity.DATABASE_NAME

    @Provides
    @Singleton
    fun provideMemoraDatabaseHandle(
        @ApplicationContext context: Context,
    ): MemoraDatabaseHandle = MemoraDatabaseHandle(context)

    @Provides
    fun provideMemoraDatabase(handle: MemoraDatabaseHandle): MemoraDatabase = handle.database()

    @Provides
    @Singleton
    fun provideAssetRepository(handle: MemoraDatabaseHandle): AssetRepository =
        RoomAssetRepository(assetDao = { handle.database().assetDao() })

    @Provides
    @Singleton
    fun provideImageExifExtractionPersistence(
        handle: MemoraDatabaseHandle,
    ): ImageExifExtractionPersistence =
        RoomImageExifExtractionPersistencePort { handle.database() }

    @Provides
    @Singleton
    fun provideScreenshotOcrExtractionPersistence(
        handle: MemoraDatabaseHandle,
    ): ScreenshotOcrExtractionPersistence =
        RoomScreenshotOcrExtractionPersistencePort { handle.database() }

    @Provides
    @Singleton
    fun providePhotoOcrExtractionPersistence(
        handle: MemoraDatabaseHandle,
    ): PhotoOcrExtractionPersistence =
        RoomPhotoOcrExtractionPersistencePort { handle.database() }

    @Provides
    @Singleton
    fun provideMemoryRepository(handle: MemoraDatabaseHandle): MemoryRepository =
        RoomMemoryRepository { handle.database() }

    @Provides
    @Singleton
    fun provideAssetMemoryFactSource(handle: MemoraDatabaseHandle): AssetMemoryFactSource =
        RoomAssetMemoryFactSource { handle.database() }

    @Provides
    @Singleton
    fun provideDiscoveryCheckpointRepository(
        handle: MemoraDatabaseHandle,
    ): DiscoveryCheckpointRepository =
        RoomDiscoveryCheckpointRepository(
            checkpointDao = { handle.database().discoveryCheckpointDao() },
        )

    @Provides
    @Singleton
    fun provideDocumentTreeApprovalRepository(
        handle: MemoraDatabaseHandle,
    ): DocumentTreeApprovalRepository =
        RoomDocumentTreeApprovalRepository(
            approvalDao = { handle.database().documentTreeApprovalDao() },
        )

    @Provides
    @Singleton
    fun provideDiscoveryPageStore(handle: MemoraDatabaseHandle): DiscoveryPageStore =
        RoomDiscoveryPageStore(database = { handle.database() })
}
