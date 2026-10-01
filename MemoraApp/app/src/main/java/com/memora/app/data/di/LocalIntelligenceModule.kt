package com.memora.app.data.di

import android.content.Context
import com.memora.app.data.intelligence.HttpOnDeviceEmbeddingModelDownloader
import com.memora.app.data.intelligence.HttpMeaningEncoderChallengerDownloader
import com.memora.app.data.intelligence.NoBackupAiPackPayloadStore
import com.memora.app.data.intelligence.NoBackupMeaningEncoderChallengerStore
import com.memora.app.data.intelligence.NoBackupOnDeviceEmbeddingModelStore
import com.memora.app.data.intelligence.OnnxBgeSmallEmbeddingEngine
import com.memora.app.data.intelligence.StageARecallRanker
import com.memora.app.domain.intelligence.AiPackPayloadStore
import com.memora.app.domain.intelligence.DeterministicMemoryBuilder
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.MeaningEncoderChallengerDownloader
import com.memora.app.domain.intelligence.MeaningEncoderChallengerPackPresence
import com.memora.app.domain.intelligence.MemoryBuilder
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelDownloader
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelStore
import com.memora.app.domain.intelligence.RecallRanker
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

    @Binds
    @Singleton
    abstract fun bindOnDeviceEmbeddingModelDownloader(
        impl: HttpOnDeviceEmbeddingModelDownloader,
    ): OnDeviceEmbeddingModelDownloader

    @Binds
    @Singleton
    abstract fun bindMeaningEncoderChallengerDownloader(
        impl: HttpMeaningEncoderChallengerDownloader,
    ): MeaningEncoderChallengerDownloader

    @Binds
    @Singleton
    abstract fun bindMeaningEncoderChallengerPackPresence(
        impl: NoBackupMeaningEncoderChallengerStore,
    ): MeaningEncoderChallengerPackPresence

    /** MIG-04: production MemoryBuilder is deterministic assembly, not Unavailable. */
    @Binds
    @Singleton
    abstract fun bindMemoryBuilder(
        impl: DeterministicMemoryBuilder,
    ): MemoryBuilder

    /** FC-02 Stage A: ONNX when pack present, else identity. */
    @Binds
    @Singleton
    abstract fun bindRecallRanker(
        impl: StageARecallRanker,
    ): RecallRanker
}

@Module
@InstallIn(SingletonComponent::class)
object LocalIntelligenceProvidesModule {
    @Provides
    @Singleton
    fun provideOnnxBgeSmallEmbeddingEngine(
        @ApplicationContext context: Context,
        modelStore: OnDeviceEmbeddingModelStore,
    ): OnnxBgeSmallEmbeddingEngine = OnnxBgeSmallEmbeddingEngine(
        appContext = context.applicationContext,
        modelStore = modelStore,
    )

    @Provides
    @Singleton
    fun provideEmbeddingEngine(engine: OnnxBgeSmallEmbeddingEngine): EmbeddingEngine = engine
}
