package dev.hossain.remotenotify.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import dev.hossain.remotenotify.model.AlertMediumConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TelegramConfigDataStoreTest {
    private lateinit var context: Context
    private lateinit var telegramConfigDataStore: TelegramConfigDataStore
    private val testDataStoreName = "test_telegram_config"

    companion object { // Use a companion object for static initialization
        private var firebaseInitialized = false

        @JvmStatic // Important for JUnit to recognize the @BeforeClass method
        @BeforeClass
        fun setup() {
            // Avoid `./gradlew :app:testReleaseUnitTest` test failure
            // - java.lang.IllegalStateException: Default FirebaseApp is not initialized in this process
            // dev.hossain.remotenotify. Make sure to call FirebaseApp.initializeApp(Context) first.
            if (!firebaseInitialized) {
                val context = ApplicationProvider.getApplicationContext<Context>()
                FirebaseApp.initializeApp(context)
                firebaseInitialized = true
            }
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Clean up any existing test files
        File(context.filesDir, "$testDataStoreName.preferences").delete()

        // Create test instance with our DataStore
        telegramConfigDataStore = TelegramConfigDataStore(context)
    }

    @After
    fun tearDown() {
        // Clean up the test DataStore file
        File(context.filesDir, "$testDataStoreName.preferences").delete()
    }

    @Test
    fun `saveBotToken saves token and can be retrieved`() =
        runTest {
            val testToken = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"

            telegramConfigDataStore.saveBotToken(testToken)

            val result = telegramConfigDataStore.botToken.first()
            assertThat(result).isEqualTo(testToken)
        }

    @Test
    fun `saveChatId saves chat id and can be retrieved`() =
        runTest {
            val testChatId = "@testchannel"

            telegramConfigDataStore.saveChatId(testChatId)

            val result = telegramConfigDataStore.chatId.first()
            assertThat(result).isEqualTo(testChatId)
        }

    @Test
    fun `getConfig returns config with saved values`() =
        runTest {
            val testToken = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
            val testChatId = "@testchannel"

            telegramConfigDataStore.saveBotToken(testToken)
            telegramConfigDataStore.saveChatId(testChatId)

            val config = telegramConfigDataStore.getConfig()

            assertThat(config.botToken).isEqualTo(testToken)
            assertThat(config.chatId).isEqualTo(testChatId)
        }

    @Test
    fun `getConfig returns empty strings when nothing saved`() =
        runTest {
            // Make sure the DataStore is clear
            telegramConfigDataStore.clearConfig()

            val config = telegramConfigDataStore.getConfig()

            assertThat(config.botToken).isEmpty()
            assertThat(config.chatId).isEmpty()
        }

    @Test
    fun `clearConfig removes all saved values`() =
        runTest {
            // Setup
            val testToken = "1234639012:BADwTrbllaPeF7m0Do-YgFfC1vFLNdhsVzk"
            val testChatId = "@testchannel"
            telegramConfigDataStore.saveBotToken(testToken)
            telegramConfigDataStore.saveChatId(testChatId)

            // Action
            telegramConfigDataStore.clearConfig()

            // Verify
            val botToken = telegramConfigDataStore.botToken.first()
            val chatId = telegramConfigDataStore.chatId.first()

            assertThat(botToken).isNull()
            assertThat(chatId).isNull()
        }

    @Test
    fun `hasValidConfig returns true when config is valid`() =
        runTest {
            val testToken = "1234639012:BADwTrbllaPeF7m0Do-YgFfC1vFLNdhsVzk"
            val testChatId = "@testchannel"

            telegramConfigDataStore.saveBotToken(testToken)
            telegramConfigDataStore.saveChatId(testChatId)

            val result = telegramConfigDataStore.hasValidConfig()

            assertThat(result).isTrue()
        }

    @Test
    fun `hasValidConfig returns false when bot token is missing`() =
        runTest {
            val testChatId = "@testchannel"

            telegramConfigDataStore.saveChatId(testChatId)

            val result = telegramConfigDataStore.hasValidConfig()

            assertThat(result).isFalse()
        }

    @Test
    fun `hasValidConfig returns false when chat id is missing`() =
        runTest {
            val testToken = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"

            telegramConfigDataStore.saveBotToken(testToken)

            val result = telegramConfigDataStore.hasValidConfig()

            assertThat(result).isFalse()
        }

    @Test
    fun `validateConfig returns valid result for valid token and chat id`() =
        runTest {
            val config =
                AlertMediumConfig.TelegramConfig(
                    botToken = "1234639012:BADwTrbllaPeF7m0Do-YgFfC1vFLNdhsVzk",
                    chatId = "@testchannel",
                )

            val result = telegramConfigDataStore.validateConfig(config)

            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateConfig returns valid result for numeric chat id`() =
        runTest {
            val config =
                AlertMediumConfig.TelegramConfig(
                    botToken = "1234639012:BADwTrbllaPeF7m0Do-YgFfC1vFLNdhsVzk",
                    chatId = "-1001234567890",
                )

            val result = telegramConfigDataStore.validateConfig(config)

            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateConfig returns errors for invalid bot token format`() =
        runTest {
            val config =
                AlertMediumConfig.TelegramConfig(
                    botToken = "invalid-token",
                    chatId = "@testchannel",
                )

            val result = telegramConfigDataStore.validateConfig(config)

            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(TelegramConfigDataStore.Companion.ValidationKeys.BOT_TOKEN)
        }

    @Test
    fun `validateConfig returns errors for invalid chat id format`() =
        runTest {
            val config =
                AlertMediumConfig.TelegramConfig(
                    botToken = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11",
                    chatId = "invalid chat id",
                )

            val result = telegramConfigDataStore.validateConfig(config)

            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(TelegramConfigDataStore.Companion.ValidationKeys.CHAT_ID)
        }

    @Test
    fun `validateConfig fails for wrong config type`() =
        runTest {
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = "123",
                    domain = "example.com",
                    fromEmail = "from@example.com",
                    toEmail = "to@example.com",
                )

            val result = telegramConfigDataStore.validateConfig(config)

            assertThat(result.isValid).isFalse()
        }
}
