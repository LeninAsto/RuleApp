package com.leninasto.ruleapp

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
import androidx.webkit.WebViewFeature
import com.leninasto.ruleapp.ui.theme.RuleAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RuleAppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var webView by remember { mutableStateOf<WebView?>(null) }
                var searchText by remember { mutableStateOf("") }
                var isSearchActive by remember { mutableStateOf(false) }
                var pageTitle by remember { mutableStateOf("Loading...") }
                var currentUrl by remember { mutableStateOf("") }
                val context = LocalContext.current

                val isPostView = currentUrl.contains("/post/view/")
                val isListView = currentUrl.contains("/post/list")

                val currentPostId = remember(currentUrl) {
                    currentUrl.substringAfter("/post/view/", "").substringBefore("/").substringBefore("?").toIntOrNull()
                }

                val listPagination = remember(currentUrl) {
                    if (isListView) {
                        if (currentUrl.endsWith("/post/list") || currentUrl.endsWith("/post/list/")) {
                            Pair("", 1)
                        } else {
                            val path = currentUrl.substringAfter("/post/list/")
                            val parts = path.split("/").filter { it.isNotEmpty() }
                            if (parts.isEmpty()) {
                                Pair("", 1)
                            } else {
                                val lastAsInt = parts.last().toIntOrNull()
                                if (lastAsInt != null) {
                                    Pair(parts.dropLast(1).joinToString("/"), lastAsInt)
                                } else {
                                    Pair(parts.joinToString("/"), 1)
                                }
                            }
                        }
                    } else null
                }

                BackHandler(enabled = true) {
                    if (isSearchActive) {
                        isSearchActive = false
                    } else if (drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                    } else if (webView?.canGoBack() == true) {
                        webView?.goBack()
                    } else {
                        finish()
                    }
                }

                data class MenuItem(val label: String, val url: String, val icon: ImageVector)
                val menuItems = listOf(
                    MenuItem("Main", "https://shimmie.shishnet.org/", Icons.Default.Home),
                    MenuItem("Post", "https://shimmie.shishnet.org/post/list", Icons.Default.PhotoLibrary),
                    MenuItem("Comments", "https://shimmie.shishnet.org/comment/list", Icons.AutoMirrored.Filled.Chat),
                    MenuItem("Tags", "https://shimmie.shishnet.org/tags/map", Icons.Default.LocalOffer),
                    MenuItem("Wiki", "https://shimmie.shishnet.org/wiki", Icons.Default.Info),
                    MenuItem("Help", "https://shimmie.shishnet.org/help", Icons.AutoMirrored.Filled.Help)
                )

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = false,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Text("Shishnet Menu", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                            menuItems.forEach { item ->
                                val isSelected = when (item.label) {
                                    "Main" -> currentUrl == "https://shimmie.shishnet.org/" || currentUrl == "https://shimmie.shishnet.org/index"
                                    "Post" -> currentUrl.contains("/post/")
                                    "Comments" -> currentUrl.contains("/comment/")
                                    "Tags" -> currentUrl.contains("/tags/")
                                    "Wiki" -> currentUrl.contains("/wiki")
                                    "Help" -> currentUrl.contains("/help")
                                    else -> false
                                }
                                NavigationDrawerItem(
                                    label = { Text(item.label) },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    selected = isSelected,
                                    onClick = {
                                        webView?.loadUrl(item.url)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                navigationIcon = {
                                    if (isSearchActive) {
                                        IconButton(onClick = { isSearchActive = false }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                                        }
                                    } else {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    }
                                },
                                title = {
                                    if (isSearchActive) {
                                        TextField(
                                            value = searchText,
                                            onValueChange = { searchText = it },
                                            placeholder = { Text("Search tags...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                            ),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(
                                                onSearch = {
                                                    if (searchText.isNotBlank()) {
                                                        webView?.loadUrl("https://shimmie.shishnet.org/post/list/$searchText/1")
                                                        isSearchActive = false
                                                    }
                                                }
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = pageTitle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                actions = {
                                    if (!isSearchActive) {
                                        if (isListView && listPagination != null) {
                                            val (tags, page) = listPagination
                                            IconButton(
                                                enabled = page > 1,
                                                onClick = {
                                                    val prevPage = page - 1
                                                    val url = "https://shimmie.shishnet.org/post/list/${if (tags.isNotEmpty()) "$tags/" else ""}$prevPage"
                                                    webView?.loadUrl(url)
                                                }
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Page")
                                            }
                                            IconButton(
                                                onClick = {
                                                    val nextPage = page + 1
                                                    val url = "https://shimmie.shishnet.org/post/list/${if (tags.isNotEmpty()) "$tags/" else ""}$nextPage"
                                                    webView?.loadUrl(url)
                                                }
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                                            }
                                        }
                                        IconButton(onClick = { isSearchActive = true }) {
                                            Icon(Icons.Default.Search, contentDescription = "Search")
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                                )
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            ShimmieWebView(
                                url = "https://shimmie.shishnet.org/post/list",
                                onWebViewCreated = { webView = it },
                                onTitleChanged = { title -> pageTitle = title },
                                onUrlChanged = { url -> currentUrl = url }
                            )

                            // Floating Toolbar for Post View
                            AnimatedVisibility(
                                visible = isPostView && currentPostId != null,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 24.dp)
                            ) {
                                Surface(
                                    tonalElevation = 8.dp,
                                    shadowElevation = 4.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(onClick = {
                                            currentPostId?.let {
                                                webView?.loadUrl("https://shimmie.shishnet.org/post/view/${it + 1}")
                                            }
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Post")
                                        }

                                        IconButton(onClick = {
                                            downloadCurrentPost(webView, context)
                                        }) {
                                            Icon(Icons.Default.Download, contentDescription = "Download")
                                        }

                                        IconButton(onClick = {
                                            currentPostId?.let {
                                                webView?.loadUrl("https://shimmie.shishnet.org/post/view/${it - 1}")
                                            }
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Post")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun downloadCurrentPost(webView: WebView?, context: Context) {
        webView?.evaluateJavascript(
            "(function() { " +
                    "var img = document.querySelector('#main_image'); " +
                    "if (img) return img.src; " +
                    "var video = document.querySelector('video source'); " +
                    "if (video) return video.src; " +
                    "var videoDirect = document.querySelector('video'); " +
                    "if (videoDirect) return videoDirect.src; " +
                    "return null; " +
                    "})()"
        ) { url ->
            val cleanUrl = url?.replace("\"", "")
            if (!cleanUrl.isNullOrBlank() && cleanUrl != "null") {
                try {
                    val fileName = cleanUrl.substringAfterLast("/")
                    val request = DownloadManager.Request(Uri.parse(cleanUrl))
                        .setTitle("Downloading Post")
                        .setDescription("Downloading $fileName to Shimmie folder")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Shimmie/$fileName")
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)

                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    downloadManager.enqueue(request)
                    Toast.makeText(context, "Download started to Shimmie folder", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Could not find media to download", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ShimmieWebView(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // Configuración de Cookies (como en Chrome)
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val newUrl = request?.url?.toString() ?: return false
                        if (!newUrl.startsWith("https://shimmie.shishnet.org/")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newUrl))
                            context.startActivity(intent)
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        url?.let { onUrlChanged(it) }
                        onTitleChanged(view?.title ?: "Rule App")

                        val viewportContent = when {
                            url?.contains("/post/view") == true -> "width=475, initial-scale=0.7"
                            url?.contains("/post/list") == true -> "width=475, initial-scale=0.8"
                            url?.contains("/wiki") == true -> "width=device-width, initial-scale=1.0"
                            else -> "width=device-width, initial-scale=1.0"
                        }

                        view?.evaluateJavascript("""
                            (function() {
                                var meta = document.querySelector('meta[name="viewport"]') || document.createElement('meta');
                                meta.name = 'viewport';
                                meta.content = '$viewportContent, maximum-scale=10.0, user-scalable=yes';
                                if (!meta.parentNode) document.getElementsByTagName('head')[0].appendChild(meta);

                                var style = document.getElementById('app-fix-style') || document.createElement('style');
                                style.id = 'app-fix-style';
                                style.innerHTML = `
                                    img, video, canvas { 
                                        max-width: 100% !important; 
                                        height: auto !important; 
                                    }
                                    body { 
                                        overflow-x: hidden !important; 
                                        width: 100vw !important;
                                        margin: 0 !important;
                                        padding: 0 !important;
                                    }
                                    #right_col { display: none !important; }
                                `;
                                if (!style.parentNode) document.head.appendChild(style);
                            })();
                        """.trimIndent(), null)
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        url?.let { onUrlChanged(it) }
                    }
                }
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true // Permite guardar datos locales del sitio
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }

                applyDarkMode(this, isDarkTheme)
                
                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            applyDarkMode(webView, isDarkTheme)
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun applyDarkMode(webView: WebView, isDarkTheme: Boolean) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        // Modo automático: Android oscurece si no hay tema web detectado
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, isDarkTheme)
    } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        @Suppress("DEPRECATION")
        WebSettingsCompat.setForceDark(webView.settings, if (isDarkTheme) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF)
    }
    
    // Preferir el tema propio de la página si existe
    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
        WebSettingsCompat.setForceDarkStrategy(webView.settings, DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING)
    }
}
