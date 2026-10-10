package com.leninasto.ruleapp

import android.app.DownloadManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.leninasto.ruleapp.data.local.*
import com.leninasto.ruleapp.data.remote.NetworkRepository
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class DownloadTest {
    @Test fun shimmieOriginalCompletesThroughDownloadManager() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        val manager = context.getSystemService(DownloadManager::class.java)
        var id: Long? = null
        try {
            val apiPost = NetworkRepository({ null }).post(Source.SHIMMIE, 6605)!!
            // A unique QA filename avoids replacing an owner's existing download.
            val post = apiPost.copy(key = PostKey("SHIMMIE_QA_${System.currentTimeMillis()}", apiPost.key.postId))
            assertNotNull(post.originalUrl)
            val downloads = Downloads(context, db.library())
            id = downloads.enqueue(post)
            withTimeout(60_000) {
                var finished = false
                while (!finished) {
                    val state = downloads.statuses(listOf(db.library().download(post.key.stableId)!!)).single()
                    assertNotEquals("Falló", state.label)
                    finished = state.label == "Completada"
                    if (!finished) delay(250)
                }
            }
            context.getExternalFilesDir(null)!!.resolve("download-validation.txt").writeText("Shimmie: original autorizado descargado con DownloadManager; archivo de prueba retirado. API ${android.os.Build.VERSION.SDK_INT}.\n")
        } finally { id?.let { manager.remove(it) }; db.close() }
    }
}
