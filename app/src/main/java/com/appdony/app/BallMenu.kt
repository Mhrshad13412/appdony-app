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

class BallMenu(private val context: Context, private val isDark: Boolean, private val accent: Int) {

    val view: FrameLayout = FrameLayout(context).apply {
        visibility = View.GONE
        setOnClickListener { hide() }
    }

    private val menuBox: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            setColor(if (isDark) Color.parseColor("#252525") else Color.WHITE)
            cornerRadius = 40f
            setStroke(2, Color.parseColor("#330D7377"))
        }
        elevation = 20f
        setPadding(12, 12, 12, 12)
    }

    var isOpen = false
        private set

    private var onDownloads: (() -> Unit)? = null
    private var onSettings: (() -> Unit)? = null

    init {
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.END }

        view.addView(menuBox, params)
    }

    fun setOnDownloads(cb: () -> Unit) { onDownloads = cb }
    fun setOnSettings(cb: () -> Unit) { onSettings = cb }

    fun show(anchorX: Float, anchorY: Float, ballSize: Int) {
        menuBox.removeAllViews()

        menuBox.addView(makeItem(R.drawable.ic_carton, "دانلودها") {
            hide()
            onDownloads?.invoke()
        })

        menuBox.addView(makeDivider())

        menuBox.addView(makeItem(android.R.drawable.ic_menu_manage, "تنظیمات") {
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

    private fun makeItem(iconRes: Int, label: String, onClick: () -> Unit): LinearLayout {
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(30, 26, 30, 26)
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = 30f
            }
            setOnClickListener { onClick() }

            addView(ImageView(context).apply {
                setImageResource(iconRes)
                setColorFilter(accent)
                layoutParams = LinearLayout.LayoutParams(60, 60).apply {
                    marginEnd = 20
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

    private fun makeDivider(): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 2).apply {
                // ⭐ این دو خط اصلاح شدن
                topMargin = 4
                bottomMargin = 4
                marginStart = 30
                marginEnd = 30
            }
            setBackgroundColor(Color.parseColor("#22888888"))
        }
    }
}
