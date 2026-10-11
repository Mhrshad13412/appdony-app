package com.appdony.app

import android.content.Context
import android.graphics.Color

class SettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences("appdony_settings", Context.MODE_PRIVATE)

    var theme: String
        get() = prefs.getString("theme", "light") ?: "light"
        set(value) = prefs.edit().putString("theme", value).apply()

    var fontSize: String
        get() = prefs.getString("font_size", "medium") ?: "medium"
        set(value) = prefs.edit().putString("font_size", value).apply()

    var accentTheme: String
        get() = prefs.getString("accent_theme", "green") ?: "green"
        set(value) = prefs.edit().putString("accent_theme", value).apply()

    fun isDark(): Boolean = theme == "dark"

    fun getTextZoom(): Int = when (fontSize) {
        "small" -> 85
        "large" -> 120
        else -> 100
    }

    // ⭐ رنگ اصلی بر اساس تم انتخاب‌شده
    fun getAccent(): Int = getAccentFor(accentTheme, isDark())

    companion object {
        // لیست همه رنگ‌ها: نام + رنگ روشن + رنگ تیره
        val ACCENT_OPTIONS = listOf(
            AccentOption("green",  "سبز",      "#0D7377", "#4DD0C7"),
            AccentOption("blue",   "آبی",      "#1565C0", "#64B5F6"),
            AccentOption("purple", "بنفش",     "#6A1B9A", "#BA68C8"),
            AccentOption("pink",   "صورتی",    "#C2185B", "#F06292"),
            AccentOption("red",    "قرمز",     "#C62828", "#EF5350"),
            AccentOption("brown",  "قهوه‌ای",  "#5D4037", "#A1887F"),
            AccentOption("gray",   "خاکستری",  "#37474F", "#90A4AE")
        )

        fun getAccentFor(themeName: String, isDark: Boolean): Int {
            val opt = ACCENT_OPTIONS.firstOrNull { it.key == themeName } ?: ACCENT_OPTIONS[0]
            return Color.parseColor(if (isDark) opt.darkHex else opt.lightHex)
        }

        fun getAccentFor(themeName: String): Int {
            val opt = ACCENT_OPTIONS.firstOrNull { it.key == themeName } ?: ACCENT_OPTIONS[0]
            return Color.parseColor(opt.lightHex)
        }
    }
}

data class AccentOption(
    val key: String,
    val label: String,
    val lightHex: String,
    val darkHex: String
)
