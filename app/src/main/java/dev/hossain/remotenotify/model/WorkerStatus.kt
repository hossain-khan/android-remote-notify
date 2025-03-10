package dev.hossain.remotenotify.model

/**
 * Data class representing the status of a worker.
 *
 * @property state The current state of the worker.
 * @property nextRunTimeMs The next scheduled run time in milliseconds, or null if not scheduled.
 * @property lastRunTimeMs The last run time in milliseconds, or `null` or `0L` if never run.
 */
data class WorkerStatus(
    val state: String,
    val nextRunTimeMs: Long?,
    val lastRunTimeMs: Long?,
)
