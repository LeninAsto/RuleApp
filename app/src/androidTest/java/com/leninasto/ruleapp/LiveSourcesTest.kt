package com.leninasto.ruleapp

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leninasto.ruleapp.data.remote.NetworkRepository
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real read-only probes. Rule34 is deliberately validated from Settings with personal credentials. */
@RunWith(AndroidJUnit4::class)
class LiveSourcesTest {
    @Test fun publicTlsCompatibilityDiagnostic() {
        listOf("https://shimmie.shishnet.org/api/danbooru/post/index.xml?limit=1", "https://rule34.paheal.net/api/danbooru/post/index.xml?limit=1").forEach { url ->
            try { com.leninasto.ruleapp.data.remote.apiClient().newCall(okhttp3.Request.Builder().url(url).build()).execute().use { record("TLS API ${android.os.Build.VERSION.SDK_INT}: ${java.net.URI(url).host} HTTP ${it.code}") } }
            catch (error: java.io.IOException) { record("TLS API ${android.os.Build.VERSION.SDK_INT}: ${java.net.URI(url).host} ${error.javaClass.simpleName}: ${error.message}; ${error.cause?.javaClass?.simpleName}: ${error.cause?.message}") }
        }
    }
    private val repository = NetworkRepository({ null })
    @Test fun pahealListAndDetail() = runBlocking {
        val page = repository.posts(SearchSpec(Source.PAHEAL), PageKey.Number(1), 1)
        assertTrue(page.items.isNotEmpty())
        val item = page.items.first()
        assertEquals("PAHEAL", item.key.siteId)
        assertEquals(item.key, repository.post(Source.PAHEAL, item.key.postId)?.key)
        record("Paheal: lista y detalle verificados con API real.")
    }
    @Test fun shimmieListAndDetail() = runBlocking {
        val page = repository.posts(SearchSpec(Source.SHIMMIE), PageKey.Number(1), 2)
        assertTrue(page.items.isNotEmpty())
        val item = page.items.first()
        assertTrue(item.previewUrl!!.startsWith("https://")); assertTrue(item.originalUrl!!.startsWith("https://"))
        val detail = repository.post(Source.SHIMMIE, item.key.postId)
        assertEquals(item.key, detail?.key)
        record("Shimmie: lista y detalle verificados con API real.")
    }
    @Test fun recordDanbooruAccessWithoutBypassingRestrictions() = runBlocking {
        try {
            val result = repository.posts(SearchSpec(Source.DANBOORU, "rating:g"), PageKey.Number(1), 1)
            assertTrue(result.items.isNotEmpty())
            record("Danbooru: lista pública verificada con API real.")
        } catch (error: BooruException) {
            record("Danbooru: conexión no validada, ${error.kind}. ${error.userMessage()}")
            assertTrue(error.kind in setOf(ErrorKind.DENIED, ErrorKind.CONNECTION, ErrorKind.SERVER, ErrorKind.RATE_LIMIT))
        }
    }
    private fun record(line: String) {
        ApplicationProvider.getApplicationContext<android.content.Context>().getExternalFilesDir(null)!!.resolve("live-validation.txt").appendText(line + "\n")
    }
}
