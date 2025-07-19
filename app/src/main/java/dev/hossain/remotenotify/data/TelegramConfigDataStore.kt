package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hossain.remotenotify.di.ApplicationContext
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.telegramConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "telegram_config")

@SingleIn(AppScope::class)
class TelegramConfigDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlertMediumConfigStore {
        companion object {
            private val BOT_TOKEN_KEY = stringPreferencesKey("bot_token")
            private val CHAT_ID_KEY = stringPreferencesKey("chat_id")

            object ValidationKeys {
                const val BOT_TOKEN = "botToken"
                const val CHAT_ID = "chatId"
            }
        }

        val botToken: Flow<String?> =
            context.telegramConfigDataStore.data
                .map { preferences ->
                    preferences[BOT_TOKEN_KEY]
                }

        val chatId: Flow<String?> =
            context.telegramConfigDataStore.data
                .map { preferences ->
                    preferences[CHAT_ID_KEY]
                }

        suspend fun getConfig(): AlertMediumConfig.TelegramConfig {
            val botToken = botToken.first() ?: ""
            val chatId = chatId.first() ?: ""
            return AlertMediumConfig.TelegramConfig(botToken, chatId)
        }

        suspend fun saveBotToken(botToken: String) {
            Timber.d("Saving bot token: $botToken")
            context.telegramConfigDataStore.edit { preferences ->
                preferences[BOT_TOKEN_KEY] = botToken
            }
        }

        suspend fun saveChatId(chatId: String) {
            Timber.d("Saving chat id: $chatId")
            context.telegramConfigDataStore.edit { preferences ->
                preferences[CHAT_ID_KEY] = chatId
            }
        }

        override suspend fun clearConfig() {
            Timber.d("Clearing Telegram configuration")
            context.telegramConfigDataStore.edit { preferences ->
                preferences.remove(BOT_TOKEN_KEY)
                preferences.remove(CHAT_ID_KEY)
            }
        }

        override suspend fun hasValidConfig(): Boolean {
            val botToken = botToken.first() ?: return false
            val chatId = chatId.first() ?: return false
            return validateConfig(AlertMediumConfig.TelegramConfig(botToken, chatId)).isValid
        }

        override suspend fun validateConfig(config: AlertMediumConfig): ConfigValidationResult {
            val (botToken, chatId) =
                when (config) {
                    is AlertMediumConfig.TelegramConfig -> Pair(config.botToken, config.chatId)
                    else -> return ConfigValidationResult(false, emptyMap())
                }

            val errors = mutableMapOf<String, String>()

            // Bot token format: 123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11
            val isValidBotToken = botToken.matches(Regex("""\d+:[A-Za-z0-9_-]{35}"""))
            if (!isValidBotToken) {
                Timber.w("Invalid bot token format")
                errors[ValidationKeys.BOT_TOKEN] = "Invalid bot token format. Example format: 123456:ABCDEF1234ghIklzyx57W2v1u123ew11"
            }

            // Chat ID can be numeric or @channelusername
            val isValidChatId =
                chatId.matches(Regex("""^-?\d+$""")) ||
                    (chatId.startsWith("@") && chatId.length > 1)
            if (!isValidChatId) {
                Timber.w("Invalid chat ID format")
                errors[ValidationKeys.CHAT_ID] = "Invalid chat ID format. Chat ID should be numeric or @channelusername"
            }

            Timber.i("Telegram config is valid")
            return ConfigValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
            )
        }
    }
