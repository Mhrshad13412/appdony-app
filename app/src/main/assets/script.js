(function() {
    if (window._appdonyInit) return;
    window._appdonyInit = true;

    function notifyNavStart() {
        if (window.AppdonyBridge) try { window.AppdonyBridge.onNavStart(); } catch(e) {}
    }
    function recordClick(x, y) {
        if (window.AppdonyBridge) try { window.AppdonyBridge.onAnyClick(x, y); } catch(e) {}
    }

    document.addEventListener('touchstart', function(e) {
        if (e.touches && e.touches[0]) recordClick(e.touches[0].clientX, e.touches[0].clientY);
    }, { passive: true, capture: true });
    document.addEventListener('click', function(e) { recordClick(e.clientX, e.clientY); }, true);

    document.addEventListener('click', function(e) {
        var nav = e.target.closest('.bottom-nav-item, .bottom-nav a, .cat-link');
        if (nav) { notifyNavStart(); showCircle(e, nav); }
    }, true);

    function showCircle(e, el) {
        var rect = el.getBoundingClientRect();
        var x = rect.left + rect.width / 2;
        var y = rect.top + rect.height / 2;
        var accent = window._appdonyAccent || '#0D7377';
        var maxDist = Math.max(
            Math.sqrt(x * x + y * y),
            Math.sqrt((window.innerWidth - x) ** 2 + y * y),
            Math.sqrt(x * x + (window.innerHeight - y) ** 2),
            Math.sqrt((window.innerWidth - x) ** 2 + (window.innerHeight - y) ** 2)
        );
        var c = document.createElement('div');
        c.style.cssText = 'position:fixed;left:' + x + 'px;top:' + y + 'px;width:0;height:0;border-radius:50%;background:' + accent + ';opacity:0.18;transform:translate(-50%,-50%);pointer-events:none;z-index:99999;transition:width .55s cubic-bezier(.22,1,.36,1),height .55s cubic-bezier(.22,1,.36,1),opacity .55s ease';
        document.body.appendChild(c);
        requestAnimationFrame(function() {
            c.style.width = (maxDist * 2) + 'px';
            c.style.height = (maxDist * 2) + 'px';
            c.style.opacity = '0';
        });
        setTimeout(function() { if (c.parentNode) c.parentNode.removeChild(c); }, 700);
    }

    if (!window._appdonyContentObserver) {
        var t = document.querySelector('main, #main, .content, .site-content') || document.body;
        window._appdonyContentObserver = new MutationObserver(function() {
            if (window.AppdonyBridge) try { window.AppdonyBridge.onContentReady(); } catch(e) {}
        });
        window._appdonyContentObserver.observe(t, { childList: true });
    }

    document.addEventListener('touchstart', function(e) {
        var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"]');
        if (t) {
            t.style.transition = 'transform 0.12s cubic-bezier(0.34, 1.56, 0.64, 1)';
            t.style.transform = 'scale(0.94)';
        }
    }, { passive: true });
    document.addEventListener('touchend', function(e) {
        var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"]');
        if (t) setTimeout(function() {
            t.style.transform = '';
            t.style.transition = 'transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)';
        }, 90);
    }, { passive: true });

    // ⭐⭐⭐ اعمال رنگ تم روی کل سایت
    window._appdonySetAccent = function(hex) {
        window._appdonyAccent = hex;

        var old = document.getElementById('appdony-accent-style');
        if (old) old.remove();

        var css = `
            /* ==== متغیرهای رنگ تم ==== */
            :root {
                --appdony-accent: ${hex};
                --appdony-accent-10: ${hex}1A;
                --appdony-accent-20: ${hex}33;
                --appdony-accent-30: ${hex}4D;
            }

            /* ==== نوار پایین: تب فعال ==== */
            .bottom-nav .active,
            .bottom-nav-item.active,
            .bottom-nav a.active,
            .bottom-nav a[aria-current="page"],
            .bottom-nav li.active > a {
                background-color: var(--appdony-accent-20) !important;
                color: var(--appdony-accent) !important;
                border-radius: 50px !important;
                padding: 6px 20px !important;
            }
            .bottom-nav .active i,
            .bottom-nav .active svg,
            .bottom-nav .active span,
            .bottom-nav-item.active i,
            .bottom-nav-item.active span {
                color: var(--appdony-accent) !important;
                fill: var(--appdony-accent) !important;
            }

            /* ==== نوار دسته‌بندی بالا: تب فعال ==== */
            .cat-link.active,
            .cat-link[aria-current] {
                color: var(--appdony-accent) !important;
                border-bottom-color: var(--appdony-accent) !important;
            }

            /* ==== دکمه‌های اصلی سایت ==== */
            .btn-primary,
            .btn-install,
            [class*="btn-download"],
            [class*="download-btn"],
            button[class*="download"],
            input[type="submit"] {
                background-color: var(--appdony-accent) !important;
                border-color: var(--appdony-accent) !important;
            }

            /* ==== دکمه‌های ثانویه (با حاشیه) ==== */
            .btn-outline,
            [class*="btn-outline"],
            button[class*="outline"] {
                border-color: var(--appdony-accent) !important;
                color: var(--appdony-accent) !important;
            }
            .btn-outline:active,
            [class*="btn-outline"]:active {
                background-color: var(--appdony-accent-10) !important;
            }

            /* ==== لینک‌ها و متن‌های تیتر ==== */
            a { color: var(--appdony-accent) !important; }
            h1 a, h2 a, h3 a { color: var(--appdony-accent) !important; }

            /* ==== بج‌ها و برچسب‌ها ==== */
            .badge, .tag, [class*="badge"], [class*="tag"] {
                background-color: var(--appdony-accent-10) !important;
                color: var(--appdony-accent) !important;
            }

            /* ==== نوار پیشرفت ==== */
            progress, .progress-bar {
                accent-color: var(--appdony-accent) !important;
            }

            /* ==== فوکوس اینپوت‌ها ==== */
            input:focus, textarea:focus, select:focus {
                border-color: var(--appdony-accent) !important;
                box-shadow: 0 0 0 3px var(--appdony-accent-20) !important;
            }

            /* ==== اسکرول‌بار ==== */
            ::-webkit-scrollbar-thumb {
                background: var(--appdony-accent-30) !important;
            }
        `;

        var s = document.createElement('style');
        s.id = 'appdony-accent-style';
        s.appendChild(document.createTextNode(css));
        document.head.appendChild(s);
    };
})();
