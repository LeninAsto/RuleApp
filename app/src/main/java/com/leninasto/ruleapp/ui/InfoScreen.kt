package com.leninasto.ruleapp.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

internal data class AppRelease(val version: String, val title: String, val changes: List<String>)
internal val appReleases = listOf(
    AppRelease("0.9_beta", "Una nueva base nativa", listOf(
        "Galería nativa con Shimmie2, Paheal, Rule34.xxx y Danbooru.",
        "Búsqueda por tags, visor de imágenes, GIF y video, descargas y colecciones locales.",
        "Sugerencias desde la primera letra: autocomplete remoto o tags locales según la fuente.",
        "Material 3: acento por fuente, colores del sistema e indicadores ondulados.",
        "Bloqueo propio con PIN o contraseña, credenciales cifradas y restricciones administradas.",
        "Bienvenida, historial de versiones, Información y Ajustes en una actividad propia.",
        "Beta: la validación autenticada de Rule34.xxx sigue pendiente."
    )),
    AppRelease("0.5_beta", "El comienzo", listOf("Primera etapa de RuleApp: la base del proyecto y la experiencia inicial con páginas web."))
)

@Composable
internal fun ReleaseHistory() {
    appReleases.forEach { release ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(release.version, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(release.title, style = MaterialTheme.typography.titleMedium)
                release.changes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
internal fun WelcomeScreen(onContinue: () -> Unit) {
    Scaffold(bottomBar = { Surface { Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { Text("Continuar a RuleApp") } } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) }
            item { Text("Bienvenido a RuleApp", style = MaterialTheme.typography.headlineLarge); Text("0.9_beta · Un nuevo capítulo para tu galería.", style = MaterialTheme.typography.bodyLarge) }
            item { Text("Novedades e historial", style = MaterialTheme.typography.titleLarge) }
            item { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { ReleaseHistory() } }
        }
    }
}

@Composable
internal fun InfoScreen(onSettings: () -> Unit) {
    val context = LocalContext.current
    val open: (String) -> Unit = { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(it)) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Información", style = MaterialTheme.typography.headlineLarge); Text("RuleApp · 0.9_beta", style = MaterialTheme.typography.titleMedium) }
        item { Text("Cliente Android independiente para explorar galerías. Tus favoritos y colecciones se guardan en este dispositivo. Sin anuncios ni backend propio. RuleApp no está afiliada a las fuentes.") }
        item { FilledTonalButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(8.dp)); Text("Abrir Ajustes") } }
        item { OutlinedButton(onClick = { open("https://github.com/LeninAsto") }, modifier = Modifier.fillMaxWidth()) { Text("GitHub del creador") } }
        item { OutlinedButton(onClick = { open("https://github.com/LeninAsto/RuleApp") }, modifier = Modifier.fillMaxWidth()) { Text("Código del proyecto") } }
        item { Button(onClick = { open("https://buymeacoffee.com/lenin_anonimo_of") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Favorite, null); Spacer(Modifier.width(8.dp)); Text("Donar · Buy Me a Coffee") }; Text("Apoyo voluntario; no desbloquea funciones.", style = MaterialTheme.typography.bodySmall) }
        item { Text("Historial de versiones", style = MaterialTheme.typography.titleLarge); Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { ReleaseHistory() } }
    }
}
