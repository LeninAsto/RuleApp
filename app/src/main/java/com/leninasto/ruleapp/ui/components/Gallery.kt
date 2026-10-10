package com.leninasto.ruleapp.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.decode.BitmapFactoryDecoder
import com.leninasto.ruleapp.domain.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(post: BooruPost, saved: Boolean, onOpen: () -> Unit, onActions: () -> Unit, onSave: () -> Unit) {
    val ratio = if ((post.width ?: 0) > 0 && (post.height ?: 0) > 0) (post.width!!.toFloat() / post.height!!).coerceIn(0.5f, 1.8f) else 0.8f
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()
        .combinedClickable(onClick = onOpen, onLongClick = onActions, onLongClickLabel = "Acciones de la publicación")
        .semantics { contentDescription = "Publicación ${post.key.postId}, ${post.mediaKind.label}, ${post.rating.label}" }) {
        Box(Modifier.fillMaxWidth().aspectRatio(ratio)) {
            var failed by remember(post.previewUrl) { mutableStateOf(post.previewUrl == null) }
            if (!failed) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(post.previewUrl)
                .decoderFactory(BitmapFactoryDecoder.Factory()).memoryCacheKey("static:${post.previewUrl}").build(), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(), onError = { failed = true })
            if (failed) Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BrokenImage, null)
                Text("Sin miniatura", style = MaterialTheme.typography.labelMedium)
                if (post.previewUrl != null) TextButton(onClick = { failed = false }) { Text("Reintentar") }
            }
            if (post.mediaKind != MediaKind.IMAGE) Surface(Modifier.align(Alignment.TopStart).padding(8.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
                Text(post.mediaKind.label, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
            }
            FilledTonalIconButton(onClick = onSave, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(48.dp)) {
                Icon(if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, if (saved) "Quitar de favoritos" else "Guardar en favoritos")
            }
        }
    }
}

@Composable
fun StaticGallery(posts: List<BooruPost>, savedKeys: Set<PostKey>, onOpen: (BooruPost) -> Unit, onActions: (BooruPost) -> Unit, onSave: (BooruPost) -> Unit, state: LazyStaggeredGridState = rememberLazyStaggeredGridState()) {
    LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Adaptive(160.dp), state = state,
        contentPadding = PaddingValues(12.dp), verticalItemSpacing = 8.dp, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(posts, key = { it.key.stableId }) { post -> PostCard(post, post.key in savedKeys, { onOpen(post) }, { onActions(post) }, { onSave(post) }) }
    }
}
@Composable
fun MessageState(title: String, description: String, onRetry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.PhotoLibrary, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium)
        if (onRetry != null) FilledTonalButton(onClick = onRetry) { Text("Reintentar") }
    }
}
@Preview(showBackground = true)
@Composable private fun GalleryPreview() = AppTheme(null) {
    StaticGallery(listOf(previewPost(1, 800, 1200), previewPost(2, 1200, 800), previewPost(3, 800, 1000)), emptySet(), {}, {}, {})
}
internal fun previewPost(id: Long, width: Int = 800, height: Int = 1200) = BooruPost(PostKey(Source.SHIMMIE.name, id), null, null, null, width, height, listOf("landscape"), Rating.UNKNOWN, "jpg", MediaKind.IMAGE, Source.SHIMMIE.postUrl(id))
@Preview(showBackground = true) @Composable private fun EmptyPreview() = AppTheme(null) { MessageState("Nada por aquí todavía", "Prueba con otros tags o cambia de fuente.") }
