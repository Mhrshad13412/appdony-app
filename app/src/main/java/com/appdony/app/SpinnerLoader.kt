package com.appdony.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator

class SpinnerLoader(
    context: Context,
    private val accentProvider: () -> Int,
    private val strokeW: Float
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokeW
    }
    private val rect = RectF()
    private var rotation = 0f
    private var sweep = 90f

    init {
        ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { rotation = it.animatedValue as Float; invalidate() }
            start()
        }
        ValueAnimator.ofFloat(30f, 280f).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { sweep = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val accent = accentProvider()
        val r = Color.red(accent)
        val g = Color.green(accent)
        val b = Color.blue(accent)
        val colorMain = Color.rgb(r, g, b)
        val colorLight = Color.rgb(
            (r + (255 - r) * 0.4).toInt(),
            (g + (255 - g) * 0.4).toInt(),
            (b + (255 - b) * 0.4).toInt()
        )
        val colorTransparent = Color.argb(32, r, g, b)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) - strokeW) / 2f
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        paint.shader = SweepGradient(cx, cy,
            intArrayOf(colorMain, colorLight, colorTransparent, colorMain),
            floatArrayOf(0f, 0.35f, 0.75f, 1f))

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        canvas.drawArc(rect, 0f, sweep, false, paint)
        canvas.restore()
    }
}
