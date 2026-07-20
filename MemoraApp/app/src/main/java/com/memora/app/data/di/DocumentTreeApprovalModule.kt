package com.memora.app.data.di

import com.memora.app.application.documents.ApproveDocumentTree
import com.memora.app.application.documents.DocumentTreeApprover
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DocumentTreeApprovalModule {
    @Binds
    @Singleton
    abstract fun bindDocumentTreeApprover(
        implementation: ApproveDocumentTree,
    ): DocumentTreeApprover
}
