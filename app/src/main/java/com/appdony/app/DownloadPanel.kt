package com.appdony.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.net.URL

class DownloadPanel(
    private val context: Context,
    private val isDark: Boolean,
    private val accent: Int,
    private val helper: DownloadHelper
) {

    val view = FrameLayout(context).apply {
        setBackgroundColor(Color.parseColor("#CC000000"))
        visibility = View.GONE
    }

    private val panelBox: LinearLayout
    private val list: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000)
        }
    }
    var isOpen = false
        private set

    init {
        panelBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (isDark) Color.parseColor("#1A1A1A") else Color.WHITE)
            setPadding(20, 20, 20, 20)
        }
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (context.resources.displayMetrics.heightPixels * 0.72).toInt()
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
            text = "دانلودها"
            textSize = 20f
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        panelBox.addView(title)

        val scroll = ScrollView(context)
        list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 16)
        }
        scroll.addView(list)
        panelBox.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

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

    fun open() {
        isOpen = true
        refresh()
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(200).start()
        panelBox.translationY = panelBox.height.toFloat()
        panelBox.animate()
            .translationY(0f)
            .setInterpolator(OvershootInterpolator(1.1f))
            .setDuration(420)
            .start()
        handler.post(refreshRunnable)
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
        handler.removeCallbacks(refreshRunnable)
    }

    fun refresh() {
        list.removeAllViews()
        val items = helper.getAll()

        if (items.isEmpty()) {
            list.addView(TextView(context).apply {
                text = "هنوز دانلودی نداری"
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 60)
                setTextColor(Color.GRAY)
            })
            return
        }

        items.forEach { list.addView(createItem(it)) }
    }

    private fun createItem(item: DownloadItem): View {
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val cardBg = if (isDark) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5")

        val outer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        outer.layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = 14; marginStart = 20; marginEnd = 20
        }

        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20, 22, 20, 22)
            background = GradientDrawable().apply {
                setColor(cardBg); cornerRadius = 24f
            }
        }
        outer.addView(box, LinearLayout.LayoutParams(-1, -2))

        val iconView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_carton)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 22f
                setColor(if (isDark) Color.parseColor("#1A1A1A") else Color.WHITE)
            }
            clipToOutline = true
        }
        box.addView(iconView, LinearLayout.LayoutParams(90, 90).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })

        helper.getIcon(item.id)?.let { loadImageAsync(it, iconView) }

        box.addView(TextView(context).apply {
            text = item.title
            textSize = 15f
            setTextColor(textColor)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 14, 0, 0)
        }, LinearLayout.LayoutParams(-1, -2))

        box.addView(TextView(context).apply {
            textSize = 13f
            setPadding(0, 10, 0, 16)
            setTextColor(accent)
            gravity = Gravity.CENTER
            text = when (item.status) {
                DownloadManager.STATUS_RUNNING -> {
                    val pct = if (item.total > 0) (item.downloaded * 100 / item.total) else 0
                    "در حال دانلود  $pct%   (${helper.formatSize(item.downloaded)} / ${helper.formatSize(item.total)})"
                }
                DownloadManager.STATUS_SUCCESSFUL -> "کامل شد  ${helper.formatSize(item.total)}"
                DownloadManager.STATUS_FAILED -> "خطا در دانلود"
                DownloadManager.STATUS_PAUSED -> "متوقف"
                else -> "در انتظار"
            }
        }, LinearLayout.LayoutParams(-1, -2))

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        if (item.status == DownloadManager.STATUS_SUCCESSFUL && item.localUri != null) {
            btnRow.addView(makeBtn("نصب", accent) { installApk(item.localUri) })
            btnRow.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(14, 1)
            })
        }
        btnRow.addView(makeBtn("حذف", Color.parseColor("#E53935")) {
            helper.remove(item.id)
            refresh()
            Toast.makeText(context, "حذف شد", Toast.LENGTH_SHORT).show()
        })
        box.addView(btnRow, LinearLayout.LayoutParams(-1, -2))

        return outer
    }

    private fun makeBtn(text: String, bg: Int, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(45, 14, 45, 14)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(bg); cornerRadius = 30f
            }
            setOnClickListener { onClick() }
        }
    }

    private fun loadImageAsync(url: String, imageView: ImageView) {
        Thread {
            try {
                val stream = URL(url).openStream()
                val bmp = BitmapFactory.decodeStream(stream)
                stream.close()
                if (bmp != null) {
                    (context as? android.app.Activity)?.runOnUiThread {
                        imageView.setImageBitmap(bmp)
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun installApk(localUri: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(context, "لطفاً اجازه نصب از این منبع را فعال کنید", Toast.LENGTH_LONG).show()
                    context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}")))
                    return
                }
            }
            val uri = Uri.parse(localUri)
            val file = File(uri.path ?: return)
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else uri

            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در نصب: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun destroy() {
        handler.removeCallbacks(refreshRunnable)
    }
}
