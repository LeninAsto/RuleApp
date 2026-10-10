package com.leninasto.ruleapp.ui

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
            item { Text("Un espacio para explorar", style = MaterialTheme.typography.headlineLarge); Text("Fotos, GIF y videos de tus fuentes favoritas. Tú eliges lo que aparece.") }
            item { Text("Cliente no oficial para adultos. Las fuentes pueden contener material explícito; sus etiquetas no garantizan una clasificación correcta.") }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(adult, { adult = it }); Text("Confirmo que soy mayor de edad", Modifier.weight(1f)) } }
            item { RatingChoices(selected) { selected = it } }
            item { Text("Shimmie y Paheal pueden devolver publicaciones «Sin clasificar». Activa esa categoría si deseas verlas.", style = MaterialTheme.typography.bodySmall) }
            item { Button(onClick = { onConfirm(selected) }, enabled = adult && selected.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Empezar a explorar") } }
            item { Text("Tus guardados son locales. Cada fuente tiene sus propias condiciones de acceso.", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
@Composable
internal fun RatingChoices(selected: Set<Rating>, onChange: (Set<Rating>) -> Unit) {
    Text("Clasificaciones visibles", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Rating.entries.forEach { rating ->
        FilterChip(selected = rating in selected, onClick = { onChange(if (rating in selected) selected - rating else selected + rating) }, label = { Text(rating.label) })
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
        AppTopBar(title = { Text("Tus guardados", style = MaterialTheme.typography.headlineMedium) }, actions = {
            IconButton(onClick = { create = true }) { Icon(Icons.Default.CreateNewFolder, "Crear colección") }
            if (selected != null) IconButton(onClick = { delete = true }) { Icon(Icons.Default.DeleteOutline, "Eliminar colección") }
        })
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected == null, { selected = null }, label = { Text("Todos") }) }
            items(collections, key = { it.id }) { collection -> FilterChip(selected == collection.id, { selected = collection.id }, label = { Text(collection.name) }) }
        }
        if (filtered.isEmpty()) MessageState("Aquí empieza tu colección", "Guarda publicaciones desde Explorar. Los metadatos quedan disponibles sin conexión; los archivos necesitan caché o descarga.")
        else StaticGallery(filtered, keys, { onOpen(it, filtered) }, onActions, onSave)
    }
    if (create) NameDialog("Nueva colección", { create = false }) { onCreate(it, null); create = false }
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Eliminar colección") }, text = { Text("Los posts seguirán en favoritos.") }, confirmButton = { TextButton(onClick = { selected?.let(onDelete); selected = null; delete = false }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { delete = false }) { Text("Cancelar") } })
}
@Composable internal fun NameDialog(title: String, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(name, { name = it.take(80) }, label = { Text("Nombre") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSubmit(name) }) { Text("Crear") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
@Composable internal fun CollectionPicker(post: BooruPost, collections: List<CollectionEntity>, memberships: List<Membership>, onDismiss: () -> Unit, onCollect: (Long) -> Unit, onRemove: (Long) -> Unit, onCreate: (String) -> Unit) {
    var create by remember { mutableStateOf(false) }
    AppBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(24.dp)) {
            item { Text("Añadir a colección", style = MaterialTheme.typography.headlineSmall) }
            items(collections, key = { it.id }) { collection ->
                val checked = memberships.any { it.collectionId == collection.id && it.postKey == post.key.stableId }
                ListItem(headlineContent = { Text(collection.name) }, leadingContent = { Checkbox(checked, { if (checked) onRemove(collection.id) else onCollect(collection.id) }) })
            }
            item { TextButton(onClick = { create = true }) { Icon(Icons.Default.Add, null); Text("Nueva colección") }; Button(onClick = onDismiss) { Text("Listo") } }
        }
    }
    if (create) NameDialog("Nueva colección", { create = false }) { onCreate(it); create = false }
}
