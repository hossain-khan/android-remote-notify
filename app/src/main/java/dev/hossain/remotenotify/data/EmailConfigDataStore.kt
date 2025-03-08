package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.anvil.annotations.optional.SingleIn
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.di.ApplicationContext
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.notifier.mailgun.MailgunConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.emailConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "email_config")

@SingleIn(AppScope::class)
class EmailConfigDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
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
            context.emailConfigDataStore.edit { preferences ->
                preferences[TO_EMAIL] = config.toEmail
            }
        }

        override suspend fun clearConfig() {
            context.emailConfigDataStore.edit { preferences ->
                preferences.clear()
            }
        }

        override suspend fun hasValidConfig(): Boolean {
            val toEmail = toEmail.first() ?: return false
            return validateConfig(
                AlertMediumConfig.EmailConfig(
                    apiKey = MailgunConfig.API_KEY,
                    domain = MailgunConfig.DOMAIN,
                    fromEmail = MailgunConfig.FROM_EMAIL,
                    toEmail = toEmail,
                ),
            ).isValid
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
