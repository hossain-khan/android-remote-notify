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
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "telegram_config")

class TelegramConfigDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private val BOT_TOKEN_KEY = stringPreferencesKey("bot_token")
            private val CHAT_ID_KEY = stringPreferencesKey("chat_id")
        }

        val botToken: Flow<String?> =
            context.dataStore.data
                .map { preferences ->
                    preferences[BOT_TOKEN_KEY]
                }

        val chatId: Flow<String?> =
            context.dataStore.data
                .map { preferences ->
                    preferences[CHAT_ID_KEY]
                }

        suspend fun saveBotToken(botToken: String) {
            context.dataStore.edit { preferences ->
                preferences[BOT_TOKEN_KEY] = botToken
            }
        }

        suspend fun saveChatId(chatId: String) {
            context.dataStore.edit { preferences ->
                preferences[CHAT_ID_KEY] = chatId
            }
        }
    }
