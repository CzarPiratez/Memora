package com.memora.app.data.di

import android.content.Context
import androidx.work.WorkManager
import com.memora.app.work.DefaultSafPdfDiscoveryWorkScheduler
import com.memora.app.work.SafPdfDiscoveryWorkScheduler
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
}
