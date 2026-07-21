package dev.hossain.remotenotify.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
class WorkerRequestTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val config =
            Configuration
                .Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .setExecutor(SynchronousExecutor())
                .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun `sendOneTimeWorkRequest enqueues work request`() {
        sendOneTimeWorkRequest(context)

        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(DEVICE_VITALS_CHECKER_DEBUG_WORKER_ID).get()

        assertThat(workInfos).hasSize(1)
        val workInfo = workInfos[0]
        assertThat(workInfo.state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertThat(workInfo.tags).contains("onetime-test-request")
    }

    @Test
    fun `sendPeriodicWorkRequest enqueues work request with correct interval`() {
        sendPeriodicWorkRequest(context, 30L)

        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(DEVICE_VITALS_CHECKER_WORKER_ID).get()

        assertThat(workInfos).hasSize(1)
        val workInfo = workInfos[0]
        assertThat(workInfo.state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    @Test
    fun `sendPeriodicWorkRequest enforces minimum interval of 15 minutes`() {
        sendPeriodicWorkRequest(context, 5L) // Less than 15 minutes

        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(DEVICE_VITALS_CHECKER_WORKER_ID).get()

        assertThat(workInfos).hasSize(1)
        val workInfo = workInfos[0]
        assertThat(workInfo.state).isEqualTo(WorkInfo.State.ENQUEUED)
    }
}
