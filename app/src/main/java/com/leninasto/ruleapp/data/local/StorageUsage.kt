package com.leninasto.ruleapp.data.local

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StorageUsage(val total: Long, val available: Long, val appData: Long, val cache: Long, val trackedDownloads: Long) {
    val used get() = (total - available).coerceAtLeast(0)
}
object StorageMeter {
    suspend fun read(context: Context, downloads: List<DownloadStatus>): StorageUsage = withContext(Dispatchers.IO) {
        val volume = StatFs(Environment.getDataDirectory().absolutePath)
        fun size(file: File): Long = if (!file.exists()) 0 else file.walkTopDown().filter(File::isFile).sumOf { it.length() }
        val cache = size(context.cacheDir) + size(context.codeCacheDir)
        // Files, databases and preferences only; no unrestricted scan of public storage.
        val data = size(context.filesDir) + size(File(context.applicationInfo.dataDir, "databases")) + size(File(context.applicationInfo.dataDir, "shared_prefs"))
        StorageUsage(volume.totalBytes, volume.availableBytes, data, cache, downloads.sumOf { it.bytes })
    }
}
