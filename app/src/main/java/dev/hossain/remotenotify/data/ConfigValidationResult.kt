package dev.hossain.remotenotify.data

data class ConfigValidationResult(
    /**
     * If the [AlertMediumConfig] is valid.
     *
     * @see AlertMediumConfigStore.validateConfig
     */
    val isValid: Boolean,
    /**
     * Map of error key and error message.
     *
     * @see WebhookConfigDataStore.ValidationKeys
     * @see TelegramConfigDataStore.ValidationKeys
     */
    val errors: Map<String, String> = emptyMap(),
)
