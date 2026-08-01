package com.memora.app.data.notes

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.memora.app.domain.notes.NotesProviderSession
import com.memora.app.domain.notes.NotesProviderTokenVault
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/**
 * Keystore AES-GCM vault for OneNote-class session material.
 * Writes only under [Context.getNoBackupFilesDir]; never SharedPreferences plaintext.
 */
class KeystoreNotesProviderTokenVault(
    context: Context,
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val sessionFileName: String = DEFAULT_SESSION_FILE,
) : NotesProviderTokenVault {
    private val appContext = context.applicationContext

    override fun readSession(): NotesProviderSession? {
        val file = sessionFile()
        if (!file.exists()) return null
        return try {
            val wrapped = decodeWrapper(file.readBytes())
            if (wrapped.keyAlias != keyAlias || wrapped.formatVersion != FORMAT_VERSION) {
                return null
            }
            val secretKey = loadWrappingKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, wrapped.nonce),
            )
            val json = String(cipher.doFinal(wrapped.ciphertext), StandardCharsets.UTF_8)
            decodeSession(json)
        } catch (_: Exception) {
            null
        }
    }

    override fun writeSession(session: NotesProviderSession) {
        require(session.providerId == NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE) {
            "N2 vault accepts only the Microsoft OneNote provider id."
        }
        val plaintext = encodeSession(session).toByteArray(StandardCharsets.UTF_8)
        val secretKey = createOrLoadWrappingKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val ciphertext = cipher.doFinal(plaintext)
        val payload = encodeWrapper(
            WrappedSession(
                formatVersion = FORMAT_VERSION,
                keyAlias = keyAlias,
                nonce = cipher.iv,
                ciphertext = ciphertext,
            ),
        )
        val target = sessionFile()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeBytes(payload)
        check(temporary.renameTo(target) || (target.delete() && temporary.renameTo(target))) {
            "Unable to persist the Notes provider session wrapper."
        }
    }

    override fun clearSession() {
        sessionFile().delete()
        File(sessionFile().parentFile, "${sessionFile().name}.tmp").delete()
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }
        } catch (_: Exception) {
            // Best-effort; subsequent write recreates the key.
        }
    }

    private fun sessionFile(): File = File(appContext.noBackupFilesDir, sessionFileName)

    private fun createOrLoadWrappingKey(): SecretKey {
        loadWrappingKey()?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private fun loadWrappingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey
    }

    private fun encodeSession(session: NotesProviderSession): String =
        JSONObject()
            .put(KEY_PROVIDER_ID, session.providerId)
            .put(KEY_ACCOUNT_ID, session.accountId)
            .put(KEY_ACCOUNT_LABEL, session.accountDisplayLabel)
            .put(KEY_ACCESS_TOKEN, session.accessToken)
            .put(KEY_EXPIRES_AT, session.accessTokenExpiresAtEpochMs)
            .toString()

    private fun decodeSession(json: String): NotesProviderSession {
        val obj = JSONObject(json)
        return NotesProviderSession(
            providerId = obj.getString(KEY_PROVIDER_ID),
            accountId = obj.getString(KEY_ACCOUNT_ID),
            accountDisplayLabel = obj.getString(KEY_ACCOUNT_LABEL),
            accessToken = obj.getString(KEY_ACCESS_TOKEN),
            accessTokenExpiresAtEpochMs = obj.getLong(KEY_EXPIRES_AT),
        )
    }

    private fun encodeWrapper(wrapped: WrappedSession): ByteArray {
        val aliasBytes = wrapped.keyAlias.toByteArray(StandardCharsets.UTF_8)
        val buffer = ByteBuffer.allocate(
            Int.SIZE_BYTES +
                Int.SIZE_BYTES + aliasBytes.size +
                Int.SIZE_BYTES + wrapped.nonce.size +
                Int.SIZE_BYTES + wrapped.ciphertext.size,
        )
        buffer.putInt(wrapped.formatVersion)
        buffer.putInt(aliasBytes.size)
        buffer.put(aliasBytes)
        buffer.putInt(wrapped.nonce.size)
        buffer.put(wrapped.nonce)
        buffer.putInt(wrapped.ciphertext.size)
        buffer.put(wrapped.ciphertext)
        return buffer.array()
    }

    private fun decodeWrapper(bytes: ByteArray): WrappedSession {
        val buffer = ByteBuffer.wrap(bytes)
        val formatVersion = buffer.int
        val aliasLength = buffer.int
        require(aliasLength in 1..MAX_ALIAS_BYTES) { "Invalid Notes vault alias length." }
        val aliasBytes = ByteArray(aliasLength)
        buffer.get(aliasBytes)
        val nonceLength = buffer.int
        require(nonceLength in MIN_NONCE_BYTES..MAX_NONCE_BYTES) { "Invalid Notes vault nonce." }
        val nonce = ByteArray(nonceLength)
        buffer.get(nonce)
        val ciphertextLength = buffer.int
        require(ciphertextLength in 1..MAX_CIPHERTEXT_BYTES) { "Invalid Notes vault ciphertext." }
        val ciphertext = ByteArray(ciphertextLength)
        buffer.get(ciphertext)
        require(!buffer.hasRemaining()) { "Unexpected trailing Notes vault bytes." }
        return WrappedSession(
            formatVersion = formatVersion,
            keyAlias = aliasBytes.toString(StandardCharsets.UTF_8),
            nonce = nonce,
            ciphertext = ciphertext,
        )
    }

    private data class WrappedSession(
        val formatVersion: Int,
        val keyAlias: String,
        val nonce: ByteArray,
        val ciphertext: ByteArray,
    )

    companion object {
        const val DEFAULT_KEY_ALIAS = "memora.notes.onenote.session.v1"
        const val DEFAULT_SESSION_FILE = "memora_notes_onenote_session_v1.bin"
        const val FORMAT_VERSION = 1

        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val MAX_ALIAS_BYTES = 128
        private const val MIN_NONCE_BYTES = 12
        private const val MAX_NONCE_BYTES = 16
        private const val MAX_CIPHERTEXT_BYTES = 16_384

        private const val KEY_PROVIDER_ID = "providerId"
        private const val KEY_ACCOUNT_ID = "accountId"
        private const val KEY_ACCOUNT_LABEL = "accountDisplayLabel"
        private const val KEY_ACCESS_TOKEN = "accessToken"
        private const val KEY_EXPIRES_AT = "accessTokenExpiresAtEpochMs"

        /** Test helper: encode opaque bytes without implying JWT structure in fixtures. */
        fun syntheticAccessToken(seed: String): String =
            Base64.encodeToString(seed.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }
}
