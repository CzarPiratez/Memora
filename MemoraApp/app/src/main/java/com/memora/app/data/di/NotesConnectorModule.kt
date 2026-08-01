package com.memora.app.data.di

import android.content.Context
import com.memora.app.BuildConfig
import com.memora.app.application.notes.IndexOneNotePages
import com.memora.app.application.notes.OneNoteInteractiveAuth
import com.memora.app.application.notes.OneNotePagesIndexer
import com.memora.app.data.local.RoomNotePageExtractionPersistencePort
import com.memora.app.data.notes.GraphOneNotePageContentReader
import com.memora.app.data.notes.HttpOneNotePagesGraphGateway
import com.memora.app.data.notes.KeystoreNotesProviderTokenVault
import com.memora.app.data.notes.MsalOneNoteInteractiveAuth
import com.memora.app.data.notes.OneNotePageContentReader
import com.memora.app.data.notes.OneNotePagesDiscoverySource
import com.memora.app.data.notes.OneNotePagesGraphGateway
import com.memora.app.data.security.MemoraDatabaseHandle
import com.memora.app.domain.extraction.NotePageExtractionPersistence
import com.memora.app.domain.notes.NotesProviderTokenVault
import com.memora.app.domain.notes.OneNoteAuthConfiguration
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotesConnectorModule {
    @Provides
    @Singleton
    fun provideOneNoteAuthConfiguration(): OneNoteAuthConfiguration =
        OneNoteAuthConfiguration(
            clientId = BuildConfig.ONENOTE_CLIENT_ID.trim(),
            signatureHash = BuildConfig.ONENOTE_SIGNATURE_HASH.trim(),
        )

    @Provides
    @Singleton
    fun provideNotesProviderTokenVault(
        @ApplicationContext context: Context,
    ): NotesProviderTokenVault = KeystoreNotesProviderTokenVault(context)

    @Provides
    @Singleton
    fun provideOneNoteInteractiveAuth(
        @ApplicationContext context: Context,
        configuration: OneNoteAuthConfiguration,
        tokenVault: NotesProviderTokenVault,
    ): OneNoteInteractiveAuth = MsalOneNoteInteractiveAuth(
        context = context,
        configuration = configuration,
        tokenVault = tokenVault,
    )

    @Provides
    @Singleton
    fun provideOneNotePagesGraphGateway(): OneNotePagesGraphGateway =
        HttpOneNotePagesGraphGateway()

    @Provides
    @Singleton
    fun provideOneNotePagesDiscoverySource(
        tokenVault: NotesProviderTokenVault,
        graphGateway: OneNotePagesGraphGateway,
    ): OneNotePagesDiscoverySource = OneNotePagesDiscoverySource(
        tokenVault = tokenVault,
        graphGateway = graphGateway,
    )

    @Provides
    @Singleton
    fun provideOneNotePagesIndexer(indexOneNotePages: IndexOneNotePages): OneNotePagesIndexer =
        indexOneNotePages

    @Provides
    @Singleton
    fun provideNotePageExtractionPersistence(
        handle: MemoraDatabaseHandle,
    ): NotePageExtractionPersistence =
        RoomNotePageExtractionPersistencePort { handle.database() }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NotesExtractBindingsModule {
    @Binds
    @Singleton
    abstract fun bindOneNotePageContentReader(
        impl: GraphOneNotePageContentReader,
    ): OneNotePageContentReader
}
