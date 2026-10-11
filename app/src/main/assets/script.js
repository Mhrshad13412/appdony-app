(function() {

    // ⭐⭐ انیمیشن دایره‌ای موقع کلیک روی منوی پایین
    function initCircleReveal() {
        document.addEventListener('click', function(e) {
            var navItem = e.target.closest('.bottom-nav-item, .bottom-nav a, .cat-link');
            if (!navItem) return;

            // مختصات کلیک
            var rect = navItem.getBoundingClientRect();
            var x = rect.left + rect.width / 2;
            var y = rect.top + rect.height / 2;

            // بزرگ‌ترین شعاع لازم برای پوشش صفحه
            var maxDist = Math.max(
                Math.sqrt(x * x + y * y),
                Math.sqrt((window.innerWidth - x) ** 2 + y * y),
                Math.sqrt(x * x + (window.innerHeight - y) ** 2),
                Math.sqrt((window.innerWidth - x) ** 2 + (window.innerHeight - y) ** 2)
            );

            // المان دایره
            var circle = document.createElement('div');
            circle.style.cssText = [
                'position: fixed',
                'left: ' + x + 'px',
                'top: ' + y + 'px',
                'width: 0',
                'height: 0',
                'border-radius: 50%',
                'background: #0D7377',
                'opacity: 0.15',
                'transform: translate(-50%, -50%)',
                'pointer-events: none',
                'z-index: 99999',
                'transition: width 0.55s cubic-bezier(0.22, 1, 0.36, 1), height 0.55s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.55s ease'
            ].join(';');
            document.body.appendChild(circle);

            // انیمیشن
            requestAnimationFrame(function() {
                circle.style.width = (maxDist * 2) + 'px';
                circle.style.height = (maxDist * 2) + 'px';
                circle.style.opacity = '0';
            });

            setTimeout(function() {
                if (circle.parentNode) circle.parentNode.removeChild(circle);
            }, 700);
        }, true);
    }

    // ⭐⭐ انیمیشن پرنده شدن آیکون دانلود
    function initDownloadFly() {
        document.addEventListener('click', function(e) {
            var dlBtn = e.target.closest('[class*="download"], [class*="btn-download"], a[href*=".apk"], a[href*=".zip"]');
            if (!dlBtn) return;

            var rect = dlBtn.getBoundingClientRect();
            var startX = rect.left + rect.width / 2;
            var startY = rect.top + rect.height / 2;

            // موقعیت توپ شناور (پایین راست، حدود 80 از راست و 300 از پایین)
            var endX = window.innerWidth - 80;
            var endY = window.innerHeight - 320;

            var flyer = document.createElement('div');
            flyer.style.cssText = [
                'position: fixed',
                'left: ' + startX + 'px',
                'top: ' + startY + 'px',
                'width: 40px',
                'height: 40px',
                'border-radius: 50%',
                'background: linear-gradient(135deg, #0D7377, #4DD0C7)',
                'box-shadow: 0 4px 12px rgba(13, 115, 119, 0.5)',
                'transform: translate(-50%, -50%)',
                'pointer-events: none',
                'z-index: 999999',
                'opacity: 1',
                'transition: all 0.75s cubic-bezier(0.4, 0, 0.2, 1)',
                'display: flex',
                'align-items: center',
                'justify-content: center',
                'color: white',
                'font-size: 20px'
            ].join(';');
            flyer.textContent = '↓';
            document.body.appendChild(flyer);

            // انیمیشن
            requestAnimationFrame(function() {
                flyer.style.left = endX + 'px';
                flyer.style.top = endY + 'px';
                flyer.style.transform = 'translate(-50%, -50%) scale(0.3)';
                flyer.style.opacity = '0.2';
            });

            setTimeout(function() {
                if (flyer.parentNode) flyer.parentNode.removeChild(flyer);
            }, 800);
        }, true);
    }

    // ⭐⭐ انیمیشن ورود محتوا موقع تغییر بخش
    function initContentTransition() {
        var observer = new MutationObserver(function(mutations) {
            // اگه محتوای اصلی عوض شد، انیمیشن بزن
            var main = document.querySelector('main, #main, .content, .site-content');
            if (main && !main.dataset.appdonyAnimated) {
                main.dataset.appdonyAnimated = '1';
                main.style.animation = 'none';
                main.offsetHeight;
                main.style.animation = 'appdonyContentIn 0.45s cubic-bezier(0.22, 1, 0.36, 1)';
                setTimeout(function() {
                    delete main.dataset.appdonyAnimated;
                }, 500);
            }
        });
        var target = document.querySelector('main, #main, .content, .site-content, body');
        if (target) {
            observer.observe(target, { childList: true, subtree: true });
        }
    }

    // ⭐⭐ افکت ضربه روی هر چیزی
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

    // شروع
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            initCircleReveal();
            initDownloadFly();
            initContentTransition();
        });
    } else {
        initCircleReveal();
        initDownloadFly();
        initContentTransition();
    }
})();
