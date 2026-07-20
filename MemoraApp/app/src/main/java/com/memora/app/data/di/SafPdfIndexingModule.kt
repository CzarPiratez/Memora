package com.memora.app.data.di

import android.content.Context
import com.memora.app.application.documents.IndexSafPdfFolder
import com.memora.app.application.documents.SafPdfFolderIndexer
import com.memora.app.data.saf.ContentResolverSafDocumentTreeCatalog
import com.memora.app.data.saf.SafDocumentTreeCatalog
import com.memora.app.data.saf.SafPdfDiscoverySourceFactory
import com.memora.app.domain.discovery.PdfFolderDiscoverySourceFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SafPdfIndexingBindings {
    @Binds
    @Singleton
    abstract fun bindSafPdfFolderIndexer(
        implementation: IndexSafPdfFolder,
    ): SafPdfFolderIndexer

    @Binds
    @Singleton
    abstract fun bindPdfFolderDiscoverySourceFactory(
        implementation: SafPdfDiscoverySourceFactory,
    ): PdfFolderDiscoverySourceFactory
}

@Module
@InstallIn(SingletonComponent::class)
object SafPdfIndexingDependencies {
    @Provides
    @Singleton
    fun provideSafDocumentTreeCatalog(
        @ApplicationContext context: Context,
    ): SafDocumentTreeCatalog = ContentResolverSafDocumentTreeCatalog(context)
}
