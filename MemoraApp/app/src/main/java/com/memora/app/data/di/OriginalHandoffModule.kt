package com.memora.app.data.di

import com.memora.app.application.handoff.ExternalOriginalViewer
import com.memora.app.application.handoff.HandoffImageUriCandidates
import com.memora.app.application.handoff.HandoffPdfUriAccess
import com.memora.app.application.handoff.ReadableContentUri
import com.memora.app.application.handoff.ShareOriginalChooser
import com.memora.app.data.mediastore.AndroidHandoffImageUriCandidates
import com.memora.app.data.mediastore.AndroidReadableContentUri
import com.memora.app.data.open.AndroidExternalOriginalViewer
import com.memora.app.data.saf.AndroidHandoffPdfUriAccess
import com.memora.app.data.share.AndroidShareOriginalChooser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OriginalHandoffModule {
    @Binds
    @Singleton
    abstract fun bindReadableContentUri(
        impl: AndroidReadableContentUri,
    ): ReadableContentUri

    @Binds
    @Singleton
    abstract fun bindHandoffImageUriCandidates(
        impl: AndroidHandoffImageUriCandidates,
    ): HandoffImageUriCandidates

    @Binds
    @Singleton
    abstract fun bindHandoffPdfUriAccess(
        impl: AndroidHandoffPdfUriAccess,
    ): HandoffPdfUriAccess

    @Binds
    @Singleton
    abstract fun bindShareOriginalChooser(
        impl: AndroidShareOriginalChooser,
    ): ShareOriginalChooser

    @Binds
    @Singleton
    abstract fun bindExternalOriginalViewer(
        impl: AndroidExternalOriginalViewer,
    ): ExternalOriginalViewer
}
