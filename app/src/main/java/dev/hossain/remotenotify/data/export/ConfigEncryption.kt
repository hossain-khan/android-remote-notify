package dev.hossain.remotenotify.data.export

import android.util.Base64
import timber.log.Timber
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility for encrypting and decrypting sensitive configuration data using
 * password-based encryption (PBKDF2 with AES-GCM).
 */
object ConfigEncryption {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val IV_LENGTH = 12
    private const val SALT_LENGTH = 16
    private const val TAG_LENGTH = 128
    private const val ITERATIONS = 100000

    /**
     * Encrypts data using password-based encryption.
     *
     * @param data The plaintext data to encrypt
     * @param password The password to use for encryption
     * @return Base64-encoded encrypted data with salt and IV prepended
     */
    fun encrypt(
        data: String,
        password: String,
    ): String {
        try {
            val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }

            val secretKey = deriveKey(password, salt)
            val cipher =
                Cipher.getInstance(ALGORITHM).apply {
                    init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
                }

            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            // Combine salt + iv + encrypted data
            val combined = ByteArray(salt.size + iv.size + encryptedBytes.size)
            System.arraycopy(salt, 0, combined, 0, salt.size)
            System.arraycopy(iv, 0, combined, salt.size, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, salt.size + iv.size, encryptedBytes.size)

            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Timber.e(e, "Failed to encrypt data")
            throw ConfigEncryptionException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypts data using password-based encryption.
     *
     * @param encryptedData Base64-encoded encrypted data with salt and IV prepended
     * @param password The password to use for decryption
     * @return The decrypted plaintext data
     */
    fun decrypt(
        encryptedData: String,
        password: String,
    ): String {
        try {
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)

            if (combined.size < SALT_LENGTH + IV_LENGTH) {
                throw ConfigEncryptionException("Invalid encrypted data format")
            }

            // Extract salt, iv, and encrypted data
            val salt = combined.copyOfRange(0, SALT_LENGTH)
            val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
            val encryptedBytes = combined.copyOfRange(SALT_LENGTH + IV_LENGTH, combined.size)

            val secretKey = deriveKey(password, salt)
            val cipher =
                Cipher.getInstance(ALGORITHM).apply {
                    init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
                }

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: ConfigEncryptionException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt data")
            throw ConfigEncryptionException("Failed to decrypt data. Invalid password or corrupted data.", e)
        }
    }

    private fun deriveKey(
        password: String,
        salt: ByteArray,
    ): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, KEY_ALGORITHM)
    }
}

/**
 * Exception thrown when encryption or decryption fails.
 */
class ConfigEncryptionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
