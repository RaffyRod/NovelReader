# -*- coding: utf-8 -*-
"""Full English manual for QuickNovel Ultimate 1.1.4 (HTML + Word + bug template)."""
from __future__ import annotations

from datetime import date
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = Path(__file__).resolve().parent / "en"
IMG = ROOT / "imagenes"
PLANTILLAS = ROOT / "plantillas"
VERSION = "1.1.4"
TODAY = date.today().strftime("%Y-%m-%d")
APP_ID = "com.lagradost.quicknovel.ultimate"
APK_NAME = "QuickNovel-Ultimate-1.1.4-universal.apk"

IMAGENES = [
    ("01-biblioteca.png", "Library: local search, Import-B and novels"),
    ("02-buscar.png", "Search: field, popular and provider access"),
    ("03-historial.png", "History: Reading / Visited"),
    ("04-ajustes.png", "Settings (top)"),
    ("12-ajustes-general.png", "Settings: general / translation / tutorial area"),
    ("09-ajustes-tutorial-y-proveedores.png", "Settings: Tutorial + Custom providers"),
    ("10-dialogo-agregar-proveedor.png", "Add provider dialog (Blog / Website)"),
    ("11-administrar-proveedores.png", "Manage custom providers"),
    ("13-dialogo-proveedores-buscar.png", "Search provider multi-select"),
    ("06-secciones.png", "Library sections"),
    ("07-buscar-resultados.png", "Search results with provider chips"),
    ("08-detalle-novela.png", "Novel detail flow"),
]


def esc(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")


def fig(file: str, caption: str) -> str:
    if not (IMG / file).exists():
        return f"<p><em>(Missing image {esc(file)})</em></p>"
    return f"""<figure>
  <img src="imagenes/{esc(file)}" alt="{esc(caption)}" />
  <figcaption>{esc(caption)}</figcaption>
</figure>"""


def build_html() -> str:
    gallery = "\n".join(fig(f, c) for f, c in IMAGENES)
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>QuickNovel Ultimate {VERSION} Full Manual (EN)</title>
<style>
:root {{ --ink:#132238; --muted:#5b6b7c; --accent:#1a66b3; --bg:#eef3f9; --card:#fff; --line:#d5deea; }}
* {{ box-sizing:border-box; }}
body {{ margin:0; font-family:"Segoe UI",Tahoma,sans-serif; color:var(--ink); background:linear-gradient(#dfeaf6,#eef3f9 180px); line-height:1.55; }}
header {{ max-width:1000px; margin:0 auto; padding:40px 24px 20px; }}
.badge {{ display:inline-block; background:var(--accent); color:#fff; border-radius:999px; padding:4px 12px; font-size:.85rem; }}
h1 {{ margin:10px 0 6px; font-size:2.15rem; }}
header p {{ margin:4px 0; color:var(--muted); }}
main {{ max-width:1000px; margin:0 auto 60px; padding:0 24px; }}
nav.toc, section {{ background:var(--card); border:1px solid var(--line); border-radius:16px; padding:22px 26px; margin-bottom:18px; }}
nav.toc h2, section > h2 {{ margin-top:0; border-bottom:2px solid var(--line); padding-bottom:8px; }}
h3 {{ color:#163552; margin-top:1.35em; }}
a {{ color:var(--accent); text-decoration:none; }}
table {{ width:100%; border-collapse:collapse; margin:12px 0 16px; font-size:.94rem; }}
th,td {{ border:1px solid var(--line); padding:9px 11px; vertical-align:top; text-align:left; }}
th {{ background:#eaf1f9; }}
code,.mono {{ font-family:Consolas,monospace; background:#eaf0f6; padding:1px 6px; border-radius:5px; }}
figure {{ margin:16px 0; text-align:center; }}
figure img {{ max-width:min(340px,100%); border-radius:18px; border:1px solid var(--line); box-shadow:0 8px 24px rgba(19,34,56,.12); }}
figcaption {{ color:var(--muted); font-size:.88rem; margin-top:7px; }}
.gallery {{ display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:14px; }}
.gallery figure img {{ max-width:100%; }}
.callout {{ background:#f3f8ff; border-left:4px solid var(--accent); padding:10px 14px; margin:12px 0; border-radius:0 10px 10px 0; }}
ol.steps > li {{ margin-bottom:7px; }}
footer {{ max-width:1000px; margin:0 auto 40px; padding:0 24px; color:var(--muted); font-size:.9rem; }}
</style>
</head>
<body>
<header>
  <div class="badge">Stable version {VERSION}</div>
  <h1>QuickNovel Ultimate — Full Manual (English)</h1>
  <p>Onboarding guide: where everything is, what each module does, custom providers, official tutorials, and how to file bugs.</p>
  <p>Date: {TODAY} · Package: <span class="mono">{APP_ID}</span> · APK: <span class="mono">{APK_NAME}</span></p>
  <p>Spanish edition: <a href="../es/Manual-QuickNovel-Ultimate-1.1.4.html">../es/</a></p>
</header>
<main>
<nav class="toc" id="toc">
  <h2>Table of contents</h2>
  <ol>
    <li><a href="#intro">Introduction</a></li>
    <li><a href="#install">Install &amp; first launch</a></li>
    <li><a href="#map">Navigation map</a></li>
    <li><a href="#library">Library</a></li>
    <li><a href="#search">Search</a></li>
    <li><a href="#history">History</a></li>
    <li><a href="#sections">Sections</a></li>
    <li><a href="#settings">Settings (all groups)</a></li>
    <li><a href="#providers">Custom providers (add / manage / advanced)</a></li>
    <li><a href="#detail">Novel detail, download &amp; export</a></li>
    <li><a href="#reader">Reader &amp; TTS</a></li>
    <li><a href="#tutorials">Official in-app tutorials</a></li>
    <li><a href="#flows">Step-by-step flows for beginners</a></li>
    <li><a href="#shots">Screenshot gallery</a></li>
    <li><a href="#bugs">How to write bugs for a Word handoff</a></li>
    <li><a href="#qa">Automated tests</a></li>
    <li><a href="#annex">Annex</a></li>
  </ol>
</nav>

<section id="intro">
  <h2>1. Introduction</h2>
  <p><strong>QuickNovel Ultimate</strong> is an Android app to <strong>search, read, listen (TTS) and download</strong> web novels, and to <strong>import EPUB/PDF</strong> files.</p>
  <p>It is a fork of QuickNovel with its own branding:</p>
  <ul>
    <li><strong>Fire</strong> themes (Blue / Black / Lava).</li>
    <li>Distinct package: <span class="mono">{APP_ID}</span> (not stock <span class="mono">com.lagradost.quicknovel</span>).</li>
    <li>History split into <em>Reading</em> / <em>Visited</em>.</li>
    <li><strong>Import-B</strong> drop zone in Library.</li>
    <li><strong>Custom providers</strong> (Blogger blog or Madara-like site) with simple + advanced CSS modes.</li>
    <li>TTS prefers Piper/Sherpa neural voices.</li>
    <li>Search <strong>blocks special characters by default</strong> (optional toggle).</li>
  </ul>
  <div class="callout">If you see two QuickNovel icons, open <strong>QuickNovel Ultimate</strong> only. Stock lacks Ultimate Sections, Import-B and our search/provider changes.</div>
</section>

<section id="install">
  <h2>2. Install &amp; first launch</h2>
  <ul>
    <li>Android 6.0+.</li>
    <li>Network for online providers.</li>
    <li>Optional: Sherpa TTS / Piper for better ES/EN voices.</li>
  </ul>
  <ol class="steps">
    <li>Uninstall older Ultimate and stock QuickNovel if present.</li>
    <li>Install <span class="mono">{APK_NAME}</span>.</li>
    <li>Allow notifications if prompted.</li>
    <li>If an update dialog offers upstream 3.x → <strong>Skip update / Cancel</strong>.</li>
  </ol>
</section>

<section id="map">
  <h2>3. Navigation map (bottom bar)</h2>
  <table>
    <tr><th>Tab</th><th>Purpose</th><th>What you see first</th></tr>
    <tr><td><strong>Library</strong></td><td>Local books (downloaded/imported)</td><td>Search, Import-B, list</td></tr>
    <tr><td><strong>Search</strong></td><td>Online providers</td><td>Query field, provider tune, Popular</td></tr>
    <tr><td><strong>History</strong></td><td>Opened / reading items</td><td>Reading / Visited pills</td></tr>
    <tr><td><strong>Settings</strong></td><td>All configuration</td><td>General, theme, app, chapters, translation, tutorial, custom providers, reader, info</td></tr>
    <tr><td><strong>Sections</strong></td><td>Library shelves</td><td>Custom sections</td></tr>
  </table>
  {fig("01-biblioteca.png", "Typical start: Library")}
</section>

<section id="library">
  <h2>4. Library module</h2>
  <p><strong>Where:</strong> Library tab. <strong>Code:</strong> <span class="mono">ui/download/DownloadScreen.kt</span>.</p>
  <ul>
    <li><strong>Search my novels…</strong> — filters local library (pinned while scrolling).</li>
    <li><strong>Import-B</strong> — “Import a book / EPUB or PDF”.</li>
    <li><strong>+</strong> — import one or many.</li>
    <li>List/grid toggle, <strong>Continue reading</strong>, <strong>Follow</strong>, progress “Chapter X of Y”.</li>
    <li>Sort FAB for ordering criteria.</li>
  </ul>
</section>

<section id="search">
  <h2>5. Search module</h2>
  <p><strong>Where:</strong> Search tab. <strong>Code:</strong> <span class="mono">ui/search/SearchScreen.kt</span>.</p>
  <ul>
    <li>Query field; provider tune icon → multi-select dialog.</li>
    <li>Recent searches, Popular carousel, result chips per provider.</li>
    <li>Special characters (@#$%…) blocked by default; enable in Settings.</li>
  </ul>
  {fig("13-dialogo-proveedores-buscar.png", "Provider multi-select")}
  {fig("07-buscar-resultados.png", "Results with chips")}
</section>

<section id="history">
  <h2>6. History module</h2>
  <p><strong>Where:</strong> History tab. Reading vs Visited pills; date groups (Today, Yesterday, This week, Earlier); clear options.</p>
</section>

<section id="sections">
  <h2>7. Sections module</h2>
  <p><strong>Where:</strong> Sections tab (Ultimate-only). Organize library novels into shelves/sections with Fire theming.</p>
  {fig("06-secciones.png", "Sections")}
</section>

<section id="settings">
  <h2>8. Settings (all groups)</h2>
  <p><strong>Code:</strong> <span class="mono">ui/settings/SettingScreen.kt</span>.</p>
  <h3>8.1 General</h3>
  <table>
    <tr><th>Option</th><th>What it does</th></tr>
    <tr><td>Locale</td><td>UI language (es, en, tr, ru…).</td></tr>
    <tr><td>Provider languages</td><td>Which non-English source languages appear.</td></tr>
    <tr><td>Show English providers</td><td>Off by default.</td></tr>
    <tr><td>Allow special characters in search</td><td>Off by default.</td></tr>
    <tr><td>Search providers list</td><td>Which built-in providers are active.</td></tr>
  </table>
  <h3>8.2 Theme</h3>
  <ul>
    <li>App theme (Ultimate Blue, etc.).</li>
    <li>Fire icon pack + launcher icon (Blue/Black/Lava).</li>
    <li>Bottom nav style, settings style, primary color, rating format.</li>
  </ul>
  <h3>8.3 App</h3>
  <ul>
    <li>Check for update (current {VERSION}).</li>
    <li>APK installer preference.</li>
    <li>Show app updates (keep off on Ultimate to avoid stock APK).</li>
    <li>Backup / restore + backup folder.</li>
  </ul>
  <h3>8.4 New chapters</h3>
  <ul><li>Notify new chapters, frequency 3/6/12/24h, check now.</li></ul>
  <h3>8.5 Translation (Gemini)</h3>
  <ul><li>Get API key, paste key, pick model, test key.</li></ul>
  <h3>8.6 Tutorial</h3>
  <p>Three help dialogs: Custom providers, Downloads, TTS (see §12).</p>
  <h3>8.7 Custom providers</h3>
  <p>See §9.</p>
  <h3>8.8 Reader settings / Info</h3>
  <ul>
    <li>External reader toggle; download path (e.g. Download/My Novels/).</li>
    <li>Logcat viewer (save / copy / clear).</li>
  </ul>
  {fig("09-ajustes-tutorial-y-proveedores.png", "Tutorial + Custom providers")}
</section>

<section id="providers">
  <h2>9. Custom providers (add / manage / advanced)</h2>
  <p><strong>Where:</strong> Settings → <strong>Custom providers</strong>.</p>
  <p><strong>Code:</strong> <span class="mono">CustomProviderDialogs.kt</span>, <span class="mono">CustomProviders.kt</span>.</p>

  <h3>9.1 Simple add</h3>
  <ol class="steps">
    <li>Settings → <strong>Add provider</strong>.</li>
    <li>Pick type:
      <ul>
        <li><strong>Blog (Blogger / Blogspot)</strong> — e.g. myblog.blogspot.com. Novel lists appear one-by-one in Search.</li>
        <li><strong>Website (Madara / other)</strong> — WordPress/Madara novel sites; auto templates.</li>
      </ul>
    </li>
    <li>Enter <strong>Name</strong>, <strong>site address</strong>, <strong>language</strong> (2 letters, e.g. es).</li>
    <li>Tap <strong>VALIDATE</strong>, wait, then <strong>Add provider</strong>.</li>
  </ol>
  <div class="callout">Tip: for Spanish blogs pick Blog. English filler words like “the” often return nothing; empty/Spanish queries work better.</div>
  {fig("10-dialogo-agregar-proveedor.png", "Simple add form")}

  <h3>9.2 If auto-detect fails → Advanced</h3>
  <ol class="steps">
    <li>Tap <strong>Advanced</strong>.</li>
    <li>Fill CSS selectors (or Madara preset).</li>
    <li><strong>Test</strong> with a sample query.</li>
    <li>Save when search → novel → chapter all pass.</li>
  </ol>
  <table>
    <tr><th>Advanced field</th><th>Meaning</th></tr>
    <tr><td>Name / site URL / lang</td><td>Identity</td></tr>
    <tr><td>Search URL</td><td>Must include <span class="mono">{{query}}</span></td></tr>
    <tr><td>Result item / link / image</td><td>Search list CSS</td></tr>
    <tr><td>Novel title / author / cover / synopsis</td><td>Novel page CSS</td></tr>
    <tr><td>Chapter links / content</td><td>Chapter list + body</td></tr>
    <tr><td>Newest first / Madara ajax</td><td>List behaviour flags</td></tr>
    <tr><td>Test word / Madara fill</td><td>Validation helpers</td></tr>
  </table>

  <h3>9.3 Manage</h3>
  <ol class="steps">
    <li>Settings → <strong>Manage providers</strong>.</li>
    <li>Empty → message + add button.</li>
    <li>Pick one → Edit or Delete (downloads are kept).</li>
  </ol>
  {fig("11-administrar-proveedores.png", "Manage providers")}

  <h3>9.4 After adding</h3>
  <p>The provider appears in Search multi-select and Settings provider lists; you can search it like built-ins.</p>
</section>

<section id="detail">
  <h2>10. Novel detail, download &amp; export</h2>
  <ul>
    <li>Cover, synopsis, chapter list.</li>
    <li><strong>Stream / read online</strong> vs <strong>Download</strong>.</li>
    <li>Download dialog: all chapters or range; chapters-per-file batches; EPUB/PDF flows; preview where available.</li>
    <li>Then open from Library; Update fetches new chapters; Delete removes local files.</li>
  </ul>
  <div class="callout">Slow sources: prefer online reading. Gemini translation can make downloads much slower.</div>
</section>

<section id="reader">
  <h2>11. Reader &amp; TTS</h2>
  <ul>
    <li>Tap center to show/hide bars; chapters, TTS, reader settings.</li>
    <li>External reader setting may open EPUB/PDF in another app.</li>
  </ul>
  <ol class="steps">
    <li>Open a chapter → speaker icon.</li>
    <li>TTS bar: play/pause, stop, prev/next sentence.</li>
    <li>Reader settings → Voice: language, voice, rate, pitch, sleep timer, lock TTS area.</li>
    <li>Also controllable from the notification.</li>
  </ol>
  <p>Recommended voices: Piper Dave (ES) / Lessac (EN) via Sherpa-ONNX.</p>
</section>

<section id="tutorials">
  <h2>12. Official in-app tutorials</h2>
  <p>Under <strong>Settings → Tutorial</strong>:</p>
  <h3>Custom providers</h3>
  <ol class="steps">
    <li>Settings → Custom providers → Add provider.</li>
    <li>Blog or Website + name/URL → validate.</li>
    <li>Manage to edit/delete; Advanced for manual CSS.</li>
  </ol>
  <h3>Downloads</h3>
  <ol class="steps">
    <li>Novel → Download → all or range.</li>
    <li>Optional chapters-per-file.</li>
    <li>Path in Settings → reader / download path → open from Library.</li>
  </ol>
  <h3>TTS</h3>
  <ol class="steps">
    <li>Chapter → speaker → TTS bar / notification.</li>
    <li>Voice tab for language/rate/sleep.</li>
    <li>Install Sherpa/Piper for best quality.</li>
  </ol>
</section>

<section id="flows">
  <h2>13. Beginner flows</h2>
  <h3>A) First search &amp; read</h3>
  <ol class="steps"><li>Search tab → pick providers → query → open cover → read online.</li></ol>
  <h3>B) Offline download</h3>
  <ol class="steps"><li>Detail → Download → wait → Library.</li></ol>
  <h3>C) Import EPUB/PDF</h3>
  <ol class="steps"><li>Library → Import a book → pick file → open.</li></ol>
  <h3>D) Add your blog/site</h3>
  <ol class="steps"><li>Settings → Add provider → Validate → Search with it enabled.</li></ol>
  <h3>E) Follow updates</h3>
  <ol class="steps"><li>Follow on library card → Settings → New chapters notify + frequency.</li></ol>
</section>

<section id="shots">
  <h2>14. Screenshot gallery</h2>
  <div class="gallery">{gallery}</div>
</section>

<section id="bugs">
  <h2>15. How to write bugs for a Word handoff</h2>
  <p>Template: <span class="mono">plantillas/Bug-Report-Template.docx</span>.</p>
  <ol class="steps">
    <li>Confirm Ultimate APK {VERSION}.</li>
    <li>Device + Android version.</li>
    <li>Reproduce ≥ 2 times; attach screenshot.</li>
    <li>One bug = one problem; numbered steps; expected vs actual.</li>
    <li>Save as <span class="mono">Bugs-QuickNovel-Ultimate-{VERSION}-YYYY-MM-DD.docx</span>.</li>
  </ol>
  <table>
    <tr><th>Field</th><th>Example</th></tr>
    <tr><td>ID</td><td>BUG-012</td></tr>
    <tr><td>Title</td><td>Custom providers: VALIDATE never finishes for blogspot X</td></tr>
    <tr><td>Severity</td><td>High</td></tr>
    <tr><td>Module</td><td>Custom providers</td></tr>
    <tr><td>Steps / Expected / Actual</td><td>Numbered, clear</td></tr>
  </table>
</section>

<section id="qa">
  <h2>16. Automated tests</h2>
  <ul>
    <li>Unit: <span class="mono">./gradlew :app:testDebugUnitTest</span></li>
    <li>E2E: <span class="mono">e2e/</span> specs 01–09 (library, nav, search, history, settings, import, reader/TTS seed, providers, special chars)</li>
    <li>Allure: <span class="mono">e2e/allure-report</span></li>
  </ul>
</section>

<section id="annex">
  <h2>17. Annex</h2>
  <ul>
    <li>https://github.com/RaffyRod/NovelReader</li>
    <li>https://github.com/LagradOst/QuickNovel</li>
    <li>Package: <span class="mono">{APP_ID}</span></li>
    <li>Spanish manual: <span class="mono">docs/manual-1.1.4/es/</span></li>
  </ul>
</section>
</main>
<footer>Generated for QuickNovel Ultimate {VERSION} · {TODAY} · English onboarding edition</footer>
</body>
</html>"""


def set_run_font(run, size=11, bold=False, color=None):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.name = "Calibri"
    run._element.get_or_add_rPr().get_or_add_rFonts().set(qn("w:eastAsia"), "Calibri")
    if color:
        run.font.color.rgb = color


def add_para(doc, text):
    p = doc.add_paragraph()
    r = p.add_run(text)
    set_run_font(r, 11)
    return p


def bullets(doc, items):
    for i in items:
        p = doc.add_paragraph(i, style="List Bullet")
        for r in p.runs:
            set_run_font(r, 11)


def numbers(doc, items):
    for i in items:
        p = doc.add_paragraph(i, style="List Number")
        for r in p.runs:
            set_run_font(r, 11)


def table(doc, headers, rows):
    t = doc.add_table(rows=1 + len(rows), cols=len(headers))
    t.style = "Table Grid"
    for i, h in enumerate(headers):
        t.rows[0].cells[i].text = h
        for p in t.rows[0].cells[i].paragraphs:
            for r in p.runs:
                set_run_font(r, 10, True)
    for ri, row in enumerate(rows):
        for ci, val in enumerate(row):
            t.rows[ri + 1].cells[ci].text = val
            for p in t.rows[ri + 1].cells[ci].paragraphs:
                for r in p.runs:
                    set_run_font(r, 10)
    doc.add_paragraph()


def img(doc, name, caption):
    path = IMG / name
    if not path.exists():
        return
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(str(path), width=Cm(5.6))
    c = doc.add_paragraph()
    c.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = c.add_run(caption)
    set_run_font(r, 9, color=RGBColor(90, 106, 122))


def build_manual_docx():
    doc = Document()
    for s in doc.sections:
        s.top_margin = Cm(1.8)
        s.bottom_margin = Cm(1.8)
        s.left_margin = Cm(2)
        s.right_margin = Cm(2)
    t = doc.add_paragraph()
    t.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = t.add_run(f"QuickNovel Ultimate {VERSION} — Full Manual (EN)")
    set_run_font(r, 20, True, RGBColor(26, 102, 179))
    add_para(doc, f"Onboarding + modules + custom providers + bugs · {TODAY}")
    add_para(doc, f"Package: {APP_ID} | APK: {APK_NAME}")

    doc.add_heading("Table of contents", 1)
    numbers(
        doc,
        [
            "Introduction",
            "Install",
            "Navigation map",
            "Library / Search / History / Sections",
            "Settings",
            "Custom providers",
            "Detail / download / reader / TTS",
            "Tutorials & beginner flows",
            "Screenshots",
            "Bug Word handoff",
            "QA & annex",
        ],
    )

    doc.add_heading("1. Introduction", 1)
    add_para(
        doc,
        "Ultimate searches, reads, listens (TTS) and downloads novels; imports EPUB/PDF. Fire themes, package com.lagradost.quicknovel.ultimate, Reading/Visited history, Import-B, custom providers, special-char filter off by default.",
    )

    doc.add_heading("2. Install", 1)
    numbers(doc, ["Uninstall old builds", f"Install {APK_NAME}", "Skip upstream 3.x update dialog", "Open Ultimate only"])

    doc.add_heading("3. Navigation", 1)
    table(
        doc,
        ["Tab", "Role"],
        [
            ["Library", "Local books + Import-B"],
            ["Search", "Online providers + multi-select"],
            ["History", "Reading / Visited"],
            ["Settings", "All configuration"],
            ["Sections", "Library shelves"],
        ],
    )

    doc.add_heading("4–7. Main modules", 1)
    bullets(
        doc,
        [
            "Library: local search, Import-B, continue reading, follow, sort.",
            "Search: query, provider tune, popular, chips; special chars blocked unless enabled.",
            "History: Reading/Visited + date groups.",
            "Sections: Ultimate shelves.",
        ],
    )

    doc.add_heading("8. Settings groups", 1)
    bullets(
        doc,
        [
            "General: locale, provider langs, English providers, special chars, provider list",
            "Theme: Fire icons/launcher, nav/settings styles, colors",
            "App: updates, backup/restore",
            "New chapters: notify + frequency",
            "Gemini translation",
            "Tutorial dialogs",
            "Custom providers",
            "External reader + download path",
            "Logcat",
        ],
    )

    doc.add_heading("9. Custom providers", 1)
    numbers(
        doc,
        [
            "Settings → Add provider → Blog or Website → name/URL/lang → VALIDATE → Add",
            "Advanced: CSS fields, Madara preset, Test",
            "Manage: edit/delete (keeps downloads)",
        ],
    )
    table(
        doc,
        ["Advanced field", "Use"],
        [
            ["Search URL + {query}", "Search endpoint"],
            ["resultItem / link / image", "Result list"],
            ["novel* selectors", "Novel page"],
            ["chapterLink / content", "Chapters + body"],
            ["Ajax / newest first", "List behaviour"],
        ],
    )
    img(doc, "10-dialogo-agregar-proveedor.png", "Add provider")
    img(doc, "11-administrar-proveedores.png", "Manage")

    doc.add_heading("10–11. Detail, download, reader, TTS", 1)
    bullets(
        doc,
        [
            "Read online vs Download (ranges, chapters-per-file, EPUB/PDF)",
            "TTS: speaker → bar/notification; Voice settings; Piper Dave/Lessac via Sherpa",
        ],
    )

    doc.add_heading("12–13. Tutorials & flows", 1)
    bullets(
        doc,
        [
            "In-app Tutorial: providers, downloads, TTS",
            "Flows: first search, offline download, import EPUB, add blog/site, follow updates",
        ],
    )

    doc.add_heading("14. Screenshots", 1)
    for f, c in IMAGENES:
        img(doc, f, c)

    doc.add_heading("15. Bugs for Word", 1)
    add_para(doc, "Use plantillas/Bug-Report-Template.docx. Required: ID, title, severity, module, environment, preconditions, steps, expected, actual, attachments.")
    numbers(doc, [f"Confirm APK {VERSION}", "Reproduce 2+", f"Save Bugs-QuickNovel-Ultimate-{VERSION}-YYYY-MM-DD.docx"])

    doc.add_heading("16. QA & annex", 1)
    bullets(doc, ["gradlew :app:testDebugUnitTest", "e2e specs 01-09 + Allure", "https://github.com/RaffyRod/NovelReader", "Spanish: docs/manual-1.1.4/es/"])

    out = ROOT / f"Manual-QuickNovel-Ultimate-{VERSION}.docx"
    doc.save(out)
    return out


def build_bug_template():
    doc = Document()
    t = doc.add_paragraph()
    t.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = t.add_run(f"Bug report template — QuickNovel Ultimate {VERSION}")
    set_run_font(r, 18, True, RGBColor(26, 102, 179))
    add_para(doc, "Date: ________  Tester: ________  Device: ________  Android: ________")
    add_para(doc, f"APK: {APK_NAME}")
    doc.add_heading("How to fill", 1)
    numbers(doc, ["One bug per block", "Name the module clearly", "Numbered reproducible steps", "Attach evidence"])
    for i in range(1, 6):
        doc.add_heading(f"Bug BUG-{i:03d}", 1)
        table(
            doc,
            ["Field", "Content"],
            [
                ["Title", ""],
                ["Severity", "Blocker / High / Medium / Low"],
                ["Module", "Library / Search / History / Sections / Settings / Custom providers / Detail / Download / Reader / TTS / Import / Translation"],
                ["Environment", f"Ultimate {VERSION}"],
                ["Preconditions", ""],
                ["Steps", "1.\n2.\n3."],
                ["Expected", ""],
                ["Actual", ""],
                ["Attachments", ""],
                ["Status", "New"],
            ],
        )
    out = PLANTILLAS / "Bug-Report-Template.docx"
    doc.save(out)
    return out


def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    PLANTILLAS.mkdir(exist_ok=True)
    IMG.mkdir(exist_ok=True)
    html = ROOT / f"Manual-QuickNovel-Ultimate-{VERSION}.html"
    # fix double braces for {query} display in HTML
    html.write_text(build_html().replace("{{query}}", "{query}"), encoding="utf-8")
    print("HTML", html)
    print("DOCX", build_manual_docx())
    print("BUGS", build_bug_template())
    (ROOT / "README.txt").write_text(
        f"QuickNovel Ultimate {VERSION} English manual\nSee Manual-QuickNovel-Ultimate-{VERSION}.html / .docx\nSpanish: ../es/\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
