package com.appdony.app

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashView: FrameLayout
    private lateinit var topProgress: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ============ 🌟 ست کردن رنگ Status Bar ============
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val splashBg = if (isDark) Color.parseColor("#001C1A") else Color.WHITE
        val accentColor = if (isDark) Color.parseColor("#4DD0C7") else Color.parseColor("#0D7377")

        // رنگ نوار بالا (Status Bar) — سفید در روشن، تیره در تاریک
        window.statusBarColor = splashBg

        // رنگ آیکون‌های نوار بالا (ساعت، باتری، وای‌فای)
        // در حالت روشن: آیکون‌های تیره، در حالت تاریک: آیکون‌های روشن
        val wic = WindowInsetsControllerCompat(window, window.decorView)
        wic.isAppearanceLightStatusBars = !isDark

        // ============ UI ============
        val root = FrameLayout(this)

        // SPLASH
        splashView = FrameLayout(this).apply {
            setBackgroundColor(splashBg)
        }
        val loader = SpinnerLoader(this, isDark)
        val loaderParams = FrameLayout.LayoutParams(260, 260)
        loaderParams.gravity = Gravity.CENTER
        splashView.addView(loader, loaderParams)

        // نوار پیشرفت بالای صفحه
        topProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
            progressTintList = ColorStateList.valueOf(accentColor)
            progressBackgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        }
        val topBarParams = FrameLayout.LayoutParams(-1, 8)
        topBarParams.gravity = Gravity.TOP

        // WEBVIEW
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
            }
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    topProgress.visibility = View.VISIBLE
                    topProgress.progress = 10
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    topProgress.progress = 100
                    Handler(Looper.getMainLooper()).postDelayed({
                        topProgress.visibility = View.GONE
                    }, 250)
                    injectCustomCSS()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    topProgress.progress = newProgress
                    if (newProgress >= 100) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            topProgress.visibility = View.GONE
                        }, 250)
                    }
                }
            }

            loadUrl("https://appdony.ir/")
        }

        root.addView(splashView, FrameLayout.LayoutParams(-1, -1))
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        root.addView(topProgress, topBarParams)
        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            splashView.animate()
                .alpha(0f)
                .setDuration(600)
                .withEndAction {
                    splashView.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                }
                .start()
        }, 2500)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // لودینگ دایره‌ای
    class SpinnerLoader(context: Context, private val isDark: Boolean) : View(context) {
        private val strokeWidth = 18f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = strokeWidth
        }
        private val rect = RectF()
        private val colorMain = if (isDark) Color.parseColor("#4DD0C7") else Color.parseColor("#0D7377")
        private val colorLight = if (isDark) Color.parseColor("#7FE5DD") else Color.parseColor("#2AA5A0")
        private val colorTransparent = if (isDark) Color.parseColor("#204DD0C7") else Color.parseColor("#200D7377")

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
            val cx = width / 2f
            val cy = height / 2f
            val r = (minOf(width, height) - strokeWidth) / 2f
            rect.set(cx - r, cy - r, cx + r, cy + r)
            paint.shader = SweepGradient(cx, cy,
                intArrayOf(colorMain, colorLight, colorTransparent, colorMain),
                floatArrayOf(0f, 0.35f, 0.75f, 1f))
            canvas.save()
            canvas.rotate(rotation, cx, cy)
            canvas.drawArc(rect, 0f, sweep, false, paint)
            canvas.restore()
        }
    }

    // تزریق CSS
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
                    .bottom-nav a, .bottom-nav-item {
                        background-color: transparent !important;
                    }
                    .bottom-nav a:active, .bottom-nav-item:active {
                        background-color: transparent !important;
                    }

                    .bottom-nav .active,
                    .bottom-nav-item.active,
                    .bottom-nav a.active,
                    .bottom-nav a[aria-current="page"],
                    .bottom-nav li.active > a {
                        background-color: rgba(13, 115, 119, 0.12) !important;
                        border-radius: 50px !important;
                        color: #0D7377 !important;
                        box-shadow: none !important;
                        padding: 6px 20px !important;
                        animation: appdonyPillIn 0.55s cubic-bezier(0.34, 1.4, 0.64, 1) !important;
                        transform-origin: center !important;
                    }
                    @keyframes appdonyPillIn {
                        0% {
                            background-color: rgba(13, 115, 119, 0) !important;
                            transform: scale(0.85);
                            opacity: 0.4;
                        }
                        60% {
                            background-color: rgba(13, 115, 119, 0.15) !important;
                            transform: scale(1.03);
                            opacity: 1;
                        }
                        100% {
                            background-color: rgba(13, 115, 119, 0.12) !important;
                            transform: scale(1);
                            opacity: 1;
                        }
                    }
                    .bottom-nav .active i, .bottom-nav .active svg, .bottom-nav .active span,
                    .bottom-nav-item.active i, .bottom-nav-item.active span {
                        color: #0D7377 !important;
                        fill: #0D7377 !important;
                    }

                    .cat-link {
                        -webkit-tap-highlight-color: transparent !important;
                    }

                    input[type="text"], input[type="search"], input[type="email"],
                    input[type="password"], textarea {
                        border-radius: 16px !important;
                    }

                    ::-webkit-scrollbar { width: 5px; height: 5px; }
                    ::-webkit-scrollbar-thumb {
                        background: rgba(13, 115, 119, 0.5);
                        border-radius: 5px;
                    }
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
