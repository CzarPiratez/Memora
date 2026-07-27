package com.memora.app.data.di

import com.memora.app.application.documents.PdfPagePreviewRenderer
import com.memora.app.data.saf.AndroidPdfPagePreviewRenderer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfViewerModule {
    @Binds
    @Singleton
    abstract fun bindPdfPagePreviewRenderer(
        implementation: AndroidPdfPagePreviewRenderer,
    ): PdfPagePreviewRenderer
}
