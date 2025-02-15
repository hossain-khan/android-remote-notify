package dev.hossain.remotenotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON Payload that is sent for webhook request.
 *
 * Sample Payload:
 * ```json
 * {
 *   "alertType": "BATTERY",
 *   "deviceModel": "Pixel 7",
 *   "androidVersion": "12",
 *   "batteryLevel": 5,
 *   "isoDateTime": "2025-02-15T17:12:25.982"
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
    @SerialName("availableStorageGb")
    val availableStorageGb: Double? = null,
    @SerialName("isoDateTime")
    val isoDateTime: String,
) {
    internal fun toJson(): String = Json.encodeToString(this)
}
