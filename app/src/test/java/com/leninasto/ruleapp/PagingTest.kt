package com.leninasto.ruleapp

import androidx.paging.PagingSource
import com.leninasto.ruleapp.data.remote.BooruPagingSource
import com.leninasto.ruleapp.data.remote.FeedSession
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PagingTest {
    private fun post(id: Long) = BooruPost(PostKey("SHIMMIE", id), null, null, null, null, null, emptyList(), Rating.UNKNOWN, null, MediaKind.UNKNOWN, "https://example.test/$id")
    @Test fun deduplicatesPagesWithoutLosingServerNextKey() = runBlocking {
        val repo = object : BooruRepository {
            override suspend fun posts(spec: SearchSpec, page: PageKey, limit: Int) = if (page == PageKey.Number(1)) PostPage(listOf(post(8), post(7)), PageKey.Number(2)) else PostPage(listOf(post(7), post(5)), null)
            override suspend fun post(source: Source, id: Long): BooruPost? = null
            override suspend fun suggestions(source: Source, prefix: String) = emptyList<TagSuggestion>()
        }
        val source = BooruPagingSource(repo, SearchSpec())
        val first = source.load(PagingSource.LoadParams.Refresh(null, 30, false)) as PagingSource.LoadResult.Page
        val next = source.load(PagingSource.LoadParams.Append(first.nextKey!!, 30, false)) as PagingSource.LoadResult.Page
        assertEquals(listOf(8L, 7L), first.data.map { it.key.postId }); assertEquals(listOf(5L), next.data.map { it.key.postId })
    }
    @Test fun propagatesCancellationInsteadOfShowingAnError() = runBlocking {
        val repo = object : BooruRepository {
            override suspend fun posts(spec: SearchSpec, page: PageKey, limit: Int): PostPage = throw CancellationException()
            override suspend fun post(source: Source, id: Long): BooruPost? = null
            override suspend fun suggestions(source: Source, prefix: String) = emptyList<TagSuggestion>()
        }
        try { BooruPagingSource(repo, SearchSpec()).load(PagingSource.LoadParams.Refresh(null, 30, false)); fail() } catch (_: CancellationException) {}
    }
    @Test fun restoringSourceReplaysItsLoadedPagesWithoutRefetching() = runBlocking {
        var calls = 0
        val repo = object : BooruRepository {
            override suspend fun posts(spec: SearchSpec, page: PageKey, limit: Int): PostPage { calls++; return PostPage(listOf(post(calls.toLong())), PageKey.Number(calls + 1)) }
            override suspend fun post(source: Source, id: Long): BooruPost? = null
            override suspend fun suggestions(source: Source, prefix: String) = emptyList<TagSuggestion>()
        }
        val session = FeedSession()
        val first = BooruPagingSource(repo, SearchSpec(), session)
        first.load(PagingSource.LoadParams.Refresh(null, 30, false))
        first.load(PagingSource.LoadParams.Append(PageKey.Number(2), 30, false))
        val restored = BooruPagingSource(repo, SearchSpec(), session).load(PagingSource.LoadParams.Refresh(null, 30, false)) as PagingSource.LoadResult.Page
        assertEquals(2, calls); assertEquals(listOf(1L, 2L), restored.data.map { it.key.postId }); assertEquals(PageKey.Number(3), restored.nextKey)
    }
}
