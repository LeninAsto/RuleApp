package com.leninasto.ruleapp.ui
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.leninasto.ruleapp.domain.*
import com.leninasto.ruleapp.ui.components.*
import kotlinx.coroutines.launch

@Composable
internal fun DetailScreen(posts: List<BooruPost>, initialKey: PostKey, saved: Set<PostKey>, onClose: () -> Unit, onSave: (BooruPost) -> Unit,
    onCollect: (BooruPost) -> Unit, onDownload: (BooruPost) -> Unit, onShare: (BooruPost) -> Unit, onTag: (Source, String) -> Unit, onLoadMore: () -> Unit, onSelected: (PostKey) -> Unit = {}) {
    BackHandler { onClose() }
    if (posts.isEmpty()) { MessageState(t(R.string.ui_post_unavailable), t(R.string.ui_current_filters_or_permissions_prevent_displaying_this_post), onClose); return }
    val pager = rememberPagerState(initialPage = posts.indexOfFirst { it.key == initialKey }.coerceAtLeast(0), pageCount = { posts.size })
    val post = posts[pager.currentPage.coerceAtMost(posts.lastIndex)]
    LaunchedEffect(post.key) { onSelected(post.key) }
    var zoomed by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage, posts.size) { if (pager.currentPage >= posts.lastIndex - 3) onLoadMore() }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = { Text(t(R.string.post_id_title, post.key.postId), style = MaterialTheme.typography.titleLarge) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t(R.string.ui_back_to_gallery)) } },
            actions = { IconButton(onClick = { info = true }) { Icon(Icons.Default.Info, t(R.string.ui_information_and_tags)) } })
        HorizontalPager(state = pager, userScrollEnabled = !zoomed, key = { posts[it].key.stableId }, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
            val item = posts[index]
            if (index == pager.currentPage) {
                when (item.mediaKind) {
                    MediaKind.IMAGE, MediaKind.GIF -> ZoomImage(item) { zoomed = it }
                    MediaKind.VIDEO -> Video(item)
                    else -> MessageState(t(R.string.ui_unsupported_format), t(R.string.ui_you_can_download_the_original_if_the_source))
                }
            }
        }
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.Center) {
            IconButton(enabled = pager.currentPage > 0 && !zoomed, onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t(R.string.ui_previous_post)) }
            FilledTonalIconButton(onClick = { onSave(post) }) { Icon(if (post.key in saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, if (post.key in saved) t(R.string.ui_remove_from_favorites) else t(R.string.ui_save)) }
            IconButton(onClick = { onCollect(post) }) { Icon(Icons.Default.CreateNewFolder, t(R.string.ui_add_to_collection)) }
            IconButton(enabled = post.originalUrl != null, onClick = { onDownload(post) }) { Icon(Icons.Default.Download, t(R.string.ui_download_original)) }
            IconButton(onClick = { onShare(post) }) { Icon(Icons.Default.Share, t(R.string.ui_share_link)) }
            IconButton(enabled = pager.currentPage < posts.lastIndex && !zoomed, onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, t(R.string.ui_next_post)) }
        }
        Text(t(R.string.viewer_media_rating_position, post.mediaKind.uiLabel(), post.rating.uiLabel(), pager.currentPage + 1, posts.size), Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp), style = MaterialTheme.typography.labelMedium)
    }
    if (info) AppBottomSheet(onDismissRequest = { info = false }) {
        LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(t(R.string.ui_about_this_post), style = MaterialTheme.typography.headlineSmall); Text(t(R.string.post_source_and_id, Source.valueOf(post.key.siteId).label, post.key.postId)); Text(t(R.string.post_dimensions_rating, post.width ?: "?", post.height ?: "?", post.rating.uiLabel())); post.author?.let { Text(t(R.string.ui_author_s, it)) } }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { post.tags.forEach { tag -> AssistChip(onClick = { info = false; onTag(Source.valueOf(post.key.siteId), tag) }, label = { Text(tag) }) } } }
        }
    }
}
@Composable private fun ZoomImage(post: BooruPost, onZoom: (Boolean) -> Unit) {
    var scale by remember(post.key) { mutableFloatStateOf(1f) }
    var offset by remember(post.key) { mutableStateOf(Offset.Zero) }
    var failed by remember(post.key) { mutableStateOf(false) }
    var loading by remember(post.key) { mutableStateOf(true) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val url = if (post.mediaKind == MediaKind.GIF) post.originalUrl ?: post.sampleUrl else post.sampleUrl ?: post.originalUrl
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        val next = offset + pan
        offset = if (scale > 1f) Offset(next.x.coerceIn(-size.width * (scale - 1) / 2, size.width * (scale - 1) / 2), next.y.coerceIn(-size.height * (scale - 1) / 2, size.height * (scale - 1) / 2)) else Offset.Zero
        onZoom(scale > 1.01f)
    }
    LaunchedEffect(post.key) { onZoom(false) }
    Box(Modifier.fillMaxSize().clipToBounds().onSizeChanged { size = it }, contentAlignment = Alignment.Center) {
        if (url == null || failed) MessageState(t(R.string.ui_file_unavailable), t(R.string.ui_the_source_does_not_provide_the_file_or), if (url != null) ({ failed = false }) else null)
        else AsyncImage(url, t(R.string.ui_post_s, post.key.postId), contentScale = ContentScale.Fit, onSuccess = { loading = false }, onError = { failed = true; loading = false }, modifier = Modifier.fillMaxSize()
            .pointerInput(post.key) { detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero; onZoom(scale > 1f) }) }
            .transformable(state = transform, canPan = { scale > 1.01f })
            .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y })
        if (loading && !failed && url != null) AppLoadingIndicator()
    }
}
@Composable private fun Video(post: BooruPost) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val url = post.originalUrl ?: post.sampleUrl
    if (url == null) { MessageState(t(R.string.ui_video_unavailable), t(R.string.ui_the_source_does_not_allow_access_to_this)); return }
    var failed by remember(post.key) { mutableStateOf(false) }
    var position by androidx.compose.runtime.saveable.rememberSaveable(post.key.stableId) { mutableLongStateOf(0L) }
    val playback = remember(post.key, owner) { com.leninasto.ruleapp.ui.components.VideoPlayback(context, owner, url) }
    val player = playback.player
    LaunchedEffect(player) { player.seekTo(position) }
    DisposableEffect(player, owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) { position = player.currentPosition; player.pause() } }
        val listener = object : Player.Listener { override fun onPlayerError(error: PlaybackException) { failed = true } }
        owner.lifecycle.addObserver(observer); player.addListener(listener)
        onDispose { position = player.currentPosition; owner.lifecycle.removeObserver(observer); player.removeListener(listener); playback.close() }
    }
    if (failed) MessageState(t(R.string.ui_playback_failed), t(R.string.ui_the_server_or_device_does_not_support_this)) { failed = false; player.prepare() }
    else AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true } }, update = { it.player = player }, modifier = Modifier.fillMaxSize(), onRelease = { it.player = null })
}
@Preview(showBackground = true) @Composable private fun DetailPreview() = AppTheme(null) {
    DetailScreen(listOf(previewPost(1)), PostKey(Source.SHIMMIE.name, 1), emptySet(), {}, {}, {}, {}, {}, { _, _ -> }, {})
}
