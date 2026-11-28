package com.example.peliselprimazo.ui.utils

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

data class AppLanguage(
    val code: String,
    val displayName: String,
    val flagEmoji: String
)

object LanguageManager {

    val supportedLanguages = listOf(
        AppLanguage("es", "Español", "🇪🇸"),
        AppLanguage("en", "English", "🇺🇸"),
        AppLanguage("pt", "Português", "🇧🇷"),
        AppLanguage("fr", "Français", "🇫🇷"),
        AppLanguage("de", "Deutsch", "🇩🇪"),
        AppLanguage("it", "Italiano", "🇮🇹"),
        AppLanguage("ru", "Русский", "🇷🇺"),
        AppLanguage("zh", "中文", "🇨🇳"),
        AppLanguage("cs", "Čeština", "🇨🇿"),
        AppLanguage("ja", "日本語", "🇯🇵"),
        AppLanguage("ko", "한국어", "🇰🇷"),
        AppLanguage("tr", "Türkçe", "🇹🇷"),
        AppLanguage("ar", "العربية", "🇸🇦"),
        AppLanguage("hi", "हिन्दी", "🇮🇳"),
        AppLanguage("id", "Bahasa Indonesia", "🇮🇩"),
        AppLanguage("nl", "Nederlands", "🇳🇱"),
        AppLanguage("pl", "Polski", "🇵🇱"),
        AppLanguage("vi", "Tiếng Việt", "🇻🇳"),
        AppLanguage("th", "ไทย", "🇹🇭")
    )

    fun getCurrentLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences("cfilm_language_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("user_language_code", null)
        if (!saved.isNullOrEmpty()) return saved

        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val tag = appLocales.get(0)?.language
            if (!tag.isNullOrEmpty()) return tag
        }

        val systemLang = Locale.getDefault().language
        return if (supportedLanguages.any { it.code == systemLang }) systemLang else "es"
    }

    fun applyLocaleToContext(context: Context): Context {
        val langCode = getCurrentLanguageCode(context)
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun setAppLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences("cfilm_language_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_language_code", languageCode).apply()

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)

        if (context is Activity) {
            context.recreate()
        }
    }
}
