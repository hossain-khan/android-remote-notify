package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.anvil.annotations.optional.SingleIn
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.di.ApplicationContext
import dev.hossain.remotenotify.worker.DEFAULT_PERIODIC_INTERVAL_MINUTES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@SingleIn(AppScope::class)
class AppPreferencesDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private val WORKER_INTERVAL_KEY = longPreferencesKey("worker_interval_minutes")
            private val LAST_REVIEW_REQUEST_KEY = longPreferencesKey("last_review_request_time")
            private val FIRST_TIME_DIALOG_SHOWN = booleanPreferencesKey("first_time_dialog_shown")
        }

        suspend fun saveWorkerInterval(intervalMinutes: Long) {
            context.appPreferencesDataStore.edit { preferences ->
                preferences[WORKER_INTERVAL_KEY] = intervalMinutes
            }
        }

        suspend fun saveLastReviewRequestTime(timestamp: Long) {
            context.appPreferencesDataStore.edit { preferences ->
                preferences[LAST_REVIEW_REQUEST_KEY] = timestamp
            }
        }

        val workerIntervalFlow: Flow<Long> =
            context.appPreferencesDataStore.data
                .map { preferences ->
                    preferences[WORKER_INTERVAL_KEY] ?: DEFAULT_PERIODIC_INTERVAL_MINUTES
                }

        val lastReviewRequestFlow: Flow<Long> =
            context.appPreferencesDataStore.data
                .map { preferences ->
                    preferences[LAST_REVIEW_REQUEST_KEY] ?: 0L
                }

        val isFirstTimeDialogShown: Flow<Boolean> =
            context.appPreferencesDataStore.data
                .map { preferences ->
                    preferences[FIRST_TIME_DIALOG_SHOWN] ?: false
                }

        suspend fun markFirstTimeDialogShown() {
            context.appPreferencesDataStore.edit { preferences ->
                preferences[FIRST_TIME_DIALOG_SHOWN] = true
            }
        }
    }
