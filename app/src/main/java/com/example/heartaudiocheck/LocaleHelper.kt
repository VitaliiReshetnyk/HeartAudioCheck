package com.example.heartaudiocheck

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun wrap(base: Context, code: String): Context {
        if (code == "auto") return base

        val locale = when (code) {
            "uk" -> Locale("uk")
            "en" -> Locale.ENGLISH
            else -> return base
        }

        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return base.createConfigurationContext(config)
    }
}
