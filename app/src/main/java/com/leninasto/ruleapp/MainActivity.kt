package com.leninasto.ruleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leninasto.ruleapp.ui.AppViewModel
import com.leninasto.ruleapp.ui.NativeApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: AppViewModel = viewModel(factory = AppViewModel.factory((application as RuleApplication).container))
            NativeApp(model)
        }
    }
}
