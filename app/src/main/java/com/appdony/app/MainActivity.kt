package com.appdony.app

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashView: FrameLayout

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val splashBg = if (isDark) Color.parseColor("#001C1A") else Color.parseColor("#DFF5F2")

        // ============ SPLASH با لودینگ دایره‌ای ============
        splashView = FrameLayout(this).apply {
            setBackgroundColor(splashBg)
        }
        val loader = SpinnerLoader(this, isDark)
        val loaderParams = FrameLayout.LayoutParams(260, 260)
        loaderParams.gravity = Gravity.CENTER
        splashView.addView(loader, loaderParams)

        // ============ WEBVIEW ============
        webView = WebView(this).apply {
            visibility = View.GONE
            setBackgroundColor(splashBg)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    // بدون animation برای رفع سفیدی
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectCustomCSS()
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl("https://appdony.ir/")
        }

        root.addView(splashView, FrameLayout.LayoutParams(-1, -1))
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        // مخفی کردن Splash بعد از ۲.۵ ثانیه
        Handler(Looper.getMainLooper()).postDelayed({
            splashView.animate()
                .alpha(0f)
                .setDuration(600)
                .withEndAction {
                    splashView.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    webView.alpha = 0f
                    webView.animate().alpha(1f).setDuration(500).start()
                }
                .start()
        }, 2500)

        // مدیریت دکمه برگشت
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

    // ============ لودینگ دایره‌ای با گرادیانت ============
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
            paint.shader = SweepGradient(
                cx, cy,
                intArrayOf(colorMain, colorLight, colorTransparent, colorMain),
                floatArrayOf(0f, 0.35f, 0.75f, 1f)
            )
            canvas.save()
            canvas.rotate(rotation, cx, cy)
            canvas.drawArc(rect, 0f, sweep, false, paint)
            canvas.restore()
        }
    }

    // ============ 🎨 تزریق CSS ============
    private fun injectCustomCSS() {
        val js = """
            (function() {
                var old = document.getElementById('appdony-custom-style');
                if (old) old.remove();

                var css = `
                    /* ===== فقط دکمه‌های اصلی — نه نوار پایین، نه نوار بالا ===== */
                    button:not(.bottom-nav-item):not(.device-chip):not(.cat-link),
                    input[type="submit"],
                    input[type="button"],
                    .elementor-button,
                    a.btn-install,
                    [class*="download-btn"],
                    [class*="btn-download"] {
                        border-radius: 20px !important;
                        padding: 12px 26px !important;
                        font-weight: 600 !important;
                        transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
                                    box-shadow 0.25s ease !important;
                        -webkit-tap-highlight-color: transparent !important;
                    }
                    button:not(.bottom-nav-item):not(.device-chip):not(.cat-link):active,
                    input[type="submit"]:active,
                    input[type="button"]:active,
                    .elementor-button:active,
                    a.btn-install:active {
                        transform: scale(0.95) !important;
                        box-shadow: 0 2px 6px rgba(13, 115, 119, 0.3) !important;
                    }

                    /* ===== نوار پایین — دست نزن ===== */
                    .bottom-nav-item {
                        border-radius: 12px !important;
                        transition: background-color 0.25s ease, transform 0.25s ease !important;
                    }
                    .bottom-nav-item:active {
                        transform: scale(0.95) !important;
                    }

                    /* ===== نوار دسته‌بندی بالا ===== */
                    .cat-link {
                        transition: color 0.25s ease, border-color 0.25s ease !important;
                    }

                    /* ===== اینپوت سرچ ===== */
                    input[type="text"],
                    input[type="search"],
                    input[type="email"],
                    input[type="password"],
                    textarea {
                        border-radius: 16px !important;
                    }

                    /* ===== اسکرول‌بار سبز نازک ===== */
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
