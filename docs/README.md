# QuickNovel Ultimate — docs

| Path | Description |
|------|-------------|
| [`manual-1.1.4/`](manual-1.1.4/) | User manuals ES + EN (HTML, Word, screenshots, bug templates) |
| [`previews/`](previews/) | Interactive HTML UI mocks |
| [`plugin-import-sample.json`](plugin-import-sample.json) | Sample LNReader catalog entry shape for paste/import tests |

## Previews

| File | Topic |
|------|--------|
| [`previews/plugin-providers-flow.html`](previews/plugin-providers-flow.html) | Plugins hub: repo → languages → install cards |
| [`previews/chapter-select-fab-highlight.html`](previews/chapter-select-fab-highlight.html) | Chapter **Select** FAB styles (shipped: **J — Halo ring**, theme `colorPrimary`) |

Open the HTML files in a browser. Theme chips in the FAB preview switch Ultimate Blue / Fire / Party / Pink / Green / Banana so the halo color matches each theme.

## Regenerate manuals

```bash
python docs/manual-1.1.4/_generar_docs_es.py
python docs/manual-1.1.4/_generar_docs_en.py
```
