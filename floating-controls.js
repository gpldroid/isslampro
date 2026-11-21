/* Floating controls for IsslamPro. Kept separate from the Quran application logic. */
(() => {
  "use strict";
  const root = document.documentElement;
  const STORAGE_KEY = "isslampro.appearance";
  const icons = {
    up: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 19V5M6 11l6-6 6 6"/></svg>',
    down: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14m-6-6 6 6 6-6"/></svg>',
    moon: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z"/></svg>',
    sun: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42"/></svg>'
  };

  let savedTheme = "dark";
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    if (value === "light" || value === "dark") savedTheme = value;
  } catch {}

  const applyTheme = (theme) => {
    const light = theme === "light";
    root.dataset.theme = light ? "light" : "dark";
    const button = document.getElementById("uqThemeToggle");
    if (button) {
      button.innerHTML = light ? icons.moon : icons.sun;
      button.setAttribute("aria-label", light ? "التبديل إلى الوضع الداكن" : "التبديل إلى الوضع الفاتح");
      button.setAttribute("aria-pressed", String(light));
      button.title = light ? "الوضع الداكن" : "الوضع الفاتح";
    }
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute("content", light ? "#f3f6f2" : "#082423");
    try { localStorage.setItem(STORAGE_KEY, light ? "light" : "dark"); } catch {}
  };

  const controls = document.createElement("div");
  controls.id = "uq-floating-controls";
  controls.setAttribute("aria-label", "أدوات العرض والتنقل");
  controls.innerHTML = [
    '<button class="uq-float-btn" id="uqScrollTop" type="button" aria-label="الانتقال إلى أعلى الصفحة" title="إلى الأعلى">' + icons.up + "</button>",
    '<button class="uq-float-btn uq-theme" id="uqThemeToggle" type="button" aria-label="التبديل إلى الوضع الفاتح" aria-pressed="false" title="الوضع الفاتح">' + icons.sun + "</button>",
    '<button class="uq-float-btn" id="uqScrollBottom" type="button" aria-label="الانتقال إلى أسفل الصفحة" title="إلى الأسفل">' + icons.down + "</button>"
  ].join("");

  const mount = () => {
    if (!document.body || document.getElementById(controls.id)) return;
    document.body.appendChild(controls);
    const top = document.getElementById("uqScrollTop");
    const bottom = document.getElementById("uqScrollBottom");
    const theme = document.getElementById("uqThemeToggle");
    top.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));
    bottom.addEventListener("click", () => window.scrollTo({ top: document.documentElement.scrollHeight, behavior: "smooth" }));
    theme.addEventListener("click", () => applyTheme(root.dataset.theme === "light" ? "dark" : "light"));

    const updateScrollButtons = () => {
      const y = window.scrollY || root.scrollTop;
      const max = Math.max(0, root.scrollHeight - window.innerHeight);
      top.hidden = y < 120;
      bottom.hidden = max < 120 || y > max - 120;
    };
    window.addEventListener("scroll", updateScrollButtons, { passive: true });
    window.addEventListener("resize", updateScrollButtons, { passive: true });
    applyTheme(savedTheme);
    updateScrollButtons();
  };

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", mount, { once: true });
  else mount();
})();
