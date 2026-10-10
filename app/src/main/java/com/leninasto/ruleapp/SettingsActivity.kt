package com.leninasto.ruleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leninasto.ruleapp.domain.Source
import com.leninasto.ruleapp.ui.AppViewModel
import com.leninasto.ruleapp.ui.NativeApp

/** Separate, internal settings destination with the same lock and supervision gate. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: AppViewModel = viewModel(factory = AppViewModel.factory((application as RuleApplication).container))
            androidx.compose.runtime.LaunchedEffect(Unit) {
                intent.getStringExtra("source")?.let { name -> Source.entries.find { it.name == name }?.let(model::source) }
            }
            NativeApp(model, settingsOnly = true, onSettingsBack = { finish() })
        }
    }
}
