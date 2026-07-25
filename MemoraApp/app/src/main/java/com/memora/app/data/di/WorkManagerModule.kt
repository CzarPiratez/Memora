package com.memora.app.data.di

import android.content.Context
import androidx.work.WorkManager
import com.memora.app.work.DefaultSafPdfDiscoveryWorkScheduler
import com.memora.app.work.DefaultSafPdfExtractWorkScheduler
import com.memora.app.work.SafPdfDiscoveryWorkScheduler
import com.memora.app.work.SafPdfExtractWorkScheduler
import com.memora.app.application.documents.PendingPdfLocalReader
import com.memora.app.application.documents.RunPendingPdfLocalReading
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SafPdfDiscoveryWorkSchedulerModule {
    @Binds
    @Singleton
    abstract fun bindSafPdfDiscoveryWorkScheduler(
        impl: DefaultSafPdfDiscoveryWorkScheduler,
    ): SafPdfDiscoveryWorkScheduler

    @Binds
    @Singleton
    abstract fun bindSafPdfExtractWorkScheduler(
        impl: DefaultSafPdfExtractWorkScheduler,
    ): SafPdfExtractWorkScheduler

    @Binds
    @Singleton
    abstract fun bindPendingPdfLocalReader(
        impl: RunPendingPdfLocalReading,
    ): PendingPdfLocalReader
}
