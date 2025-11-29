package dev.hossain.remotenotify.data.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
class ConfigEncryptionTest {
    @Before
    fun setup() {
        // Initialize Firebase if not already done - needed for release tests
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        // Clean up any existing Timber tree to avoid calling into crashlytics
        Timber.uprootAll()
    }

    @Test
    fun `encrypt and decrypt returns original data`() {
        val originalData = "test data with special chars: @#$%^&*()"
        val password = "testPassword123"

        val encrypted = ConfigEncryption.encrypt(originalData, password)
        val decrypted = ConfigEncryption.decrypt(encrypted, password)

        assertThat(decrypted).isEqualTo(originalData)
    }

    @Test
    fun `encrypt produces different output each time due to random salt and iv`() {
        val data = "same data"
        val password = "samePassword"

        val encrypted1 = ConfigEncryption.encrypt(data, password)
        val encrypted2 = ConfigEncryption.encrypt(data, password)

        assertThat(encrypted1).isNotEqualTo(encrypted2)
    }

    @Test(expected = ConfigEncryptionException::class)
    fun `decrypt with wrong password throws exception`() {
        val data = "secret data"
        val encrypted = ConfigEncryption.encrypt(data, "correctPassword")

        ConfigEncryption.decrypt(encrypted, "wrongPassword")
    }

    @Test
    fun `encrypt and decrypt handles empty string`() {
        val originalData = ""
        val password = "testPassword"

        val encrypted = ConfigEncryption.encrypt(originalData, password)
        val decrypted = ConfigEncryption.decrypt(encrypted, password)

        assertThat(decrypted).isEqualTo(originalData)
    }

    @Test
    fun `encrypt and decrypt handles unicode characters`() {
        val originalData = "Hello 世界 🌍 مرحبا"
        val password = "unicode🔐password"

        val encrypted = ConfigEncryption.encrypt(originalData, password)
        val decrypted = ConfigEncryption.decrypt(encrypted, password)

        assertThat(decrypted).isEqualTo(originalData)
    }

    @Test
    fun `encrypt and decrypt handles json string`() {
        val jsonData = """{"botToken":"123456:ABC","chatId":"@channel"}"""
        val password = "securePass"

        val encrypted = ConfigEncryption.encrypt(jsonData, password)
        val decrypted = ConfigEncryption.decrypt(encrypted, password)

        assertThat(decrypted).isEqualTo(jsonData)
    }

    @Test(expected = ConfigEncryptionException::class)
    fun `decrypt with corrupted data throws exception`() {
        val password = "testPassword"

        // Invalid base64 encoded data
        ConfigEncryption.decrypt("invalid_data!!!", password)
    }

    @Test(expected = ConfigEncryptionException::class)
    fun `decrypt with truncated data throws exception`() {
        val password = "testPassword"
        val encrypted = ConfigEncryption.encrypt("test", password)

        // Truncate the encrypted data
        val truncated = encrypted.substring(0, 10)
        ConfigEncryption.decrypt(truncated, password)
    }
}
