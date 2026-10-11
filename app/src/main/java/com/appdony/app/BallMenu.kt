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
    }

    var isOpen = false
        private set

    private var onDownloads: (() -> Unit)? = null
    private var onSettings: (() -> Unit)? = null

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

    init {
        view.addView(menuBox, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    fun setOnDownloads(cb: () -> Unit) { onDownloads = cb }
    fun setOnSettings(cb: () -> Unit) { onSettings = cb }

    fun show(anchorX: Float, anchorY: Float, ballSize: Int) {
        val isDark = SettingsManager(context).isDark()
        val accent = accentProvider()
        val bg = if (isDark) Color.parseColor("#1F1F1F") else Color.WHITE
        val txt = if (isDark) Color.WHITE else Color.parseColor("#222222")

        menuBox.setPadding(dp(4), dp(4), dp(4), dp(4))
        menuBox.background = GradientDrawable().apply {
            setColor(bg)
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), if (isDark) Color.parseColor("#333333") else Color.parseColor("#E5E5E5"))
        }

        menuBox.removeAllViews()
        menuBox.addView(item(android.R.drawable.stat_sys_download, "دانلودها", txt, accent) {
            hide(); onDownloads?.invoke()
        })
        menuBox.addView(divider(isDark))
        menuBox.addView(item(android.R.drawable.ic_menu_manage, "تنظیمات", txt, accent) {
            hide(); onSettings?.invoke()
        })

        view.visibility = View.VISIBLE
        view.post {
            val w = menuBox.width
            val h = menuBox.height
            val screenW = view.width
            val m = dp(10).toFloat()
            val gap = dp(6).toFloat()

            // راست‌چین نسبت به توپ
            var x = anchorX + ballSize - w
            if (x + w > screenW - m) x = screenW - w - m
            if (x < m) x = m

            // بالای توپ
            var y = anchorY - h - gap
            val pivotY: Float
            if (y < m) {
                y = anchorY + ballSize + gap
                pivotY = 0f // از بالا باز میشه
            } else {
                pivotY = h.toFloat() // از پایین باز میشه
            }

            menuBox.x = x
            menuBox.y = y
            menuBox.pivotX = w.toFloat()  // از گوشه راست
            menuBox.pivotY = pivotY
            menuBox.scaleX = 0.6f
            menuBox.scaleY = 0.6f
            menuBox.alpha = 0f
            menuBox.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(240)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                .start()
        }

        isOpen = true
    }

    fun hide() {
        if (!isOpen) return
        menuBox.animate()
            .scaleX(0.6f).scaleY(0.6f).alpha(0f)
            .setDuration(160)
            .withEndAction { view.visibility = View.GONE }
            .start()
        isOpen = false
    }

    private fun item(icon: Int, label: String, txt: Int, accent: Int, cb: () -> Unit) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(10), dp(18), dp(10))
        setOnClickListener { cb() }
        addView(ImageView(context).apply {
            setImageResource(icon)
            setColorFilter(accent)
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(10) }
        })
        addView(TextView(context).apply {
            text = label
            textSize = 14f
            setTextColor(txt)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
    }

    private fun divider(isDark: Boolean) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply {
            marginStart = dp(10); marginEnd = dp(10)
        }
        setBackgroundColor(if (isDark) Color.parseColor("#333333") else Color.parseColor("#EEEEEE"))
    }
}
