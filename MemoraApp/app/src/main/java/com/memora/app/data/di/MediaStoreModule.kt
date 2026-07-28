package com.memora.app.data.di

import android.content.Context
import com.memora.app.data.mediastore.ContentResolverImageExifReader
import com.memora.app.data.mediastore.ImageExifReader
import com.memora.app.data.mediastore.MediaStoreImageDiscoverySource
import com.memora.app.domain.discovery.ImageLibraryDiscoverySource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the read-only Android MediaStore adapter without initiating a scan. */
@Module
@InstallIn(SingletonComponent::class)
object MediaStoreModule {
    @Provides
    @Singleton
    fun provideImageLibraryDiscoverySource(
        @ApplicationContext context: Context,
    ): ImageLibraryDiscoverySource = MediaStoreImageDiscoverySource(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaStoreImageExifModule {
    @Binds
    @Singleton
    abstract fun bindImageExifReader(
        impl: ContentResolverImageExifReader,
    ): ImageExifReader
}
