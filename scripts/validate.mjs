import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { execFileSync } from "node:child_process";

const root = process.cwd();
const read = (file) => readFileSync(path.join(root, file), "utf8");
const parseJSON = (file) => {
  try { return JSON.parse(read(file)); }
  catch (error) { throw new Error("Invalid JSON in " + file + ": " + error.message); }
};
const localPath = (raw, from = ".") => {
  const value = raw.trim().replace(/\\/g, "/");
  if (!value || /^(?:#|https?:|\/\/|data:|blob:|mailto:|tel:|javascript:)/i.test(value)) return null;
  const pathname = value.split(/[?#]/, 1)[0];
  if (!pathname || pathname.startsWith("/")) return null;
  return path.resolve(root, from, decodeURIComponent(pathname));
};
const checkLocalReference = (raw, from, label) => {
  const target = localPath(raw, from);
  if (!target) return;
  assert.ok(target.startsWith(root + path.sep) || target === root, label + " escapes the repository: " + raw);
  assert.ok(existsSync(target), label + " points to a missing file: " + raw);
};

for (const file of ["app.js", "prayer-calc.js", "sw.js"]) {
  execFileSync(process.execPath, ["--check", file], { cwd: root, stdio: "pipe" });
}

const manifest = parseJSON("manifest.webmanifest");
assert.equal(manifest.lang, "ar", "PWA language should be Arabic");
assert.equal(manifest.dir, "rtl", "PWA direction should be RTL");
assert.equal(manifest.start_url, "./", "Project Pages start_url should stay relative");
assert.equal(manifest.scope, "./", "Project Pages scope should stay relative");
assert.ok(Array.isArray(manifest.icons) && manifest.icons.length >= 2, "PWA icons are required");
for (const icon of manifest.icons) checkLocalReference(icon.src, ".", "manifest icon");

const meta = parseJSON("data/meta.json");
const reciters = parseJSON("data/reciters.json");
const radios = parseJSON("data/radios.json");
const adhan = parseJSON("data/adhan.json");
assert.equal(meta.chapters?.length, 114, "Quran metadata must contain all 114 surahs");
assert.equal(meta.spages?.length, 114, "Surah page index must contain 114 entries");
assert.ok(Array.isArray(reciters) && reciters.length >= 100, "Reciter catalogue looks incomplete");
assert.ok(Array.isArray(radios) && radios.length >= 100, "Radio catalogue looks incomplete");
assert.ok(Array.isArray(adhan) && adhan.length > 0, "Adhan catalogue must not be empty");

const html = read("index.html");
for (const match of html.matchAll(/\b(?:src|href)\s*=\s*["']([^"']+)["']/gi)) {
  checkLocalReference(match[1], ".", "index.html asset");
}
const css = read("style.css");
for (const match of css.matchAll(/url\(\s*["']?([^"')]+)["']?\s*\)/gi)) {
  checkLocalReference(match[1], ".", "style.css asset");
}

const require = createRequire(import.meta.url);
const prayerCalc = require("../prayer-calc.js");
const prayers = prayerCalc.times([2026, 11, 21], 33.5731, -7.5898, "morocco");
const ordered = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"].map((key) => prayers[key]);
assert.ok(ordered.every((date) => date instanceof Date && Number.isFinite(date.getTime())), "Prayer calculations must return valid dates");
assert.ok(ordered.every((date, index) => index === 0 || date > ordered[index - 1]), "Morocco prayer times must be in chronological order");

console.log("IsslamPro validation passed: JavaScript syntax, local assets, PWA metadata, Quran catalogues, and prayer calculations.");
