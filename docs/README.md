# 📚 QuickNovel Ultimate — Docs

Guía rápida de la documentación del proyecto.

| | |
|---|---|
| 🏷️ **App** | **1.2.3** |
| 🌙 **Defaults** | Modo oscuro ON · **Ultimate Blue** |
| 本 **Lector** | Glyph Pulse (sin URL cruda) |

---

## 🗺️ Mapa

| Carpeta / archivo | Qué es |
|-------------------|--------|
| 📘 [`manual-1.2.3/`](manual-1.2.3/) | Manuales usuario ES + EN (HTML, Word, capturas, plantillas bugs) |
| 🎨 [`previews/`](previews/) | Mocks HTML interactivos (temas, iconos, loading, FABs…) |
| 🧩 [`plugin-import-sample.json`](plugin-import-sample.json) | Ejemplo de catálogo LNReader para pegar/importar |

---

## 📖 Manuales (tutoriales)

| 🌍 | HTML | Word | Bugs |
|----|------|------|------|
| 🇪🇸 Español | [Abrir](manual-1.2.3/es/Manual-QuickNovel-Ultimate-1.2.3.html) | [DOCX](manual-1.2.3/es/Manual-QuickNovel-Ultimate-1.2.3.docx) | [Plantilla](manual-1.2.3/es/plantillas/Plantilla-Reporte-Bugs.docx) |
| 🇬🇧 English | [Open](manual-1.2.3/en/Manual-QuickNovel-Ultimate-1.2.3.html) | [DOCX](manual-1.2.3/en/Manual-QuickNovel-Ultimate-1.2.3.docx) | [Template](manual-1.2.3/en/plantillas/Bug-Report-Template.docx) |

Índice detallado → [`manual-1.2.3/README.md`](manual-1.2.3/README.md)

### 🔄 Regenerar

```bash
python docs/manual-1.2.3/_generar_docs_es.py
python docs/manual-1.2.3/_generar_docs_en.py
```

---

## 🎨 Previews (abrir en el navegador)

| Preview | Tema |
|---------|------|
| 🔌 [`plugin-providers-flow.html`](previews/plugin-providers-flow.html) | Hub de plugins LNReader |
| 🎯 [`chapter-select-fab-highlight.html`](previews/chapter-select-fab-highlight.html) | FAB Select (**E — Icon disc** shipped) |
| ℹ️ [`result-actions-info.html`](previews/result-actions-info.html) | Detalle novela / CTAs |
| ✨ [`chapter-fabs-pair.html`](previews/chapter-fabs-pair.html) | Select + Filter |
| 📌 [`chapter-filter-in-bottom-bar.html`](previews/chapter-filter-in-bottom-bar.html) | Filtro en barra inferior |
| 📑 [`result-tabs-cta-bar.html`](previews/result-tabs-cta-bar.html) | Tabs + Descargar/Leer |
| 🌈 [`ultimate-themes.html`](previews/ultimate-themes.html) | 11 acentos Ultimate |
| 🔠 [`ultimate-icon-themes.html`](previews/ultimate-icon-themes.html) | Estilos de icono A–J |
| 本 [`reader-loading.html`](previews/reader-loading.html) | Loading lector — **F Glyph Pulse** |

---

## 💻 Pack Desktop

| Contenido | Ubicación típica |
|-----------|------------------|
| 📦 APK + manuals + previews | `Desktop/QuickNovel-Ultimate-1.2.3-Docs/` |
| 📱 Solo APK | `Desktop/QuickNovel-Ultimate-1.2.3-universal.apk` |

Copia desde `docs/manual-1.2.3/` + el APK firmado cuando regeneres docs.
