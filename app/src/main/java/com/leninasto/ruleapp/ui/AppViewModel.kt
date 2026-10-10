package com.leninasto.ruleapp.ui

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.*
import com.leninasto.ruleapp.AppContainer
import com.leninasto.ruleapp.data.local.*
import com.leninasto.ruleapp.data.remote.BooruPagingSource
import com.leninasto.ruleapp.data.remote.FeedSession
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AppViewModel(val container: AppContainer, private val savedState: SavedStateHandle) : ViewModel() {
    var savedViewerPosts: List<BooruPost> = emptyList()
    val preferences = container.preferences.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val saved = container.library.posts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val collections = container.library.dao.collections().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val memberships = container.library.dao.memberships().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val history = container.library.dao.history().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val downloadRows = container.library.dao.downloads().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _spec = MutableStateFlow(SearchSpec(source = runCatching { Source.valueOf(savedState.get<String>("source").orEmpty()) }.getOrDefault(Source.SHIMMIE), query = savedState["query"] ?: ""))
    val spec = _spec.asStateFlow()
    private val sessions = mutableMapOf<Source, SearchSpec>()
    private val feeds = mutableMapOf<Source, Pair<SearchSpec, FeedSession>>()
    val pages = combine(_spec, container.lock.accessible, container.lock.restricted) { spec, access, restricted -> spec to (access && !restricted) }.flatMapLatest { (spec, access) ->
        if (!access) return@flatMapLatest flowOf(PagingData.empty())
        val feed = feeds[spec.source]?.takeIf { it.first == spec }?.second ?: FeedSession().also { feeds[spec.source] = spec to it }
        Pager(PagingConfig(pageSize = 30, initialLoadSize = 30, prefetchDistance = 6, enablePlaceholders = false)) { BooruPagingSource(container.repository, spec, feed) }.flow
    }.cachedIn(viewModelScope)
    private val draft = MutableStateFlow("")
    private val knownPosts = MutableStateFlow<Map<Source, List<BooruPost>>>(emptyMap())
    data class Suggestions(val source: Source, val prefix: String, val items: List<TagSuggestion> = emptyList(), val loading: Boolean = false, val message: String? = null)
    private data class TagRequest(val source: Source, val prefix: String, val local: List<TagSuggestion>)
    private val tagRequests = combine(draft, _spec.map { it.source }.distinctUntilChanged(), saved, knownPosts, preferences) { text, source, savedPosts, observed, prefs ->
        val prefix = activeTagInput(text).prefix
        TagRequest(source, prefix, localTagSuggestions(source, prefix, observed[source].orEmpty() + savedPosts, prefs?.ratings.orEmpty()))
    }.flowOn(Dispatchers.Default)
    val suggestions = combine(tagRequests, container.lock.accessible, container.lock.restricted) { request, access, restricted -> request to (access && !restricted) }
        .transformLatest { (request, access) ->
            val (source, prefix, local) = request
            if (!access || prefix.isBlank() || ':' in prefix) { emit(Suggestions(source, prefix)); return@transformLatest }
            emit(Suggestions(source, prefix, local, loading = source.capabilities.autocomplete))
            if (!source.capabilities.autocomplete) {
                emit(Suggestions(source, prefix, local, message = "Tags de publicaciones cargadas y guardadas; esta fuente no ofrece autocomplete por prefijo."))
                return@transformLatest
            }
            delay(350)
            try {
                val remote = container.repository.suggestions(source, prefix)
                emit(Suggestions(source, prefix, (remote + local).distinctBy { it.name }.take(12), message = if (remote.isEmpty() && local.isEmpty()) "No hay sugerencias para este prefijo. Puedes buscarlo igualmente." else null))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { emit(Suggestions(source, prefix, local, message = e.userMessage() + if (local.isNotEmpty()) " Mostrando tags locales." else "")) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Suggestions(_spec.value.source, ""))
    fun observeTags(source: Source, posts: List<BooruPost>) {
        // Bounded, session-only vocabulary; saved metadata provides suggestions after restart.
        knownPosts.value = knownPosts.value + (source to (knownPosts.value[source].orEmpty() + posts.filter { it.key.siteId == source.name }).distinctBy { it.key }.takeLast(1000))
    }
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()
    fun consumeNotice() { _notice.value = null }
    fun draft(text: String) { draft.value = text }
    fun source(source: Source) {
        sessions[_spec.value.source] = _spec.value
        update(sessions[source] ?: SearchSpec(source)); draft.value = ""
    }
    fun search(query: String) {
        val source = _spec.value.source
        update(_spec.value.copy(query = query.trim(), generation = _spec.value.generation + 1))
        if (query.isNotBlank()) action { container.library.dao.addHistory(SearchHistory(source.name, query.trim())) }
    }
    fun order(order: SortOrder) { update(_spec.value.copy(order = order, generation = _spec.value.generation + 1)) }
    fun refresh() { update(_spec.value.copy(generation = _spec.value.generation + 1)) }
    private fun update(value: SearchSpec) { _spec.value = value; savedState["source"] = value.source.name; savedState["query"] = value.query }
    fun toggle(post: BooruPost) = action { container.library.toggle(post) }
    fun collect(post: BooruPost, id: Long) = action { container.library.collect(post, id) }
    fun uncollect(post: BooruPost, id: Long) = action { container.library.dao.removeMembership(id, post.key.stableId) }
    fun collection(name: String, post: BooruPost? = null) = action {
        val clean = name.trim(); require(clean.isNotEmpty())
        val id = container.library.dao.createCollection(CollectionEntity(name = clean.take(80)))
        if (post != null) container.library.collect(post, id)
    }
    fun deleteCollection(id: Long) = action { container.library.dao.deleteCollection(id) }
    fun confirm(ratings: Set<Rating>) = action { container.preferences.confirm(ratings) }
    fun ratings(ratings: Set<Rating>) = action { container.preferences.ratings(ratings) }
    fun theme(theme: ThemeMode) = action { container.preferences.theme(theme) }
    fun dynamic(value: Boolean) = action { container.preferences.dynamic(value) }
    fun credentials(source: Source, user: String, key: String) = action {
        container.credentials.put(source, Credentials(user, key)); refresh(); _notice.value = "Credenciales guardadas en este dispositivo."
    }
    fun clearCredentials(source: Source? = null) = action { container.credentials.clear(source); refresh(); _notice.value = "Credenciales eliminadas."
    }
    fun clearHistory() = action { container.library.dao.clearHistory() }
    fun clearLibrary() = action { container.library.dao.clearLibrary() }
    fun download(post: BooruPost) = action { container.downloads.enqueue(post); _notice.value = "Descarga añadida. Revisa su estado en Ajustes."
    }
    fun validate(source: Source) = action {
        val page = container.repository.posts(SearchSpec(source), PageKey.Number(1), 1)
        _notice.value = "${source.label}: conexión válida (${page.items.size} publicación)."
    }
    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { _notice.value = e.userMessage() }
    }
    companion object {
        fun factory(container: AppContainer) = viewModelFactory { initializer { AppViewModel(container, createSavedStateHandle()) } }
    }
}
