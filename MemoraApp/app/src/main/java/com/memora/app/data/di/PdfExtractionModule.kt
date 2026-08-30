package com.memora.app.data.di

import com.memora.app.application.documents.PdfIsolatedLocalReadingSession
import com.memora.app.application.documents.PdfReadOnlyDescriptorAccess
import com.memora.app.application.documents.ValidatedPdfLocalReadingPersister
import com.memora.app.data.pdfbox.isolation.DefaultPdfIsolatedLocalReadingSession
import com.memora.app.data.pdfbox.isolation.RoomValidatedPdfLocalReadingPersister
import com.memora.app.data.saf.SafPdfReadOnlyDescriptorAccess
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfExtractionBindingsModule {
    @Binds
    @Singleton
    abstract fun bindPdfReadOnlyDescriptorAccess(
        impl: SafPdfReadOnlyDescriptorAccess,
    ): PdfReadOnlyDescriptorAccess

    @Binds
    @Singleton
    abstract fun bindPdfIsolatedLocalReadingSession(
        impl: DefaultPdfIsolatedLocalReadingSession,
    ): PdfIsolatedLocalReadingSession

    @Binds
    @Singleton
    abstract fun bindValidatedPdfLocalReadingPersister(
        impl: RoomValidatedPdfLocalReadingPersister,
    ): ValidatedPdfLocalReadingPersister
}
