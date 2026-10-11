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

    document.addEventListener('click', function(e) {
        recordClick(e.clientX, e.clientY);
    }, true);

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

    // ⭐ رنگ تم رو از اندروید می‌گیریم
    window._appdonySetAccent = function(hex) {
        window._appdonyAccent = hex;
        var old = document.getElementById('appdony-accent-style');
        if (old) old.remove();
        var s = document.createElement('style');
        s.id = 'appdony-accent-style';
        s.appendChild(document.createTextNode(
            ':root { --appdony-accent: ' + hex + '; }' +
            '.cat-link.active, .cat-link[aria-current], .bottom-nav .active, .bottom-nav-item.active {' +
            '  color: ' + hex + ' !important;' +
            '  border-color: ' + hex + ' !important;' +
            '}' +
            '.bottom-nav .active, .bottom-nav-item.active {' +
            '  background-color: ' + hex + '20 !important;' +
            '}' +
            '.bottom-nav .active i, .bottom-nav .active svg, .bottom-nav .active span,' +
            '.bottom-nav-item.active i, .bottom-nav-item.active span {' +
            '  color: ' + hex + ' !important; fill: ' + hex + ' !important;' +
            '}' +
            'a:hover, button:hover { color: ' + hex + ' !important; }' +
            '.btn-primary, .btn-install, [class*="btn-download"], [class*="download-btn"] {' +
            '  background-color: ' + hex + ' !important;' +
            '}'
        ));
        document.head.appendChild(s);
    };
})();
