package com.leninasto.ruleapp.data.remote

import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

interface ShimmieService {
    @GET("api/danbooru/post/index.xml")
    suspend fun posts(@Query("page") page: Int, @Query("limit") limit: Int, @Query("tags") tags: String?, @Query("id") id: Long? = null): ResponseBody
}
interface Rule34Service {
    @GET("index.php?page=dapi&s=post&q=index&json=1")
    suspend fun posts(@Query("pid") page: Int, @Query("limit") limit: Int, @Query("tags") tags: String?, @Query("user_id") user: String, @Query("api_key") key: String, @Query("id") id: Long? = null): ResponseBody
    @GET("autocomplete.php")
    suspend fun suggestions(@Query("q") query: String, @Query("user_id") user: String, @Query("api_key") key: String): ResponseBody
}
interface DanbooruService {
    @GET("posts.json")
    suspend fun posts(@Query("page") page: String, @Query("limit") limit: Int, @Query("tags") tags: String?, @Query("login") user: String?, @Query("api_key") key: String?): ResponseBody
    @GET("posts/{id}.json")
    suspend fun post(@Path("id") id: Long, @Query("login") user: String?, @Query("api_key") key: String?): ResponseBody
    @GET("autocomplete.json")
    suspend fun suggestions(@Query("search[query]") query: String, @Query("search[type]") type: String = "tag_query", @Query("limit") limit: Int = 12): ResponseBody
}

// No logging, shared authenticated cache, credential interceptor or cross-host redirects.
fun apiClient() = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
    .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", "RuleApp/2.0 (personal Android client)").build()) }.build()

class NetworkRepository(
    private val credentials: (Source) -> Credentials?,
    private val client: OkHttpClient = apiClient(),
    private val bases: Map<Source, String> = Source.entries.associateWith { it.baseUrl },
    private val pause: suspend (Long) -> Unit = { delay(it) }
) : BooruRepository {
    private val cooldowns = ConcurrentHashMap<Source, Long>()
    private fun retrofit(source: Source) = Retrofit.Builder().baseUrl(bases.getValue(source)).client(client).build()
    private val shimmie = Source.entries.filter { it.isShimmie }.associateWith { retrofit(it).create(ShimmieService::class.java) }
    private val rule = retrofit(Source.RULE34).create(Rule34Service::class.java)
    private val dan = retrofit(Source.DANBOORU).create(DanbooruService::class.java)
    override suspend fun posts(spec: SearchSpec, page: PageKey, limit: Int): PostPage = request(spec.source) {
        val number = (page as? PageKey.Number)?.value ?: 1
        val auth = auth(spec.source)
        val text = when (spec.source) {
            Source.SHIMMIE, Source.PAHEAL -> shimmie.getValue(spec.source).posts(number, limit.coerceAtMost(100), spec.wireTags).use { it.string() }
            Source.RULE34 -> rule.posts(number - 1, limit, spec.wireTags, auth!!.user, auth.apiKey).use { it.string() }
            Source.DANBOORU -> dan.posts(when (page) { is PageKey.Before -> "b${page.id}"; is PageKey.Number -> page.value.toString() }, limit, spec.wireTags, auth?.user, auth?.apiKey).use { it.string() }
        }
        val (items, total) = if (spec.source.isShimmie) PostParsers.shimmie(text, spec.source) else PostParsers.json(text, spec.source) to null
        val finished = items.size < limit || (total != null && number.toLong() * limit >= total)
        val next = when {
            finished -> null
            spec.source == Source.DANBOORU && spec.order == SortOrder.NEWEST -> items.lastOrNull()?.key?.postId?.let(PageKey::Before)
            else -> PageKey.Number(number + 1)
        }
        PostPage(items, next, total)
    }
    override suspend fun post(source: Source, id: Long): BooruPost? = request(source) {
        val auth = auth(source)
        when (source) {
            Source.SHIMMIE, Source.PAHEAL -> shimmie.getValue(source).posts(1, 1, null, id).use { PostParsers.shimmie(it.string(), source).first.firstOrNull() }
            Source.RULE34 -> rule.posts(0, 1, null, auth!!.user, auth.apiKey, id).use { PostParsers.json(it.string(), source).firstOrNull() }
            Source.DANBOORU -> dan.post(id, auth?.user, auth?.apiKey).use { PostParsers.json(it.string(), source).firstOrNull() }
        }
    }
    override suspend fun suggestions(source: Source, prefix: String): List<TagSuggestion> {
        if (!source.capabilities.autocomplete || prefix.isBlank() || ':' in prefix) return emptyList()
        return request(source) {
            when (source) {
                Source.SHIMMIE, Source.PAHEAL -> emptyList()
                Source.RULE34 -> { val auth = auth(source)!!; rule.suggestions(prefix, auth.user, auth.apiKey).use { PostParsers.suggestions(it.string()) } }
                Source.DANBOORU -> dan.suggestions(prefix).use { PostParsers.suggestions(it.string()) }
            }
        }
    }
    private fun auth(source: Source): Credentials? = credentials(source).also {
        if (source.capabilities.requiresCredentials && (it == null || it.user.isBlank() || it.apiKey.isBlank())) throw BooruException(ErrorKind.CREDENTIALS)
    }
    private suspend fun <T> request(source: Source, block: suspend () -> T): T = withContext(Dispatchers.IO) {
        val remaining = (cooldowns[source] ?: 0) - System.currentTimeMillis()
        if (remaining > 0) throw BooruException(ErrorKind.RATE_LIMIT, remaining)
        var attempt = 0
        while (true) {
            try { return@withContext block() } catch (e: CancellationException) { throw e } catch (e: Exception) {
                val error = when (e) {
                    is BooruException -> e
                    is HttpException -> BooruException(when (e.code()) {
                        401 -> ErrorKind.CREDENTIALS; 403 -> ErrorKind.DENIED; 404 -> ErrorKind.UNSUPPORTED
                        429 -> ErrorKind.RATE_LIMIT; in 500..599 -> ErrorKind.SERVER; else -> ErrorKind.INVALID_RESPONSE
                    }, retryAfter(e.response()?.headers()?.get("Retry-After")))
                    is IOException -> BooruException(ErrorKind.CONNECTION)
                    else -> BooruException(ErrorKind.INVALID_RESPONSE)
                }
                val wait = error.retryAfterMillis ?: (1000L shl attempt)
                if (attempt >= 2 || error.kind !in setOf(ErrorKind.SERVER, ErrorKind.CONNECTION, ErrorKind.RATE_LIMIT)) {
                    if (error.kind == ErrorKind.RATE_LIMIT) cooldowns[source] = System.currentTimeMillis() + wait
                    throw error
                }
                // Long server pauses are surfaced to the user, never retried early.
                if (wait > 30_000) { cooldowns[source] = System.currentTimeMillis() + wait; throw error }
                pause(wait); attempt++
            }
        }
        @Suppress("UNREACHABLE_CODE") error("unreachable")
    }
    private fun retryAfter(value: String?): Long? = value?.toLongOrNull()?.takeIf { it >= 0 }?.times(1000)
        ?: runCatching { (ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(0) }.getOrNull()
}
