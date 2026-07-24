package com.memora.app.data.di

import android.content.Context
import com.memora.app.data.local.AssetDao
import com.memora.app.data.local.DiscoveryCheckpointDao
import com.memora.app.data.local.DocumentTreeApprovalDao
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.RoomAssetRepository
import com.memora.app.data.local.RoomDiscoveryCheckpointRepository
import com.memora.app.data.local.RoomDocumentTreeApprovalRepository
import com.memora.app.data.local.RoomDiscoveryPageStore
import com.memora.app.data.security.MemoraEncryptedDatabaseOpener
import com.memora.app.data.security.ProductionDatabaseIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.discovery.DiscoveryCheckpointRepository
import com.memora.app.domain.discovery.DiscoveryPageStore
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
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
    fun provideMemoraDatabase(
        @ApplicationContext context: Context
    ): MemoraDatabase = MemoraEncryptedDatabaseOpener.open(context)

    @Provides
    fun provideAssetDao(database: MemoraDatabase): AssetDao = database.assetDao()

    @Provides
    fun provideDiscoveryCheckpointDao(database: MemoraDatabase): DiscoveryCheckpointDao =
        database.discoveryCheckpointDao()

    @Provides
    fun provideDocumentTreeApprovalDao(database: MemoraDatabase): DocumentTreeApprovalDao =
        database.documentTreeApprovalDao()

    @Provides
    @Singleton
    fun provideAssetRepository(assetDao: AssetDao): AssetRepository =
        RoomAssetRepository(assetDao)

    @Provides
    @Singleton
    fun provideDiscoveryCheckpointRepository(
        checkpointDao: DiscoveryCheckpointDao,
    ): DiscoveryCheckpointRepository = RoomDiscoveryCheckpointRepository(checkpointDao)

    @Provides
    @Singleton
    fun provideDocumentTreeApprovalRepository(
        approvalDao: DocumentTreeApprovalDao,
    ): DocumentTreeApprovalRepository = RoomDocumentTreeApprovalRepository(approvalDao)

    @Provides
    @Singleton
    fun provideDiscoveryPageStore(database: MemoraDatabase): DiscoveryPageStore =
        RoomDiscoveryPageStore(database)
}
