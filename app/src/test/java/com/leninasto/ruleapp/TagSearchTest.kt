package com.leninasto.ruleapp

import com.leninasto.ruleapp.domain.*
import org.junit.Assert.*
import org.junit.Test

class TagSearchTest {
    private fun post(source: Source, rating: Rating, tags: List<String>, id: Long = 1) =
        BooruPost(PostKey(source.name, id), null, null, null, null, null, tags, rating, null, MediaKind.IMAGE, source.postUrl(id))
    @Test fun oneLetterSuggestionsRespectSourceAndVisibleRatings() {
        val posts = listOf(post(Source.PAHEAL, Rating.GENERAL, listOf("landscape", "lake")),
            post(Source.DANBOORU, Rating.GENERAL, listOf("light")), post(Source.PAHEAL, Rating.UNKNOWN, listOf("leaf"), 2))
        assertEquals(listOf("lake", "landscape"), localTagSuggestions(Source.PAHEAL, "l", posts, setOf(Rating.GENERAL)).map { it.name })
        assertTrue(localTagSuggestions(Source.PAHEAL, "rating:", posts, Rating.entries.toSet()).isEmpty())
    }
    @Test fun completingLastTagPreservesOtherTagsAndExclusion() {
        assertEquals("landscape -lake ", completeTag("landscape -la", "lake"))
        assertEquals("~landscape ", completeTag("~l", "landscape"))
        assertEquals("", activeTagInput("landscape ").prefix)
    }
    @Test fun repeatedPostsDoNotBiasSuggestionFrequency() {
        val lake = post(Source.SHIMMIE, Rating.GENERAL, listOf("lake"))
        val landscape = post(Source.SHIMMIE, Rating.GENERAL, listOf("landscape"), 2)
        assertEquals(listOf("lake", "landscape"), localTagSuggestions(Source.SHIMMIE, "L", listOf(lake, landscape, landscape), setOf(Rating.GENERAL)).map { it.name })
    }
}
