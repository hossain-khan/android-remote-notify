package dev.hossain.remotenotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON Payload that is sent for webhook request.
 *
 * Sample Payload - Battery Alert:
 * ```json
 * {
 *   "alertType": "BATTERY",
 *   "deviceModel": "Google Pixel 7",
 *   "androidVersion": "12",
 *   "batteryLevel": 8,
 *   "batteryThresholdPercent": 15,
 *   "isoDateTime": "2025-02-15T17:12:25.982"
 * }
 * ```
 *
 * Sample Payload - Storage Alert:
 * ```json
 * {
 *   "alertType": "STORAGE",
 *   "deviceModel": "Samsung SM-S911W",
 *   "androidVersion": "14",
 *   "availableStorageGb": 4,
 *   "storageThresholdGb": 10,
 *   "isoDateTime": "2025-02-19T19:05:28.837761"
 * }
 * ```
 */
@Serializable
data class DeviceAlertJsonPayload constructor(
    @SerialName("alertType")
    val alertType: AlertType,
    @SerialName("deviceModel")
    val deviceModel: String,
    @SerialName("androidVersion")
    val androidVersion: String,
    @SerialName("batteryLevel")
    val batteryLevel: Int? = null,
    @SerialName("batteryThresholdPercent")
    val batteryThresholdPercent: Int? = null,
    @SerialName("availableStorageGb")
    val availableStorageGb: Double? = null,
    @SerialName("storageThresholdGb")
    val storageThresholdGb: Double? = null,
    @SerialName("isoDateTime")
    val isoDateTime: String,
) {
    internal fun toJson(): String = Json.encodeToString(this)
}
