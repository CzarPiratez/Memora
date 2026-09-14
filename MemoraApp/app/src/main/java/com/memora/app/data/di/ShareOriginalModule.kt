package com.memora.app.data.di

import com.memora.app.application.share.ReadableContentUri
import com.memora.app.application.share.ShareImageUriCandidates
import com.memora.app.application.share.ShareOriginalChooser
import com.memora.app.application.share.ShareablePdfUriAccess
import com.memora.app.data.mediastore.AndroidReadableContentUri
import com.memora.app.data.mediastore.AndroidShareImageUriCandidates
import com.memora.app.data.saf.AndroidShareablePdfUriAccess
import com.memora.app.data.share.AndroidShareOriginalChooser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ShareOriginalModule {
    @Binds
    @Singleton
    abstract fun bindReadableContentUri(
        impl: AndroidReadableContentUri,
    ): ReadableContentUri

    @Binds
    @Singleton
    abstract fun bindShareImageUriCandidates(
        impl: AndroidShareImageUriCandidates,
    ): ShareImageUriCandidates

    @Binds
    @Singleton
    abstract fun bindShareablePdfUriAccess(
        impl: AndroidShareablePdfUriAccess,
    ): ShareablePdfUriAccess

    @Binds
    @Singleton
    abstract fun bindShareOriginalChooser(
        impl: AndroidShareOriginalChooser,
    ): ShareOriginalChooser
}
