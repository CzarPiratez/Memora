package com.memora.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AiPackInstallLedgerDao {
    @Query(
        """
        SELECT * FROM ai_pack_install_ledger
        WHERE pack_id = :packId
        LIMIT 1
        """,
    )
    suspend fun find(packId: String): AiPackInstallLedgerEntity?

    @Upsert
    suspend fun upsert(entry: AiPackInstallLedgerEntity)

    @Query("SELECT COUNT(*) FROM ai_pack_install_ledger")
    suspend fun count(): Int

    @Query("DELETE FROM ai_pack_install_ledger")
    suspend fun deleteAll()
}
