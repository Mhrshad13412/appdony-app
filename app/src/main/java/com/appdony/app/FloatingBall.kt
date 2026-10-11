package com.appdony.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class FloatingBall(
    context: Context,
    private val accent: Int,
    private val onTap: () -> Unit
) {

    val size = 120
    val view: FrameLayout = FrameLayout(context)

    private val box: LinearLayout
    private val badge: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { fadeOut() }

    init {
        box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f
                setColor(accent)
            }
        }
        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_carton)
            layoutParams = LinearLayout.LayoutParams(70, 70)
        }
        box.addView(icon)

        badge = TextView(context).apply {
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E53935"))
            }
            visibility = View.GONE
        }

        view.addView(box, FrameLayout.LayoutParams(size, size))
        view.addView(badge, FrameLayout.LayoutParams(40, 40).apply {
            gravity = Gravity.TOP or Gravity.END
        })

        setupDraggable(context)
        handler.postDelayed(hideRunnable, 3000)
    }

    fun updateBadge(count: Int) {
        if (count > 0) {
            badge.text = count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }

    private fun fadeOut() {
        view.animate()
            .alpha(0.35f)
            .scaleX(0.72f).scaleY(0.72f)
            .setDuration(350)
            .start()
    }

    private fun resetFade() {
        handler.removeCallbacks(hideRunnable)
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun scheduleFade() {
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, 3000)
    }

    private fun setupDraggable(context: Context) {
        var downX = 0f; var downY = 0f
        var startX = 0f; var startY = 0f
        var isDragging = false
        val slop = ViewConfiguration.get(context).scaledTouchSlop

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = v.x; startY = v.y
                    isDragging = false
                    resetFade()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!isDragging && (Math.abs(dx) > slop || Math.abs(dy) > slop)) {
                        isDragging = true
                        v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(100).start()
                    }
                    if (isDragging) {
                        val parent = v.parent as View
                        v.x = (startX + dx).coerceIn(0f, (parent.width - v.width).toFloat())
                        v.y = (startY + dy).coerceIn(0f, (parent.height - v.height).toFloat())
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    if (!isDragging) onTap()
                    scheduleFade()
                    true
                }
                else -> false
            }
        }
    }
}
