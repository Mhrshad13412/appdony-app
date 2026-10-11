(function() {
    if (window._appdonyInit) return;
    window._appdonyInit = true;

    // ⭐ به اندروید میگیم ناوبری شروع شد
    function notifyNavStart() {
        if (window.AppdonyBridge) {
            try { window.AppdonyBridge.onNavStart(); } catch(e) {}
        }
    }

    // ⭐ آخرین کلیک کاربر رو به اندروید می‌فرستیم
    function recordClick(x, y) {
        if (window.AppdonyBridge) {
            try { window.AppdonyBridge.onAnyClick(x, y); } catch(e) {}
        }
    }

    // ثبت موقعیت هر کلیک (لمسی)
    document.addEventListener('touchstart', function(e) {
        if (e.touches && e.touches[0]) {
            recordClick(e.touches[0].clientX, e.touches[0].clientY);
        }
    }, { passive: true, capture: true });

    // ثبت موقعیت هر کلیک (ماوس/دسکتاپ)
    document.addEventListener('click', function(e) {
        recordClick(e.clientX, e.clientY);
    }, true);

    // ⭐⭐ کلیک روی منو → دایره + notify
    document.addEventListener('click', function(e) {
        var nav = e.target.closest('.bottom-nav-item, .bottom-nav a, .cat-link');
        if (nav) {
            notifyNavStart();
            showCircle(e, nav);
        }
    }, true);

    function showCircle(e, el) {
        var rect = el.getBoundingClientRect();
        var x = rect.left + rect.width / 2;
        var y = rect.top + rect.height / 2;
        var maxDist = Math.max(
            Math.sqrt(x * x + y * y),
            Math.sqrt((window.innerWidth - x) ** 2 + y * y),
            Math.sqrt(x * x + (window.innerHeight - y) ** 2),
            Math.sqrt((window.innerWidth - x) ** 2 + (window.innerHeight - y) ** 2)
        );
        var circle = document.createElement('div');
        circle.style.cssText = 'position: fixed; left:' + x + 'px; top:' + y + 'px; width:0; height:0; border-radius:50%; background:#0D7377; opacity:0.18; transform:translate(-50%,-50%); pointer-events:none; z-index:99999; transition: width 0.55s cubic-bezier(0.22,1,0.36,1), height 0.55s cubic-bezier(0.22,1,0.36,1), opacity 0.55s ease';
        document.body.appendChild(circle);
        requestAnimationFrame(function() {
            circle.style.width = (maxDist * 2) + 'px';
            circle.style.height = (maxDist * 2) + 'px';
            circle.style.opacity = '0';
        });
        setTimeout(function() { if (circle.parentNode) circle.parentNode.removeChild(circle); }, 700);
    }

    // ⭐ وقتی محتوای جدید اومد، به اندروید بگو
    if (!window._appdonyContentObserver) {
        var contentTarget = document.querySelector('main, #main, .content, .site-content') || document.body;
        window._appdonyContentObserver = new MutationObserver(function() {
            if (window.AppdonyBridge) {
                try { window.AppdonyBridge.onContentReady(); } catch(e) {}
            }
        });
        window._appdonyContentObserver.observe(contentTarget, { childList: true });
    }

    // افکت ضربه
    document.addEventListener('touchstart', function(e) {
        var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"]');
        if (t) {
            t.style.transition = 'transform 0.12s cubic-bezier(0.34, 1.56, 0.64, 1)';
            t.style.transform = 'scale(0.94)';
        }
    }, { passive: true });

    document.addEventListener('touchend', function(e) {
        var t = e.target.closest('a, button, [class*="btn"], [class*="button"], [role="button"]');
        if (t) {
            setTimeout(function() {
                t.style.transform = '';
                t.style.transition = 'transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)';
            }, 90);
        }
    }, { passive: true });
})();
