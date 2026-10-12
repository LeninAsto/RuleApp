package com.leninasto.ruleapp.core

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object AppLanguage {
    private fun prefs(context: Context) = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
    fun selected(context: Context) = prefs(context).getString("language", "es") ?: "es"
    fun tag(context: Context): String = selected(context).let { if (it == "system") Locale.getDefault().toLanguageTag() else it }
    fun wrap(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(tag(context)))
        return context.createConfigurationContext(configuration)
    }
    fun select(activity: Activity, language: String) {
        require(language in listOf("es", "en", "system"))
        check(prefs(activity).edit().putString("language", language).commit())
        AppStrings.invalidate()
        activity.recreate()
    }
}

/** Resource lookup also works in ViewModels and non-composable action callbacks. */
object AppStrings {
    private var context: Context? = null
    private var localized: Context? = null
    private var tag: String? = null
    fun initialize(value: Context) { if (context !== value) { context = value; invalidate() } }
    @Synchronized fun invalidate() { localized = null; tag = null }
    @Synchronized private fun resourcesContext(): Context {
        val original = checkNotNull(context) { "Application resources not initialized" }
        val language = AppLanguage.tag(original)
        if (localized == null || language != tag) { localized = AppLanguage.wrap(original); tag = language }
        return localized!!
    }
    fun get(id: Int, vararg args: Any?): String = resourcesContext().let { ctx -> if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args) }
    val locale: Locale get() = resourcesContext().resources.configuration.locales[0]
}
fun t(id: Int, vararg args: Any?): String = AppStrings.get(id, *args)
