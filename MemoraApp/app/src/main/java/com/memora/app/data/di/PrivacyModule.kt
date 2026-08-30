package com.memora.app.data.di

import com.memora.app.data.security.MemoraUserConfirmedDerivedDataClearer
import com.memora.app.domain.privacy.UserConfirmedDerivedDataClearer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PrivacyBindingsModule {
    @Binds
    @Singleton
    abstract fun bindUserConfirmedDerivedDataClearer(
        impl: MemoraUserConfirmedDerivedDataClearer,
    ): UserConfirmedDerivedDataClearer
}
