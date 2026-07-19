package com.memora.app.data.di

import com.memora.app.application.discovery.IndexMediaStoreImages
import com.memora.app.application.discovery.MediaStoreImageIndexer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the explicit, read-only MediaStore indexing use case for presentation code. */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaStoreIndexingModule {
    @Binds
    @Singleton
    abstract fun bindMediaStoreImageIndexer(
        implementation: IndexMediaStoreImages,
    ): MediaStoreImageIndexer
}
