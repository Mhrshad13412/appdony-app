(function() {
    // انیمیشن دایره‌ای موقع کلیک روی منو
    document.addEventListener('click', function(e) {
        var navItem = e.target.closest('.bottom-nav-item, .bottom-nav a, .cat-link');
        if (!navItem) return;
        var rect = navItem.getBoundingClientRect();
        var x = rect.left + rect.width / 2;
        var y = rect.top + rect.height / 2;
        var maxDist = Math.max(
            Math.sqrt(x * x + y * y),
            Math.sqrt((window.innerWidth - x) ** 2 + y * y),
            Math.sqrt(x * x + (window.innerHeight - y) ** 2),
            Math.sqrt((window.innerWidth - x) ** 2 + (window.innerHeight - y) ** 2)
        );
        var circle = document.createElement('div');
        circle.style.cssText = [
            'position: fixed',
            'left: ' + x + 'px',
            'top: ' + y + 'px',
            'width: 0',
            'height: 0',
            'border-radius: 50%',
            'background: #0D7377',
            'opacity: 0.18',
            'transform: translate(-50%, -50%)',
            'pointer-events: none',
            'z-index: 99999',
            'transition: width 0.55s cubic-bezier(0.22, 1, 0.36, 1), height 0.55s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.55s ease'
        ].join(';');
        document.body.appendChild(circle);
        requestAnimationFrame(function() {
            circle.style.width = (maxDist * 2) + 'px';
            circle.style.height = (maxDist * 2) + 'px';
            circle.style.opacity = '0';
        });
        setTimeout(function() { if (circle.parentNode) circle.parentNode.removeChild(circle); }, 700);
    }, true);

    // ⭐⭐⭐ پرنده شدن آیکون — از طریق Android Bridge
    function notifyAndroidFly(el) {
        if (!el || !window.AppdonyBridge) return;
        var rect = el.getBoundingClientRect();
        var x = rect.left + rect.width / 2;
        var y = rect.top + rect.height / 2;
        try {
            window.AppdonyBridge.onDownloadFly(x, y);
        } catch(e) {}
    }

    // روی کلیک دکمه‌های دانلود
    document.addEventListener('click', function(e) {
        var btn = e.target.closest('a[href*=".apk"], a[href*=".zip"], a[href*=".obb"], [class*="download-btn"], [class*="btn-download"], button[class*="download"]');
        if (btn) notifyAndroidFly(btn);
    }, true);

    // افکت ضربه
    document.addEventListener('touchstart', function(e) {
        var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"], [class*="card"], [class*="item"]');
        if (t) {
            t.style.transition = 'transform 0.12s cubic-bezier(0.34, 1.56, 0.64, 1)';
            t.style.transform = 'scale(0.94)';
        }
    }, { passive: true });

    document.addEventListener('touchend', function(e) {
        var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"], [class*="card"], [class*="item"]');
        if (t) {
            setTimeout(function() {
                t.style.transform = '';
                t.style.transition = 'transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)';
            }, 90);
        }
    }, { passive: true });
})();
