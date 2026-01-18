package com.example.heartaudiocheck

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class HeartAudioApp : Application() {

    override fun onCreate() {
        super.onCreate()
        applySavedLanguage()
    }

    private fun applySavedLanguage() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val code = prefs.getString("lang", "auto") ?: "auto"

        val locales = when (code) {
            "en" -> LocaleListCompat.forLanguageTags("en")
            "uk" -> LocaleListCompat.forLanguageTags("uk")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
