package com.leninasto.ruleapp.data.local

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class DownloadStatus(val id: Long, val name: String, val label: String, val progress: Int?, val active: Boolean)
class Downloads(context: Context, private val dao: LibraryDao) {
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val mutex = Mutex()
    suspend fun enqueue(post: BooruPost): Long = mutex.withLock { withContext(Dispatchers.IO) {
        val url = post.originalUrl?.let { resolveHttps(it, it) } ?: throw BooruException(ErrorKind.UNSUPPORTED)
        dao.download(post.key.stableId)?.let { previous ->
            status(previous)?.takeIf { it.active }?.let { return@withContext previous.downloadId }
        }
        val ext = post.extension?.takeIf { it.matches(Regex("[a-zA-Z0-9]{1,8}")) } ?: "bin"
        val name = "${post.key.siteId.lowercase()}_${post.key.postId}.$ext"
        val request = DownloadManager.Request(Uri.parse(url)).setTitle(name)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "RuleApp/$name")
            .setAllowedOverRoaming(false)
        val id = manager.enqueue(request)
        dao.recordDownload(DownloadEntity(post.key.stableId, id, name)); id
    } }
    suspend fun statuses(rows: List<DownloadEntity>): List<DownloadStatus> = withContext(Dispatchers.IO) { rows.mapNotNull { status(it) } }
    private fun status(row: DownloadEntity): DownloadStatus? = manager.query(DownloadManager.Query().setFilterById(row.downloadId))?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val state = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        DownloadStatus(row.downloadId, row.fileName, when (state) {
            DownloadManager.STATUS_SUCCESSFUL -> "Completada"; DownloadManager.STATUS_FAILED -> "Falló"
            DownloadManager.STATUS_PAUSED -> "En pausa"; DownloadManager.STATUS_RUNNING -> "Descargando"; else -> "En cola"
        }, if (total > 0) ((bytes * 100) / total).toInt() else null,
            state in setOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED))
    }
}
