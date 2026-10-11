package com.appdony.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashView: LinearLayout

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)

        // ============ SPLASH SCREEN ============
        splashView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0D7377"))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(420, 420)
        }

        val appName = TextView(this).apply {
            text = "اپدونی"
            setTextColor(Color.WHITE)
            textSize = 34f
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 0)
            typeface = Typeface.DEFAULT_BOLD
        }

        val tagline = TextView(this).apply {
            text = "مرجع دانلود برنامه و بازی"
            setTextColor(Color.parseColor("#B2DFDB"))
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        splashView.addView(logo)
        splashView.addView(appName)
        splashView.addView(tagline)

        // حالت اولیه برای انیمیشن
        logo.alpha = 0f
        logo.scaleX = 0.5f
        logo.scaleY = 0.5f
        appName.alpha = 0f
        tagline.alpha = 0f

        // ============ WEBVIEW ============
        webView = WebView(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#FAFAFA"))
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

        root.addView(splashView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        root.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(root)

        // ============ انیمیشن‌های Splash ============
        logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(900)
            .setInterpolator(DecelerateInterpolator())
            .start()

        appName.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(400)
            .start()

        tagline.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(700)
            .start()

        // پالس ملایم روی لوگو
        Handler(Looper.getMainLooper()).postDelayed({
            logo.animate()
                .scaleX(1.08f).scaleY(1.08f)
                .setDuration(700)
                .withEndAction {
                    logo.animate().scaleX(1f).scaleY(1f).setDuration(700).start()
                }
                .start()
        }, 1200)

        // مخفی کردن Splash بعد از 2.6 ثانیه
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
        }, 2600)

        // مدیریت دکمه برگشت
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

    private fun injectCustomCSS() {
        val js = """
            (function() {
                if (document.getElementById('appdony-custom-style')) return;
                
                var css = ''
                + 'button, .btn, input[type="submit"], input[type="button"], a.button, [class*="btn"], [class*="button"], [role="button"] {'
                + '  border-radius: 14px !important;'
                + '  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s ease !important;'
                + '  -webkit-tap-highlight-color: transparent !important;'
                + '}'
                + 'button:active, .btn:active, [class*="btn"]:active, [class*="button"]:active, [role="button"]:active {'
                + '  transform: scale(0.95) !important;'
                + '}'
                + '.card, .post, article, [class*="card"], [class*="item"], [class*="post"], [class*="product"] {'
                + '  border-radius: 16px !important;'
                + '  transition: transform 0.3s ease, box-shadow 0.3s ease !important;'
                + '}'
                + '.card:active, article:active, [class*="card"]:active {'
                + '  transform: scale(0.98) !important;'
                + '}'
                + 'img { border-radius: 12px !important; }'
                + 'input, textarea, select {'
                + '  border-radius: 12px !important;'
                + '  transition: box-shadow 0.3s ease !important;'
                + '}'
                + 'input:focus, textarea:focus {'
                + '  box-shadow: 0 0 0 3px rgba(13, 115, 119, 0.25) !important;'
                + '  outline: none !important;'
                + '}'
                + 'a { transition: color 0.2s ease, opacity 0.2s ease !important; }'
                + 'a:active { opacity: 0.7 !important; }'
                + 'html { scroll-behavior: smooth !important; }'
                + '::-webkit-scrollbar { width: 4px; }'
                + '::-webkit-scrollbar-thumb { background: rgba(13, 115, 119, 0.5); border-radius: 4px; }';
                
                var style = document.createElement('style');
                style.id = 'appdony-custom-style';
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                document.head.appendChild(style);
                
                // انیمیشن ضربه روی همه عناصر قابل کلیک
                document.addEventListener('touchstart', function(e) {
                    var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"]');
                    if (t) {
                        t.style.transition = 'transform 0.1s';
                        t.style.transform = 'scale(0.95)';
                    }
                }, { passive: true });
                
                document.addEventListener('touchend', function(e) {
                    var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"]');
                    if (t) {
                        setTimeout(function() {
                            t.style.transform = '';
                            t.style.transition = 'transform 0.25s';
                        }, 80);
                    }
                }, { passive: true });
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js, null)
    }
}
