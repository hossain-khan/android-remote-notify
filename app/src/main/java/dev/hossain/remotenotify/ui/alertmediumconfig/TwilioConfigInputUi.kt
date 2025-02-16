package dev.hossain.remotenotify.ui.alertmediumconfig

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.TwilioConfigDataStore.Companion.ValidationKeys
import dev.hossain.remotenotify.model.AlertMediumConfig

@Composable
internal fun TwilioConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    configValidationResult: ConfigValidationResult,
    shouldShowValidationError: Boolean,
    onConfigUpdate: (AlertMediumConfig?) -> Unit,
) {
    val config = alertMediumConfig as? AlertMediumConfig.TwilioConfig
    val errors = configValidationResult.errors

    Column {
        TextField(
            value = config?.accountSid.orEmpty(),
            onValueChange = { onConfigUpdate(config?.copy(accountSid = it)) },
            label = { Text("Account SID") },
            modifier = Modifier.fillMaxWidth(),
            isError = shouldShowValidationError && errors[ValidationKeys.ACCOUNT_SID] != null,
            supportingText = {
                if (shouldShowValidationError && errors[ValidationKeys.ACCOUNT_SID] != null) {
                    Text(errors[ValidationKeys.ACCOUNT_SID]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Enter your Twilio Account SID")
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = config?.authToken.orEmpty(),
            onValueChange = { onConfigUpdate(config?.copy(authToken = it)) },
            label = { Text("Auth Token") },
            modifier = Modifier.fillMaxWidth(),
            isError = shouldShowValidationError && errors[ValidationKeys.AUTH_TOKEN] != null,
            supportingText = {
                if (shouldShowValidationError && errors[ValidationKeys.AUTH_TOKEN] != null) {
                    Text(errors[ValidationKeys.AUTH_TOKEN]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Enter your Twilio Auth Token")
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = config?.fromPhone.orEmpty(),
            onValueChange = { onConfigUpdate(config?.copy(fromPhone = it)) },
            label = { Text("From Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            isError = shouldShowValidationError && errors[ValidationKeys.FROM_PHONE] != null,
            supportingText = {
                if (shouldShowValidationError && errors[ValidationKeys.FROM_PHONE] != null) {
                    Text(errors[ValidationKeys.FROM_PHONE]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Enter the phone number in E.164 format (e.g., +1234567890)")
                }
            },
        )
    }
}
