package com.leninasto.ruleapp

import com.leninasto.ruleapp.data.remote.PostParsers
import com.leninasto.ruleapp.domain.*
import org.junit.Assert.*
import org.junit.Test

class PostParsersTest {
    @Test fun pahealKeepsSeparateIdentityAndResolvesItsOwnHost() {
        val item = PostParsers.shimmie("<posts><post id='7' file_url='/images/a.jpg' rating='?'/></posts>", Source.PAHEAL).first.single()
        assertEquals(PostKey("PAHEAL", 7), item.key)
        assertEquals("https://rule34.paheal.net/images/a.jpg", item.originalUrl)
        assertEquals(Rating.UNKNOWN, item.rating)
    }
    @Test fun missingAuthenticationWithHttp200IsNotAnEmptyGallery() {
        assertFailure(ErrorKind.CREDENTIALS) { PostParsers.json("\"Missing authentication. Go to api.rule34.xxx for more information\"", Source.RULE34) }
    }
    private fun fixture(name: String) = javaClass.getResource("/$name")!!.readText()
    @Test fun xmlResolvesRelativeMediaAndPreservesUnknownRatings() {
        val (posts, count) = PostParsers.shimmie(fixture("shimmie_posts.xml"))
        assertEquals(3L, count); assertEquals(2, posts.size)
        assertEquals("https://shimmie.shishnet.org/images/landscape.jpg", posts[0].originalUrl)
        assertEquals(Rating.UNKNOWN, posts[0].rating)
        assertEquals(MediaKind.GIF, posts[1].mediaKind)
        assertNotEquals(posts[0].sourceUrl, posts[0].originalUrl)
    }
    @Test fun ratingsAreNormalizedBySourceAndMissingMediaIsValid() {
        val rule = PostParsers.json(fixture("rule34_posts.json"), Source.RULE34).single()
        val dan = PostParsers.json(fixture("danbooru_posts.json"), Source.DANBOORU).single()
        assertEquals(Rating.GENERAL, rule.rating); assertEquals(Rating.SENSITIVE, dan.rating)
        assertNull(dan.originalUrl); assertEquals(MediaKind.VIDEO, dan.mediaKind)
        assertNotEquals(rule.key, dan.key)
    }
    @Test fun acceptsDocumentedPrototypeWrappersAndEmptyResponses() {
        assertEquals(1, PostParsers.json("{\"post\":${fixture("rule34_posts.json")}}", Source.RULE34).size)
        assertTrue(PostParsers.json("[]", Source.RULE34).isEmpty())
        assertTrue(PostParsers.shimmie("<posts count='0'/>").first.isEmpty())
    }
    @Test fun rejectsSearchErrorsHtmlMalformedFieldsAndExternalEntities() {
        assertFailure(ErrorKind.SERVER) { PostParsers.json("{\"success\":false,\"message\":\"search down\"}", Source.RULE34) }
        assertFailure(ErrorKind.INVALID_RESPONSE) { PostParsers.json("<html>blocked</html>", Source.DANBOORU) }
        assertFailure(ErrorKind.INVALID_RESPONSE) { PostParsers.json("[{\"id\":{}}]", Source.RULE34) }
        assertFailure(ErrorKind.INVALID_RESPONSE) { PostParsers.shimmie("<!DOCTYPE posts [<!ENTITY file SYSTEM 'file:///etc/passwd'>]><posts/>") }
        assertFailure(ErrorKind.INVALID_RESPONSE) { PostParsers.shimmie("<html/>") }
    }
    @Test fun onlyAcceptsHttpsMediaWithoutUserInfo() {
        assertNull(resolveHttps(Source.SHIMMIE.baseUrl, "file:///private/file.jpg"))
        assertNull(resolveHttps(Source.SHIMMIE.baseUrl, "http://example.test/a.jpg"))
        assertNull(resolveHttps(Source.SHIMMIE.baseUrl, "https://user:password@example.test/a.jpg"))
        assertEquals("https://cdn.example.test/a.jpg", resolveHttps(Source.SHIMMIE.baseUrl, "//cdn.example.test/a.jpg"))
    }
    private fun assertFailure(kind: ErrorKind, block: () -> Unit) { try { block(); fail("Expected error") } catch (e: BooruException) { assertEquals(kind, e.kind) } }
}
