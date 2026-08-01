package com.memora.app.data.di

import android.content.Context
import com.memora.app.BuildConfig
import com.memora.app.data.notes.KeystoreNotesProviderTokenVault
import com.memora.app.data.notes.MsalOneNoteInteractiveAuth
import com.memora.app.application.notes.OneNoteInteractiveAuth
import com.memora.app.domain.notes.NotesProviderTokenVault
import com.memora.app.domain.notes.OneNoteAuthConfiguration
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
}
