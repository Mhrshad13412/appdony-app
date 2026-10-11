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

        // تشخیص حالت شب/روز
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val splashBg = if (isDark) Color.parseColor("#001C1A") else Color.parseColor("#DFF5F2")

        // ============ SPLASH ============
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
            setBackgroundColor(splashBg) // همرنگ با اسپلش برای رفع سفیدی
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

    // ============ 🎨 لودینگ ساده و نرم — دایره گرادیانتی چرخان ============
    class SpinnerLoader(context: Context, private val isDark: Boolean) : View(context) {

        private val strokeWidth = 20f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = strokeWidth
        }

        private val rect = RectF()

        // رنگ‌های گرادیانت
        private val colorMain = if (isDark) Color.parseColor("#4DD0C7") else Color.parseColor("#0D7377")
        private val colorLight = if (isDark) Color.parseColor("#7FE5DD") else Color.parseColor("#2AA5A0")
        private val colorTransparent = if (isDark) Color.parseColor("#204DD0C7") else Color.parseColor("#200D7377")

        private var rotation = 0f
        private var sweep = 90f

        init {
            // چرخش دائم — ۱.۴ ثانیه برای یه دور کامل
            ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 1400
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { rotation = it.animatedValue as Float; invalidate() }
                start()
            }

            // طول کمان دائم تغییر می‌کنه: کوتاه → بلند → کوتاه (نرم)
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

            // گرادیانت دورادور
            paint.shader = SweepGradient(
                cx, cy,
                intArrayOf(colorMain, colorLight, colorTransparent, colorMain),
                floatArrayOf(0f, 0.35f, 0.75f, 1f)
            )

            // کانواس رو می‌چرخونیم تا گرادیانت هم بچرخه
            canvas.save()
            canvas.rotate(rotation, cx, cy)
            canvas.drawArc(rect, 0f, sweep, false, paint)
            canvas.restore()
        }
    }

    // ============ تزریق CSS ============
    private fun injectCustomCSS() {
        val js = """
            (function() {
                if (document.getElementById('appdony-custom-style')) return;

                var css = `
                    button, .btn, input[type="submit"], input[type="button"],
                    a.button, [class*="btn"], [class*="button"], [role="button"] {
                        border-radius: 18px !important;
                        transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1),
                                    box-shadow 0.3s ease !important;
                        -webkit-tap-highlight-color: transparent !important;
                    }
                    button:active, .btn:active, [class*="btn"]:active,
                    [class*="button"]:active, [role="button"]:active {
                        transform: scale(0.92) !important;
                    }

                    .card, .post, article, [class*="card"], [class*="item"],
                    [class*="post"], [class*="product"], [class*="box"] {
                        border-radius: 20px !important;
                        transition: transform 0.35s ease, box-shadow 0.35s ease !important;
                    }
                    .card:active, article:active, [class*="card"]:active, [class*="item"]:active {
                        transform: scale(0.97) !important;
                    }

                    img {
                        border-radius: 16px !important;
                        transition: transform 0.4s ease, filter 0.4s ease !important;
                    }
                    img:active { transform: scale(0.97) !important; }

                    input, textarea, select {
                        border-radius: 16px !important;
                        transition: box-shadow 0.3s ease, border-color 0.3s ease !important;
                    }
                    input:focus, textarea:focus, select:focus {
                        box-shadow: 0 0 0 4px rgba(13, 115, 119, 0.2) !important;
                        border-color: #0D7377 !important;
                        outline: none !important;
                    }

                    a {
                        transition: color 0.25s ease, opacity 0.25s ease, transform 0.25s ease !important;
                        -webkit-tap-highlight-color: transparent !important;
                    }
                    a:active { opacity: 0.65 !important; transform: scale(0.97) !important; }

                    html { scroll-behavior: smooth !important; }

                    ::-webkit-scrollbar { width: 6px; height: 6px; }
                    ::-webkit-scrollbar-thumb {
                        background: rgba(13, 115, 119, 0.55);
                        border-radius: 6px;
                    }

                    li, .menu-item, [class*="menu"] a {
                        transition: background-color 0.3s ease, transform 0.3s ease !important;
                    }

                    header, [class*="header"], [class*="navbar"], [class*="top-bar"] {
                        backdrop-filter: blur(10px) !important;
                        -webkit-backdrop-filter: blur(10px) !important;
                    }

                    body { animation: appdonyFadeIn 0.4s ease-in !important; }
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
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}
