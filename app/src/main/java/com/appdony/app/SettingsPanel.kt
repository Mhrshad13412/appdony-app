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
import android.widget.ScrollView
import android.widget.TextView

class SettingsPanel(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val onThemeChanged: () -> Unit,
    private val onFontChanged: () -> Unit,
    private val onAccentChanged: () -> Unit
) {

    val view = FrameLayout(context).apply {
        setBackgroundColor(Color.parseColor("#CC000000"))
        visibility = View.GONE
    }

    private val panelBox: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    var isOpen = false
        private set

    private var themeToggleContainer: LinearLayout? = null
    private var fontToggleContainer: LinearLayout? = null
    private var accentContainer: LinearLayout? = null

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

    init {
        panelBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.BOTTOM }

        val handle = View(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#888888"))
                cornerRadius = dp(10).toFloat()
            }
        }
        panelBox.addView(handle, LinearLayout.LayoutParams(dp(100), dp(6)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(16)
        })

        val title = TextView(context).apply {
            text = "تنظیمات"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(20))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        panelBox.addView(title)

        val scroll = ScrollView(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(content)
        panelBox.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { height = 0 })

        // حالت شب/روز
        content.addView(sectionTitle("حالت نمایش"))
        themeToggleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        content.addView(themeToggleContainer, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(10); bottomMargin = dp(22)
        })
        rebuildThemeToggle()

        // تم رنگی
        content.addView(sectionTitle("تم رنگی"))
        accentContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(accentContainer, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(10); bottomMargin = dp(22)
        })
        rebuildAccentPicker()

        // سایز فونت
        content.addView(sectionTitle("اندازه فونت"))
        fontToggleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        content.addView(fontToggleContainer, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(10); bottomMargin = dp(22)
        })
        rebuildFontToggle()

        val closeBtn = TextView(context).apply {
            text = "بستن"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, dp(20), 0, dp(20))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setOnClickListener { close() }
        }
        panelBox.addView(closeBtn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        view.addView(panelBox, params)
        view.setOnClickListener { close() }

        refreshColors()
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 14f
            gravity = Gravity.START
            setPadding(dp(8), 0, 0, 0)
        }
    }

    private fun rebuildAccentPicker() {
        accentContainer?.removeAllViews()
        val currentKey = settingsManager.accentTheme

        val row1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val row2 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        SettingsManager.ACCENT_OPTIONS.forEachIndexed { index, opt ->
            val circle = makeColorCircle(opt, opt.key == currentKey)
            val lp = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                marginStart = dp(6); marginEnd = dp(6)
                topMargin = dp(6); bottomMargin = dp(6)
            }
            if (index < 4) row1.addView(circle, lp) else row2.addView(circle, lp)
        }

        accentContainer?.addView(row1)
        accentContainer?.addView(row2, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
    }

    private fun makeColorCircle(opt: AccentOption, isActive: Boolean): View {
        val colorHex = if (settingsManager.isDark()) opt.darkHex else opt.lightHex
        val color = Color.parseColor(colorHex)

        return FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(if (isActive) 3 else 0), color)
            }
            addView(View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
            }, FrameLayout.LayoutParams(dp(38), dp(38)).apply { gravity = Gravity.CENTER })

            if (isActive) {
                addView(TextView(context).apply {
                    text = "✓"
                    textSize = 18f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }, FrameLayout.LayoutParams(dp(38), dp(38)).apply { gravity = Gravity.CENTER })
            }

            setOnClickListener {
                settingsManager.accentTheme = opt.key
                rebuildAccentPicker()
                refreshColors()
                onAccentChanged()
            }
        }
    }

    private fun rebuildThemeToggle() {
        themeToggleContainer?.removeAllViews()
        val current = settingsManager.theme

        themeToggleContainer?.addView(makeToggleBtn("روشن", current == "light") {
            settingsManager.theme = "light"
            onThemeChanged()
            rebuildThemeToggle(); rebuildAccentPicker(); rebuildFontToggle(); refreshColors()
        })
        themeToggleContainer?.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(16), 1)
        })
        themeToggleContainer?.addView(makeToggleBtn("تاریک", current == "dark") {
            settingsManager.theme = "dark"
            onThemeChanged()
            rebuildThemeToggle(); rebuildAccentPicker(); rebuildFontToggle(); refreshColors()
        })
    }

    private fun rebuildFontToggle() {
        fontToggleContainer?.removeAllViews()
        val current = settingsManager.fontSize

        fontToggleContainer?.addView(makeToggleBtn("کوچک", current == "small") {
            settingsManager.fontSize = "small"; onFontChanged(); rebuildFontToggle()
        })
        fontToggleContainer?.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), 1)
        })
        fontToggleContainer?.addView(makeToggleBtn("متوسط", current == "medium") {
            settingsManager.fontSize = "medium"; onFontChanged(); rebuildFontToggle()
        })
        fontToggleContainer?.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), 1)
        })
        fontToggleContainer?.addView(makeToggleBtn("بزرگ", current == "large") {
            settingsManager.fontSize = "large"; onFontChanged(); rebuildFontToggle()
        })
    }

    private fun makeToggleBtn(text: String, active: Boolean, onClick: () -> Unit): TextView {
        val accent = settingsManager.getAccent()
        val isDark = settingsManager.isDark()
        val textColor = if (active) Color.WHITE else (if (isDark) Color.WHITE else Color.parseColor("#333333"))
        val bg = if (active) accent
            else (if (isDark) Color.parseColor("#2A2A2A") else Color.parseColor("#EEEEEE"))

        return TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(14), dp(28), dp(14))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(bg)
                cornerRadius = dp(30).toFloat()
                if (!active) setStroke(dp(1), Color.parseColor("#33888888"))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun refreshColors() {
        val isDark = settingsManager.isDark()
        val bg = if (isDark) Color.parseColor("#1A1A1A") else Color.WHITE
        val titleColor = if (isDark) Color.WHITE else Color.parseColor("#222222")
        val sectionColor = if (isDark) Color.parseColor("#AAAAAA") else Color.parseColor("#666666")

        panelBox.setBackgroundColor(bg)

        fun updateAll(parent: ViewGroup) {
            for (i in 0 until parent.childCount) {
                val c = parent.getChildAt(i)
                if (c is TextView) {
                    when (c.text?.toString()) {
                        "حالت نمایش", "اندازه فونت", "تم رنگی" -> c.setTextColor(sectionColor)
                        "تنظیمات" -> c.setTextColor(titleColor)
                        "بستن" -> {
                            c.background = GradientDrawable().apply {
                                setColor(settingsManager.getAccent())
                                cornerRadius = dp(50).toFloat()
                            }
                        }
                    }
                } else if (c is ViewGroup) {
                    updateAll(c)
                }
            }
        }
        updateAll(panelBox)
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
