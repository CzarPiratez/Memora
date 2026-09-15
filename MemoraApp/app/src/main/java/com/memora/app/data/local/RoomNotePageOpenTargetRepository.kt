package com.memora.app.data.local

import com.memora.app.domain.notes.NotePageOpenTarget
import com.memora.app.domain.notes.NotePageOpenTargetRepository

/** Room-backed implementation of the note open-target boundary. */
class RoomNotePageOpenTargetRepository(
    private val dao: () -> NotePageOpenTargetDao,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) : NotePageOpenTargetRepository {
    override suspend fun find(sourceId: String, sourceAssetKey: String): NotePageOpenTarget? =
        dao().find(sourceId, sourceAssetKey)?.toDomainOrNull()

    override suspend fun save(target: NotePageOpenTarget) {
        dao().upsert(
            NotePageOpenTargetEntity(
                sourceId = target.sourceId,
                sourceAssetKey = target.sourceAssetKey,
                clientUrl = target.clientUrl,
                webUrl = target.webUrl,
                updatedAtEpochMillis = nowEpochMillis(),
            ),
        )
    }
}

/**
 * A row with neither URL cannot open anything, and [NotePageOpenTarget] refuses
 * to represent one. Treat it as absent so the caller falls back to Graph rather
 * than crashing on data it could recover from.
 */
private fun NotePageOpenTargetEntity.toDomainOrNull(): NotePageOpenTarget? {
    val client = clientUrl?.takeIf { it.isNotBlank() }
    val web = webUrl?.takeIf { it.isNotBlank() }
    if (client == null && web == null) return null
    return NotePageOpenTarget(
        sourceId = sourceId,
        sourceAssetKey = sourceAssetKey,
        clientUrl = client,
        webUrl = web,
    )
}
