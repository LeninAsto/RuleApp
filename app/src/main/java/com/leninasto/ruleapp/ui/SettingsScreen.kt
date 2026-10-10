package com.leninasto.ruleapp.ui

import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.SingletonImageLoader
import com.leninasto.ruleapp.data.local.*
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Composable
internal fun SettingsScreen(model: AppViewModel, preferences: AppPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var credentialSource by remember { mutableStateOf<Source?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val rows by model.downloadRows.collectAsStateWithLifecycle()
    var statuses by remember { mutableStateOf(emptyList<DownloadStatus>()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(rows, owner) { owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        while (true) { statuses = model.container.downloads.statuses(rows); delay(1500) }
    } }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineLarge); Text("Haz espacio para lo que te gusta", style = MaterialTheme.typography.bodyMedium) }
        item { Text("Apariencia", style = MaterialTheme.typography.titleLarge); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { theme -> FilterChip(preferences.theme == theme, { model.theme(theme) }, label = { Text(theme.label) }) } } }
        item { ListItem(headlineContent = { Text("Color del sistema") }, supportingContent = { Text("Desde Android 12; tiene prioridad sobre el acento de la fuente.") }, trailingContent = { Switch(preferences.dynamicColor, model::dynamic, enabled = Build.VERSION.SDK_INT >= 31) }) }
        item { LockSettings(model.container.lock) }
        item { Text("Supervisión del dispositivo", style = MaterialTheme.typography.titleLarge); Text("Family Link puede bloquear esta app desde el dispositivo del supervisor. RuleApp no puede consultar de forma fiable si el dispositivo está supervisado. También respeta app_disabled si un administrador Android configura esa restricción.", style = MaterialTheme.typography.bodySmall) }
        item { RatingChoices(preferences.ratings, model::ratings) }
        item { Text("Fuentes y credenciales", style = MaterialTheme.typography.titleLarge) }
        items(Source.entries) { source -> Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(source.label, style = MaterialTheme.typography.titleMedium)
                Text(if (source.isShimmie) "Lectura pública mediante extensión XML." else if (source == Source.RULE34) "Introduce user_id y API key de tu cuenta." else "Lectura pública; login y API key opcionales según permisos.", style = MaterialTheme.typography.bodySmall)
                FlowRow {
                    if (!source.isShimmie) TextButton(onClick = { credentialSource = source }) { Text("Configurar") }
                    TextButton(onClick = { model.validate(source) }) { Text("Probar conexión") }
                    TextButton(onClick = { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(if (source == Source.RULE34) "https://rule34.xxx/index.php?page=account&s=options" else source.baseUrl)) }) { Text("Sitio web") }
                }
            }
        } }
        item { Text("Descargas", style = MaterialTheme.typography.titleLarge); if (statuses.isEmpty()) Text("Los originales se guardan en Downloads/RuleApp.") }
        items(statuses, key = { it.id }) { status -> ListItem(headlineContent = { Text(status.name) }, supportingContent = { Text(status.label + (status.progress?.let { " · $it %" } ?: "")) }) }
        item { Text("Privacidad y almacenamiento", style = MaterialTheme.typography.titleLarge) }
        item { TextButton(onClick = { scope.launch { withContext(Dispatchers.IO) { SingletonImageLoader.get(context).memoryCache?.clear(); SingletonImageLoader.get(context).diskCache?.clear() } } }) { Text("Borrar caché de imágenes") }
            TextButton(onClick = { model.clearHistory() }) { Text("Borrar historial") }
            TextButton(onClick = { model.clearCredentials() }) { Text("Quitar todas las credenciales") }
            TextButton(onClick = { confirmDelete = true }) { Text("Borrar guardados y datos locales") }
        }
        item { Text("RuleApp · cliente no oficial\nSin anuncios, sin backend y sin analítica. Las fuentes reciben tu IP y las búsquedas realizadas. Tus credenciales permanecen cifradas en este dispositivo y no se incluyen en backups.\nLos archivos descargados permanecen al borrar datos locales.", style = MaterialTheme.typography.bodySmall) }
    }
    credentialSource?.let { source -> CredentialDialog(source, model, { credentialSource = null }) }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Borrar datos locales") }, text = { Text("Se eliminarán favoritos, colecciones, historial y registro de descargas. Los archivos descargados y credenciales se conservan.") }, confirmButton = { TextButton(onClick = { model.clearLibrary(); confirmDelete = false }) { Text("Borrar") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } })
}
@Composable private fun CredentialDialog(source: Source, model: AppViewModel, onDismiss: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Cuenta de ${source.label}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Introduce una credencial personal. No se mostrará después de guardarla.")
            OutlinedTextField(user, { user = it.trim() }, label = { Text(if (source == Source.RULE34) "user_id" else "login") }, singleLine = true)
            OutlinedTextField(key, { key = it.trim() }, label = { Text("API key") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            TextButton(onClick = { model.clearCredentials(source); onDismiss() }) { Text("Quitar credenciales de esta fuente") }
        }
    }, confirmButton = { TextButton(enabled = user.isNotBlank() && key.isNotBlank() && (source != Source.RULE34 || user.toLongOrNull() != null), onClick = { model.credentials(source, user, key); key = ""; onDismiss() }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
