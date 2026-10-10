package com.leninasto.ruleapp.ui

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
        Text(if (restricted) "Acceso restringido" else "RuleApp bloqueada", style = MaterialTheme.typography.headlineMedium)
        if (restricted) Text("El administrador del dispositivo deshabilitó esta app.") else {
            Text("Introduce tu ${if (lock.pin) "PIN" else "contraseña"} de RuleApp.")
            OutlinedTextField(secret, { secret = it.take(128); error = false }, singleLine = true, label = { Text(if (lock.pin) "PIN" else "Contraseña") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = if (lock.pin) KeyboardType.NumberPassword else KeyboardType.Password), isError = error)
            if (error) Text("Clave incorrecta o espera activa. Tras cinco fallos, espera al menos 30 segundos.")
            Button(enabled = !busy && secret.isNotEmpty(), onClick = { busy = true; scope.launch { try { error = !lock.unlock(secret); secret = "" } finally { busy = false } } }) { Text(if (busy) "Comprobando…" else "Desbloquear") }
        }
    }
}

@Composable
fun LockSettings(lock: AppLock) {
    val enabled by lock.enabled.collectAsState()
    var show by remember { mutableStateOf(false) }
    Text("Bloqueo de la app", style = MaterialTheme.typography.titleLarge)
    Text(if (enabled) "Activo: se bloquea al salir de la app." else "PIN o contraseña propios, sin biometría.")
    TextButton(onClick = { show = true }) { Text(if (enabled) "Cambiar o quitar bloqueo" else "Activar bloqueo") }
    if (show) {
        var current by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
        var pin by remember { mutableStateOf(true) }; var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        fun submit(remove: Boolean) { busy = true; scope.launch {
            try { if (lock.configure(current, if (remove) null else next, pin)) show = false else error = "Clave actual incorrecta o espera activa." }
            catch (_: Exception) { error = "No se pudo guardar el bloqueo." } finally { busy = false }
        } }
        AlertDialog(onDismissRequest = { if (!busy) show = false }, title = { Text("Bloqueo propio") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (enabled) OutlinedTextField(current, { current = it.take(128) }, label = { Text("Clave actual") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Row { FilterChip(pin, { pin = true }, label = { Text("PIN") }); Spacer(Modifier.width(8.dp)); FilterChip(!pin, { pin = false }, label = { Text("Contraseña") }) }
                Text(if (pin) "De 6 a 12 dígitos." else "De 8 a 128 caracteres.")
                OutlinedTextField(next, { next = it.take(128) }, label = { Text("Nueva clave") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = if(pin) KeyboardType.NumberPassword else KeyboardType.Password))
                OutlinedTextField(confirm, { confirm = it.take(128) }, label = { Text("Repetir clave") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Text("No hay recuperación de esta clave. Restablecer los datos de la app elimina también tus guardados.", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (enabled) TextButton(enabled = !busy && current.isNotEmpty(), onClick = { submit(true) }) { Text("Quitar bloqueo") }
            }
        }, confirmButton = { TextButton(enabled = !busy && next == confirm && (if(pin) next.matches(Regex("[0-9]{6,12}")) else next.length in 8..128) && (!enabled || current.isNotEmpty()), onClick = { submit(false) }) { Text("Guardar") } }, dismissButton = { TextButton(enabled = !busy, onClick = { show = false }) { Text("Cancelar") } })
    }
}
