# QuickNovel Ultimate — docs

**Current app version:** `1.2.3`  
**Defaults:** dark mode ON · Ultimate Blue (`BlueFire`) · reader loading **F — Glyph Pulse** (本, no raw URL).

| Path | Description |
|------|-------------|
| [`manual-1.1.4/`](manual-1.1.4/) | User manuals ES + EN (HTML, Word, screenshots, bug templates) — historical 1.1.4 pack |
| [`previews/`](previews/) | Interactive HTML UI mocks |
| [`plugin-import-sample.json`](plugin-import-sample.json) | Sample LNReader catalog entry shape for paste/import tests |

## Previews

| File | Topic |
|------|--------|
| [`previews/plugin-providers-flow.html`](previews/plugin-providers-flow.html) | Plugins hub: repo → languages → install cards |
| [`previews/chapter-select-fab-highlight.html`](previews/chapter-select-fab-highlight.html) | Chapter **Select** FAB styles (shipped: **E — Icon disc**, theme `colorPrimary`) |
| [`previews/result-actions-info.html`](previews/result-actions-info.html) | Novel detail: action row, info stats, **Leer libro** CTA proposals |
| [`previews/chapter-fabs-pair.html`](previews/chapter-fabs-pair.html) | Chapter **Select + Filter** pair — 12 layout/glow options × themes |
| [`previews/chapter-filter-in-bottom-bar.html`](previews/chapter-filter-in-bottom-bar.html) | Move **Filter/Organize** into sticky bottom bar (8 layouts) |
| [`previews/result-tabs-cta-bar.html`](previews/result-tabs-cta-bar.html) | Detail **tabs** + **Descargar/Leer** bar proposals |
| [`previews/ultimate-themes.html`](previews/ultimate-themes.html) | **Ultimate color packs** — 11 accents (Blue…Soft) |
| [`previews/ultimate-icon-themes.html`](previews/ultimate-icon-themes.html) | **10 icon treatments** (A–J) for Settings / nav — pick one |
| [`previews/reader-loading.html`](previews/reader-loading.html) | Reader loading mocks — **shipped: F Glyph Pulse** |

Open the HTML files in a browser. Prefer [`ultimate-themes.html`](previews/ultimate-themes.html) for the Ultimate-only palette; older FAB mocks may still show retired stock colors.

## 1.2.3 notes

- Reader overlay: Glyph Pulse (`loading_glyph` 本 + chapter title + optional source chip).
- Appearance: `AppearanceMode.DEFAULT_DARK = true`; Ultimate pack default `UiIconTheme.DEFAULT_KEY = BlueFire`.
- E2E: `12-reader-loading`, `13-theme-defaults`; unit: `ReaderGlyphLoadingTest`, `AppearanceAndThemeDefaultsTest`.
- Desktop release APK: keep only `QuickNovel-Ultimate-1.2.3-universal.apk` (older `1.0.x`/`1.1.x` builds removed).

## Regenerate manuals

```bash
python docs/manual-1.1.4/_generar_docs_es.py
python docs/manual-1.1.4/_generar_docs_en.py
```
