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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

private val Context.twilioConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "twilio_config")

@SingleIn(AppScope::class)
class TwilioConfigDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlertMediumConfigStore {
        companion object {
            private val ACCOUNT_SID_KEY = stringPreferencesKey("twilio_account_sid")
            private val AUTH_TOKEN_KEY = stringPreferencesKey("twilio_auth_token")
            private val FROM_PHONE_KEY = stringPreferencesKey("twilio_from_phone")

            object ValidationKeys {
                const val ACCOUNT_SID = "accountSid"
                const val AUTH_TOKEN = "authToken"
                const val FROM_PHONE = "fromPhone"
            }
        }

        val accountSid: Flow<String?> = context.twilioConfigDataStore.data.map { it[ACCOUNT_SID_KEY] }
        val authToken: Flow<String?> = context.twilioConfigDataStore.data.map { it[AUTH_TOKEN_KEY] }
        val fromPhone: Flow<String?> = context.twilioConfigDataStore.data.map { it[FROM_PHONE_KEY] }

        suspend fun getConfig(): AlertMediumConfig.TwilioConfig {
            val sid = accountSid.first().orEmpty()
            val token = authToken.first().orEmpty()
            val phone = fromPhone.first().orEmpty()
            return AlertMediumConfig.TwilioConfig(sid, token, phone)
        }

        suspend fun saveConfig(config: AlertMediumConfig.TwilioConfig) {
            Timber.d("Saving Twilio config...")
            context.twilioConfigDataStore.edit {
                it[ACCOUNT_SID_KEY] = config.accountSid
                it[AUTH_TOKEN_KEY] = config.authToken
                it[FROM_PHONE_KEY] = config.fromPhone
            }
        }

        override suspend fun clearConfig() {
            Timber.d("Clearing Twilio configuration")
            context.twilioConfigDataStore.edit {
                it.remove(ACCOUNT_SID_KEY)
                it.remove(AUTH_TOKEN_KEY)
                it.remove(FROM_PHONE_KEY)
            }
        }

        override suspend fun hasValidConfig(): Boolean {
            val config = getConfig()
            return validateConfig(config).isValid
        }

        override suspend fun validateConfig(config: AlertMediumConfig): ConfigValidationResult {
            val errors = mutableMapOf<String, String>()
            if (config is AlertMediumConfig.TwilioConfig) {
                if (!config.accountSid.matches(Regex("""^AC[a-zA-Z0-9]{32}$"""))) {
                    errors[ValidationKeys.ACCOUNT_SID] = "Invalid Account SID format"
                }
                if (config.authToken.length != 32) {
                    errors[ValidationKeys.AUTH_TOKEN] = "Auth Token should be 32 characters"
                }
                if (!config.fromPhone.matches(Regex("""^\+\d{10,}$"""))) {
                    errors[ValidationKeys.FROM_PHONE] = "Phone number must be in E.164 format (e.g., +1234567890)"
                }
            }
            return ConfigValidationResult(isValid = errors.isEmpty(), errors = errors)
        }
    }
