package com.appdony.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class BallMenu(private val context: Context, private val accent: Int) {

    val view: FrameLayout = FrameLayout(context).apply {
        visibility = View.GONE
        setOnClickListener { hide() }
    }

    private val menuBox: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(10, 10, 10, 10)
    }

    var isOpen = false
        private set

    private var onDownloads: (() -> Unit)? = null
    private var onSettings: (() -> Unit)? = null

    init {
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        view.addView(menuBox, params)
    }

    fun setOnDownloads(cb: () -> Unit) { onDownloads = cb }
    fun setOnSettings(cb: () -> Unit) { onSettings = cb }

    fun show(anchorX: Float, anchorY: Float, ballSize: Int) {
        val isDark = SettingsManager(context).isDark()
        val bgColor = if (isDark) Color.parseColor("#1F1F1F") else Color.WHITE
        val textColor = if (isDark) Color.WHITE else Color.BLACK

        menuBox.background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 48f
            setStroke(3, if (isDark) Color.parseColor("#3A3A3A") else Color.parseColor("#E0E0E0"))
        }

        menuBox.removeAllViews()

        menuBox.addView(makeItem(android.R.drawable.stat_sys_download, "دانلودها", textColor, isDark) {
            hide()
            onDownloads?.invoke()
        })

        menuBox.addView(makeDivider(isDark))

        menuBox.addView(makeItem(android.R.drawable.ic_menu_manage, "تنظیمات", textColor, isDark) {
            hide()
            onSettings?.invoke()
        })

        val margin = 16f
        view.post {
            val actualWidth = menuBox.width
            val actualHeight = menuBox.height
            menuBox.x = anchorX + ballSize - actualWidth
            menuBox.y = anchorY - actualHeight - margin
            if (menuBox.y < 40f) {
                menuBox.y = anchorY + ballSize + margin
            }
        }

        view.visibility = View.VISIBLE
        menuBox.scaleX = 0.7f
        menuBox.scaleY = 0.7f
        menuBox.alpha = 0f
        menuBox.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(220)
            .start()
        isOpen = true
    }

    fun hide() {
        if (!isOpen) return
        menuBox.animate()
            .scaleX(0.7f).scaleY(0.7f).alpha(0f)
            .setDuration(180)
            .withEndAction { view.visibility = View.GONE }
            .start()
        isOpen = false
    }

    private fun makeItem(iconRes: Int, label: String, textColor: Int, isDark: Boolean, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(50, 40, 60, 40)
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = 35f
            }
            setOnClickListener { onClick() }

            addView(ImageView(context).apply {
                setImageResource(iconRes)
                setColorFilter(accent)
                layoutParams = LinearLayout.LayoutParams(58, 58).apply {
                    marginEnd = 30
                }
            })

            addView(TextView(context).apply {
                text = label
                textSize = 16f
                setTextColor(textColor)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
    }

    private fun makeDivider(isDark: Boolean): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 2).apply {
                topMargin = 4
                bottomMargin = 4
                marginStart = 40
                marginEnd = 40
            }
            setBackgroundColor(if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
        }
    }
}
