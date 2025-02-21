package dev.hossain.remotenotify.model

data class WorkerStatus(
    val state: String,
    val nextRunTimeMs: Long?,
    val lastRunTimeMs: Long?,
)
