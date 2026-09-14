package com.memora.app.data.di

import com.memora.app.application.find.FindThumbnailCache
import com.memora.app.application.find.ImageThumbnailLoader
import com.memora.app.application.find.MediaStoreImageUriResolver
import com.memora.app.application.find.MemoryFindThumbnailCache
import com.memora.app.data.mediastore.AndroidImageThumbnailLoader
import com.memora.app.data.mediastore.AndroidMediaStoreImageUriResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FindThumbnailModule {
    @Binds
    @Singleton
    abstract fun bindFindThumbnailCache(
        impl: MemoryFindThumbnailCache,
    ): FindThumbnailCache

    @Binds
    @Singleton
    abstract fun bindImageThumbnailLoader(
        impl: AndroidImageThumbnailLoader,
    ): ImageThumbnailLoader

    @Binds
    @Singleton
    abstract fun bindMediaStoreImageUriResolver(
        impl: AndroidMediaStoreImageUriResolver,
    ): MediaStoreImageUriResolver
}
