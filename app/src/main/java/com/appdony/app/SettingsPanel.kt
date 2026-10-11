package com.appdony.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class SettingsPanel(
    private val context: Context,
    private val accent: Int,
    private val settingsManager: SettingsManager,
    private val onThemeChanged: () -> Unit,
    private val onFontChanged: () -> Unit
) {

    val view = FrameLayout(context).apply {
        setBackgroundColor(Color.parseColor("#CC000000"))
        visibility = View.GONE
    }

    private val panelBox: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    var isOpen = false
        private set

    // رفرنس به دکمه‌های toggle برای آپدیت
    private var themeToggleContainer: LinearLayout? = null
    private var fontToggleContainer: LinearLayout? = null

    init {
        panelBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (settingsManager.isDark()) Color.parseColor("#1A1A1A") else Color.WHITE)
            setPadding(20, 20, 20, 20)
        }
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.BOTTOM }

        val handle = View(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#888888"))
                cornerRadius = 10f
            }
        }
        panelBox.addView(handle, LinearLayout.LayoutParams(100, 6).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = 16
        })

        val title = TextView(context).apply {
            text = "تنظیمات"
            textSize = 20f
            setTextColor(if (settingsManager.isDark()) Color.WHITE else Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        panelBox.addView(title)

        // ⭐ حالت شب و روز
        panelBox.addView(sectionTitle("حالت نمایش"))
        themeToggleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        panelBox.addView(themeToggleContainer, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = 8; bottomMargin = 24
        })
        rebuildThemeToggle()

        // ⭐ سایز فونت
        panelBox.addView(sectionTitle("اندازه فونت"))
        fontToggleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        panelBox.addView(fontToggleContainer, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = 8; bottomMargin = 24
        })
        rebuildFontToggle()

        // دکمه بستن
        val closeBtn = TextView(context).apply {
            text = "بستن"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 20, 0, 20)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(accent)
                cornerRadius = 50f
            }
            setOnClickListener { close() }
        }
        panelBox.addView(closeBtn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16 })

        view.addView(panelBox, params)
        view.setOnClickListener { close() }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(if (settingsManager.isDark()) Color.parseColor("#AAAAAA") else Color.parseColor("#666666"))
            gravity = Gravity.START
            setPadding(10, 0, 0, 0)
        }
    }

    private fun rebuildThemeToggle() {
        themeToggleContainer?.removeAllViews()
        val current = settingsManager.theme

        themeToggleContainer?.addView(makeToggleBtn("روشن", current == "light") {
            settingsManager.theme = "light"
            onThemeChanged()
            rebuildThemeToggle()
            refreshColors()
        })

        themeToggleContainer?.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(20, 1)
        })

        themeToggleContainer?.addView(makeToggleBtn("تاریک", current == "dark") {
            settingsManager.theme = "dark"
            onThemeChanged()
            rebuildThemeToggle()
            refreshColors()
        })
    }

    private fun rebuildFontToggle() {
        fontToggleContainer?.removeAllViews()
        val current = settingsManager.fontSize

        fontToggleContainer?.addView(makeToggleBtn("کوچک", current == "small") {
            settingsManager.fontSize = "small"
            onFontChanged()
            rebuildFontToggle()
        })

        fontToggleContainer?.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(14, 1)
        })

        fontToggleContainer?.addView(makeToggleBtn("متوسط", current == "medium") {
            settingsManager.fontSize = "medium"
            onFontChanged()
            rebuildFontToggle()
        })

        fontToggleContainer?.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(14, 1)
        })

        fontToggleContainer?.addView(makeToggleBtn("بزرگ", current == "large") {
            settingsManager.fontSize = "large"
            onFontChanged()
            rebuildFontToggle()
        })
    }

    private fun makeToggleBtn(text: String, active: Boolean, onClick: () -> Unit): TextView {
        val textColor = if (active) Color.WHITE else (if (settingsManager.isDark()) Color.WHITE else Color.BLACK)
        val bg = if (active) accent else (if (settingsManager.isDark()) Color.parseColor("#2A2A2A") else Color.parseColor("#EEEEEE"))

        return TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(40, 16, 40, 16)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(bg)
                cornerRadius = 30f
                if (!active) {
                    setStroke(2, Color.parseColor("#44888888"))
                }
            }
            setOnClickListener { onClick() }
        }
    }

    private fun refreshColors() {
        panelBox.setBackgroundColor(if (settingsManager.isDark()) Color.parseColor("#1A1A1A") else Color.WHITE)
    }

    fun open() {
        isOpen = true
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(200).start()
        panelBox.translationY = panelBox.height.toFloat()
        panelBox.animate()
            .translationY(0f)
            .setInterpolator(OvershootInterpolator(1.1f))
            .setDuration(420)
            .start()
    }

    fun close() {
        isOpen = false
        panelBox.animate()
            .translationY(panelBox.height.toFloat())
            .setDuration(280)
            .start()
        view.animate().alpha(0f).setDuration(280).withEndAction {
            view.visibility = View.GONE
        }.start()
        handler.removeCallbacksAndMessages(null)
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
    }
}
