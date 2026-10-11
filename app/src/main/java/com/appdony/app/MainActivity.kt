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
                    view?.alpha = 0f
                    view?.animate()?.alpha(1f)?.setDuration(350)?.start()
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.alpha = 1f
                    injectCustomCSS()
                }
                // ⭐ تزریق روی هر صفحه جدید داخل SPA
                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    injectCustomCSS()
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl("https://appdony.ir/")
        }

        root.addView(splashView, FrameLayout.LayoutParams(-1, -1))
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

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
            paint.shader = SweepGradient(cx, cy,
                intArrayOf(colorMain, colorLight, colorTransparent, colorMain),
                floatArrayOf(0f, 0.35f, 0.75f, 1f))
            canvas.save()
            canvas.rotate(rotation, cx, cy)
            canvas.drawArc(rect, 0f, sweep, false, paint)
            canvas.restore()
        }
    }

    // ============ 🎨 تزریق CSS — قدرت اصلی! ============
    private fun injectCustomCSS() {
        val js = """
            (function() {
                // ⭐ حذف استایل قبلی (برای تزریق مجدد)
                var old = document.getElementById('appdony-custom-style');
                if (old) old.remove();

                var css = `
                    /* ===== دکمه‌ها ===== */
                    button, .btn, input[type="submit"], input[type="button"],
                    a.button, a[class*="btn"], [class*="button"]:not(.no-style),
                    [role="button"], .elementor-button {
                        border-radius: 22px !important;
                        padding: 12px 24px !important;
                        transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
                                    box-shadow 0.35s ease,
                                    background 0.35s ease !important;
                        -webkit-tap-highlight-color: transparent !important;
                        font-weight: 600 !important;
                        letter-spacing: 0.3px !important;
                    }
                    button:active, .btn:active, [class*="button"]:active,
                    [role="button"]:active, .elementor-button:active {
                        transform: scale(0.93) !important;
                        box-shadow: 0 2px 8px rgba(13, 115, 119, 0.35) !important;
                    }

                    /* ===== کارت‌ها ===== */
                    .card, .post, article, [class*="card"], [class*="post"],
                    [class*="product"], [class*="box"], .elementor-post {
                        border-radius: 24px !important;
                        overflow: hidden !important;
                        transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
                                    box-shadow 0.4s ease !important;
                    }
                    .card:active, article:active, [class*="card"]:active,
                    [class*="post"]:active, .elementor-post:active {
                        transform: scale(0.97) translateY(2px) !important;
                        box-shadow: 0 12px 28px rgba(0, 0, 0, 0.15) !important;
                    }

                    /* ===== عکس‌ها ===== */
                    img {
                        border-radius: 18px !important;
                        transition: transform 0.5s ease, filter 0.4s ease !important;
                    }
                    img:active {
                        transform: scale(0.96) !important;
                        filter: brightness(0.92) saturate(1.1) !important;
                    }
                    /* عکس‌های داخل کارت نباید گردی اضافه بگیرن */
                    .card img, article img, [class*="card"] img {
                        border-radius: 0 !important;
                    }

                    /* ===== اینپوت‌ها ===== */
                    input:not([type="submit"]):not([type="button"]),
                    textarea, select {
                        border-radius: 18px !important;
                        transition: box-shadow 0.35s ease, border-color 0.35s ease,
                                    background 0.35s ease !important;
                        padding: 12px 16px !important;
                    }
                    input:focus, textarea:focus, select:focus {
                        box-shadow: 0 0 0 4px rgba(13, 115, 119, 0.25) !important;
                        border-color: #0D7377 !important;
                        outline: none !important;
                    }

                    /* ===== لینک‌ها ===== */
                    a {
                        transition: color 0.3s ease, opacity 0.3s ease,
                                    transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
                        -webkit-tap-highlight-color: transparent !important;
                    }
                    a:active {
                        opacity: 0.7 !important;
                    }

                    /* ===== اسکرول نرم ===== */
                    html { scroll-behavior: smooth !important; }

                    /* ===== اسکرول‌بار سبز ===== */
                    ::-webkit-scrollbar { width: 6px; height: 6px; }
                    ::-webkit-scrollbar-thumb {
                        background: linear-gradient(180deg, #0D7377, #2AA5A0);
                        border-radius: 6px;
                    }
                    ::-webkit-scrollbar-track { background: transparent; }

                    /* ===== هدر شیشه‌ای ===== */
                    header, [class*="header"], [class*="navbar"],
                    [class*="top-bar"], .elementor-sticky {
                        backdrop-filter: blur(14px) saturate(1.2) !important;
                        -webkit-backdrop-filter: blur(14px) saturate(1.2) !important;
                        transition: box-shadow 0.35s ease, background 0.35s ease !important;
                    }

                    /* ===== آیتم‌های منو ===== */
                    li, .menu-item, [class*="menu"] a, nav a {
                        transition: background-color 0.3s ease,
                                    transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
                    }

                    /* ===== انیمیشن ورود صفحه ===== */
                    body {
                        animation: appdonyFadeIn 0.5s ease-out !important;
                    }
                    @keyframes appdonyFadeIn {
                        from { opacity: 0; transform: translateY(12px); }
                        to { opacity: 1; transform: translateY(0); }
                    }

                    /* ===== انیمیشن ورود کارت‌ها (یکی یکی) ===== */
                    .card, article, [class*="card"]:not(.no-anim), .elementor-post {
                        animation: appdonyCardIn 0.55s cubic-bezier(0.22, 1, 0.36, 1) backwards;
                    }
                    @keyframes appdonyCardIn {
                        from { opacity: 0; transform: translateY(20px) scale(0.96); }
                        to { opacity: 1; transform: translateY(0) scale(1); }
                    }
                    .card:nth-child(1), article:nth-child(1) { animation-delay: 0.05s; }
                    .card:nth-child(2), article:nth-child(2) { animation-delay: 0.10s; }
                    .card:nth-child(3), article:nth-child(3) { animation-delay: 0.15s; }
                    .card:nth-child(4), article:nth-child(4) { animation-delay: 0.20s; }
                    .card:nth-child(5), article:nth-child(5) { animation-delay: 0.25s; }
                    .card:nth-child(6), article:nth-child(6) { animation-delay: 0.30s; }
                `;

                var style = document.createElement('style');
                style.id = 'appdony-custom-style';
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                document.head.appendChild(style);

                // ===== افکت لمس روی عناصر =====
                document.addEventListener('touchstart', function(e) {
                    var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"], [class*="card"], [class*="item"], .elementor-button');
                    if (t) {
                        t.style.transition = 'transform 0.12s cubic-bezier(0.34, 1.56, 0.64, 1)';
                        t.style.transform = 'scale(0.94)';
                    }
                }, { passive: true });

                document.addEventListener('touchend', function(e) {
                    var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"], [class*="card"], [class*="item"], .elementor-button');
                    if (t) {
                        setTimeout(function() {
                            t.style.transform = '';
                            t.style.transition = 'transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)';
                        }, 90);
                    }
                }, { passive: true });
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}
