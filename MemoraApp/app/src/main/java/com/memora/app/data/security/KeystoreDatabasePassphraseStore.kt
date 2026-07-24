package com.memora.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Wraps a randomly generated database passphrase with a non-exportable Android Keystore
 * AES-256-GCM key. Persists only wrapper metadata under no-backup private storage.
 *
 * This helper has no Room, SQLCipher, source, UI, or network dependency.
 */
class KeystoreDatabasePassphraseStore(
    context: Context,
    private val keyAlias: String = DEFAULT_POC_KEY_ALIAS,
    private val wrapperFileName: String = DEFAULT_POC_WRAPPER_FILE,
) {
    private val appContext = context.applicationContext
    private val secureRandom = SecureRandom()

    fun createAndWrapNewPassphrase(): PassphraseUnwrapResult {
        if (wrapperFile().exists()) {
            return PassphraseUnwrapResult.Denied(DatabaseSecretFailureCategory.WRAPPER_INVALID)
        }

        val passphrase = ByteArray(PASSPHRASE_BYTE_LENGTH).also(secureRandom::nextBytes)
        return try {
            val secretKey = createOrLoadWrappingKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val ciphertext = cipher.doFinal(passphrase)
            val nonce = cipher.iv
            persistWrapper(
                WrappedPassphrase(
                    wrapperFormatVersion = WRAPPER_FORMAT_VERSION,
                    keyAlias = keyAlias,
                    nonce = nonce,
                    ciphertext = ciphertext,
                ),
            )
            PassphraseUnwrapResult.Unwrapped(passphrase)
        } catch (_: Exception) {
            passphrase.fill(0)
            wrapperFile().delete()
            PassphraseUnwrapResult.Denied(DatabaseSecretFailureCategory.KEY_UNAVAILABLE)
        }
    }

    fun unwrapExistingPassphrase(): PassphraseUnwrapResult {
        val wrapped = readWrapper()
            ?: return PassphraseUnwrapResult.Denied(DatabaseSecretFailureCategory.WRAPPER_INVALID)
        if (wrapped.wrapperFormatVersion != WRAPPER_FORMAT_VERSION || wrapped.keyAlias != keyAlias) {
            return PassphraseUnwrapResult.Denied(DatabaseSecretFailureCategory.WRAPPER_INVALID)
        }

        return try {
            val secretKey = loadWrappingKey()
                ?: return PassphraseUnwrapResult.Denied(DatabaseSecretFailureCategory.KEY_UNAVAILABLE)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, wrapped.nonce),
            )
            PassphraseUnwrapResult.Unwrapped(cipher.doFinal(wrapped.ciphertext))
        } catch (_: Exception) {
            PassphraseUnwrapResult.Denied(DatabaseSecretFailureCategory.WRAPPER_INVALID)
        }
    }

    fun clearOwnedState() {
        wrapperFile().delete()
        File(wrapperFile().parentFile, "${wrapperFile().name}.tmp").delete()
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }
        } catch (_: Exception) {
            // Best-effort; subsequent create will surface KEY_UNAVAILABLE.
        }
    }

    fun clearForTest() {
        clearOwnedState()
    }

    fun tamperWrapperForTest() {
        val file = wrapperFile()
        check(file.exists()) { "PoC wrapper must exist before tampering." }
        val bytes = file.readBytes()
        check(bytes.isNotEmpty()) { "PoC wrapper must not be empty before tampering." }
        bytes[bytes.lastIndex] = (bytes[bytes.lastIndex].toInt() xor 0x5A).toByte()
        file.writeBytes(bytes)
    }

    fun clearPassphrase(passphrase: ByteArray) {
        passphrase.fill(0)
    }

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

    private fun persistWrapper(wrapped: WrappedPassphrase) {
        val payload = encodeWrapper(wrapped)
        val target = wrapperFile()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeBytes(payload)
        check(temporary.renameTo(target) || (target.delete() && temporary.renameTo(target))) {
            "Unable to persist the PoC passphrase wrapper."
        }
    }

    private fun readWrapper(): WrappedPassphrase? {
        val file = wrapperFile()
        if (!file.exists()) {
            return null
        }
        return try {
            decodeWrapper(file.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    private fun wrapperFile(): File = File(appContext.noBackupFilesDir, wrapperFileName)

    private fun encodeWrapper(wrapped: WrappedPassphrase): ByteArray {
        val keyAliasBytes = wrapped.keyAlias.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(
            Int.SIZE_BYTES +
                Int.SIZE_BYTES + keyAliasBytes.size +
                Int.SIZE_BYTES + wrapped.nonce.size +
                Int.SIZE_BYTES + wrapped.ciphertext.size,
        )
        buffer.putInt(wrapped.wrapperFormatVersion)
        buffer.putInt(keyAliasBytes.size)
        buffer.put(keyAliasBytes)
        buffer.putInt(wrapped.nonce.size)
        buffer.put(wrapped.nonce)
        buffer.putInt(wrapped.ciphertext.size)
        buffer.put(wrapped.ciphertext)
        return buffer.array()
    }

    private fun decodeWrapper(bytes: ByteArray): WrappedPassphrase {
        val buffer = ByteBuffer.wrap(bytes)
        val formatVersion = buffer.int
        val aliasLength = buffer.int
        require(aliasLength in 1..MAX_ALIAS_BYTES) { "Invalid wrapper alias length." }
        val aliasBytes = ByteArray(aliasLength)
        buffer.get(aliasBytes)
        val nonceLength = buffer.int
        require(nonceLength in MIN_NONCE_BYTES..MAX_NONCE_BYTES) { "Invalid wrapper nonce length." }
        val nonce = ByteArray(nonceLength)
        buffer.get(nonce)
        val ciphertextLength = buffer.int
        require(ciphertextLength in 1..MAX_CIPHERTEXT_BYTES) { "Invalid wrapper ciphertext length." }
        val ciphertext = ByteArray(ciphertextLength)
        buffer.get(ciphertext)
        require(!buffer.hasRemaining()) { "Unexpected trailing wrapper bytes." }
        return WrappedPassphrase(
            wrapperFormatVersion = formatVersion,
            keyAlias = aliasBytes.toString(Charsets.UTF_8),
            nonce = nonce,
            ciphertext = ciphertext,
        )
    }

    private data class WrappedPassphrase(
        val wrapperFormatVersion: Int,
        val keyAlias: String,
        val nonce: ByteArray,
        val ciphertext: ByteArray,
    )

    companion object {
        const val DEFAULT_POC_KEY_ALIAS = "memora.poc.db.wrap.v1"
        const val DEFAULT_POC_WRAPPER_FILE = "memora_poc_db_wrap_v1.bin"
        const val WRAPPER_FORMAT_VERSION = 1
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val PASSPHRASE_BYTE_LENGTH = 32
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val MAX_ALIAS_BYTES = 128
        private const val MIN_NONCE_BYTES = 12
        private const val MAX_NONCE_BYTES = 16
        private const val MAX_CIPHERTEXT_BYTES = 512
    }
}

sealed class PassphraseUnwrapResult {
    data class Unwrapped(val passphrase: ByteArray) : PassphraseUnwrapResult()
    data class Denied(val category: DatabaseSecretFailureCategory) : PassphraseUnwrapResult()
}
