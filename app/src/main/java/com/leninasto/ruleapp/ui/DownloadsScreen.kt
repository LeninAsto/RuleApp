package com.leninasto.ruleapp.ui
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.leninasto.ruleapp.data.local.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun DownloadsScreen(model: AppViewModel) {
    val rows by model.downloadRows.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statuses by remember { mutableStateOf(emptyList<DownloadStatus>()) }
    var storage by remember { mutableStateOf<StorageUsage?>(null) }
    var filter by remember { mutableStateOf(t(R.string.ui_all)) }
    var deletion by remember { mutableStateOf<DownloadStatus?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(rows, owner) { owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        var tick = 0
        while (true) {
            statuses = model.container.downloads.statuses(rows)
            if (tick++ % 6 == 0) storage = StorageMeter.read(context, statuses)
            delay(1500)
        }
    } }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(t(R.string.ui_downloads_and_storage), style = MaterialTheme.typography.headlineSmall) }
        item { storage?.let { StorageCard(it) } }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(t(R.string.ui_all), t(R.string.ui_active), t(R.string.ui_completed), t(R.string.ui_failed)).forEach { choice -> FilterChip(filter == choice, { filter = choice }, label = { Text(choice) }) }
        } }
        item { message?.let { Text(it, color = MaterialTheme.colorScheme.error) }; if (statuses.isEmpty()) Text(t(R.string.ui_no_downloads_yet_originals_are_saved_in_downloads)) }
        items(statuses.filter { when (filter) { t(R.string.ui_active) -> it.active; t(R.string.ui_completed) -> it.complete; t(R.string.ui_failed) -> it.failed; else -> true } }, key = { it.id }) { row ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.name, style = MaterialTheme.typography.titleSmall)
                Text(t(R.string.download_status_and_bytes, row.label, android.text.format.Formatter.formatFileSize(context, row.bytes)) + (row.total?.let { t(R.string.download_total_suffix, android.text.format.Formatter.formatFileSize(context, it)) } ?: ""))
                if (row.active) row.progress?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth()) }
                FlowRow {
                    if (row.complete) TextButton(onClick = { try { model.container.downloads.openIntent(row.id)?.let(context::startActivity) ?: run { message = t(R.string.ui_the_file_is_no_longer_available) } } catch (_: android.content.ActivityNotFoundException) { message = t(R.string.ui_no_app_is_available_to_open_this_format) } }) { Text(t(R.string.ui_open)) }
                    if (row.failed) TextButton(onClick = { scope.launch { try { model.container.downloads.retry(row.id) } catch (_: Exception) { message = t(R.string.ui_we_could_not_restart_the_download) } } }) { Text(t(R.string.ui_retry)) }
                    TextButton(onClick = { deletion = row }) { Text(if (row.active) t(R.string.ui_cancel) else if (row.complete) t(R.string.ui_delete_file) else t(R.string.ui_remove_record)) }
                }
            } }
        }
    }
    deletion?.let { row -> AlertDialog(onDismissRequest = { deletion = null }, title = { Text(if (row.active) t(R.string.ui_cancel_download) else t(R.string.ui_remove_download)) },
        text = { Text(t(R.string.ui_this_download_and_its_file_if_present_will)) },
        confirmButton = { TextButton(onClick = { scope.launch { model.container.downloads.remove(row.id) }; deletion = null }) { Text(t(R.string.ui_confirm)) } },
        dismissButton = { TextButton(onClick = { deletion = null }) { Text(t(R.string.ui_back)) } }) }
}

@Composable
internal fun StorageCard(usage: StorageUsage) {
    val context = LocalContext.current
    fun bytes(value: Long) = android.text.format.Formatter.formatFileSize(context, value)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t(R.string.ui_device_storage), style = MaterialTheme.typography.titleMedium)
        Text(t(R.string.ui_s_used_of_s_s_available, bytes(usage.used), bytes(usage.total), bytes(usage.available)))
        LinearProgressIndicator(progress = { if (usage.total > 0) (usage.used.toDouble() / usage.total).toFloat() else 0f }, modifier = Modifier.fillMaxWidth())
        Text(t(R.string.ui_daparushi_data_s_cache_s_tracked_downloads_s, bytes(usage.appData), bytes(usage.cache), bytes(usage.trackedDownloads)), style = MaterialTheme.typography.bodySmall)
        Text(t(R.string.ui_the_total_is_the_internal_storage_volume_download), style = MaterialTheme.typography.labelSmall)
    } }
}
