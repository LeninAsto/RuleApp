package com.leninasto.ruleapp.data.local
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class DownloadStatus(val id: Long, val name: String, val label: String, val progress: Int?, val active: Boolean,
    val bytes: Long = 0, val total: Long? = null, val complete: Boolean = false, val failed: Boolean = false)
class Downloads(context: Context, private val dao: LibraryDao) {
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val mutex = Mutex()
    suspend fun enqueue(post: BooruPost): Long = mutex.withLock { withContext(Dispatchers.IO) {
        val url = post.originalUrl?.let { resolveHttps(it, it) } ?: throw BooruException(ErrorKind.UNSUPPORTED)
        dao.download(post.key.stableId)?.let { previous ->
            status(previous)?.takeIf { it.active || (it.complete && manager.getUriForDownloadedFile(previous.downloadId) != null) }?.let { return@withContext previous.downloadId }
        }
        val ext = post.extension?.takeIf { it.matches(Regex("[a-zA-Z0-9]{1,8}")) } ?: "bin"
        val name = "${post.key.siteId.lowercase()}_${post.key.postId}.$ext"
        val request = DownloadManager.Request(Uri.parse(url)).setTitle(name)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Daparushi/$name")
            .setAllowedOverRoaming(false)
        val id = manager.enqueue(request)
        dao.recordDownload(DownloadEntity(post.key.stableId, id, name)); id
    } }
    suspend fun statuses(rows: List<DownloadEntity>): List<DownloadStatus> = withContext(Dispatchers.IO) { rows.mapNotNull { status(it) } }
    suspend fun remove(id: Long) = withContext(Dispatchers.IO) { manager.remove(id); dao.removeDownload(id) }
    suspend fun retry(id: Long) = mutex.withLock { withContext(Dispatchers.IO) {
        val row = dao.downloadById(id) ?: return@withContext
        if (status(row)?.failed != true) return@withContext
        val url = manager.query(DownloadManager.Query().setFilterById(id))?.use { c ->
            if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)) else null
        }?.let { resolveHttps(it, it) } ?: throw BooruException(ErrorKind.UNSUPPORTED)
        manager.remove(id)
        val replacement = manager.enqueue(DownloadManager.Request(Uri.parse(url)).setTitle(row.fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Daparushi/${row.fileName}").setAllowedOverRoaming(false))
        dao.recordDownload(row.copy(downloadId = replacement))
    } }
    fun openIntent(id: Long): android.content.Intent? = manager.getUriForDownloadedFile(id)?.let { uri ->
        android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri, manager.getMimeTypeForDownloadedFile(id) ?: "application/octet-stream")
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    private fun status(row: DownloadEntity): DownloadStatus? = manager.query(DownloadManager.Query().setFilterById(row.downloadId))?.use { cursor ->
        if (!cursor.moveToFirst()) return@use DownloadStatus(row.downloadId, row.fileName, t(R.string.ui_record_unavailable), null, false)
        val state = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        DownloadStatus(row.downloadId, row.fileName, when (state) {
            DownloadManager.STATUS_SUCCESSFUL -> t(R.string.ui_completed_377783); DownloadManager.STATUS_FAILED -> t(R.string.ui_failed_125216)
            DownloadManager.STATUS_PAUSED -> t(R.string.ui_waiting_for_network_or_retry); DownloadManager.STATUS_RUNNING -> t(R.string.ui_downloading); else -> t(R.string.ui_queued)
        }, if (total > 0) ((bytes * 100) / total).toInt() else null,
            state in setOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED),
            bytes.coerceAtLeast(0), total.takeIf { it > 0 }, state == DownloadManager.STATUS_SUCCESSFUL, state == DownloadManager.STATUS_FAILED)
    }
}
