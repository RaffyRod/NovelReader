# 📚 QuickNovel Ultimate — Docs

Project documentation map.

| | |
|---|---|
| 🏷️ **App** | **1.2.4** |
| 🌙 **Defaults** | Dark mode ON · **Ultimate Blue** |
| 本 **Reader** | Glyph Pulse (no raw URL) |

---

## 🗺️ Map

| Path | What it is |
|------|------------|
| 📘 [`manual-1.2.3/`](manual-1.2.3/) | User manuals ES + EN (HTML, Word, screenshots, bug templates) |
| 🎨 [`previews/`](previews/) | Interactive HTML UI mocks (themes, icons, loading, FABs…) |
| 🧩 [`plugin-import-sample.json`](plugin-import-sample.json) | Sample LNReader catalog entry for paste/import tests |

---

## 📖 Manuals (tutorials)

| 🌍 | HTML | Word | Bugs |
|----|------|------|------|
| 🇪🇸 Español | [Open](manual-1.2.3/es/Manual-QuickNovel-Ultimate-1.2.3.html) | [DOCX](manual-1.2.3/es/Manual-QuickNovel-Ultimate-1.2.3.docx) | [Template](manual-1.2.3/es/plantillas/Plantilla-Reporte-Bugs.docx) |
| 🇬🇧 English | [Open](manual-1.2.3/en/Manual-QuickNovel-Ultimate-1.2.3.html) | [DOCX](manual-1.2.3/en/Manual-QuickNovel-Ultimate-1.2.3.docx) | [Template](manual-1.2.3/en/plantillas/Bug-Report-Template.docx) |

Detailed index → [`manual-1.2.3/README.md`](manual-1.2.3/README.md)

### 🔄 Regenerate

```bash
python docs/manual-1.2.3/_generar_docs_es.py
python docs/manual-1.2.3/_generar_docs_en.py
```

---

## 🎨 Previews (open in a browser)

| Preview | Topic |
|---------|--------|
| 🔌 [`plugin-providers-flow.html`](previews/plugin-providers-flow.html) | LNReader plugins hub |
| 🎯 [`chapter-select-fab-highlight.html`](previews/chapter-select-fab-highlight.html) | Select FAB (**E — Icon disc** shipped) |
| ℹ️ [`result-actions-info.html`](previews/result-actions-info.html) | Novel detail / CTAs |
| ✨ [`chapter-fabs-pair.html`](previews/chapter-fabs-pair.html) | Select + Filter |
| 📌 [`chapter-filter-in-bottom-bar.html`](previews/chapter-filter-in-bottom-bar.html) | Filter in bottom bar |
| 📑 [`result-tabs-cta-bar.html`](previews/result-tabs-cta-bar.html) | Tabs + Download/Read |
| 🌈 [`ultimate-themes.html`](previews/ultimate-themes.html) | 11 Ultimate accents |
| 🔠 [`ultimate-icon-themes.html`](previews/ultimate-icon-themes.html) | Icon styles A–J |
| 本 [`reader-loading.html`](previews/reader-loading.html) | Reader loading — **F Glyph Pulse** |

---

## 💻 Desktop pack

| Content | Typical location |
|---------|------------------|
| 📦 APK + manuals + previews | `Desktop/QuickNovel-Ultimate-1.2.3-Docs/` |
| 📱 APK only | `Desktop/QuickNovel-Ultimate-1.2.4-universal.apk` |

Copy from `docs/manual-1.2.3/` + the signed APK when you regenerate docs.
