package com.memora.app.data.di

import android.content.Context
import com.memora.app.data.intelligence.MediaPipeEmbeddingEngine
import com.memora.app.data.intelligence.NoBackupAiPackPayloadStore
import com.memora.app.data.intelligence.NoBackupOnDeviceEmbeddingModelStore
import com.memora.app.domain.intelligence.AiPackPayloadStore
import com.memora.app.domain.intelligence.DeterministicMemoryBuilder
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.MemoryBuilder
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Binds
    @Singleton
    abstract fun bindOnDeviceEmbeddingModelStore(
        impl: NoBackupOnDeviceEmbeddingModelStore,
    ): OnDeviceEmbeddingModelStore

    /** MIG-04: production MemoryBuilder is deterministic assembly, not Unavailable. */
    @Binds
    @Singleton
    abstract fun bindMemoryBuilder(
        impl: DeterministicMemoryBuilder,
    ): MemoryBuilder
}

@Module
@InstallIn(SingletonComponent::class)
object LocalIntelligenceProvidesModule {
    @Provides
    @Singleton
    fun provideMediaPipeEmbeddingEngine(
        @ApplicationContext context: Context,
        modelStore: OnDeviceEmbeddingModelStore,
    ): MediaPipeEmbeddingEngine = MediaPipeEmbeddingEngine(
        appContext = context.applicationContext,
        modelStore = modelStore,
    )

    @Provides
    @Singleton
    fun provideEmbeddingEngine(engine: MediaPipeEmbeddingEngine): EmbeddingEngine = engine
}
