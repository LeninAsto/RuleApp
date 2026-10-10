package com.leninasto.ruleapp.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.CancellationException

class FeedSession {
    var ready = false
    var items: List<BooruPost> = emptyList()
    var next: PageKey? = null
}
class BooruPagingSource(private val repository: BooruRepository, private val spec: SearchSpec, private val session: FeedSession = FeedSession()) : PagingSource<PageKey, BooruPost>() {
    private val seen = mutableSetOf<PostKey>()
    override suspend fun load(params: LoadParams<PageKey>): LoadResult<PageKey, BooruPost> { return try {
        if (params is LoadParams.Refresh && session.ready) {
            seen.addAll(session.items.map { it.key })
            return LoadResult.Page(session.items, null, session.next)
        }
        val page = repository.posts(spec, params.key ?: PageKey.Number(1), 30)
        // Forward-only loading: refresh starts a fresh source and a fresh identity set.
        val unique = page.items.filter { seen.add(it.key) }
        session.items = if (params is LoadParams.Refresh) unique else session.items + unique
        session.next = page.next
        session.ready = true
        LoadResult.Page(unique, null, page.next)
    } catch (e: CancellationException) { throw e } catch (e: Exception) { LoadResult.Error(e) } }
    override fun getRefreshKey(state: PagingState<PageKey, BooruPost>): PageKey? = null
}
