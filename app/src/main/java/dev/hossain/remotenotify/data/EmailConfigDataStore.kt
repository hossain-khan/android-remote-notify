package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.notifier.mailgun.MailgunConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.emailConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "email_config")

@SingleIn(AppScope::class)
@Inject
class EmailConfigDataStore
    constructor(
        private val context: Context,
    ) : AlertMediumConfigStore {
        companion object {
            private val TO_EMAIL = stringPreferencesKey("to_email")

            object ValidationKeys {
                const val TO_EMAIL = "toEmail"
            }
        }

        val toEmail: Flow<String?> =
            context.emailConfigDataStore.data
                .map { it[TO_EMAIL] }

        suspend fun getConfig(): AlertMediumConfig.EmailConfig =
            AlertMediumConfig.EmailConfig(
                apiKey = MailgunConfig.API_KEY,
                domain = MailgunConfig.DOMAIN,
                fromEmail = MailgunConfig.FROM_EMAIL,
                toEmail = toEmail.first() ?: "",
            )

        suspend fun saveConfig(config: AlertMediumConfig.EmailConfig) {
            Timber.d("Saving email configuration")
            context.emailConfigDataStore.edit { preferences ->
                preferences[TO_EMAIL] = config.toEmail
            }
            Timber.i("Email config saved successfully")
        }

        override suspend fun clearConfig() {
            Timber.d("Clearing email configuration")
            context.emailConfigDataStore.edit { preferences ->
                preferences.clear()
            }
            Timber.i("Email configuration cleared")
        }

        override suspend fun hasValidConfig(): Boolean {
            val toEmail = toEmail.first() ?: return false
            val isValid =
                validateConfig(
                    AlertMediumConfig.EmailConfig(
                        apiKey = MailgunConfig.API_KEY,
                        domain = MailgunConfig.DOMAIN,
                        fromEmail = MailgunConfig.FROM_EMAIL,
                        toEmail = toEmail,
                    ),
                ).isValid
            Timber.d("Email config validation: isValid=$isValid")
            return isValid
        }

        override suspend fun validateConfig(config: AlertMediumConfig): ConfigValidationResult {
            val emailConfig =
                config as? AlertMediumConfig.EmailConfig
                    ?: return ConfigValidationResult(false, emptyMap())

            val errors = mutableMapOf<String, String>()
            val emailRegex = "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"

            if (!emailConfig.toEmail.matches(emailRegex.toRegex())) {
                errors[ValidationKeys.TO_EMAIL] = "Invalid email address"
            }

            return ConfigValidationResult(errors.isEmpty(), errors)
        }
    }
