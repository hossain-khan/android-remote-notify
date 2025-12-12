package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hossain.remotenotify.data.EmailQuotaManager.Companion.ValidationKeys.EMAIL_DAILY_QUOTA
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant

private val Context.emailQuotaDataStore: DataStore<Preferences> by preferencesDataStore(name = "email_quota")

@SingleIn(AppScope::class)
@Inject
class EmailQuotaManager
    constructor(
        private val context: Context,
    ) {
        companion object {
            private val LAST_EMAIL_DATE = longPreferencesKey("last_email_date")
            private val EMAIL_COUNT_TODAY = intPreferencesKey("email_count_today")
            private const val MAX_EMAILS_PER_DAY = 2 // Limit to 2 emails per day

            object ValidationKeys {
                const val EMAIL_DAILY_QUOTA = "email_quota"
            }
        }

        private val lastEmailDate =
            context.emailQuotaDataStore.data
                .map { it[LAST_EMAIL_DATE] ?: 0L }

        private val emailCountToday =
            context.emailQuotaDataStore.data
                .map { it[EMAIL_COUNT_TODAY] ?: 0 }

        suspend fun canSendEmail(): Boolean {
            val lastDate = Instant.ofEpochMilli(lastEmailDate.first())
            val today = Instant.now()
            val count = emailCountToday.first()

            // Reset counter if it's a new day
            if (!isSameDay(lastDate, today)) {
                Timber.d("New day detected, resetting email quota")
                resetQuota()
                return true
            }

            val canSend = count < MAX_EMAILS_PER_DAY
            Timber.d("Email quota check: $count/$MAX_EMAILS_PER_DAY sent today, canSend=$canSend")
            return canSend
        }

        suspend fun validateQuota(): ConfigValidationResult =
            if (canSendEmail().not()) {
                ConfigValidationResult(
                    isValid = false,
                    errors =
                        mapOf(
                            EMAIL_DAILY_QUOTA to
                                "Unfortunately, the email notification has limited quota that has been exceeded. Please try again tomorrow.",
                        ),
                )
            } else {
                ConfigValidationResult(isValid = true)
            }

        suspend fun recordEmailSent() {
            context.emailQuotaDataStore.edit { preferences ->
                val now = Instant.now()
                val lastDate = Instant.ofEpochMilli(preferences[LAST_EMAIL_DATE] ?: 0L)

                if (!isSameDay(lastDate, now)) {
                    // New day, reset counter
                    preferences[EMAIL_COUNT_TODAY] = 1
                    Timber.d("Email sent: Starting new day count (1/$MAX_EMAILS_PER_DAY)")
                } else {
                    // Increment counter
                    val currentCount = preferences[EMAIL_COUNT_TODAY] ?: 0
                    preferences[EMAIL_COUNT_TODAY] = currentCount + 1
                    Timber.d("Email sent: Updated count (${currentCount + 1}/$MAX_EMAILS_PER_DAY)")
                }
                preferences[LAST_EMAIL_DATE] = now.toEpochMilli()
            }
        }

        suspend fun resetQuota() {
            Timber.d("Resetting email quota")
            context.emailQuotaDataStore.edit { preferences ->
                preferences[EMAIL_COUNT_TODAY] = 0
                preferences[LAST_EMAIL_DATE] = Instant.now().toEpochMilli()
            }
        }

        private fun isSameDay(
            date1: Instant,
            date2: Instant,
        ): Boolean = date1.epochSecond / 86400 == date2.epochSecond / 86400

        suspend fun getRemainingQuota(): Int {
            val lastDate = Instant.ofEpochMilli(lastEmailDate.first())
            val today = Instant.now()
            val count = emailCountToday.first()

            return if (!isSameDay(lastDate, today)) {
                MAX_EMAILS_PER_DAY
            } else {
                (MAX_EMAILS_PER_DAY - count).coerceAtLeast(0)
            }
        }
    }
