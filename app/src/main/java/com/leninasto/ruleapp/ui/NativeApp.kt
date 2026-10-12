package com.leninasto.ruleapp.ui
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.leninasto.ruleapp.domain.*
import com.leninasto.ruleapp.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun NativeApp(model: AppViewModel, settingsOnly: Boolean = false, openDownloads: Boolean = false, onSettingsBack: () -> Unit = {}) {
    val prefs by model.preferences.collectAsStateWithLifecycle()
    val spec by model.spec.collectAsStateWithLifecycle()
    val lock = model.container.lock
    val access by lock.accessible.collectAsStateWithLifecycle()
    val restricted by lock.restricted.collectAsStateWithLifecycle()
    val restored by model.sourceRestored.collectAsStateWithLifecycle()
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val contextForLock = LocalContext.current
    val lockEnabled by lock.enabled.collectAsStateWithLifecycle()
    DisposableEffect(owner, lock) {
        lock.readPolicy()
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: Intent) { lock.readPolicy() }
        }
        ContextCompat.registerReceiver(contextForLock, receiver, android.content.IntentFilter(android.content.Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { contextForLock.unregisterReceiver(receiver) }
    }
    DisposableEffect(lockEnabled) {
        val activity = contextForLock as? android.app.Activity
        if (lockEnabled) activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { }
    }
    AppTheme(prefs, source = spec.source) {
        if (!access || restricted) { LockScreen(lock, restricted); return@AppTheme }
        if (prefs == null || !restored) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppLoadingIndicator() }; return@AppTheme }
        if (!prefs!!.adultConfirmed) { Onboarding(model::confirm); return@AppTheme }
        if (settingsOnly) {
            var downloads by rememberSaveable { mutableStateOf(openDownloads) }
            androidx.activity.compose.BackHandler(downloads) { downloads = false }
            Scaffold(topBar = { AppTopBar(title = { Text(if (downloads) t(R.string.ui_downloads) else t(R.string.ui_settings)) }, navigationIcon = {
                IconButton(onClick = { if (downloads) downloads = false else onSettingsBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t(R.string.ui_back)) }
            }) }) { padding -> Box(Modifier.padding(padding)) { if (downloads) DownloadsScreen(model) else SettingsScreen(model, prefs!!, onDownloads = { downloads = true }) } }
            return@AppTheme
        }
        if (prefs!!.seenVersionCode != com.leninasto.ruleapp.BuildConfig.VERSION_CODE) {
            WelcomeScreen(onContinue = model::acknowledgeVersion); return@AppTheme
        }
        LaunchedEffect(Unit) { model.checkUpdates() }
        val saved by model.saved.collectAsStateWithLifecycle()
        val collections by model.collections.collectAsStateWithLifecycle()
        val memberships by model.memberships.collectAsStateWithLifecycle()
        val pages = model.pages.collectAsLazyPagingItems()
        val savedKeys = remember(saved) { saved.map { it.key }.toSet() }
        val snackbar = remember { SnackbarHostState() }
        val notice by model.notice.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val updates by model.updateState.collectAsStateWithLifecycle()
        var dismissedUpdate by rememberSaveable { mutableStateOf<String?>(null) }
        if (updates.available && updates.latest != dismissedUpdate) AlertDialog(onDismissRequest = { dismissedUpdate = updates.latest },
            title = { Text(t(R.string.ui_new_version_available)) }, text = { Text(t(R.string.ui_daparushi_s_is_available_review_the_apks_and, updates.latest)) },
            confirmButton = { TextButton(onClick = {
                dismissedUpdate = updates.latest
                androidx.browser.customtabs.CustomTabsIntent.Builder().build().launchUrl(context, android.net.Uri.parse("https://github.com/LeninAsto/RuleApp/releases"))
            }) { Text(t(R.string.ui_view_download)) } }, dismissButton = { TextButton(onClick = { dismissedUpdate = updates.latest }) { Text(t(R.string.ui_later)) } })
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var viewerKey by rememberSaveable(stateSaver = listSaver<PostKey?, Any>(save = { key -> key?.let { listOf(it.siteId, it.postId) } ?: emptyList() }, restore = { if (it.isEmpty()) null else PostKey(it[0] as String, it[1] as Long) })) { mutableStateOf<PostKey?>(null) }
        var viewerSaved by rememberSaveable { mutableStateOf(false) }
        var actionPost by remember { mutableStateOf<BooruPost?>(null) }
        var collectionPost by remember { mutableStateOf<BooruPost?>(null) }
        var filters by remember { mutableStateOf(false) }
        var kinds by remember { mutableStateOf(MediaKind.entries.toSet()) }
        var pendingDownload by remember { mutableStateOf<BooruPost?>(null) }
        val storage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
            val post = pendingDownload; pendingDownload = null
            if (allowed && post != null) model.download(post)
            else scope.launch { snackbar.showSnackbar(t(R.string.ui_storage_permission_is_required_on_this_android_version)) }
        }
        val download: (BooruPost) -> Unit = { post ->
            if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingDownload = post; storage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else model.download(post)
        }
        LaunchedEffect(notice) { notice?.let { model.consumeNotice(); snackbar.showSnackbar(it) } }
        val visibleFeed = pages.itemSnapshotList.items.filter { it.key.siteId == spec.source.name && it.rating in prefs!!.ratings && it.mediaKind in kinds }
        val visibleSaved = saved.filter { it.rating in prefs!!.ratings && it.mediaKind in kinds }
        val shimmieGrid = rememberLazyStaggeredGridState()
        val pahealGrid = rememberLazyStaggeredGridState()
        val ruleGrid = rememberLazyStaggeredGridState()
        val danGrid = rememberLazyStaggeredGridState()
        val grid = when(spec.source) { Source.SHIMMIE -> shimmieGrid; Source.PAHEAL -> pahealGrid; Source.RULE34 -> ruleGrid; Source.DANBOORU -> danGrid }
        val open: (BooruPost) -> Unit = { viewerSaved = tab == 1; viewerKey = it.key }
        val share: (BooruPost) -> Unit = { post ->
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, post.publicUrl) }, t(R.string.ui_share_post)))
        }
        val wide = LocalConfiguration.current.screenWidthDp >= 600
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            if (!wide && viewerKey == null) NavigationBar {
                listOf(t(R.string.ui_explore), t(R.string.ui_saved), t(R.string.ui_info)).forEachIndexed { i, label -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(navIcon(i), label) }, label = { Text(label) }) }
            }
        }) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (wide && viewerKey == null) NavigationRail {
                    listOf(t(R.string.ui_explore), t(R.string.ui_saved), t(R.string.ui_info)).forEachIndexed { i, label -> NavigationRailItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(navIcon(i), label) }, label = { Text(label) }) }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (viewerKey != null) {
                        DetailScreen(if (viewerSaved) model.savedViewerPosts.ifEmpty { visibleSaved } else visibleFeed, viewerKey!!, savedKeys,
                            onClose = { viewerKey = null }, onSave = model::toggle, onCollect = { collectionPost = it }, onDownload = download, onShare = share,
                            onTag = { source, tag -> viewerKey = null; tab = 0; model.source(source); model.search(tag)
                                scope.launch { when (source) { Source.SHIMMIE -> shimmieGrid; Source.PAHEAL -> pahealGrid; Source.RULE34 -> ruleGrid; Source.DANBOORU -> danGrid }.scrollToItem(0) } },
                            onLoadMore = { if (!viewerSaved && pages.itemCount > 0) pages[pages.itemCount - 1] }, onSelected = { viewerKey = it })
                    } else when (tab) {
                        0 -> ExploreScreen(model, spec, prefs!!, pages, grid, savedKeys, kinds, open, { actionPost = it }, model::toggle,
                            { filters = true }, { model.search(it); scope.launch { grid.scrollToItem(0) } })
                        1 -> SavedScreen(visibleSaved, savedKeys, collections, memberships, { post, list -> model.savedViewerPosts = list; open(post) }, { actionPost = it }, model::toggle, model::collection, model::deleteCollection)
                        else -> InfoScreen(model = model,
                            onSource = { model.source(it); tab = 0 },
                            onDownloads = { context.startActivity(Intent(context, com.leninasto.ruleapp.SettingsActivity::class.java).putExtra("source", spec.source.name).putExtra("downloads", true)) },
                            onSettings = { context.startActivity(Intent(context, com.leninasto.ruleapp.SettingsActivity::class.java).putExtra("source", spec.source.name)) })
                    }
                }
            }
        }
        if (filters) AppBottomSheet(onDismissRequest = { filters = false }) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(t(R.string.ui_your_gallery_your_way), style = MaterialTheme.typography.headlineSmall)
                Text(t(R.string.ui_sort_on_s, spec.source.label), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { spec.source.capabilities.orders.forEach { order -> FilterChip(selected = spec.order == order, onClick = { model.order(order); scope.launch { grid.scrollToItem(0) } }, label = { Text(order.uiLabel()) }) } }
                Text(t(R.string.ui_file_type_local_filter))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MediaKind.entries.forEach { kind -> FilterChip(selected = kind in kinds, onClick = { kinds = if (kind in kinds) kinds - kind else kinds + kind }, label = { Text(kind.uiLabel()) }) } }
                RatingChoices(prefs!!.ratings, model::ratings)
                Text(t(R.string.ui_local_filters_hide_cards_search_continues_using_the), style = MaterialTheme.typography.bodySmall)
                Button(onClick = { filters = false }) { Text(t(R.string.ui_done)) }
            }
        }
        actionPost?.let { post -> AppBottomSheet(onDismissRequest = { actionPost = null }) {
            Column(Modifier.padding(24.dp)) {
                Text(t(R.string.ui_post_s_486a92, post.key.postId), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { model.toggle(post); actionPost = null }) { Text(if (post.key in savedKeys) t(R.string.ui_remove_from_favorites) else t(R.string.ui_save_to_favorites)) }
                TextButton(onClick = { collectionPost = post; actionPost = null }) { Text(t(R.string.ui_add_to_collection)) }
                TextButton(enabled = post.originalUrl != null, onClick = { download(post); actionPost = null }) { Text(t(R.string.ui_download_original)) }
                TextButton(onClick = { share(post); actionPost = null }) { Text(t(R.string.ui_share_link)) }
            }
        } }
        collectionPost?.let { post -> CollectionPicker(post, collections, memberships, { collectionPost = null }, { model.collect(post, it) }, { model.uncollect(post, it) }, { model.collection(it, post) }) }
    }
}
private fun navIcon(i: Int) = when(i) { 0 -> Icons.Default.Explore; 1 -> Icons.Default.Bookmarks; else -> Icons.Default.Info }
