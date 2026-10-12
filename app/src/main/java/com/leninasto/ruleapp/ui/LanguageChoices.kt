package com.leninasto.ruleapp.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leninasto.ruleapp.R
import com.leninasto.ruleapp.core.AppLanguage
import com.leninasto.ruleapp.core.t

@Composable
internal fun LanguageChoices() {
    val context = LocalContext.current
    val language = AppLanguage.selected(context)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t(R.string.ui_language), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("es" to "Español", "en" to "English", "system" to t(R.string.ui_system_language)).forEach { (tag, name) ->
                FilterChip(selected = language == tag, onClick = { (context as? Activity)?.let { AppLanguage.select(it, tag) } }, label = { Text(name) })
            }
        }
        Text(t(R.string.ui_spanish_and_english_are_available_your_selection_is), style = MaterialTheme.typography.bodySmall)
    }
}
