package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hossain.remotenotify.di.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

private val Context.telegramConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "telegram_config")

class TelegramConfigDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlertMediumConfigStore {
        companion object {
            private val BOT_TOKEN_KEY = stringPreferencesKey("bot_token")
            private val CHAT_ID_KEY = stringPreferencesKey("chat_id")
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
    }
