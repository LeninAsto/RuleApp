package com.leninasto.ruleapp

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class LocalizationTest {
    private fun resources(tag: String) = ApplicationProvider.getApplicationContext<Context>().let { context ->
        context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }).resources
    }
    @Test fun spanishAndEnglishStringsResolveWithoutChangingUserPreferences() {
        assertEquals("Bienvenido a Daparushi", resources("es").getString(R.string.ui_welcome_to_daparushi))
        assertEquals("Welcome to Daparushi", resources("en").getString(R.string.ui_welcome_to_daparushi))
        assertEquals("Downloads and storage", resources("en").getString(R.string.ui_downloads_and_storage))
    }
    @Test fun formattedAccessibilityTextPreservesArgumentsInBothLanguages() {
        assertEquals("Publicación 42, Imagen, General", resources("es").getString(R.string.post_accessibility_description, 42, "Imagen", "General"))
        assertEquals("Post 42, Image, General", resources("en").getString(R.string.post_accessibility_description, 42, "Image", "General"))
    }
}
