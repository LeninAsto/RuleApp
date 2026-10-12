package com.leninasto.ruleapp.ui
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leninasto.ruleapp.data.local.*
import com.leninasto.ruleapp.domain.*
import com.leninasto.ruleapp.ui.components.*

@Composable
internal fun Onboarding(onConfirm: (Set<Rating>) -> Unit) {
    var adult by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf(Rating.GENERAL)) }
    Surface(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Spacer(Modifier.height(32.dp)); Icon(Icons.Default.AutoAwesome, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary) }
            item { LanguageChoices() }
            item { Text(t(R.string.ui_a_space_to_explore), style = MaterialTheme.typography.headlineLarge); Text(t(R.string.ui_photos_gifs_and_videos_from_your_favorite_sources)) }
            item { Text(t(R.string.ui_unofficial_client_for_adults_sources_may_contain_explicit)) }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(adult, { adult = it }); Text(t(R.string.ui_i_confirm_i_am_an_adult), Modifier.weight(1f)) } }
            item { RatingChoices(selected) { selected = it } }
            item { Text(t(R.string.ui_shimmie_and_paheal_may_return_unrated_posts_enable), style = MaterialTheme.typography.bodySmall) }
            item { Button(onClick = { onConfirm(selected) }, enabled = adult && selected.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(t(R.string.ui_start_exploring)) } }
            item { Text(t(R.string.ui_your_saved_posts_are_local_each_source_has), style = MaterialTheme.typography.bodySmall) }
        }
    }
}
@Composable
internal fun RatingChoices(selected: Set<Rating>, onChange: (Set<Rating>) -> Unit) {
    Text(t(R.string.ui_visible_ratings), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Rating.entries.forEach { rating ->
        FilterChip(selected = rating in selected, onClick = { onChange(if (rating in selected) selected - rating else selected + rating) }, label = { Text(rating.uiLabel()) })
    } }
}
@Composable
internal fun SavedScreen(posts: List<BooruPost>, keys: Set<PostKey>, collections: List<CollectionEntity>, memberships: List<Membership>, onOpen: (BooruPost, List<BooruPost>) -> Unit, onActions: (BooruPost) -> Unit, onSave: (BooruPost) -> Unit,
    onCreate: (String, BooruPost?) -> Unit, onDelete: (Long) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var create by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    val filtered = if (selected == null) posts else posts.filter { p -> memberships.any { it.collectionId == selected && it.postKey == p.key.stableId } }
    Column {
        AppTopBar(title = { Text(t(R.string.ui_your_saved_posts), style = MaterialTheme.typography.headlineMedium) }, actions = {
            IconButton(onClick = { create = true }) { Icon(Icons.Default.CreateNewFolder, t(R.string.ui_create_collection)) }
            if (selected != null) IconButton(onClick = { delete = true }) { Icon(Icons.Default.DeleteOutline, t(R.string.ui_delete_collection)) }
        })
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected == null, { selected = null }, label = { Text(t(R.string.ui_all_a4114a)) }) }
            items(collections, key = { it.id }) { collection -> FilterChip(selected == collection.id, { selected = collection.id }, label = { Text(collection.name) }) }
        }
        if (filtered.isEmpty()) MessageState(t(R.string.ui_your_collection_starts_here), t(R.string.ui_save_posts_from_explore_metadata_is_available_offline))
        else StaticGallery(filtered, keys, { onOpen(it, filtered) }, onActions, onSave)
    }
    if (create) NameDialog(t(R.string.ui_new_collection), { create = false }) { onCreate(it, null); create = false }
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text(t(R.string.ui_delete_collection)) }, text = { Text(t(R.string.ui_posts_will_remain_in_your_favorites)) }, confirmButton = { TextButton(onClick = { selected?.let(onDelete); selected = null; delete = false }) { Text(t(R.string.ui_delete_54e140)) } }, dismissButton = { TextButton(onClick = { delete = false }) { Text(t(R.string.ui_cancel)) } })
}
@Composable internal fun NameDialog(title: String, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(name, { name = it.take(80) }, label = { Text(t(R.string.ui_name)) }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSubmit(name) }) { Text(t(R.string.ui_create)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(t(R.string.ui_cancel)) } })
}
@Composable internal fun CollectionPicker(post: BooruPost, collections: List<CollectionEntity>, memberships: List<Membership>, onDismiss: () -> Unit, onCollect: (Long) -> Unit, onRemove: (Long) -> Unit, onCreate: (String) -> Unit) {
    var create by remember { mutableStateOf(false) }
    AppBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(24.dp)) {
            item { Text(t(R.string.ui_add_to_collection), style = MaterialTheme.typography.headlineSmall) }
            items(collections, key = { it.id }) { collection ->
                val checked = memberships.any { it.collectionId == collection.id && it.postKey == post.key.stableId }
                ListItem(headlineContent = { Text(collection.name) }, leadingContent = { Checkbox(checked, { if (checked) onRemove(collection.id) else onCollect(collection.id) }) })
            }
            item { TextButton(onClick = { create = true }) { Icon(Icons.Default.Add, null); Text(t(R.string.ui_new_collection)) }; Button(onClick = onDismiss) { Text(t(R.string.ui_done)) } }
        }
    }
    if (create) NameDialog(t(R.string.ui_new_collection), { create = false }) { onCreate(it); create = false }
}
