package com.leninasto.ruleapp.ui
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

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
internal fun SettingsScreen(model: AppViewModel, preferences: AppPreferences, onDownloads: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var credentialSource by remember { mutableStateOf<Source?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val exportBackup = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try { withContext(Dispatchers.IO) {
                val text = BackupCodec.export(model.container.library.dao)
                context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
            }; backupMessage = t(R.string.ui_backup_saved_it_does_not_contain_credentials_or) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { backupMessage = t(R.string.ui_we_could_not_export_the_library) }
        }
    }
    val importBackup = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try { withContext(Dispatchers.IO) {
                val text = context.contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).use { reader ->
                    val result = StringBuilder(); val buffer = CharArray(8192)
                    while (true) { val size = reader.read(buffer); if (size < 0) break; require(result.length + size <= 8 * 1024 * 1024); result.append(buffer, 0, size) }
                    result.toString()
                }
                BackupCodec.restore(model.container.library.dao, text)
            }; backupMessage = t(R.string.ui_library_restored_and_merged_with_your_existing_saved) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { backupMessage = t(R.string.ui_the_backup_is_invalid_or_unreadable_your_saved) }
        }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(t(R.string.ui_your_preferences), style = MaterialTheme.typography.headlineMedium); Text(t(R.string.ui_appearance_accounts_and_privacy), style = MaterialTheme.typography.bodyMedium) }
        item { LanguageChoices() }
        item { Text(t(R.string.ui_appearance), style = MaterialTheme.typography.titleLarge); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { theme -> FilterChip(preferences.theme == theme, { model.theme(theme) }, label = { Text(theme.uiLabel()) }) } } }
        item { ListItem(headlineContent = { Text(t(R.string.ui_system_colors)) }, supportingContent = { Text(t(R.string.ui_android_and_later_takes_priority_over_the_source)) }, trailingContent = { Switch(preferences.dynamicColor, model::dynamic, enabled = Build.VERSION.SDK_INT >= 31) }) }
        item { ListItem(headlineContent = { Text(t(R.string.ui_source_accent)) }, supportingContent = { Text(t(R.string.ui_off_daparushi_s_pastel_brown_system_colors_take)) }, trailingContent = { Switch(preferences.sourceAccent, model::sourceAccent, enabled = !preferences.dynamicColor) }) }
        item { ListItem(headlineContent = { Text(t(R.string.ui_compact_gallery)) }, supportingContent = { Text(t(R.string.ui_shorter_cards_for_less_scrolling)) }, trailingContent = { Switch(preferences.compactGallery, model::compact) }) }
        item { LockSettings(model.container.lock) }
        item { Text(t(R.string.ui_device_supervision), style = MaterialTheme.typography.titleLarge); Text(t(R.string.ui_family_link_can_block_this_app_from_the), style = MaterialTheme.typography.bodySmall) }
        item { RatingChoices(preferences.ratings, model::ratings) }
        item { Text(t(R.string.ui_sources_and_credentials), style = MaterialTheme.typography.titleLarge) }
        items(Source.entries) { source -> Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(source.label, style = MaterialTheme.typography.titleMedium)
                Text(if (source.isShimmie) t(R.string.ui_public_access_through_the_xml_extension) else if (source == Source.RULE34) t(R.string.ui_enter_your_account_s_user_id_and_api) else t(R.string.ui_public_access_optional_login_and_api_key_depending), style = MaterialTheme.typography.bodySmall)
                FlowRow {
                    if (!source.isShimmie) TextButton(onClick = { credentialSource = source }) { Text(t(R.string.ui_configure)) }
                    TextButton(onClick = { model.validate(source) }) { Text(t(R.string.ui_test_connection)) }
                    TextButton(onClick = { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(if (source == Source.RULE34) "https://rule34.xxx/index.php?page=account&s=options" else source.baseUrl)) }) { Text(t(R.string.ui_website)) }
                }
            }
        } }
        item { OutlinedButton(onClick = onDownloads, modifier = Modifier.fillMaxWidth()) { Text(t(R.string.ui_manage_downloads_and_storage)) } }
        item { Text(t(R.string.ui_backup), style = MaterialTheme.typography.titleLarge)
            Text(t(R.string.ui_export_favorites_and_collections_before_switching_from_the), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { exportBackup.launch("Daparushi-biblioteca.json") }) { Text(t(R.string.ui_export_library)) }
            TextButton(onClick = { importBackup.launch(arrayOf("application/json", "text/plain")) }) { Text(t(R.string.ui_restore_library)) }
            backupMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        item { Text(t(R.string.ui_privacy_and_storage), style = MaterialTheme.typography.titleLarge) }
        item { TextButton(onClick = { scope.launch { withContext(Dispatchers.IO) { SingletonImageLoader.get(context).memoryCache?.clear(); SingletonImageLoader.get(context).diskCache?.clear() } } }) { Text(t(R.string.ui_clear_image_cache)) }
            TextButton(onClick = { model.clearHistory() }) { Text(t(R.string.ui_clear_history)) }
            TextButton(onClick = { model.clearCredentials() }) { Text(t(R.string.ui_remove_all_credentials)) }
            TextButton(onClick = { confirmDelete = true }) { Text(t(R.string.ui_delete_saved_posts_and_local_data)) }
        }
        item { Text(t(R.string.ui_daparushi_unofficial_client_no_ads_backend_or_analytics), style = MaterialTheme.typography.bodySmall) }
    }
    credentialSource?.let { source -> CredentialDialog(source, model, { credentialSource = null }) }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(t(R.string.ui_delete_local_data)) }, text = { Text(t(R.string.ui_favorites_collections_history_and_download_records_will_be)) }, confirmButton = { TextButton(onClick = { model.clearLibrary(); confirmDelete = false }) { Text(t(R.string.ui_delete)) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(t(R.string.ui_cancel)) } })
}
@Composable private fun CredentialDialog(source: Source, model: AppViewModel, onDismiss: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var credentialError by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(t(R.string.ui_s_account, source.label)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t(R.string.ui_enter_a_personal_credential_it_will_not_be))
            credentialError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(user, { user = it.trim() }, label = { Text(if (source == Source.RULE34) t(R.string.ui_user_id) else t(R.string.ui_login)) }, singleLine = true)
            OutlinedTextField(key, { key = it.trim() }, label = { Text(t(R.string.ui_api_key)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password))
            TextButton(onClick = { model.clearCredentials(source); onDismiss() }) { Text(t(R.string.ui_remove_this_source_s_credentials)) }
        }
    }, confirmButton = { TextButton(enabled = !busy && user.isNotBlank() && key.isNotBlank() && (source != Source.RULE34 || user.toLongOrNull() != null), onClick = {
        if (source == Source.RULE34) scope.launch {
            busy = true; credentialError = null
            try { model.connectRule34(user, key); key = ""; onDismiss() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { credentialError = e.localizedError() }
            finally { busy = false }
        } else { model.credentials(source, user, key); key = ""; onDismiss() }
    }) { Text(if (busy) t(R.string.ui_checking) else t(R.string.ui_save)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(t(R.string.ui_cancel)) } })
}
