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

        // Migration: if an older build saved "auto", treat it as not set
        val saved = prefs.getString("lang", null)
        val code = if (saved.isNullOrBlank() || saved == "auto") {
            val sys = java.util.Locale.getDefault().language.lowercase()
            val picked = when (sys) {
                "uk" -> "uk"
                "de" -> "de"
                else -> "en"
            }
            prefs.edit().putString("lang", picked).apply()
            picked
        } else {
            saved
        }

        val locales = when (code) {
            "uk" -> LocaleListCompat.forLanguageTags("uk")
            "de" -> LocaleListCompat.forLanguageTags("de")
            else -> LocaleListCompat.forLanguageTags("en")
        }

        AppCompatDelegate.setApplicationLocales(locales)
    }

}
