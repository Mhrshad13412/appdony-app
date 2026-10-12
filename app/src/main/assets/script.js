:root {
    --appdony-accent: #0D7377;
    --appdony-accent-10: rgba(13, 115, 119, 0.10);
    --appdony-accent-20: rgba(13, 115, 119, 0.20);
    --appdony-accent-30: rgba(13, 115, 119, 0.30);
}

* {
    -webkit-tap-highlight-color: transparent !important;
    -webkit-touch-callout: none !important;
}

/* ===== دکمه‌های اصلی ===== */
button:not(.bottom-nav-item):not(.device-chip):not(.cat-link):not(.charts-tab),
input[type="submit"], input[type="button"],
.elementor-button, a.btn-install,
[class*="download-btn"], [class*="btn-download"] {
    border-radius: 20px !important;
    padding: 12px 26px !important;
    font-weight: 600 !important;
    transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
}
button:not(.bottom-nav-item):not(.device-chip):not(.cat-link):not(.charts-tab):active,
input[type="submit"]:active, .elementor-button:active, a.btn-install:active {
    transform: scale(0.95) !important;
}

/* ===== نوار پایین ===== */
.bottom-nav, .bottom-nav *, .bottom-nav-item, .bottom-nav-item * {
    -webkit-tap-highlight-color: transparent !important;
    outline: none !important;
}
.bottom-nav a, .bottom-nav-item { background-color: transparent !important; }
.bottom-nav a:active, .bottom-nav-item:active { background-color: transparent !important; }

.bottom-nav .active,
.bottom-nav-item.active,
.bottom-nav a.active,
.bottom-nav a[aria-current="page"],
.bottom-nav li.active > a {
    background-color: var(--appdony-accent-20) !important;
    border-radius: 50px !important;
    color: var(--appdony-accent) !important;
    box-shadow: none !important;
    padding: 6px 20px !important;
    animation: appdonyPillIn 0.55s cubic-bezier(0.34, 1.4, 0.64, 1) !important;
}

@keyframes appdonyPillIn {
    0% { background-color: transparent !important; transform: scale(0.85); opacity: 0.4; }
    60% { background-color: var(--appdony-accent-30) !important; transform: scale(1.03); opacity: 1; }
    100% { background-color: var(--appdony-accent-20) !important; transform: scale(1); opacity: 1; }
}

.bottom-nav .active i, .bottom-nav .active svg, .bottom-nav .active span,
.bottom-nav-item.active i, .bottom-nav-item.active span {
    color: var(--appdony-accent) !important;
    fill: var(--appdony-accent) !important;
}

/* ===== نوار دسته‌بندی بالا ===== */
.cat-link { -webkit-tap-highlight-color: transparent !important; }
.cat-link.active, .cat-link[aria-current], .cat-link[aria-current="page"] {
    color: var(--appdony-accent) !important;
    border-bottom-color: var(--appdony-accent) !important;
}

/* ===== دکمه‌های دستگاه (کار می‌کرده) ===== */
.device-bar, .device-tabs, .platform-tabs,
[class*="device"], [class*="platform"] {
    -webkit-tap-highlight-color: transparent !important;
    outline: none !important;
}
.device-chip, .device-tab, .platform-chip, .platform-tab,
[class*="device-chip"], [class*="device-tab"],
[class*="platform-chip"], [class*="platform-tab"] {
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
    -webkit-tap-highlight-color: transparent !important;
    border-radius: 50px !important;
    border: 2px solid transparent !important;
}
.device-chip.active, .device-chip[aria-current], .device-chip[aria-selected="true"],
.device-bar .active, .device-bar .chip.active, .device-bar li.active .device-chip,
.device-tabs .active, .device-tab.active,
.platform-tabs .active, .platform-chip.active, .platform-tab.active,
[class*="device"] .active, [class*="device"][class*="active"],
[class*="platform"] .active, [class*="device-chip"].active, [class*="device-tab"].active {
    background-color: var(--appdony-accent-20) !important;
    border-color: var(--appdony-accent) !important;
    color: var(--appdony-accent) !important;
    border-radius: 50px !important;
}
.device-chip.active svg, .device-chip.active i, .device-chip.active span,
.device-bar .active svg, .device-bar .active i, .device-bar .active span,
.device-tabs .active svg, .device-tabs .active i, .device-tabs .active span,
[class*="device"].active svg, [class*="device"].active i, [class*="device"].active span,
[class*="platform"].active svg, [class*="platform"].active i, [class*="platform"].active span {
    color: var(--appdony-accent) !important;
    fill: var(--appdony-accent) !important;
}

/* ===============================================
   ⭐⭐⭐ دکمه‌های پایین: بیشترین نصب / بیشترین امتیاز
   =============================================== */
.charts-tabs,
.charts-tabs * {
    -webkit-tap-highlight-color: transparent !important;
    outline: none !important;
}

.charts-tab {
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
    border-radius: 50px !important;
    padding: 8px 20px !important;
    -webkit-tap-highlight-color: transparent !important;
    border: 2px solid transparent !important;
    background-color: transparent !important;
    color: inherit !important;
}
.charts-tab:active {
    transform: scale(0.95) !important;
}
.charts-tab.active,
.charts-tab[aria-selected="true"],
.charts-tab.active:focus {
    background-color: var(--appdony-accent-20) !important;
    color: var(--appdony-accent) !important;
    border-color: var(--appdony-accent) !important;
    border-radius: 50px !important;
}

/* ===============================================
   ⭐⭐⭐ متن دسته‌بندی (gta) در برترین‌ها
   =============================================== */
.chart-info .meta-line,
.chart-row .meta-line,
.charts-list .meta-line,
.charts-list .chart-info .meta-line {
    color: var(--appdony-accent) !important;
}

/* ریست رنگ rating و ستاره‌ها (خاکستری بمونن) */
.chart-info .meta-line .rating,
.chart-row .meta-line .rating,
.charts-list .meta-line .rating,
.chart-info .meta-line .rating svg,
.chart-row .meta-line .rating svg {
    color: #888888 !important;
    fill: #888888 !important;
}

/* ===== دکمه‌های اصلی سایت ===== */
.btn-primary, .btn-install, a.btn-install,
button[class*="download"], [class*="btn-download"], [class*="download-btn"],
input[type="submit"] {
    background-color: var(--appdony-accent) !important;
    border-color: var(--appdony-accent) !important;
}

/* ===== لینک‌ها ===== */
a { color: var(--appdony-accent) !important; }

/* ===== اینپوت‌ها ===== */
input[type="text"], input[type="search"], input[type="email"],
input[type="password"], textarea {
    border-radius: 16px !important;
}
input:focus, textarea:focus, select:focus {
    border-color: var(--appdony-accent) !important;
    box-shadow: 0 0 0 3px var(--appdony-accent-20) !important;
    outline: none !important;
}

/* ===== اسکرول‌بار ===== */
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-thumb {
    background: var(--appdony-accent-30) !important;
    border-radius: 5px;
}
::-webkit-scrollbar-track { background: transparent; }
