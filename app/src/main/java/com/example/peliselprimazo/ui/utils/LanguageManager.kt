package com.example.peliselprimazo.ui.utils

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
        AppLanguage("cs", "Čeština", "🇨🇿")
    )

    fun getCurrentLanguageCode(context: Context): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val tag = appLocales.get(0)?.language
            if (!tag.isNullOrEmpty()) return tag
        }
        val prefs = context.getSharedPreferences("cfilm_language_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("user_language_code", null)
        if (!saved.isNullOrEmpty()) return saved
        val systemLang = Locale.getDefault().language
        return if (supportedLanguages.any { it.code == systemLang }) systemLang else "es"
    }

    fun setAppLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences("cfilm_language_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_language_code", languageCode).apply()

        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
