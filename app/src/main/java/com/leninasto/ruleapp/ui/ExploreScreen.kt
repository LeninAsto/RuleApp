package com.leninasto.ruleapp.ui

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
    val loadedPosts = pages.itemSnapshotList.items
    LaunchedEffect(spec.source, loadedPosts) { model.observeTags(spec.source, loadedPosts) }
    BackHandler(searchOpen) { searchOpen = false; model.draft("") }
    Column {
        AppTopBar(title = { Column { Text("Explorar", style = MaterialTheme.typography.headlineMedium); Text("Encuentra tu próxima inspiración", style = MaterialTheme.typography.labelMedium) } },
            actions = { IconButton(onClick = { searchOpen = !searchOpen; if (searchOpen) model.draft(draft) }) { Icon(Icons.Default.Search, "Buscar tags") }; IconButton(onClick = onFilters) { Icon(Icons.Default.Tune, "Filtros") } })
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box { AssistChip(onClick = { sources = true }, label = { Text(spec.source.label) }, leadingIcon = { Icon(Icons.Default.Language, null) })
                DropdownMenu(sources, { sources = false }) { Source.entries.forEach { source -> DropdownMenuItem(text = { Text(source.label) }, onClick = { model.source(source); sources = false; searchOpen = false }) } } }
            if (spec.query.isNotBlank()) InputChip(selected = true, onClick = { searchOpen = true }, label = { Text(spec.query.take(30)) }, trailingIcon = { IconButton(onClick = { onSearch("") }) { Icon(Icons.Default.Close, "Quitar búsqueda") } })
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { model.refresh() }) { Icon(Icons.Default.Refresh, "Actualizar galería") }
        }
        if (searchOpen) {
            OutlinedTextField(value = draft, onValueChange = { draft = it; model.draft(it) }, label = { Text("Tags separados por espacios") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch(draft); searchOpen = false; model.draft("") }),
                trailingIcon = { IconButton(onClick = { onSearch(draft); searchOpen = false; model.draft("") }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Buscar") } })
            LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (draft.isBlank()) items(history.filter { it.source == spec.source.name }, key = { it.query }) { entry -> SuggestionChip(onClick = { draft = entry.query; onSearch(draft); searchOpen = false }, label = { Text(entry.query) }) }
                else if (suggestions.source == spec.source && suggestions.prefix == activeTagInput(draft).prefix) items(suggestions.items, key = { it.name }) { tag -> SuggestionChip(onClick = { draft = completeTag(draft, tag.name); model.draft(draft) }, label = { Text(tag.name) }) }
            }
            if (draft.isNotBlank() && suggestions.source == spec.source && suggestions.prefix == activeTagInput(draft).prefix) {
                if (suggestions.loading) Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { AppLoadingIndicator(); Text("Buscando tags…", Modifier.padding(8.dp)) }
                suggestions.message?.let { Text(it, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall) }
            }
        }
        val initial = pages.loadState.refresh
        when {
            initial is LoadState.Loading && pages.itemCount == 0 -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { AppLoadingIndicator() }
            initial is LoadState.Error && pages.itemCount == 0 -> MessageState("No pudimos cargar la galería", initial.error.userMessage(), pages::retry)
            pages.itemCount == 0 -> MessageState("Sin resultados", "Prueba otros tags o cambia de fuente.")
            else -> {
                if (initial is LoadState.Error) TextButton(onClick = pages::retry) { Text("Falló la actualización · Reintentar") }
                val visible = pages.itemSnapshotList.items.withIndex().filter { (_, post) -> post.key.siteId == spec.source.name && post.rating in prefs.ratings && post.mediaKind in kinds }
                LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Adaptive(160.dp), state = grid, modifier = Modifier.weight(1f).testTag("gallery"), contentPadding = PaddingValues(12.dp), verticalItemSpacing = 8.dp, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visible.size, key = { i -> visible[i].value.key.stableId }) { i ->
                        val post = pages[visible[i].index]
                        if (post != null) PostCard(post, post.key in saved, { onOpen(post) }, { onActions(post) }, { onSave(post) })
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        when (val append = pages.loadState.append) {
                            is LoadState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { AppLoadingIndicator() }
                            is LoadState.Error -> MessageState("No pudimos cargar más", append.error.userMessage(), pages::retry)
                            else -> if (visible.isEmpty()) {
                                MessageState("Los filtros ocultan esta página", "Ajusta las clasificaciones o los tipos de archivo para ver resultados.")
                                if (!append.endOfPaginationReached) TextButton(onClick = { if (pages.itemCount > 0) pages[pages.itemCount - 1] }) { Text("Buscar en la página siguiente") }
                            } else if (!append.endOfPaginationReached && visible.last().index < pages.itemCount - 6) {
                                TextButton(onClick = { pages[pages.itemCount - 1] }) { Text("Cargar más publicaciones") }
                            }
                        }
                    }
                }
            }
        }
    }
}
