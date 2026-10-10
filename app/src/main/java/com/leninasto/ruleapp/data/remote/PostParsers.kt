package com.leninasto.ruleapp.data.remote

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.leninasto.ruleapp.domain.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

object PostParsers {
    fun shimmie(xml: String, source: Source = Source.SHIMMIE): Pair<List<BooruPost>, Long?> = parseSafely {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        // External entities and document declarations are never processed.
        require(!xml.contains("<!DOCTYPE", true) && !xml.contains("<!ENTITY", true))
        parser.setInput(StringReader(xml))
        val posts = mutableListOf<BooruPost>()
        var total: Long? = null
        var rootSeen = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                fun a(name: String) = parser.getAttributeValue(null, name)
                when (parser.name) {
                    "posts" -> { rootSeen = true; total = a("count")?.toLongOrNull() }
                    "post" -> {
                        require(rootSeen)
                        val id = a("id")?.toLongOrNull()?.takeIf { it > 0 } ?: throw BooruException(ErrorKind.INVALID_RESPONSE)
                        val ext = extensionFrom(a("file_name")) ?: extensionFrom(a("file_url"))
                        posts += BooruPost(PostKey(source.name, id),
                            resolveHttps(source.baseUrl, a("preview_url")),
                            null, resolveHttps(source.baseUrl, a("file_url")),
                            a("width")?.toIntOrNull(), a("height")?.toIntOrNull(),
                            tags(a("tags")), normalizeRating(source, a("rating")),
                            ext, mediaKind(ext), source.postUrl(id), a("source"), a("author"))
                    }
                }
            }
            parser.next()
        }
        require(rootSeen)
        posts to total
    }

    fun json(text: String, source: Source): List<BooruPost> = parseSafely {
        val root = JsonParser.parseString(text)
        if (source == Source.RULE34 && root.isJsonPrimitive && root.asString.contains("authentication", true)) throw BooruException(ErrorKind.CREDENTIALS)
        if (root.isJsonObject && root.asJsonObject.get("success")?.asString == "false") {
            throw BooruException(ErrorKind.SERVER)
        }
        val array = when {
            root.isJsonArray -> root.asJsonArray.toList()
            root.isJsonObject && root.asJsonObject.has("post") -> {
                val nested = root.asJsonObject.get("post")
                when { nested.isJsonArray -> nested.asJsonArray.toList(); nested.isJsonObject -> listOf(nested); else -> throw BooruException(ErrorKind.INVALID_RESPONSE) }
            }
            source == Source.DANBOORU && root.isJsonObject && root.asJsonObject.has("id") -> listOf(root)
            else -> throw BooruException(ErrorKind.INVALID_RESPONSE)
        }
        array.map { element ->
            require(element.isJsonObject)
            val item = element.asJsonObject
            val id = item.str("id")?.toLongOrNull()?.takeIf { it > 0 } ?: throw BooruException(ErrorKind.INVALID_RESPONSE)
            val dan = source == Source.DANBOORU
            val file = item.str("file_url")
            val ext = item.str("file_ext")?.lowercase() ?: extensionFrom(item.str("image")) ?: extensionFrom(file)
            BooruPost(PostKey(source.name, id),
                resolveHttps(source.baseUrl, item.str(if (dan) "preview_file_url" else "preview_url")),
                resolveHttps(source.baseUrl, item.str(if (dan) "large_file_url" else "sample_url")),
                resolveHttps(source.baseUrl, file),
                item.str(if (dan) "image_width" else "width")?.toIntOrNull(),
                item.str(if (dan) "image_height" else "height")?.toIntOrNull(),
                tags(item.str(if (dan) "tag_string" else "tags")), normalizeRating(source, item.str("rating")),
                ext, mediaKind(ext), source.postUrl(id), item.str("source"), item.str("owner"))
        }
    }

    fun suggestions(text: String): List<TagSuggestion> = parseSafely {
        val root = JsonParser.parseString(text)
        require(root.isJsonArray)
        root.asJsonArray.mapNotNull { value ->
            val item = value.takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return@mapNotNull null
            val name = item.str("value") ?: item.str("name") ?: return@mapNotNull null
            TagSuggestion(name, (item.str("post_count") ?: item.str("count"))?.toLongOrNull())
        }.take(12)
    }
    private fun JsonObject.str(key: String): String? = get(key)?.let {
        if (it.isJsonNull) null else { require(it.isJsonPrimitive); it.asString }
    }
    private fun tags(value: String?) = value.orEmpty().split(Regex("\\s+")).filter(String::isNotBlank)
    private inline fun <T> parseSafely(block: () -> T): T = try { block() } catch (e: BooruException) { throw e } catch (_: Exception) { throw BooruException(ErrorKind.INVALID_RESPONSE) }
}
