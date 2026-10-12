package com.leninasto.ruleapp.ui
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.leninasto.ruleapp.data.local.AppPreferences
import com.leninasto.ruleapp.domain.*
import com.leninasto.ruleapp.ui.components.*

@Composable
internal fun ExploreScreen(model: AppViewModel, spec: SearchSpec, prefs: AppPreferences, pages: LazyPagingItems<BooruPost>, grid: LazyStaggeredGridState,
    saved: Set<PostKey>, kinds: Set<MediaKind>, onOpen: (BooruPost) -> Unit, onActions: (BooruPost) -> Unit, onSave: (BooruPost) -> Unit, onFilters: () -> Unit, onSearch: (String) -> Unit) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable(spec.source.name, spec.query) { mutableStateOf(spec.query) }
    var sources by remember { mutableStateOf(false) }
    val suggestions by model.suggestions.collectAsStateWithLifecycle()
    val history by model.history.collectAsStateWithLifecycle()
    val ruleReady by model.ruleReady.collectAsStateWithLifecycle()
    val loadedPosts = pages.itemSnapshotList.items
    LaunchedEffect(spec.source, loadedPosts) { model.observeTags(spec.source, loadedPosts) }
    BackHandler(searchOpen) { searchOpen = false; model.draft("") }
    Column {
        AppTopBar(title = { Column { Text(t(R.string.ui_explore), style = MaterialTheme.typography.headlineMedium); Text(t(R.string.ui_find_your_next_inspiration), style = MaterialTheme.typography.labelMedium) } },
            actions = { IconButton(onClick = { searchOpen = !searchOpen; if (searchOpen) model.draft(draft) }) { Icon(Icons.Default.Search, t(R.string.ui_search_tags)) }; IconButton(onClick = onFilters) { Icon(Icons.Default.Tune, t(R.string.ui_filters)) } })
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box { AssistChip(onClick = { sources = true }, label = { Text(spec.source.label) }, leadingIcon = { Icon(Icons.Default.Language, null) })
                DropdownMenu(sources, { sources = false }) { Source.entries.forEach { source -> DropdownMenuItem(text = { Column { Text(source.label); Text(lastVisitLabel(prefs.visits[source]), style = MaterialTheme.typography.labelSmall) } }, onClick = { model.source(source); sources = false; searchOpen = false }) } } }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { model.refresh() }) { Icon(Icons.Default.Refresh, t(R.string.ui_refresh_gallery)) }
        }
        if (spec.query.isNotBlank()) LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val queryTags = spec.query.split(Regex("\\s+")).filter(String::isNotBlank)
            items(queryTags.size, key = { it }) { index -> InputChip(selected = true,
                onClick = { onSearch(queryTags.filterIndexed { i, _ -> i != index }.joinToString(" ")) },
                label = { Text(queryTags[index], maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                trailingIcon = { Icon(Icons.Default.Close, t(R.string.ui_remove_tag_s, queryTags[index]), Modifier.size(16.dp)) }) }
        }
        if (spec.source == Source.RULE34 && !ruleReady) { Rule34Credentials(model); return@Column }
        if (searchOpen) {
            OutlinedTextField(value = draft, onValueChange = { draft = it; model.draft(it) }, label = { Text(t(R.string.ui_tags_separated_by_spaces)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch(draft); searchOpen = false; model.draft("") }),
                trailingIcon = { IconButton(onClick = { onSearch(draft); searchOpen = false; model.draft("") }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, t(R.string.ui_search)) } })
            LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (draft.isBlank()) items(history.filter { it.source == spec.source.name }, key = { it.query }) { entry -> SuggestionChip(onClick = { draft = entry.query; onSearch(draft); searchOpen = false }, label = { Text(entry.query) }) }
                else if (suggestions.source == spec.source && suggestions.prefix == activeTagInput(draft).prefix) items(suggestions.items, key = { it.name }) { tag -> SuggestionChip(onClick = { draft = completeTag(draft, tag.name); model.draft(draft) }, label = { Text(tag.name) }) }
            }
            if (draft.isNotBlank() && suggestions.source == spec.source && suggestions.prefix == activeTagInput(draft).prefix) {
                if (suggestions.loading) Text(t(R.string.ui_finding_tags), Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                suggestions.message?.let { Text(it, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall) }
            }
        }
        val initial = pages.loadState.refresh
        when {
            initial is LoadState.Loading && pages.itemCount == 0 -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { AppLoadingIndicator() }
            initial is LoadState.Error && pages.itemCount == 0 -> MessageState(t(R.string.ui_we_could_not_load_the_gallery), initial.error.localizedError(), pages::retry)
            pages.itemCount == 0 -> MessageState(t(R.string.ui_no_results), t(R.string.ui_try_different_tags_or_switch_sources))
            else -> {
                if (initial is LoadState.Error) TextButton(onClick = pages::retry) { Text(t(R.string.ui_refresh_failed_retry)) }
                val visible = pages.itemSnapshotList.items.withIndex().filter { (_, post) -> post.key.siteId == spec.source.name && post.rating in prefs.ratings && post.mediaKind in kinds }
                LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Adaptive(if (prefs.compactGallery) 136.dp else 160.dp), state = grid, modifier = Modifier.weight(1f).testTag("gallery"), contentPadding = PaddingValues(12.dp), verticalItemSpacing = 8.dp, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visible.size, key = { i -> visible[i].value.key.stableId }) { i ->
                        val post = pages[visible[i].index]
                        if (post != null) PostCard(post, post.key in saved, { onOpen(post) }, { onActions(post) }, { onSave(post) }, compact = prefs.compactGallery)
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        when (val append = pages.loadState.append) {
                            is LoadState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { AppLoadingIndicator() }
                            is LoadState.Error -> MessageState(t(R.string.ui_we_could_not_load_more), append.error.localizedError(), pages::retry)
                            else -> if (visible.isEmpty()) {
                                MessageState(t(R.string.ui_filters_hide_this_page), t(R.string.ui_adjust_ratings_or_file_types_to_see_results))
                                if (!append.endOfPaginationReached) TextButton(onClick = { if (pages.itemCount > 0) pages[pages.itemCount - 1] }) { Text(t(R.string.ui_search_the_next_page)) }
                            } else if (!append.endOfPaginationReached && visible.last().index < pages.itemCount - 6) {
                                TextButton(onClick = { pages[pages.itemCount - 1] }) { Text(t(R.string.ui_load_more_posts)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
