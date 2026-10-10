package com.leninasto.ruleapp.domain

import java.net.URI

enum class Rating(val label: String) {
    GENERAL("General"), SENSITIVE("Sensible"), QUESTIONABLE("Cuestionable"), EXPLICIT("Explícito"), UNKNOWN("Sin clasificar")
}
enum class MediaKind(val label: String) { IMAGE("Imagen"), GIF("GIF"), VIDEO("Video"), UNKNOWN("Archivo") }
enum class SortOrder(val label: String, val tag: String) {
    NEWEST("Recientes", ""), SCORE("Más valorados", "order:score"), OLDEST("Más antiguos", "order:id_asc")
}
data class SourceCapabilities(val requiresCredentials: Boolean, val autocomplete: Boolean, val orders: List<SortOrder>)
enum class Source(val label: String, val baseUrl: String, val capabilities: SourceCapabilities) {
    SHIMMIE("Shimmie2", "https://shimmie.shishnet.org/", SourceCapabilities(false, false, listOf(SortOrder.NEWEST))),
    PAHEAL("Paheal", "https://rule34.paheal.net/", SourceCapabilities(false, false, listOf(SortOrder.NEWEST))),
    RULE34("Rule34", "https://api.rule34.xxx/", SourceCapabilities(true, true, listOf(SortOrder.NEWEST))),
    DANBOORU("Danbooru", "https://danbooru.donmai.us/", SourceCapabilities(false, true, SortOrder.entries));
    fun postUrl(id: Long) = when (this) {
        SHIMMIE, PAHEAL -> "${baseUrl}post/view/$id"
        RULE34 -> "https://rule34.xxx/index.php?page=post&s=view&id=$id"
        DANBOORU -> "${baseUrl}posts/$id"
    }
    val isShimmie get() = this == SHIMMIE || this == PAHEAL
}
data class PostKey(val siteId: String, val postId: Long) { val stableId get() = "$siteId:$postId" }
data class BooruPost(
    val key: PostKey,
    val previewUrl: String?,
    val sampleUrl: String?,
    val originalUrl: String?,
    val width: Int?, val height: Int?,
    val tags: List<String>, val rating: Rating,
    val extension: String?, val mediaKind: MediaKind,
    val publicUrl: String,
    val sourceUrl: String? = null,
    val author: String? = null
)
sealed interface PageKey {
    data class Number(val value: Int) : PageKey
    data class Before(val id: Long) : PageKey
}
data class PostPage(val items: List<BooruPost>, val next: PageKey?, val total: Long? = null)
data class SearchSpec(val source: Source = Source.SHIMMIE, val query: String = "", val order: SortOrder = SortOrder.NEWEST, val generation: Int = 0) {
    val wireTags get() = listOf(query.trim(), order.tag).filter(String::isNotBlank).joinToString(" ")
}
data class Credentials(val user: String, val apiKey: String)
data class TagSuggestion(val name: String, val count: Long? = null)
interface BooruRepository {
    suspend fun posts(spec: SearchSpec, page: PageKey, limit: Int): PostPage
    suspend fun post(source: Source, id: Long): BooruPost?
    suspend fun suggestions(source: Source, prefix: String): List<TagSuggestion>
}
enum class ErrorKind { CREDENTIALS, DENIED, RATE_LIMIT, CONNECTION, SERVER, INVALID_RESPONSE, UNSUPPORTED }
class BooruException(val kind: ErrorKind, val retryAfterMillis: Long? = null) : Exception(kind.name)
fun Throwable.userMessage(): String = when ((this as? BooruException)?.kind) {
    ErrorKind.CREDENTIALS -> "Revisa el usuario y la API key de esta fuente en Ajustes."
    ErrorKind.DENIED -> "El servidor denegó el acceso. Revisa los permisos de tu cuenta; no se reintentará automáticamente."
    ErrorKind.RATE_LIMIT -> "El servidor pidió una pausa. Espera antes de reintentar."
    ErrorKind.CONNECTION -> "No pudimos conectar. Comprueba tu conexión y vuelve a intentar."
    ErrorKind.SERVER -> "El servidor no está disponible en este momento."
    ErrorKind.INVALID_RESPONSE -> "La respuesta del servidor no tiene el formato esperado."
    ErrorKind.UNSUPPORTED -> "Esta fuente no ofrece la API necesaria."
    else -> "No se pudo completar la operación. Vuelve a intentar."
}
fun resolveHttps(base: String, value: String?): String? = runCatching {
    value?.takeIf(String::isNotBlank)?.let { URI(base).resolve(it) }
        ?.takeIf { it.scheme.equals("https", true) && !it.host.isNullOrBlank() && it.userInfo == null }?.toString()
}.getOrNull()
fun mediaKind(extension: String?) = when (extension?.lowercase()) {
    "jpg", "jpeg", "png", "webp", "avif", "bmp" -> MediaKind.IMAGE
    "gif" -> MediaKind.GIF
    "mp4", "webm", "mkv" -> MediaKind.VIDEO
    else -> MediaKind.UNKNOWN
}
fun extensionFrom(name: String?): String? = runCatching {
    URI(name.orEmpty()).path.substringAfterLast('.', "").lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
}.getOrNull()
fun normalizeRating(source: Source, raw: String?): Rating = when (raw?.lowercase()) {
    "g", "general", "safe" -> Rating.GENERAL
    "s" -> if (source == Source.DANBOORU) Rating.SENSITIVE else Rating.GENERAL
    "sensitive" -> Rating.SENSITIVE
    "q", "questionable" -> Rating.QUESTIONABLE
    "e", "explicit" -> Rating.EXPLICIT
    else -> Rating.UNKNOWN
}
