package com.appdony.app

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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

        // تشخیص حالت شب/روز گوشی
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val splashBg = if (isDark) Color.parseColor("#001C1A") else Color.parseColor("#DFF5F2")

        // ============ SPLASH با لودینگ گوگل‌پلی‌ای ============
        splashView = FrameLayout(this).apply {
            setBackgroundColor(splashBg)
        }
        val loader = MorphingLoader(this, isDark)
        val loaderParams = FrameLayout.LayoutParams(300, 300)
        loaderParams.gravity = Gravity.CENTER
        splashView.addView(loader, loaderParams)

        // ============ WEBVIEW ============
        webView = WebView(this).apply {
            visibility = View.GONE
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
            }
            webViewClient = object : WebViewClient() {
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
                .setDuration(500)
                .withEndAction {
                    splashView.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    webView.alpha = 0f
                    webView.animate().alpha(1f).setDuration(500).start()
                }
                .start()
        }, 2500)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // ============ لودینگ متغیر مثل گوگل پلی ============
    class MorphingLoader(context: Context, private val isDark: Boolean) : View(context) {

        private var sides: Float = 3f
        private var rotation: Float = 0f
        private var scale: Float = 1f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#4DD0C7") else Color.parseColor("#0D7377")
            style = Paint.Style.FILL
        }

        private val path = Path()

        init {
            // تغییر تعداد اضلاع (۳ تا ۸) — مثل گوگل پلی
            ValueAnimator.ofFloat(3f, 8f).apply {
                duration = 2000
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { sides = it.animatedValue as Float; invalidate() }
                start()
            }
            // چرخش دائم
            ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 3500
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { rotation = it.animatedValue as Float; invalidate() }
                start()
            }
            // پالس ملایم
            ValueAnimator.ofFloat(0.82f, 1f).apply {
                duration = 900
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { scale = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val r = (minOf(width, height) / 2f - 24f) * scale

            path.reset()
            val n = sides.toInt().coerceAtLeast(3)
            val angleStep = 2 * Math.PI / n
            for (i in 0 until n) {
                val a = i * angleStep + Math.toRadians(rotation.toDouble())
                val x = cx + (r * Math.cos(a)).toFloat()
                val y = cy + (r * Math.sin(a)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    // ============ تزریق CSS برای زیباسازی ============
    private fun injectCustomCSS() {
        val js = """
            (function() {
                if (document.getElementById('appdony-custom-style')) return;

                var css = `
                    /* ==== دکمه‌ها ==== */
                    button, .btn, input[type="submit"], input[type="button"],
                    a.button, [class*="btn"], [class*="button"], [role="button"] {
                        border-radius: 18px !important;
                        transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1),
                                    box-shadow 0.3s ease,
                                    background-color 0.3s ease !important;
                        -webkit-tap-highlight-color: transparent !important;
                        overflow: hidden !important;
                        position: relative !important;
                    }
                    button:active, .btn:active, [class*="btn"]:active,
                    [class*="button"]:active, [role="button"]:active {
                        transform: scale(0.92) !important;
                        box-shadow: 0 2px 8px rgba(13, 115, 119, 0.3) !important;
                    }

                    /* ==== کارت‌ها ==== */
                    .card, .post, article, [class*="card"], [class*="item"],
                    [class*="post"], [class*="product"], [class*="box"] {
                        border-radius: 20px !important;
                        transition: transform 0.35s ease, box-shadow 0.35s ease !important;
                    }
                    .card:active, article:active, [class*="card"]:active, [class*="item"]:active {
                        transform: scale(0.97) !important;
                        box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15) !important;
                    }

                    /* ==== عکس‌ها ==== */
                    img {
                        border-radius: 16px !important;
                        transition: transform 0.4s ease, filter 0.4s ease !important;
                    }
                    img:active {
                        transform: scale(0.97) !important;
                        filter: brightness(0.95) !important;
                    }

                    /* ==== اینپوت‌ها ==== */
                    input, textarea, select {
                        border-radius: 16px !important;
                        transition: box-shadow 0.3s ease, border-color 0.3s ease !important;
                    }
                    input:focus, textarea:focus, select:focus {
                        box-shadow: 0 0 0 4px rgba(13, 115, 119, 0.2) !important;
                        border-color: #0D7377 !important;
                        outline: none !important;
                    }

                    /* ==== لینک‌ها ==== */
                    a {
                        transition: color 0.25s ease, opacity 0.25s ease, transform 0.25s ease !important;
                        -webkit-tap-highlight-color: transparent !important;
                    }
                    a:active {
                        opacity: 0.65 !important;
                        transform: scale(0.97) !important;
                    }

                    /* ==== اسکرول نرم ==== */
                    html { scroll-behavior: smooth !important; }

                    /* ==== اسکرول‌بار سبز ==== */
                    ::-webkit-scrollbar { width: 6px; height: 6px; }
                    ::-webkit-scrollbar-thumb {
                        background: rgba(13, 115, 119, 0.55);
                        border-radius: 6px;
                    }
                    ::-webkit-scrollbar-thumb:hover {
                        background: rgba(13, 115, 119, 0.85);
                    }

                    /* ==== آیتم‌های منو ==== */
                    li, .menu-item, [class*="menu"] a {
                        transition: background-color 0.3s ease, transform 0.3s ease !important;
                    }
                    li:active, .menu-item:active {
                        transform: translateX(4px) !important;
                    }

                    /* ==== هدر و نوار بالا (افکت شیشه) ==== */
                    header, [class*="header"], [class*="navbar"], [class*="top-bar"] {
                        backdrop-filter: blur(10px) !important;
                        -webkit-backdrop-filter: blur(10px) !important;
                        transition: box-shadow 0.3s ease !important;
                    }

                    /* ==== انیمیشن ورود صفحه ==== */
                    body {
                        animation: appdonyFadeIn 0.4s ease-in !important;
                    }
                    @keyframes appdonyFadeIn {
                        from { opacity: 0; transform: translateY(8px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                `;

                var style = document.createElement('style');
                style.id = 'appdony-custom-style';
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                document.head.appendChild(style);

                // ==== انیمیشن ضربه روی هر چیزی که قابل کلیک باشه ====
                document.addEventListener('touchstart', function(e) {
                    var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"], [class*="card"], [class*="item"]');
                    if (t) {
                        t.style.transition = 'transform 0.1s cubic-bezier(0.34, 1.56, 0.64, 1)';
                        t.style.transform = 'scale(0.94)';
                    }
                }, { passive: true });

                document.addEventListener('touchend', function(e) {
                    var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"], [class*="card"], [class*="item"]');
                    if (t) {
                        setTimeout(function() {
                            t.style.transform = '';
                            t.style.transition = 'transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1)';
                        }, 90);
                    }
                }, { passive: true });

                document.addEventListener('touchcancel', function(e) {
                    var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"]');
                    if (t) {
                        t.style.transform = '';
                    }
                }, { passive: true });
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}
