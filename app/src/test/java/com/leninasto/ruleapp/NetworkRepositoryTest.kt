package com.leninasto.ruleapp

import com.leninasto.ruleapp.data.remote.*
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*

class NetworkRepositoryTest {
    @Test fun readTimeoutIsClassifiedAndRetriedOnlyTwice() = runBlocking {
        repeat(3) { server.enqueue(MockResponse().setBody("[]").setBodyDelay(1, java.util.concurrent.TimeUnit.SECONDS)) }
        val fast = NetworkRepository({ Credentials("1", "synthetic-key") }, client = apiClient().newBuilder().readTimeout(50, java.util.concurrent.TimeUnit.MILLISECONDS).build(), bases = Source.entries.associateWith { server.url("/").toString() }, pause = {})
        try { fast.posts(SearchSpec(Source.RULE34), PageKey.Number(1), 30); fail() } catch (e: BooruException) { assertEquals(ErrorKind.CONNECTION, e.kind) }
        assertEquals(3, server.requestCount)
    }
    private lateinit var server: MockWebServer
    private lateinit var repository: NetworkRepository
    private val pauses = mutableListOf<Long>()
    @Before fun setup() {
        server = MockWebServer().apply { start() }
        repository = NetworkRepository({ if (it == Source.RULE34) Credentials("123", "test-only-secret") else null },
            bases = Source.entries.associateWith { server.url("/").toString() }, pause = { pauses += it })
    }
    @After fun teardown() { server.shutdown() }
    @Test fun firstRulePageUsesPidZeroAndRetrofitEncodesTags() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))
        repository.posts(SearchSpec(Source.RULE34, "cat+dog rating:s"), PageKey.Number(1), 30)
        val url = server.takeRequest().requestUrl!!
        assertEquals("0", url.queryParameter("pid")); assertEquals("cat+dog rating:s", url.queryParameter("tags"))
        assertEquals("123", url.queryParameter("user_id"))
    }
    @Test fun permissionsAreNotRetriedAndMessagesNeverContainCredentials() = runBlocking {
        for ((status, kind) in listOf(401 to ErrorKind.CREDENTIALS, 403 to ErrorKind.DENIED)) {
            server.enqueue(MockResponse().setResponseCode(status))
            try { repository.posts(SearchSpec(Source.RULE34), PageKey.Number(1), 30); fail() } catch (e: BooruException) {
                assertEquals(kind, e.kind); assertFalse(e.userMessage().contains("test-only-secret")); assertFalse(e.toString().contains("api_key"))
            }
        }
        assertEquals(2, server.requestCount); assertTrue(pauses.isEmpty())
    }
    @Test fun honorsRetryAfterAndCapsTransientRetries() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "2"))
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody("[]"))
        repository.posts(SearchSpec(Source.RULE34), PageKey.Number(1), 30)
        assertEquals(listOf(2000L, 2000L), pauses); assertEquals(3, server.requestCount)
    }
    @Test fun rejectsLongCooldownAndUnexpectedSuccessBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "120"))
        try { repository.posts(SearchSpec(Source.SHIMMIE), PageKey.Number(1), 30); fail() } catch (e: BooruException) { assertEquals(ErrorKind.RATE_LIMIT, e.kind) }
        try { repository.posts(SearchSpec(Source.SHIMMIE), PageKey.Number(1), 30); fail() } catch (e: BooruException) { assertEquals(ErrorKind.RATE_LIMIT, e.kind) }
        server.enqueue(MockResponse().setBody("<html>not json</html>"))
        try { repository.posts(SearchSpec(Source.DANBOORU), PageKey.Number(1), 30); fail() } catch (e: BooruException) { assertEquals(ErrorKind.INVALID_RESPONSE, e.kind) }
        assertEquals(2, server.requestCount)
    }
    @Test fun danbooruCursorUsesLastRawId() = runBlocking {
        server.enqueue(MockResponse().setBody("[{\"id\":15},{\"id\":12}]"))
        val page = repository.posts(SearchSpec(Source.DANBOORU), PageKey.Number(1), 2)
        assertEquals(PageKey.Before(12), page.next)
        server.enqueue(MockResponse().setBody("[]"))
        repository.posts(SearchSpec(Source.DANBOORU), page.next!!, 2)
        server.takeRequest(); assertEquals("b12", server.takeRequest().requestUrl!!.queryParameter("page"))
    }
    @Test fun credentialsDoNotFollowRedirectsOrAppearInBareCdnRequests() = runBlocking {
        MockWebServer().use { cdn ->
            cdn.start()
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", cdn.url("/image.jpg")))
            try { repository.posts(SearchSpec(Source.RULE34), PageKey.Number(1), 30); fail() } catch (_: BooruException) {}
            assertEquals(0, cdn.requestCount)
            cdn.enqueue(MockResponse().setBody("image"))
            apiClient().newCall(Request.Builder().url(cdn.url("/image.jpg")).build()).execute().close()
            val request = cdn.takeRequest()
            assertNull(request.getHeader("Authorization")); assertNull(request.getHeader("Cookie"))
            assertFalse(request.path!!.contains("api_key"))
        }
    }
}
