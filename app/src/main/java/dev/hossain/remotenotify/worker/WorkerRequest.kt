package dev.hossain.remotenotify.worker

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest

fun sendOneTimeWorkRequest(context: Context) {
    val workRequest: WorkRequest =
        OneTimeWorkRequestBuilder<ObserveDeviceHealthWorker>()
            .addTag("onetime-test-request")
            .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}
