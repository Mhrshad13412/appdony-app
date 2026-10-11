package com.appdony.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashView: FrameLayout
    private lateinit var miniLoader: SpinnerLoader
    private lateinit var floatingBall: LinearLayout
    private lateinit var downloadPanel: FrameLayout
    private lateinit var downloadsList: LinearLayout
    private lateinit var badgeView: TextView
    private lateinit var downloadManager: DownloadManager
    private var panelOpen = false

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshDownloads()
            refreshHandler.postDelayed(this, 1000)
        }
    }

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Toast.makeText(this@MainActivity, "✅ دانلود کامل شد", Toast.LENGTH_SHORT).show()
            updateBadge()
            if (panelOpen) refreshDownloads()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val splashBg = if (isDark) Color.parseColor("#001C1A") else Color.WHITE
        val accent = if (isDark) Color.parseColor("#4DD0C7") else Color.parseColor("#0D7377")

        window.statusBarColor = splashBg
        val wic = WindowInsetsControllerCompat(window, window.decorView)
        wic.isAppearanceLightStatusBars = !isDark

        clearCacheIfVersionChanged()

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        registerReceiver(
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RECEIVER_NOT_EXPORTED else 0
        )

        val root = FrameLayout(this)

        // ============ SPLASH ============
        splashView = FrameLayout(this).apply { setBackgroundColor(splashBg) }
        val loader = SpinnerLoader(this, isDark, 30f)
        val lp = FrameLayout.LayoutParams(280, 280).apply { gravity = Gravity.CENTER }
        splashView.addView(loader, lp)

        miniLoader = SpinnerLoader(this, isDark, 18f)
        val miniParams = FrameLayout.LayoutParams(180, 180).apply { gravity = Gravity.CENTER }
        miniLoader.visibility = View.GONE

        // ============ FLOATING BALL (جعبه دانلود) ============
        floatingBall = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent)
                setStroke(4, Color.parseColor("#FFFFFF"))
            }
            elevation = 20f
        }
        val ballIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_download_box)
            layoutParams = LinearLayout.LayoutParams(70, 70)
        }
        floatingBall.addView(ballIcon)

        // Badge (شماره دانلودهای فعال)
        badgeView = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E53935"))
            }
            visibility = View.GONE
        }

        val ballContainer = FrameLayout(this).apply {
            addView(floatingBall, FrameLayout.LayoutParams(140, 140))
            addView(badgeView, FrameLayout.LayoutParams(45, 45).apply {
                gravity = Gravity.TOP or Gravity.END
            })
        }

        val ballParams = FrameLayout.LayoutParams(150, 150).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            bottomMargin = 280
            rightMargin = 40
        }
        ballContainer.setOnClickListener {
            if (panelOpen) closePanel() else openPanel()
        }

        // ============ DOWNLOAD PANEL ============
        downloadPanel = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
            visibility = View.GONE
            setOnClickListener { closePanel() }
        }

        val panelBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (isDark) Color.parseColor("#1A1A1A") else Color.WHITE)
            setPadding(30, 40, 30, 30)
        }
        val panelBoxParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.7).toInt()
        ).apply { gravity = Gravity.BOTTOM }

        val handle = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#888888"))
                cornerRadius = 10f
            }
        }
        panelBox.addView(handle, LinearLayout.LayoutParams(120, 8).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = 20
        })

        val panelTitle = TextView(this).apply {
            text = "📥 دانلودها"
            textSize = 22f
            setTextColor(if (isDark) Color.WHITE else Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 20)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        panelBox.addView(panelTitle)

        val scroll = ScrollView(this)
        downloadsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(downloadsList)
        panelBox.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val closeBtn = TextView(this).apply {
            text = "بستن"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 26, 0, 26)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(accent)
                cornerRadius = 50f
            }
            setOnClickListener { closePanel() }
        }
        panelBox.addView(closeBtn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 20 })

        downloadPanel.addView(panelBox, panelBoxParams)

        // ============ WEBVIEW ============
        webView = WebView(this).apply {
            visibility = View.GONE
            setBackgroundColor(splashBg)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                setSupportMultipleWindows(false)
                allowFileAccess = true
                allowContentAccess = true
            }
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    miniLoader.visibility = View.VISIBLE
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Handler(Looper.getMainLooper()).postDelayed({
                        miniLoader.visibility = View.GONE
                    }, 300)
                    injectCustomCSS()
                }
            }

            webChromeClient = WebChromeClient()

            // ⭐ DownloadListener - هر دامنه‌ای رو می‌گیره!
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                startDownload(url, userAgent, contentDisposition, mimetype)
            }

            val cacheBuster = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
            loadUrl("https://appdony.ir/?v=$cacheBuster")
        }

        root.addView(splashView, FrameLayout.LayoutParams(-1, -1))
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        root.addView(miniLoader, miniParams)
        root.addView(downloadPanel, FrameLayout.LayoutParams(-1, -1))
        root.addView(ballContainer, ballParams)
        setContentView(root)

        updateBadge()

        Handler(Looper.getMainLooper()).postDelayed({
            splashView.animate().alpha(0f).setDuration(600).withEndAction {
                splashView.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }.start()
        }, 2500)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (panelOpen) {
                    closePanel()
                } else if (webView.canGoBack()) webView.goBack()
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // ============ شروع دانلود از هر دامنه‌ای ============
    private fun startDownload(url: String, userAgent: String?, contentDisposition: String?, mimetype: String?) {
        try {
            // اگه لینک data: یا blob: بود، از JS جواب بگیر
            if (url.startsWith("blob:") || url.startsWith("data:")) {
                Toast.makeText(this, "این نوع لینک پشتیبانی نمی‌شه", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)

            // کوکی‌ها برای حفظ نشست
            val cookies = CookieManager.getInstance().getCookie(url)
            if (cookies != null) request.addRequestHeader("Cookie", cookies)
            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent)
            // Referer برای بعضی هاست‌ها لازمه
            request.addRequestHeader("Referer", "https://appdony.ir/")

            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            request.setTitle(fileName)
            request.setDescription("در حال دانلود از اپدونی...")
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            if (mimetype != null) request.setMimeType(mimetype)

            downloadManager.enqueue(request)
            Toast.makeText(this, "📥 دانلود شروع شد:\n$fileName", Toast.LENGTH_LONG).show()
            updateBadge()
            if (!panelOpen) {
                Toast.makeText(this, "برای دیدن پیشرفت، روی توپ پایین بزن", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ============ Badge شمارنده ============
    private fun updateBadge() {
        val query = DownloadManager.Query()
        val cursor = downloadManager.query(query)
        var activeCount = 0
        while (cursor.moveToNext()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) activeCount++
        }
        cursor.close()
        if (activeCount > 0) {
            badgeView.text = activeCount.toString()
            badgeView.visibility = View.VISIBLE
        } else {
            badgeView.visibility = View.GONE
        }
    }

    private fun openPanel() {
        panelOpen = true
        refreshDownloads()
        downloadPanel.visibility = View.VISIBLE
        downloadPanel.alpha = 0f
        downloadPanel.animate().alpha(1f).setDuration(250).start()
        refreshHandler.post(refreshRunnable)
        updateBadge()
    }

    private fun closePanel() {
        panelOpen = false
        downloadPanel.animate().alpha(0f).setDuration(200).withEndAction {
            downloadPanel.visibility = View.GONE
        }.start()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun refreshDownloads() {
        downloadsList.removeAllViews()
        val query = DownloadManager.Query()
        val cursor: Cursor = downloadManager.query(query)

        val items = mutableListOf<View>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "بدون نام"
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            items.add(0, createDownloadItem(id, title, status, downloaded, total, localUri))
        }
        cursor.close()

        if (items.isEmpty()) {
            val empty = TextView(this).apply {
                text = "هنوز دانلودی نداری 📭"
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 60)
                setTextColor(Color.GRAY)
            }
            downloadsList.addView(empty)
        } else {
            items.forEach { downloadsList.addView(it) }
        }
        updateBadge()
    }

    private fun createDownloadItem(id: Long, title: String, status: Int, downloaded: Long, total: Long, localUri: String?): View {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val accent = if (isDark) Color.parseColor("#4DD0C7") else Color.parseColor("#0D7377")
        val textColor = if (isDark) Color.WHITE else Color.BLACK

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            background = GradientDrawable().apply {
                setColor(if (isDark) Color.parseColor("#2A2A2A") else Color.parseColor("#F5F5F5"))
                cornerRadius = 24f
            }
        }
        box.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 16 }

        val nameTv = TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(textColor)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        box.addView(nameTv)

        val statusTv = TextView(this).apply {
            textSize = 13f
            setPadding(0, 10, 0, 12)
            setTextColor(accent)
            text = when (status) {
                DownloadManager.STATUS_RUNNING -> {
                    val pct = if (total > 0) (downloaded * 100 / total) else 0
                    "⏳ در حال دانلود — $pct٪  (${formatSize(downloaded)} / ${formatSize(total)})"
                }
                DownloadManager.STATUS_SUCCESSFUL -> "✅ کامل شد — ${formatSize(total)}"
                DownloadManager.STATUS_FAILED -> "❌ خطا در دانلود"
                DownloadManager.STATUS_PAUSED -> "⏸ متوقف"
                else -> "در انتظار..."
            }
        }
        box.addView(statusTv)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        if (status == DownloadManager.STATUS_SUCCESSFUL && localUri != null) {
            val installBtn = TextView(this).apply {
                text = "  نصب  "
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(40, 16, 40, 16)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    setColor(accent)
                    cornerRadius = 30f
                }
                setOnClickListener { installApk(localUri) }
            }
            btnRow.addView(installBtn)
        }

        val deleteBtn = TextView(this).apply {
            text = "  حذف  "
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(40, 16, 40, 16)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E53935"))
                cornerRadius = 30f
            }
            setOnClickListener {
                downloadManager.remove(id)
                refreshDownloads()
                Toast.makeText(this@MainActivity, "حذف شد", Toast.LENGTH_SHORT).show()
            }
        }
        btnRow.addView(deleteBtn)
        box.addView(btnRow)

        return box
    }

    private fun installApk(localUri: String) {
        try {
            // اگه اجازه نصب نداریم، کاربر رو ببر به تنظیمات
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(this, "لطفاً اجازه نصب از این منبع را فعال کنید", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:$packageName"))
                    startActivity(intent)
                    return
                }
            }

            val uri = Uri.parse(localUri)
            val file = File(uri.path ?: return)
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            } else {
                uri
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در نصب: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun clearCacheIfVersionChanged() {
        val prefs = getSharedPreferences("appdony_prefs", MODE_PRIVATE)
        val savedVersion = prefs.getInt("last_version", -1)
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionCode
        } catch (e: Exception) { -1 }

        if (savedVersion != currentVersion) {
            WebView(this).apply {
                clearCache(true); clearHistory(); clearFormData(); destroy()
            }
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            prefs.edit().putInt("last_version", currentVersion).apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(downloadCompleteReceiver) } catch (e: Exception) {}
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    class SpinnerLoader(context: Context, private val isDark: Boolean, private val strokeW: Float) : View(context) {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeWidth = strokeW
        }
        private val rect = android.graphics.RectF()
        private val colorMain = if (isDark) Color.parseColor("#4DD0C7") else Color.parseColor("#0D7377")
        private val colorLight = if (isDark) Color.parseColor("#7FE5DD") else Color.parseColor("#2AA5A0")
        private val colorTransparent = if (isDark) Color.parseColor("#204DD0C7") else Color.parseColor("#200D7377")
        private var rotation = 0f
        private var sweep = 90f

        init {
            android.animation.ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 1400
                repeatCount = android.animation.ValueAnimator.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener { rotation = it.animatedValue as Float; invalidate() }
                start()
            }
            android.animation.ValueAnimator.ofFloat(30f, 280f).apply {
                duration = 1600
                repeatCount = android.animation.ValueAnimator.INFINITE
                repeatMode = android.animation.ValueAnimator.REVERSE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addUpdateListener { sweep = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val r = (minOf(width, height) - strokeW) / 2f
            rect.set(cx - r, cy - r, cx + r, cy + r)
            paint.shader = android.graphics.SweepGradient(cx, cy,
                intArrayOf(colorMain, colorLight, colorTransparent, colorMain),
                floatArrayOf(0f, 0.35f, 0.75f, 1f))
            canvas.save()
            canvas.rotate(rotation, cx, cy)
            canvas.drawArc(rect, 0f, sweep, false, paint)
            canvas.restore()
        }
    }

    private fun injectCustomCSS() {
        val js = """
            (function() {
                var old = document.getElementById('appdony-custom-style');
                if (old) old.remove();
                var css = `
                    * {
                        -webkit-tap-highlight-color: transparent !important;
                        -webkit-touch-callout: none !important;
                    }
                    button:not(.bottom-nav-item):not(.device-chip):not(.cat-link),
                    input[type="submit"], input[type="button"],
                    .elementor-button, a.btn-install,
                    [class*="download-btn"], [class*="btn-download"] {
                        border-radius: 20px !important;
                        padding: 12px 26px !important;
                        font-weight: 600 !important;
                        transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
                    }
                    button:not(.bottom-nav-item):not(.device-chip):not(.cat-link):active,
                    input[type="submit"]:active, .elementor-button:active, a.btn-install:active {
                        transform: scale(0.95) !important;
                    }
                    .bottom-nav, .bottom-nav *, .bottom-nav-item, .bottom-nav-item * {
                        -webkit-tap-highlight-color: transparent !important;
                        outline: none !important;
                    }
                    .bottom-nav a, .bottom-nav-item { background-color: transparent !important; }
                    .bottom-nav a:active, .bottom-nav-item:active { background-color: transparent !important; }
                    .bottom-nav .active, .bottom-nav-item.active,
                    .bottom-nav a.active, .bottom-nav a[aria-current="page"],
                    .bottom-nav li.active > a {
                        background-color: rgba(13, 115, 119, 0.12) !important;
                        border-radius: 50px !important;
                        color: #0D7377 !important;
                        box-shadow: none !important;
                        padding: 6px 20px !important;
                        animation: appdonyPillIn 0.55s cubic-bezier(0.34, 1.4, 0.64, 1) !important;
                    }
                    @keyframes appdonyPillIn {
                        0% { background-color: rgba(13, 115, 119, 0) !important; transform: scale(0.85); opacity: 0.4; }
                        60% { background-color: rgba(13, 115, 119, 0.15) !important; transform: scale(1.03); opacity: 1; }
                        100% { background-color: rgba(13, 115, 119, 0.12) !important; transform: scale(1); opacity: 1; }
                    }
                    .bottom-nav .active i, .bottom-nav .active svg, .bottom-nav .active span,
                    .bottom-nav-item.active i, .bottom-nav-item.active span {
                        color: #0D7377 !important;
                        fill: #0D7377 !important;
                    }
                    .cat-link { -webkit-tap-highlight-color: transparent !important; }
                    input[type="text"], input[type="search"], input[type="email"],
                    input[type="password"], textarea { border-radius: 16px !important; }
                    ::-webkit-scrollbar { width: 5px; height: 5px; }
                    ::-webkit-scrollbar-thumb { background: rgba(13, 115, 119, 0.5); border-radius: 5px; }
                    ::-webkit-scrollbar-track { background: transparent; }
                `;
                var style = document.createElement('style');
                style.id = 'appdony-custom-style';
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                document.head.appendChild(style);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}
