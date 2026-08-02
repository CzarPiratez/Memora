package com.memora.app.data.di

import com.memora.app.data.intelligence.NoBackupAiPackPayloadStore
import com.memora.app.domain.intelligence.AiPackPayloadStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalIntelligenceModule {
    @Binds
    @Singleton
    abstract fun bindAiPackPayloadStore(
        impl: NoBackupAiPackPayloadStore,
    ): AiPackPayloadStore
}
