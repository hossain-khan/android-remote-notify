package dev.hossain.remotenotify.ui.alertmediumconfig

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.TwilioConfigDataStore.Companion.ValidationKeys
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.theme.ComposeAppTheme

@Composable
internal fun TwilioConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    configValidationResult: ConfigValidationResult,
    shouldShowValidationError: Boolean,
    onConfigUpdate: (AlertMediumConfig?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = alertMediumConfig as? AlertMediumConfig.TwilioConfig
    val errors = configValidationResult.errors
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier) {
        Text(
            text =
                buildAnnotatedString {
                    append("Get your Twilio credentials from ")
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = "https://console.twilio.com",
                    )
                    withStyle(
                        style =
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                            ),
                    ) {
                        append("Twilio Console")
                    }
                    append(". You'll need an Account SID, Auth Token, and a Twilio phone number.")
                },
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.clickable {
                    uriHandler.openUri("https://console.twilio.com")
                },
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = config?.accountSid.orEmpty(),
            onValueChange = { onConfigUpdate(config?.copy(accountSid = it)) },
            label = { Text("Account SID") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
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
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
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
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
            isError = shouldShowValidationError && errors[ValidationKeys.FROM_PHONE] != null,
            supportingText = {
                if (shouldShowValidationError && errors[ValidationKeys.FROM_PHONE] != null) {
                    Text(errors[ValidationKeys.FROM_PHONE]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Your Twilio phone number in E.164 format")
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = config?.toPhone.orEmpty(),
            onValueChange = { onConfigUpdate(config?.copy(toPhone = it)) },
            label = { Text("To Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                ),
            isError = shouldShowValidationError && errors[ValidationKeys.TO_PHONE] != null,
            supportingText = {
                if (shouldShowValidationError && errors[ValidationKeys.TO_PHONE] != null) {
                    Text(errors[ValidationKeys.TO_PHONE]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Destination phone number in E.164 format")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTwilioConfigInputUi() {
    ComposeAppTheme {
        TwilioConfigInputUi(
            alertMediumConfig =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "AC1234567890",
                    authToken = "auth123456",
                    fromPhone = "+12025550123",
                    toPhone = "+12025550124",
                ),
            configValidationResult =
                ConfigValidationResult(
                    isValid = false,
                    errors = mapOf(ValidationKeys.FROM_PHONE to "Invalid phone number format"),
                ),
            shouldShowValidationError = true,
            onConfigUpdate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTwilioConfigInputUiEmpty() {
    ComposeAppTheme {
        TwilioConfigInputUi(
            alertMediumConfig = null,
            configValidationResult = ConfigValidationResult(true, emptyMap()),
            shouldShowValidationError = false,
            onConfigUpdate = {},
        )
    }
}
