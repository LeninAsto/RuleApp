package com.leninasto.ruleapp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leninasto.ruleapp.data.local.AppPreferences
import com.leninasto.ruleapp.ui.InfoScreen
import com.leninasto.ruleapp.ui.WelcomeScreen
import com.leninasto.ruleapp.ui.components.AppTheme
import com.leninasto.ruleapp.ui.components.AppLoadingIndicator
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun welcomeShowsCurrentReleaseAndAllowsContinue() {
        var continued = false
        compose.setContent { AppTheme(AppPreferences()) { WelcomeScreen { continued = true } } }
        compose.onNodeWithText("Bienvenido a RuleApp").assertIsDisplayed()
        compose.onNodeWithText("0.9_beta").assertIsDisplayed()
        compose.onNodeWithText("Continuar a RuleApp").performClick()
        compose.runOnIdle { assertTrue(continued) }
    }
    @Test fun infoProvidesSettingsAndProjectLinks() {
        var opened = false
        compose.setContent { AppTheme(AppPreferences()) { InfoScreen { opened = true } } }
        compose.onNodeWithText("GitHub del creador").assertIsDisplayed()
        compose.onNodeWithText("Código del proyecto").assertIsDisplayed()
        compose.onNodeWithText("Abrir Ajustes").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
    @Test fun officialWavyIndicatorCanBeHostedInCompose() {
        compose.setContent { AppTheme(AppPreferences()) { AppLoadingIndicator() } }
        compose.waitForIdle()
        compose.runOnIdle {
            val content = compose.activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            fun containsWave(view: android.view.View): Boolean = when (view) {
                is com.google.android.material.progressindicator.CircularProgressIndicator -> view.waveAmplitude > 0 && view.isIndeterminate
                is android.view.ViewGroup -> (0 until view.childCount).any { containsWave(view.getChildAt(it)) }
                else -> false
            }
            assertTrue(containsWave(content))
        }
    }
}
