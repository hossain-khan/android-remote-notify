package dev.hossain.remotenotify.monitor

import android.os.Environment
import android.os.StatFs

class StorageMonitor {
    fun getAvailableStorageInGB(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        return bytesAvailable / (1024 * 1024 * 1024) // Convert to GB
    }

    fun getTotalStorageInGB(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesTotal = stat.blockSizeLong * stat.blockCountLong
        return bytesTotal / (1024 * 1024 * 1024) // Convert to GB
    }
}
