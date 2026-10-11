package com.appdony.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashView: FrameLayout
    private lateinit var miniLoader: SpinnerLoader
    private lateinit var root: FrameLayout
    private lateinit var ball: FloatingBall
    private lateinit var panel: DownloadPanel
    private lateinit var settingsPanel: SettingsPanel
    private lateinit var ballMenu: BallMenu
    private lateinit var helper: DownloadHelper
    private lateinit var settingsManager: SettingsManager

    private var isDark = false
    private var accent = 0
    private var loaderShownAt = 0L
    private val minLoaderTime = 250L
    private val handler = Handler(Looper.getMainLooper())

    private var lastClickX = 0f
    private var lastClickY = 0f
    private var lastClickTime = 0L

    private val maxLoaderRunnable = Runnable { hideLoader() }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Toast.makeText(this@MainActivity, "دانلود کامل شد", Toast.LENGTH_SHORT).show()
            ball.updateBadge(helper.getActiveCount())
            if (panel.isOpen) panel.refresh()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsManager = SettingsManager(this)
        isDark = settingsManager.isDark()
        accent = if (isDark) Color.parseColor("#4DD0C7") else Color.parseColor("#0D7377")

        val splashBg = if (isDark) Color.parseColor("#001C1A") else Color.WHITE
        window.statusBarColor = splashBg
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDark

        clearCacheIfVersionChanged()
        helper = DownloadHelper(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        registerReceiver(
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RECEIVER_NOT_EXPORTED else 0
        )

        root = FrameLayout(this)

        // SPLASH
        splashView = FrameLayout(this).apply { setBackgroundColor(splashBg) }
        splashView.addView(SpinnerLoader(this, isDark, 30f),
            FrameLayout.LayoutParams(280, 280).apply { gravity = Gravity.CENTER })

        // MINI LOADER
        miniLoader = SpinnerLoader(this, isDark, 18f).apply { visibility = View.GONE }

        // PANELS
        panel = DownloadPanel(this, isDark, accent, helper) { count ->
            ball.updateBadge(count)
        }

        settingsPanel = SettingsPanel(
            this, accent, settingsManager,
            onThemeChanged = { applyTheme() },
            onFontChanged = { applyFontSize() }
        )

        // BALL
        ball = FloatingBall(this, accent) {
            if (ballMenu.isOpen) ballMenu.hide()
            else ballMenu.show(ball.view.x, ball.view.y, ball.size)
        }
        ball.view.alpha = 0f
        ball.view.scaleX = 0.8f
        ball.view.scaleY = 0.8f
        ball.view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400).start()

        val ballParams = FrameLayout.LayoutParams(ball.size, ball.size).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // MENU
        ballMenu = BallMenu(this, accent).apply {
            setOnDownloads {
                if (panel.isOpen) panel.close() else panel.open()
            }
            setOnSettings {
                if (settingsPanel.isOpen) settingsPanel.close() else settingsPanel.open()
            }
        }

        // WEBVIEW
        val initialUrl = intent?.data?.toString()
            ?: "https://appdony.ir/?v=${System.currentTimeMillis() / (1000 * 60 * 60 * 24)}"

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
                mediaPlaybackRequiresUserGesture = false
                setSupportMultipleWindows(false)
                allowFileAccess = true
                allowContentAccess = true
                textZoom = settingsManager.getTextZoom()
            }
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            addJavascriptInterface(object {
                @JavascriptInterface
                fun onAnyClick(x: Float, y: Float) {
                    lastClickX = x
                    lastClickY = y
                    lastClickTime = System.currentTimeMillis()
                }

                @JavascriptInterface
                fun onNavStart() {
                    runOnUiThread { showLoader() }
                }

                @JavascriptInterface
                fun onContentReady() {
                    runOnUiThread { hideLoader() }
                }
            }, "AppdonyBridge")

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    showLoader()
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectCustomCSS()
                    injectScript()
                    applyThemeCss()
                    hideLoader()
                }
                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    injectCustomCSS()
                    injectScript()
                    applyThemeCss()
                    hideLoader()
                }
                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    injectCustomCSS()
                    injectScript()
                    applyThemeCss()
                    hideLoader()
                }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) hideLoader()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress >= 40) hideLoader()
                }
            }

            setDownloadListener { url, ua, cd, mt, _ ->
                triggerFlyerFromLastClick()
                val id = helper.startDownload(url, ua, cd, mt)
                if (id > 0) {
                    Toast.makeText(this@MainActivity, "دانلود شروع شد", Toast.LENGTH_SHORT).show()
                    ball.updateBadge(helper.getActiveCount())
                    fetchAndSaveIcon(id)
                } else {
                    Toast.makeText(this@MainActivity, "خطا در دانلود", Toast.LENGTH_SHORT).show()
                }
            }

            loadUrl(initialUrl)
        }

        root.addView(splashView, FrameLayout.LayoutParams(-1, -1))
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        root.addView(miniLoader, FrameLayout.LayoutParams(180, 180).apply { gravity = Gravity.CENTER })
        root.addView(panel.view, FrameLayout.LayoutParams(-1, -1))
        root.addView(settingsPanel.view, FrameLayout.LayoutParams(-1, -1))
        root.addView(ballMenu.view, FrameLayout.LayoutParams(-1, -1))
        root.addView(ball.view, ballParams)
        setContentView(root)

        root.post {
            val margin = 40f
            ball.view.x = root.width - ball.size - margin
            ball.view.y = root.height - ball.size - 380f
        }

        ball.updateBadge(helper.getActiveCount())

        handler.postDelayed({
            splashView.animate().alpha(0f).setDuration(600).withEndAction {
                splashView.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }.start()
        }, 2500)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    ballMenu.isOpen -> ballMenu.hide()
                    panel.isOpen -> panel.close()
                    settingsPanel.isOpen -> settingsPanel.close()
                    webView.canGoBack() -> webView.goBack()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    // اعمال حالت شب/روز
    private fun applyTheme() {
        isDark = settingsManager.isDark()
        accent = if (isDark) Color.parseColor("#4DD0C7") else Color.parseColor("#0D7377")
        applyThemeCss()
    }

    // تزریق CSS حالت تاریک به سایت
    private fun applyThemeCss() {
        val css = if (settingsManager.isDark()) {
            "html { filter: invert(1) hue-rotate(180deg) !important; background: #0f0f0f !important; } " +
            "img, video, iframe, canvas, svg, [style*='background-image'] { filter: invert(1) hue-rotate(180deg) !important; }"
        } else ""

        val escaped = css.replace("\\", "\\\\").replace("\"", "\\\"")

        val js = """
            (function() {
                var old = document.getElementById('appdony-theme-style');
                if (old) old.remove();
                if ("$escaped".length === 0) return;
                var s = document.createElement('style');
                s.id = 'appdony-theme-style';
                s.appendChild(document.createTextNode("$escaped"));
                (document.head || document.documentElement).appendChild(s);
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    // اعمال سایز فونت
    private fun applyFontSize() {
        webView.settings.textZoom = settingsManager.getTextZoom()
    }

    // انیمیشن پرنده
    private fun triggerFlyerFromLastClick() {
        val timeSinceClick = System.currentTimeMillis() - lastClickTime
        if (timeSinceClick in 0..3000 && lastClickX > 0 && lastClickY > 0) {
            handler.postDelayed({ flyIconToBall(lastClickX, lastClickY) }, 150)
        }
    }

    private fun flyIconToBall(cssX: Float, cssY: Float) {
        val density = resources.displayMetrics.density
        val startX = cssX * density
        val startY = cssY * density

        val ballCenterX = ball.view.x + ball.size / 2f
        val ballCenterY = ball.view.y + ball.size / 2f

        val flyerSize = 90

        val flyer = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent)
                setStroke(6, Color.WHITE)
            }
            elevation = 25f
        }
        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_carton)
            setPadding(20, 20, 20, 20)
        }
        flyer.addView(icon, FrameLayout.LayoutParams(-1, -1))

        root.addView(flyer, FrameLayout.LayoutParams(flyerSize, flyerSize).apply {
            leftMargin = (startX - flyerSize / 2).toInt()
            topMargin = (startY - flyerSize / 2).toInt()
        })

        flyer.scaleX = 0.3f
        flyer.scaleY = 0.3f
        flyer.alpha = 0f

        flyer.animate()
            .scaleX(1.15f).scaleY(1.15f).alpha(1f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                flyer.animate()
                    .x(ballCenterX - flyerSize / 2f)
                    .y(ballCenterY - flyerSize / 2f)
                    .scaleX(0.35f).scaleY(0.35f)
                    .alpha(0.6f)
                    .setDuration(1300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        root.removeView(flyer)
                        ball.view.animate()
                            .scaleX(1.15f).scaleY(1.15f)
                            .setDuration(180)
                            .withEndAction {
                                ball.view.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun fetchAndSaveIcon(downloadId: Long) {
        webView.evaluateJavascript("""
            (function() {
                var m = document.querySelector('meta[property="og:image"]');
                if (m && m.content) return m.content;
                var link = document.querySelector('link[rel="apple-touch-icon"]');
                if (link && link.href) return link.href;
                return '';
            })();
        """.trimIndent()) { result ->
            val url = result?.trim('"') ?: ""
            if (url.startsWith("http")) helper.saveIcon(downloadId, url)
        }
    }

    private fun showLoader() {
        loaderShownAt = System.currentTimeMillis()
        miniLoader.visibility = View.VISIBLE
        ball.view.visibility = View.GONE
        handler.removeCallbacks(maxLoaderRunnable)
        handler.postDelayed(maxLoaderRunnable, 3000)
    }

    private fun hideLoader() {
        handler.removeCallbacks(maxLoaderRunnable)
        val elapsed = System.currentTimeMillis() - loaderShownAt
        val delay = if (loaderShownAt > 0 && elapsed < minLoaderTime) minLoaderTime - elapsed else 0L
        handler.postDelayed({
            miniLoader.visibility = View.GONE
            ball.view.visibility = View.VISIBLE
        }, delay)
        loaderShownAt = 0L
    }

    private fun clearCacheIfVersionChanged() {
        val prefs = getSharedPreferences("appdony_prefs", MODE_PRIVATE)
        val saved = prefs.getInt("last_version", -1)
        val current = try { packageManager.getPackageInfo(packageName, 0).versionCode } catch (e: Exception) { -1 }
        if (saved != current) {
            WebView(this).apply { clearCache(true); clearHistory(); clearFormData(); destroy() }
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            prefs.edit().putInt("last_version", current).apply()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { webView.loadUrl(it.toString()) }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(downloadReceiver) } catch (_: Exception) {}
        panel.destroy()
        settingsPanel.destroy()
        handler.removeCallbacks(maxLoaderRunnable)
    }

    private fun injectCustomCSS() {
        try {
            val css = assets.open("style.css").bufferedReader().use { it.readText() }
            val escaped = escapeForJs(css)
            val code = """
                (function() {
                    function ensureCss() {
                        if (document.getElementById('appdony-custom-style')) return;
                        var s = document.createElement('style');
                        s.id = 'appdony-custom-style';
                        s.appendChild(document.createTextNode("$escaped"));
                        (document.head || document.documentElement).appendChild(s);
                    }
                    ensureCss();
                    if (!window._appdonyCssObserver) {
                        window._appdonyCssObserver = new MutationObserver(function() {
                            if (!document.getElementById('appdony-custom-style')) ensureCss();
                        });
                        var head = document.head || document.documentElement;
                        window._appdonyCssObserver.observe(head, { childList: true });
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(code, null)
        } catch (_: Exception) {}
    }

    private fun injectScript() {
        try {
            val js = assets.open("script.js").bufferedReader().use { it.readText() }
            webView.evaluateJavascript(js, null)
        } catch (_: Exception) {}
    }

    private fun escapeForJs(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
