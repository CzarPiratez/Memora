package com.memora.app.domain.intelligence

/**
 * App-private storage for verified AI Pack payload bytes (Spec §6).
 *
 * Implementations must not make payloads world-readable and must not upload them.
 */
interface AiPackPayloadStore {
    fun writePayload(packId: String, payload: ByteArray)

    fun readPayload(packId: String): ByteArray?

    fun deletePayload(packId: String)

    fun clearAll()
}
