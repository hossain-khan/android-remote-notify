package dev.hossain.remotenotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DeviceAlertJsonPayload(
    @SerialName("alertType")
    val alertType: AlertType,
    @SerialName("deviceModel")
    val deviceModel: String = android.os.Build.MODEL,
    @SerialName("androidVersion")
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    @SerialName("batteryLevel")
    val batteryLevel: Int? = null,
    @SerialName("availableStorageGb")
    val availableStorageGb: Double? = null,
    @SerialName("isoDateTime")
    val isoDateTime: String,
) {
    internal fun toJson(): String = Json.encodeToString(this)
}
