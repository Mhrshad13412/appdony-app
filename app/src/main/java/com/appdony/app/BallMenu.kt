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

class BallMenu(private val context: Context, private val accentProvider: () -> Int) {

    val view: FrameLayout = FrameLayout(context).apply {
        visibility = View.GONE
        setOnClickListener { hide() }
    }

    private val menuBox: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(6), dp(6), dp(6), dp(6))
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

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    fun setOnDownloads(cb: () -> Unit) { onDownloads = cb }
    fun setOnSettings(cb: () -> Unit) { onSettings = cb }

    fun show(anchorX: Float, anchorY: Float, ballSize: Int) {
        val isDark = SettingsManager(context).isDark()
        val bgColor = if (isDark) Color.parseColor("#1F1F1F") else Color.WHITE
        val textColor = if (isDark) Color.WHITE else Color.parseColor("#222222")

        menuBox.background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), if (isDark) Color.parseColor("#3A3A3A") else Color.parseColor("#E0E0E0"))
        }

        menuBox.removeAllViews()

        menuBox.addView(makeItem(android.R.drawable.stat_sys_download, "دانلودها", textColor) {
            hide()
            onDownloads?.invoke()
        })

        menuBox.addView(makeDivider(isDark))

        menuBox.addView(makeItem(android.R.drawable.ic_menu_manage, "تنظیمات", textColor) {
            hide()
            onSettings?.invoke()
        })

        view.visibility = View.VISIBLE
        view.post {
            val menuW = menuBox.width
            val menuH = menuBox.height
            val screenW = view.width
            val margin = dp(12).toFloat()
            val gap = dp(8).toFloat()

            // موقعیت افقی: راست‌چین نسبت به توپ
            var x = anchorX + ballSize - menuW
            // اگه از چپ زد بیرون، بچسبون به لبه
            if (x < margin) x = margin
            // اگه از راست زد بیرون
            if (x + menuW > screenW - margin) x = screenW - menuW - margin

            // موقعیت عمودی: بالای توپ
            var y = anchorY - menuH - gap
            // اگه از بالا زد بیرون، برو پایین توپ
            if (y < margin) y = anchorY + ballSize + gap

            menuBox.x = x
            menuBox.y = y

            menuBox.scaleX = 0.85f
            menuBox.scaleY = 0.85f
            menuBox.alpha = 0f
            menuBox.pivotX = menuW.toFloat()
            menuBox.pivotY = menuH.toFloat()
            menuBox.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(200)
                .start()
        }

        isOpen = true
    }

    fun hide() {
        if (!isOpen) return
        menuBox.animate()
            .scaleX(0.85f).scaleY(0.85f).alpha(0f)
            .setDuration(160)
            .withEndAction { view.visibility = View.GONE }
            .start()
        isOpen = false
    }

    private fun makeItem(iconRes: Int, label: String, textColor: Int, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(20), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener { onClick() }

            addView(ImageView(context).apply {
                setImageResource(iconRes)
                setColorFilter(accent)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    marginEnd = dp(12)
                }
            })

            addView(TextView(context).apply {
                text = label
                textSize = 15f
                setTextColor(textColor)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
        }
    }

    private fun makeDivider(isDark: Boolean): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply {
                topMargin = dp(2)
                bottomMargin = dp(2)
                marginStart = dp(16)
                marginEnd = dp(16)
            }
            setBackgroundColor(if (isDark) Color.parseColor("#333333") else Color.parseColor("#E8E8E8"))
        }
    }
}
