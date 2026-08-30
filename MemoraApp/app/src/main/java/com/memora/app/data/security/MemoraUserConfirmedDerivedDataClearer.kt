package com.memora.app.data.security

import com.memora.app.domain.privacy.UserConfirmedDerivedDataClearer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoraUserConfirmedDerivedDataClearer @Inject constructor(
    private val databaseHandle: MemoraDatabaseHandle,
) : UserConfirmedDerivedDataClearer {
    override fun clear() {
        databaseHandle.clearUserConfirmedDerivedData()
    }
}
