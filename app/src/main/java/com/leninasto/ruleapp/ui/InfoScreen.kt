package com.leninasto.ruleapp.ui
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leninasto.ruleapp.BuildConfig
import com.leninasto.ruleapp.domain.Source

internal data class AppRelease(val version: String, val title: Int, val changes: List<Int>)
internal val appReleases = listOf(
    AppRelease("0.9.1_beta", R.string.ui_more_comfortable_more_complete, listOf(
        R.string.ui_official_daparushi_name_and_a_pastel_brown_palette,
        R.string.ui_compact_cards_and_individual_tags_you_can_remove,
        R.string.ui_release_notes_once_per_version_and_update_alerts,
        R.string.ui_saved_last_source_and_visit_dates_guided_rule,
        R.string.ui_download_manager_and_storage_usage_overview,
        R.string.ui_owner_signed_releases_and_builds_for_each_architecture,
        R.string.ui_interface_available_in_spanish_and_english
    )),
    AppRelease("0.9_beta", R.string.ui_a_new_native_foundation, listOf(
        R.string.ui_native_gallery_with_shimmie_paheal_rule_xxx_and,
        R.string.ui_tag_search_image_gif_and_video_viewer_downloads,
        R.string.ui_suggestions_from_the_first_letter_remote_autocomplete_or,
        R.string.ui_material_source_accents_system_colors_and_wavy_progress,
        R.string.ui_app_pin_or_password_lock_encrypted_credentials_and,
        R.string.ui_welcome_release_history_info_and_settings_in_a,
        R.string.ui_beta_authenticated_rule_xxx_validation_is_still_pending
    )),
    AppRelease("0.5_beta", R.string.ui_the_beginning, listOf(R.string.ui_daparushi_s_first_stage_the_project_foundation_and))
)

@Composable
internal fun ReleaseHistory() {
    appReleases.forEach { release ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(release.version, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(t(release.title), style = MaterialTheme.typography.titleMedium)
                release.changes.forEach { Text(t(R.string.release_bullet, t(it)), style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
internal fun WelcomeScreen(onContinue: () -> Unit) {
    Scaffold(bottomBar = { Surface { Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { Text(t(R.string.ui_continue_to_daparushi)) } } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { com.leninasto.ruleapp.ui.components.AppLogo(88.dp) }
            item { Text(t(R.string.ui_welcome_to_daparushi), style = MaterialTheme.typography.headlineLarge); Text(t(R.string.ui_s_a_new_chapter_for_your_gallery, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge) }
            item { Text(t(R.string.ui_what_s_new_and_release_history), style = MaterialTheme.typography.titleLarge) }
            item { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { ReleaseHistory() } }
        }
    }
}

@Composable
internal fun InfoScreen(model: AppViewModel? = null, onDownloads: () -> Unit = {}, onSource: (Source) -> Unit = {}, onSettings: () -> Unit) {
    val context = LocalContext.current
    val open: (String) -> Unit = { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(it)) }
    val updates = model?.updateState?.collectAsStateWithLifecycle()?.value
    val prefs = model?.preferences?.collectAsStateWithLifecycle()?.value
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { com.leninasto.ruleapp.ui.components.AppLogo() }
        item { Text(t(R.string.ui_information), style = MaterialTheme.typography.headlineLarge); Text(t(R.string.app_version_label, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleMedium) }
        item { Text(t(R.string.ui_independent_android_gallery_client_favorites_and_collections_are)) }
        item { FilledTonalButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(8.dp)); Text(t(R.string.ui_open_settings)) } }
        if (model != null) {
            item { OutlinedButton(onClick = onDownloads, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text(t(R.string.ui_manage_downloads)) } }
            item { Card { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t(R.string.ui_updates), style = MaterialTheme.typography.titleMedium)
                updates?.message?.let { Text(t(it)) }
                if (updates?.available == true) Button(onClick = { open("https://github.com/LeninAsto/RuleApp/releases") }) { Text(t(R.string.ui_view_version_s, updates.latest)) }
                TextButton(enabled = updates?.checking != true, onClick = { model.checkUpdates(force = true) }) { Text(if (updates?.checking == true) t(R.string.ui_checking) else t(R.string.ui_check_for_updates)) }
                Text(t(R.string.ui_automatically_checked_every_hours_when_opening_the_app), style = MaterialTheme.typography.bodySmall)
            } } }
            item { Text(t(R.string.ui_available_sites), style = MaterialTheme.typography.titleLarge) }
            item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Source.entries.forEach { source ->
                OutlinedCard(onClick = { onSource(source) }, modifier = Modifier.fillMaxWidth()) { ListItem(headlineContent = { Text(source.label) }, supportingContent = { Text(lastVisitLabel(prefs?.visits?.get(source))) }, trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) }) }
            } } }
        }
        item { OutlinedButton(onClick = { open("https://github.com/LeninAsto") }, modifier = Modifier.fillMaxWidth()) { Text(t(R.string.ui_creator_s_github)) } }
        item { OutlinedButton(onClick = { open("https://github.com/LeninAsto/RuleApp") }, modifier = Modifier.fillMaxWidth()) { Text(t(R.string.ui_project_source_code)) } }
        item { Button(onClick = { open("https://buymeacoffee.com/lenin_anonimo_of") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Favorite, null); Spacer(Modifier.width(8.dp)); Text(t(R.string.ui_donate_buy_me_a_coffee)) }; Text(t(R.string.ui_optional_support_it_does_not_unlock_features), style = MaterialTheme.typography.bodySmall) }
        item { Text(t(R.string.ui_release_history), style = MaterialTheme.typography.titleLarge); Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { ReleaseHistory() } }
    }
}
