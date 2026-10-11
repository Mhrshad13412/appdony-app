package com.appdony.app

import android.content.Context

class SettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences("appdony_settings", Context.MODE_PRIVATE)

    var theme: String
        get() = prefs.getString("theme", "light") ?: "light"
        set(value) = prefs.edit().putString("theme", value).apply()

    var fontSize: String
        get() = prefs.getString("font_size", "medium") ?: "medium"
        set(value) = prefs.edit().putString("font_size", value).apply()

    fun isDark(): Boolean = theme == "dark"

    fun getTextZoom(): Int = when (fontSize) {
        "small" -> 85
        "large" -> 120
        else -> 100
    }
}
