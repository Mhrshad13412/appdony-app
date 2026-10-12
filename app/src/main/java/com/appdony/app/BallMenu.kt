package com.appdony.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class BallMenu(private val context: Context, private val accentProvider: () -> Int) {

    private val MENU_WIDTH_DP = 160

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
        // ⭐ عرض ثابت — دیگه کشیده نمیشه
        view.addView(menuBox, FrameLayout.LayoutParams(
            dp(MENU_WIDTH_DP),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    fun setOnDownloads(cb: () -> Unit) { onDownloads = cb }
    fun setOnSettings(cb: () -> Unit) { onSettings = cb }

    fun show(anchorX: Float, anchorY: Float, ballSize: Int) {
        val bg = Color.parseColor("#1A1A1A")

        menuBox.setPadding(dp(4), dp(4), dp(4), dp(4))
        menuBox.background = GradientDrawable().apply {
            setColor(bg)
            cornerRadius = dp(18).toFloat()
        }

        menuBox.removeAllViews()
        menuBox.addView(makeItem("دانلودها") {
            hide(); onDownloads?.invoke()
        })
        menuBox.addView(divider())
        menuBox.addView(makeItem("تنظیمات") {
            hide(); onSettings?.invoke()
        })

        view.visibility = View.VISIBLE
        view.post {
            val w = menuBox.width
            val h = menuBox.height
            val screenW = view.width
            val m = dp(10).toFloat()
            val gap = dp(8).toFloat()

            // راست‌چین نسبت به توپ
            var x = anchorX + ballSize - w
            if (x + w > screenW - m) x = screenW - w - m
            if (x < m) x = m

            // بالای توپ
            var y = anchorY - h - gap
            val pivotY: Float
            if (y < m) {
                y = anchorY + ballSize + gap
                pivotY = 0f
            } else {
                pivotY = h.toFloat()
            }

            menuBox.x = x
            menuBox.y = y
            menuBox.pivotX = w.toFloat()
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

    // ⭐ متن وسط‌چین، بدون آیکون، پر عرض
    private fun makeItem(label: String, cb: () -> Unit): TextView = TextView(context).apply {
        text = label
        textSize = 15f
        setTextColor(Color.WHITE)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setOnClickListener { cb() }
    }

    // ⭐ جداکننده — با پدینگ افقی، عرض کم
    private fun divider() = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        ).apply {
            marginStart = dp(12)
            marginEnd = dp(12)
        }
        setBackgroundColor(Color.parseColor("#2A2A2A"))
    }
}
