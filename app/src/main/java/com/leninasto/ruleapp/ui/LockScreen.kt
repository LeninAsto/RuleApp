package com.leninasto.ruleapp.ui
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.t

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.leninasto.ruleapp.data.local.AppLock
import kotlinx.coroutines.launch

@Composable
fun LockScreen(lock: AppLock, restricted: Boolean) {
    var secret by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (restricted) t(R.string.ui_access_restricted) else t(R.string.ui_daparushi_locked), style = MaterialTheme.typography.headlineMedium)
        if (restricted) Text(t(R.string.ui_the_device_administrator_disabled_this_app)) else {
            Text(t(R.string.ui_enter_your_daparushi_s, if (lock.pin) t(R.string.ui_pin) else t(R.string.ui_password_8c31b6)))
            OutlinedTextField(secret, { secret = it.take(128); error = false }, singleLine = true, label = { Text(if (lock.pin) t(R.string.ui_pin) else t(R.string.ui_password)) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = if (lock.pin) KeyboardType.NumberPassword else KeyboardType.Password), isError = error)
            if (error) Text(t(R.string.ui_incorrect_secret_or_active_cooldown_after_five_failed))
            Button(enabled = !busy && secret.isNotEmpty(), onClick = { busy = true; scope.launch { try { error = !lock.unlock(secret); secret = "" } finally { busy = false } } }) { Text(if (busy) t(R.string.ui_checking) else t(R.string.ui_unlock)) }
        }
    }
}

@Composable
fun LockSettings(lock: AppLock) {
    val enabled by lock.enabled.collectAsState()
    var show by remember { mutableStateOf(false) }
    Text(t(R.string.ui_app_lock), style = MaterialTheme.typography.titleLarge)
    Text(if (enabled) t(R.string.ui_enabled_locks_when_you_leave_the_app) else t(R.string.ui_your_own_pin_or_password_without_biometrics))
    TextButton(onClick = { show = true }) { Text(if (enabled) t(R.string.ui_change_or_remove_lock) else t(R.string.ui_enable_lock)) }
    if (show) {
        var current by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
        var pin by remember { mutableStateOf(true) }; var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        fun submit(remove: Boolean) { busy = true; scope.launch {
            try { if (lock.configure(current, if (remove) null else next, pin)) show = false else error = t(R.string.ui_incorrect_current_secret_or_active_cooldown) }
            catch (_: Exception) { error = t(R.string.ui_the_lock_could_not_be_saved) } finally { busy = false }
        } }
        AlertDialog(onDismissRequest = { if (!busy) show = false }, title = { Text(t(R.string.ui_app_security_lock)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (enabled) OutlinedTextField(current, { current = it.take(128) }, label = { Text(t(R.string.ui_current_secret)) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Row { FilterChip(pin, { pin = true }, label = { Text(t(R.string.ui_pin)) }); Spacer(Modifier.width(8.dp)); FilterChip(!pin, { pin = false }, label = { Text(t(R.string.ui_password)) }) }
                Text(if (pin) t(R.string.ui_to_digits) else t(R.string.ui_to_characters))
                OutlinedTextField(next, { next = it.take(128) }, label = { Text(t(R.string.ui_new_secret)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = if(pin) KeyboardType.NumberPassword else KeyboardType.Password))
                OutlinedTextField(confirm, { confirm = it.take(128) }, label = { Text(t(R.string.ui_repeat_secret)) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Text(t(R.string.ui_this_secret_cannot_be_recovered_resetting_app_data), style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (enabled) TextButton(enabled = !busy && current.isNotEmpty(), onClick = { submit(true) }) { Text(t(R.string.ui_remove_lock)) }
            }
        }, confirmButton = { TextButton(enabled = !busy && next == confirm && (if(pin) next.matches(Regex("[0-9]{6,12}")) else next.length in 8..128) && (!enabled || current.isNotEmpty()), onClick = { submit(false) }) { Text(t(R.string.ui_save)) } }, dismissButton = { TextButton(enabled = !busy, onClick = { show = false }) { Text(t(R.string.ui_cancel)) } })
    }
}
