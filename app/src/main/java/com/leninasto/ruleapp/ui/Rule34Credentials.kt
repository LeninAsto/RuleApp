package com.leninasto.ruleapp.ui
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.leninasto.ruleapp.domain.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun Rule34Credentials(model: AppViewModel) {
    var user by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(t(R.string.ui_connect_your_rule_account), style = MaterialTheme.typography.headlineSmall)
        Text(t(R.string.ui_this_source_requires_your_user_id_and_api))
        OutlinedTextField(user, { user = it.filter(Char::isDigit) }, enabled = !busy, label = { Text(t(R.string.ui_user_id)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(key, { key = it.trim() }, enabled = !busy, label = { Text(t(R.string.ui_api_key)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !busy && user.toLongOrNull()?.let { it > 0 } == true && key.isNotBlank(), onClick = {
            scope.launch { busy = true; error = null
                try { model.connectRule34(user, key); key = "" }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.localizedError() }
                finally { busy = false }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) t(R.string.ui_checking_connection) else t(R.string.ui_connect_and_load_gallery)) }
        Text(t(R.string.ui_how_to_get_your_credentials), style = MaterialTheme.typography.titleMedium)
        Text(t(R.string.ui_sign_in_to_rule_xxx_in_your_browser))
        TextButton(onClick = { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse("https://rule34.xxx/index.php?page=account&s=options")) }) { Text(t(R.string.ui_open_my_account_options)) }
        TextButton(onClick = { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse("https://api.rule34.xxx/")) }) { Text(t(R.string.ui_view_official_documentation)) }
        Text(t(R.string.ui_if_the_website_shows_a_cloudflare_challenge_complete), style = MaterialTheme.typography.bodySmall)
    }
}

internal fun lastVisitLabel(timestamp: Long?): String = timestamp?.let {
    t(R.string.ui_last_visit) + java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, com.leninasto.ruleapp.core.AppStrings.locale).format(java.util.Date(it))
} ?: t(R.string.ui_not_visited_yet)
