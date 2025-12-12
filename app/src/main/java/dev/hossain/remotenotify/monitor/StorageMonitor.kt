package dev.hossain.remotenotify.monitor

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.core.content.ContextCompat
import dev.zacsweers.metro.Inject
import timber.log.Timber
import java.util.UUID

@Inject
class StorageMonitor(
    private val context: Context,
) {
    fun getAvailableStorageInGBStatFs(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val gbAvailable = bytesAvailable / (1024 * 1024 * 1024)
        Timber.d("Storage check (StatFs): ${gbAvailable}GB available")
        return gbAvailable // Convert to GB
    }

    fun getTotalStorageInGBStatFs(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesTotal = stat.blockSizeLong * stat.blockCountLong
        val gbTotal = bytesTotal / (1024 * 1024 * 1024)
        Timber.d("Storage check (StatFs): ${gbTotal}GB total")
        return gbTotal // Convert to GB
    }

    fun getAvailableStorageInGB(): Long {
        val storageStatsManager = ContextCompat.getSystemService(context, StorageStatsManager::class.java)!!
        val storageManager = ContextCompat.getSystemService(context, StorageManager::class.java)!!
        val storageVolume = storageManager.primaryStorageVolume
        val uuid = storageVolume.uuid?.let { UUID.fromString(it) } ?: StorageManager.UUID_DEFAULT
        val bytesAvailable = storageStatsManager.getFreeBytes(uuid)
        val gbAvailable = bytesAvailable / (1024 * 1024 * 1024)
        Timber.d("Storage check: ${gbAvailable}GB available")
        return gbAvailable // Convert to GB
    }

    fun getTotalStorageInGB(): Long {
        val storageStatsManager = ContextCompat.getSystemService(context, StorageStatsManager::class.java)!!
        val storageManager = ContextCompat.getSystemService(context, StorageManager::class.java)!!
        val storageVolume = storageManager.primaryStorageVolume
        val uuid = storageVolume.uuid?.let { UUID.fromString(it) } ?: StorageManager.UUID_DEFAULT
        val bytesTotal = storageStatsManager.getTotalBytes(uuid)
        val gbTotal = bytesTotal / (1024 * 1024 * 1024)
        Timber.d("Storage check: ${gbTotal}GB total")
        return gbTotal // Convert to GB
    }
}
