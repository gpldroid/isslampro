/* Ultra Quran service worker: app shell offline, data refreshed in the background,
   Mushaf pages and fonts cached after first view. Audio is not intercepted
   (offline audio is read from the "uq-audio" cache by the page itself). */
const VERSION = "uq-shell-59";
const SHELL = [
  "./", "index.html", "style.css?v=56", "app.js?v=57", "prayer-calc.js?v=56", "style.css", "app.js", "manifest.webmanifest",
  "img/lattice.svg", "img/bg.webp", "img/bg-m.webp", "fonts/Cairo-SemiBold.ttf", "img/star.svg", "icons/icon-192.png?v=2", "icons/icon-512.png?v=2",
  "prayer-calc.js", "data/meta.json", "data/reciters.json", "data/radios.json", "data/adhan.json",
];
const KEEP = [VERSION, "uq-data", "uq-pages", "uq-fonts", "uq-audio", "uq-img"];

self.addEventListener("install", (e) => {
  // cache: "reload" skips the browser's HTTP cache so a new version never stores old files
  e.waitUntil(caches.open(VERSION).then((c) => c.addAll(SHELL.map((u) => new Request(u, { cache: "reload" })))).then(() => self.skipWaiting()));
});
self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => !KEEP.includes(k)).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

const PAGE_HOSTS = ["cdn.jsdelivr.net", "raw.githubusercontent.com", "files.quran.app", "api.alquran.cloud"];

async function cacheFirst(req, name) {
  const c = await caches.open(name);
  const hit = await c.match(req);
  if (hit) return hit;
  const res = await fetch(req);
  if (res.ok || res.type === "opaque") c.put(req, res.clone());
  return res;
}
async function networkFirst(req, name) {
  const c = await caches.open(name);
  try {
    const res = await fetch(req, { cache: "no-cache" });
    if (res.ok) c.put(req, res.clone());
    return res;
  } catch {
    return (await c.match(req)) || (await caches.match(req)) || Response.error();
  }
}
async function staleWhileRevalidate(req, name) {
  const c = await caches.open(name);
  const hit = await c.match(req);
  const net = fetch(req).then((res) => { if (res.ok) c.put(req, res.clone()); return res; }).catch(() => hit);
  return hit || net;
}

// Stream byte-range slices from the cached full audio response without creating a page Blob.
async function audioRangeFromCache(req, cached) {
  const header = req.headers.get("range") || "";
  const length = Number(cached.headers.get("content-length"));
  const match = /^bytes=(\\d*)-(\\d*)$/.exec(header);
  if (!match || !Number.isFinite(length) || length <= 0 || !cached.body) return cached;
  let start, end;
  if (!match[1]) { const suffix = Number(match[2]); if (!suffix) return cached; start = Math.max(0, length - suffix); end = length - 1; }
  else { start = Number(match[1]); end = match[2] ? Math.min(Number(match[2]), length - 1) : length - 1; }
  if (start >= length || end < start) return new Response(null, { status: 416, headers: { "Content-Range": "bytes */" + length } });
  const expected = end - start + 1, reader = cached.body.getReader();
  let position = 0, sent = 0;
  const body = new ReadableStream({
    async pull(controller) {
      try {
        while (sent < expected) {
          const part = await reader.read();
          if (part.done) { controller.close(); return; }
          const chunkStart = position, chunkEnd = position + part.value.byteLength;
          position = chunkEnd;
          if (chunkEnd <= start) continue;
          const from = Math.max(0, start - chunkStart), to = Math.min(part.value.byteLength, end + 1 - chunkStart);
          if (to > from) { const slice = part.value.slice(from, to); controller.enqueue(slice); sent += slice.byteLength; }
        }
        controller.close(); reader.cancel().catch(() => {});
      } catch (error) { controller.error(error); }
    },
    cancel() { reader.cancel().catch(() => {}); }
  });
  const headers = new Headers(cached.headers);
  headers.delete("content-encoding"); headers.set("Accept-Ranges", "bytes");
  headers.set("Content-Range", "bytes " + start + "-" + end + "/" + length);
  headers.set("Content-Length", String(expected));
  return new Response(body, { status: 206, statusText: "Partial Content", headers });
}

self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (req.headers.has("range")) {
    return e.respondWith((async () => {
      const audio = await caches.open("uq-audio");
      const hit = await audio.match(new Request(req.url, { method: "GET" }));
      return hit ? audioRangeFromCache(req, hit) : fetch(req);
    })());
  }
  if (url.origin === location.origin) {
    // دولة التلاوة: new episodes should show up right away when online
    if (url.pathname.includes("/data/yt/")) return e.respondWith(networkFirst(req, "uq-data"));
    if (url.pathname.includes("/data/")) return e.respondWith(staleWhileRevalidate(req, "uq-data"));
    if (url.pathname.includes("/img/p/")) return e.respondWith(cacheFirst(req, "uq-img"));
    if (req.mode === "navigate") {
      return e.respondWith(fetch(req, { cache: "no-cache" }).then((res) => {
        const copy = res.clone();
        caches.open(VERSION).then((c) => c.put("index.html", copy));
        return res;
      }).catch(() => caches.match("index.html")));
    }
    // the app's own code and styles: always the newest when online, the saved copy when offline
    if (/\.(js|css|html|webmanifest)$/.test(url.pathname)) return e.respondWith(networkFirst(req, VERSION));
    return e.respondWith(staleWhileRevalidate(req, VERSION));
  }
  if (url.host === "fonts.googleapis.com" || url.host === "fonts.gstatic.com") return e.respondWith(cacheFirst(req, "uq-fonts"));
  // المصحف التفاعلي: page fonts never change; word timings rarely do
  if (url.host === "static.qurancdn.com" && url.pathname.includes("/fonts/quran/")) return e.respondWith(cacheFirst(req, "uq-pages"));
  if (url.host === "api.quran.com" && url.pathname.includes("/chapter_recitations/")) return e.respondWith(staleWhileRevalidate(req, "uq-data"));
  if (PAGE_HOSTS.includes(url.host) && /\.(png|jpg)$|\/v1\/page\//.test(url.pathname)) return e.respondWith(cacheFirst(req, "uq-pages"));
});

self.addEventListener("notificationclick", (e) => {
  e.notification.close();
  e.waitUntil(self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((list) => {
    for (const c of list) if ("focus" in c) return c.focus();
    return self.clients.openWindow("./#/prayer");
  }));
});
