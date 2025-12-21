package com.leninasto.ruleapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.leninasto.ruleapp.ui.theme.RuleAppTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme
import android.os.Build
import android.webkit.WebSettings

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

                val menuItems = listOf(
                    "Main" to "https://shimmie.shishnet.org/",
                    "Post" to "https://shimmie.shishnet.org/post/list",
                    "Comments" to "https://shimmie.shishnet.org/comment/list",
                    "Tags" to "https://shimmie.shishnet.org/tags/map",
                    "Wiki" to "https://shimmie.shishnet.org/wiki",
                    "Help" to "https://shimmie.shishnet.org/help"
                )

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = false, // Desactiva la apertura por gestos
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(12.dp))
                            Text("Shishnet Menu", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                            menuItems.forEach { (label, url) ->
                                NavigationDrawerItem(
                                    label = { Text(label) },
                                    selected = false,
                                    onClick = {
                                        webView?.loadUrl(url)
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
                        ShimmieWebView(
                            url = "https://shimmie.shishnet.org/post/list",
                            modifier = Modifier.padding(innerPadding),
                            onWebViewCreated = { webView = it },
                            onTitleChanged = { title -> pageTitle = title }
                        )
                    }
                }
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
    onTitleChanged: (String) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onTitleChanged(view?.title ?: "Rule App")

                        val viewportContent = when {
                            // En la vista de post, usamos escala 1.0 para que se vea nítido
                            url?.contains("/post/view") == true -> "width=475, initial-scale=0.7"
                            url?.contains("/post/list") == true -> "width=475, initial-scale=0.8"
                            url?.contains("/wiki") == true -> "width=device-width, initial-scale=1.0"
                            else -> "width=device-width, initial-scale=1.0"
                        }

                        view?.evaluateJavascript("""
                            (function() {
                                // 1. Inyección de Viewport
                                var meta = document.querySelector('meta[name="viewport"]') || document.createElement('meta');
                                meta.name = 'viewport';
                                meta.content = '$viewportContent, maximum-scale=10.0, user-scalable=yes';
                                if (!meta.parentNode) document.getElementsByTagName('head')[0].appendChild(meta);

                                // 2. Inyección de CSS para contener imágenes y evitar desbordamiento
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
                                    #right_col { display: none !important; } /* Opcional: oculta barras laterales de Shimmie */
                                `;
                                if (!style.parentNode) document.head.appendChild(style);
                            })();
                        """.trimIndent(), null)
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)// Cambia esto a true para permitir el gesto de pinza (pinch-to-zoom)
                    builtInZoomControls = true
                    displayZoomControls = false

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        isAlgorithmicDarkeningAllowed = isDarkTheme
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Para Android 10 a 12
                        @Suppress("DEPRECATION")
                        forceDark = if (isDarkTheme) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
                    }
                }
                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            // Actualizamos el modo oscuro si cambia el tema del sistema sin recrear el WebView
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                webView.settings.isAlgorithmicDarkeningAllowed = isDarkTheme
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                webView.settings.forceDark = if (isDarkTheme) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
            }
        },
        modifier = modifier.fillMaxSize()
    )
}