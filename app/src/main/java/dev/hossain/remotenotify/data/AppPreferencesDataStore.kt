package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
        }

        suspend fun saveWorkerInterval(intervalMinutes: Long) {
            context.appPreferencesDataStore.edit { preferences ->
                preferences[WORKER_INTERVAL_KEY] = intervalMinutes
            }
        }

        val workerIntervalFlow: Flow<Long> =
            context.appPreferencesDataStore.data
                .map { preferences ->
                    preferences[WORKER_INTERVAL_KEY] ?: DEFAULT_PERIODIC_INTERVAL_MINUTES
                }
    }
