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
