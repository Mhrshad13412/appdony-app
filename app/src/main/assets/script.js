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

    // ⭐ اعمال رنگ تم روی کل سایت
    window._appdonySetAccent = function(hex) {
        window._appdonyAccent = hex;

        try {
            var root = document.documentElement;
            root.style.setProperty('--accent', hex);
            root.style.setProperty('--primary', hex);
            root.style.setProperty('--brand', hex);
            root.style.setProperty('--main-color', hex);
            root.style.setProperty('--theme-color', hex);
            root.style.setProperty('--link-color', hex);
        } catch(e) {}

        var old = document.getElementById('appdony-accent-style');
        if (old) old.remove();

        var css = `
            :root {
                --appdony-accent: ${hex};
                --appdony-accent-10: ${hex}1A;
                --appdony-accent-20: ${hex}33;
                --appdony-accent-30: ${hex}4D;
                --accent: ${hex};
                --primary: ${hex};
                --brand: ${hex};
            }

            /* device chips */
            .device-bar .device-chip.active,
            .device-chip.active,
            .device-chip[aria-current] {
                background-color: var(--appdony-accent-20) !important;
                border-color: var(--appdony-accent) !important;
                color: var(--appdony-accent) !important;
            }
            .device-chip.active svg,
            .device-chip.active i {
                color: var(--appdony-accent) !important;
                fill: var(--appdony-accent) !important;
            }

            /* charts-tab (بیشترین نصب/امتیاز) */
            html body .charts-tabs .charts-tab.active,
            html body .charts-tabs button.charts-tab.active,
            html body button.charts-tab.active,
            html body button[data-tab].active {
                background-color: var(--appdony-accent-20) !important;
                color: var(--appdony-accent) !important;
                border-color: var(--appdony-accent) !important;
            }

            /* meta-line "gta" */
            html body .charts-list .chart-row .chart-info .meta-line,
            html body .chart-row .chart-info .meta-line,
            html body .chart-info .meta-line,
            html body .chart-row .meta-line {
                color: var(--appdony-accent) !important;
            }

            /* cat-link active */
            html body .cat-link.active,
            html body .cat-link[aria-current] {
                color: var(--appdony-accent) !important;
                border-bottom-color: var(--appdony-accent) !important;
            }

            /* bottom-nav active */
            html body .bottom-nav .active,
            html body .bottom-nav-item.active {
                background-color: var(--appdony-accent-20) !important;
                color: var(--appdony-accent) !important;
            }
            html body .bottom-nav .active svg,
            html body .bottom-nav .active i,
            html body .bottom-nav .active span,
            html body .bottom-nav-item.active svg,
            html body .bottom-nav-item.active i,
            html body .bottom-nav-item.active span {
                color: var(--appdony-accent) !important;
                fill: var(--appdony-accent) !important;
            }

            /* لینک‌ها */
            a { color: var(--appdony-accent) !important; }
        `;

        var s = document.createElement('style');
        s.id = 'appdony-accent-style';
        s.appendChild(document.createTextNode(css));
        (document.head || document.documentElement).appendChild(s);
    };

    // ⭐ خوندن رنگ ذخیره‌شده
    function applySavedAccent() {
        try {
            var saved = localStorage.getItem('_appdony_accent');
            if (saved && window._appdonySetAccent) {
                window._appdonySetAccent(saved);
            }
        } catch(e) {}
    }

    applySavedAccent();

    if (!window._appdonyAccentObserver) {
        window._appdonyAccentObserver = new MutationObserver(function() {
            if (!document.getElementById('appdony-accent-style') &&
                !document.getElementById('appdony-accent-direct')) {
                applySavedAccent();
            }
        });
        window._appdonyAccentObserver.observe(document.documentElement, {
            childList: true, subtree: true
        });
    }

    if (!window._appdonyAccentInterval) {
        window._appdonyAccentInterval = setInterval(function() {
            if (!document.getElementById('appdony-accent-style') &&
                !document.getElementById('appdony-accent-direct')) {
                applySavedAccent();
            }
        }, 2000);
    }
})();
