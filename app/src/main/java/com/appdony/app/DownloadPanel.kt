package com.appdony.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class DownloadPanel(
    private val context: Context,
    private val isDarkProvider: () -> Boolean,
    private val accentProvider: () -> Int,
    private val helper: DownloadHelper,
    private val onBadgeChange: (Int) -> Unit
) {

    companion object {
        // ⭐ کش سراسری تصاویر — بین همه‌ی رفرش‌ها باقی می‌مونه
        private val imageCache = LruCache<String, Bitmap>(30)
        private val downloading = ConcurrentHashMap<String, Boolean>()
    }

    val view = FrameLayout(context).apply {
        setBackgroundColor(Color.parseColor("#CC000000"))
        visibility = View.GONE
    }

    private val panelBox: LinearLayout
    private val list: LinearLayout
    private val handler = Handler(Looper.getMainLooper())

    // ⭐ فقط status text‌ها رو آپدیت می‌کنیم، نه کل UI رو
    private val statusViews = HashMap<Long, TextView>()

    private val refreshRunnable = object : Runnable {
        override fun run() {
            softRefresh()
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
        fullRefresh()
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

    // ⭐ رفرش کامل — فقط وقتی تعداد یا وضعیت کلی عوض بشه
    fun fullRefresh() {
        list.removeAllViews()
        statusViews.clear()
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

    // ⭐ رفرش سبک — فقط وضعیت متن‌ها رو آپدیت میکنه
    private fun softRefresh() {
        val items = helper.getAll()

        // اگه تعداد عوض شده، رفرش کامل
        if (items.size != statusViews.size) {
            fullRefresh()
            return
        }

        // فقط آپدیت متن وضعیت
        items.forEach { item ->
            statusViews[item.id]?.text = getStatusText(item)
        }
    }

    // برای سازگاری با کد قبلی
    fun refresh() = fullRefresh()

    private fun getStatusText(item: DownloadItem): String {
        return when {
            item.isCancelled -> "لغو شده — برای شروع مجدد بزن"
            item.status == DownloadManager.STATUS_RUNNING -> {
                val pct = if (item.total > 0) (item.downloaded * 100 / item.total) else 0
                "در حال دانلود  $pct%   (${helper.formatSize(item.downloaded)} / ${helper.formatSize(item.total)})"
            }
            item.status == DownloadManager.STATUS_SUCCESSFUL -> "کامل شد  ${helper.formatSize(item.total)}"
            item.status == DownloadManager.STATUS_FAILED -> "خطا در دانلود"
            item.status == DownloadManager.STATUS_PAUSED -> "متوقف"
            else -> "در انتظار"
        }
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

        val iconUrl = item.savedIconUrl?.takeIf { it.isNotEmpty() } ?: helper.getIcon(item.id)
        iconUrl?.let { loadImageCached(it, iconView) }

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

        val statusTv = TextView(context).apply {
            text = getStatusText(item)
            textSize = 13f
            setPadding(0, 10, 0, 16)
            setTextColor(accent)
            gravity = Gravity.CENTER
        }
        box.addView(statusTv, LinearLayout.LayoutParams(-1, -2))

        // ⭐ ذخیره‌ی مرجع برای soft refresh
        statusViews[item.id] = statusTv

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        when {
            item.isCancelled -> {
                btnRow.addView(makeBtn("شروع مجدد", accent) {
                    val newId = helper.restartCancelled(item.id)
                    if (newId > 0) {
                        onBadgeChange(helper.getActiveCount())
                        fullRefresh()
                        Toast.makeText(context, "شروع مجدد شد", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "خطا در شروع مجدد", Toast.LENGTH_SHORT).show()
                    }
                })
                btnRow.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(14, 1)
                })
                btnRow.addView(makeBtn("حذف", Color.parseColor("#E53935")) {
                    helper.removePermanently(item.id)
                    onBadgeChange(helper.getActiveCount())
                    fullRefresh()
                })
            }
            item.status == DownloadManager.STATUS_SUCCESSFUL && item.localUri != null -> {
                btnRow.addView(makeBtn("نصب", accent) { installApk(item.localUri) })
                btnRow.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(14, 1)
                })
                btnRow.addView(makeBtn("حذف", Color.parseColor("#E53935")) {
                    helper.cancel(item.id, item.title, helper.getIcon(item.id))
                    onBadgeChange(helper.getActiveCount())
                    fullRefresh()
                })
            }
            item.status == DownloadManager.STATUS_RUNNING || item.status == DownloadManager.STATUS_PENDING -> {
                btnRow.addView(makeBtn("لغو", Color.parseColor("#FF9800")) {
                    helper.cancel(item.id, item.title, helper.getIcon(item.id))
                    onBadgeChange(helper.getActiveCount())
                    fullRefresh()
                    Toast.makeText(context, "لغو شد — می‌تونی بعداً دوباره شروع کنی", Toast.LENGTH_SHORT).show()
                })
            }
            else -> {
                btnRow.addView(makeBtn("حذف", Color.parseColor("#E53935")) {
                    helper.removePermanently(item.id)
                    onBadgeChange(helper.getActiveCount())
                    fullRefresh()
                })
            }
        }

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

    // ⭐⭐⭐ لود تصویر با کش — آیکون قطع و وصل نمیشه
    private fun loadImageCached(url: String, imageView: ImageView) {
        // توی کش هست؟ فوری نشون بده
        val cached = imageCache.get(url)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }

        // داره دانلود میشه؟ صبر کن
        if (downloading[url] == true) return
        downloading[url] = true

        Thread {
            try {
                val stream = URL(url).openStream()
                val bmp = BitmapFactory.decodeStream(stream)
                stream.close()
                if (bmp != null) {
                    imageCache.put(url, bmp)
                    (context as? android.app.Activity)?.runOnUiThread {
                        // ⭐ چک کن هنوز توی ویو هست
                        if (imageView.parent != null) {
                            imageView.setImageBitmap(bmp)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                downloading.remove(url)
            }
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
