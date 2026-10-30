# -*- coding: utf-8 -*-
"""Manual completo QuickNovel Ultimate 1.1.4 (HTML + Word + plantilla bugs)."""
from __future__ import annotations

import shutil
from datetime import date
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = Path(__file__).resolve().parent / "es"
IMG = ROOT / "imagenes"
PLANTILLAS = ROOT / "plantillas"
VERSION = "1.1.4"
HOY = date.today().strftime("%d/%m/%Y")
APP_ID = "com.lagradost.quicknovel.ultimate"
APK_NAME = "QuickNovel-Ultimate-1.1.4-universal.apk"

IMAGENES = [
    ("01-biblioteca.png", "Biblioteca: búsqueda local, Import-B y novelas"),
    ("02-buscar.png", "Buscar: campo, populares y acceso a proveedores"),
    ("03-historial.png", "Historial: Leyendo / Visitadas"),
    ("04-ajustes.png", "Ajustes (inicio)"),
    ("12-ajustes-general.png", "Ajustes: zona general / traducción / tutorial"),
    ("09-ajustes-tutorial-y-proveedores.png", "Ajustes: Tutorial + Proveedores propios"),
    ("10-dialogo-agregar-proveedor.png", "Diálogo Agregar proveedor (Blog / Sitio web)"),
    ("11-administrar-proveedores.png", "Administrar proveedores propios"),
    ("13-dialogo-proveedores-buscar.png", "Multi-select de proveedores en Buscar"),
    ("06-secciones.png", "Secciones de la biblioteca"),
    ("07-buscar-resultados.png", "Resultados de búsqueda con chips de proveedor"),
    ("08-detalle-novela.png", "Flujo de novela / detalle"),
]


def esc(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")


def fig(file: str, caption: str) -> str:
    if not (IMG / file).exists():
        return f"<p><em>(Falta imagen {esc(file)})</em></p>"
    return f"""<figure>
  <img src="imagenes/{esc(file)}" alt="{esc(caption)}" />
  <figcaption>{esc(caption)}</figcaption>
</figure>"""


def build_html() -> str:
    gallery = "\n".join(fig(f, c) for f, c in IMAGENES)
    return f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>Manual completo QuickNovel Ultimate {VERSION}</title>
<style>
:root {{ --ink:#132238; --muted:#5b6b7c; --accent:#1a66b3; --bg:#eef3f9; --card:#fff; --line:#d5deea; }}
* {{ box-sizing:border-box; }}
body {{ margin:0; font-family:"Segoe UI",Tahoma,sans-serif; color:var(--ink); background:linear-gradient(#dfeaf6,#eef3f9 180px); line-height:1.55; }}
header {{ max-width:1000px; margin:0 auto; padding:40px 24px 20px; }}
.badge {{ display:inline-block; background:var(--accent); color:#fff; border-radius:999px; padding:4px 12px; font-size:.85rem; }}
h1 {{ margin:10px 0 6px; font-size:2.15rem; letter-spacing:-.02em; }}
header p {{ margin:4px 0; color:var(--muted); }}
main {{ max-width:1000px; margin:0 auto 60px; padding:0 24px; }}
nav.toc, section {{ background:var(--card); border:1px solid var(--line); border-radius:16px; padding:22px 26px; margin-bottom:18px; }}
nav.toc h2, section > h2 {{ margin-top:0; border-bottom:2px solid var(--line); padding-bottom:8px; }}
h3 {{ color:#163552; margin-top:1.35em; }}
h4 {{ margin-bottom:.35em; }}
a {{ color:var(--accent); text-decoration:none; }}
a:hover {{ text-decoration:underline; }}
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
@media print {{ body {{ background:#fff; }} section,nav.toc {{ break-inside:avoid; }} }}
</style>
</head>
<body>
<header>
  <div class="badge">Versión estable {VERSION}</div>
  <h1>Manual completo de QuickNovel Ultimate</h1>
  <p>Guía para quien nunca ha visto la app: dónde está cada cosa, qué hace cada módulo, tutoriales y cómo reportar bugs.</p>
  <p>Fecha: {HOY} · Package: <span class="mono">{APP_ID}</span> · APK: <span class="mono">{APK_NAME}</span></p>
</header>
<main>
<nav class="toc" id="indice">
  <h2>Índice</h2>
  <ol>
    <li><a href="#intro">Introducción y qué es Ultimate</a></li>
    <li><a href="#instalar">Instalación y primeras pantallas</a></li>
    <li><a href="#mapa">Mapa de navegación (barra inferior)</a></li>
    <li><a href="#biblioteca">Módulo Biblioteca</a></li>
    <li><a href="#buscar">Módulo Buscar</a></li>
    <li><a href="#historial">Módulo Historial</a></li>
    <li><a href="#secciones">Módulo Secciones</a></li>
    <li><a href="#ajustes">Módulo Ajustes (todas las secciones)</a></li>
    <li><a href="#proveedores">Proveedores propios (agregar / administrar / avanzado)</a></li>
    <li><a href="#detalle">Detalle de novela, descarga y export</a></li>
    <li><a href="#lector">Lector y TTS</a></li>
    <li><a href="#tutoriales">Tutoriales oficiales de la app (co-tutorial)</a></li>
    <li><a href="#flujos">Flujos de uso paso a paso</a></li>
    <li><a href="#capturas">Galería de capturas</a></li>
    <li><a href="#bugs">Cómo crear bugs para entregar en Word</a></li>
    <li><a href="#qa">Pruebas automatizadas</a></li>
    <li><a href="#anexos">Anexos</a></li>
  </ol>
</nav>

<section id="intro">
  <h2>1. Introducción y qué es Ultimate</h2>
  <p><strong>QuickNovel Ultimate</strong> es una app Android para <strong>buscar, leer, escuchar (TTS) y descargar novelas</strong> desde proveedores web, y también para <strong>importar EPUB/PDF</strong> propios.</p>
  <p>Es un fork de QuickNovel con identidad propia:</p>
  <ul>
    <li>Marca y temas <strong>Fire</strong> (Blue / Black / Lava).</li>
    <li>Package distinto: <span class="mono">{APP_ID}</span> (no confundir con la app original <span class="mono">com.lagradost.quicknovel</span>).</li>
    <li>Historial separado en <em>Leyendo</em> / <em>Visitadas</em>.</li>
    <li><strong>Import-B</strong> en Biblioteca.</li>
    <li><strong>Proveedores propios</strong> (Blog Blogger o Sitio Madara) con modo simple y avanzado CSS.</li>
    <li>TTS priorizando voces neuronales Piper/Sherpa.</li>
    <li>Búsqueda que, por defecto, <strong>bloquea caracteres especiales</strong> (configurable).</li>
  </ul>
  <div class="callout">Si en el emulador/dispositivo ves dos iconos “QuickNovel”, abre siempre <strong>QuickNovel Ultimate</strong>. La original no tiene Secciones Ultimate, Import-B ni nuestros cambios.</div>
</section>

<section id="instalar">
  <h2>2. Instalación y primeras pantallas</h2>
  <h3>Requisitos</h3>
  <ul>
    <li>Android 6.0 o superior.</li>
    <li>Internet para buscar/descargar desde proveedores.</li>
    <li>Opcional: motor <strong>Sherpa TTS / Piper</strong> para mejor voz en español/inglés.</li>
  </ul>
  <h3>Instalar esta carpeta</h3>
  <ol class="steps">
    <li>Desinstala versiones viejas de Ultimate y, si existe, la QuickNovel stock.</li>
    <li>Instala <span class="mono">{APK_NAME}</span>.</li>
    <li>Al abrir, concede notificaciones si las pide.</li>
    <li>Si aparece “actualización” a 3.x de LagradOst → <strong>Skip update / Cancelar</strong> (no instalar ese APK sobre Ultimate).</li>
  </ol>
</section>

<section id="mapa">
  <h2>3. Mapa de navegación (barra inferior)</h2>
  <p>Casi todo parte de estos cinco tabs:</p>
  <table>
    <tr><th>Tab</th><th>Para qué sirve</th><th>Primera cosa que verás</th></tr>
    <tr><td><strong>Biblioteca</strong></td><td>Tus libros locales (descargados o importados)</td><td>Buscar mis novelas, Import-B, listado</td></tr>
    <tr><td><strong>Buscar</strong></td><td>Buscar en internet / proveedores</td><td>Campo “Buscar novelas…”, tune de proveedores, Populares</td></tr>
    <tr><td><strong>Historial</strong></td><td>Lo que abriste o estás leyendo</td><td>Pills Leyendo / Visitadas</td></tr>
    <tr><td><strong>Ajustes</strong></td><td>Configurar toda la app</td><td>Grupos: general, tema, app, capítulos nuevos, traducción, tutorial, proveedores propios, lector, info</td></tr>
    <tr><td><strong>Secciones</strong></td><td>Organizar la biblioteca en estantes</td><td>Secciones / estantes personalizados</td></tr>
  </table>
  {fig("01-biblioteca.png", "Punto de partida típico: Biblioteca")}
</section>

<section id="biblioteca">
  <h2>4. Módulo Biblioteca</h2>
  <p><strong>Dónde:</strong> tab Biblioteca. <strong>En código:</strong> <span class="mono">ui/download/DownloadScreen.kt</span>.</p>
  <h3>Elementos de la pantalla</h3>
  <ul>
    <li><strong>Buscar mis novelas…</strong> — filtra solo lo que ya tienes en biblioteca (barra fijada al hacer scroll).</li>
    <li><strong>Import-B</strong> — zona “Importar un libro / EPUB o PDF · toca para elegir”. Abre el selector del sistema.</li>
    <li><strong>+</strong> — menú de importar uno o varios.</li>
    <li><strong>Vista lista/cuadrícula</strong> — cambia cómo se muestran las portadas.</li>
    <li><strong>Continuar leyendo</strong> — acceso rápido a la novela en curso.</li>
    <li><strong>Seguir</strong> — seguimiento / actualizaciones de capítulos (junto con notificaciones en Ajustes).</li>
    <li><strong>Todas las novelas</strong> — listado completo con progreso “Capítulo X de Y”.</li>
    <li><strong>FAB Ordenar</strong> — criterios de orden (nombre, descargas, porcentaje, etc.).</li>
  </ul>
  <h3>Qué puedes hacer desde una novela de biblioteca</h3>
  <ul>
    <li>Abrir / Leer (lector interno o externo según ajuste).</li>
    <li>Actualizar capítulos nuevos.</li>
    <li>Borrar descarga local (no borra necesariamente el historial de lectura).</li>
    <li>Generar o abrir EPUB/PDF exportado según flujos de descarga.</li>
  </ul>
</section>

<section id="buscar">
  <h2>5. Módulo Buscar</h2>
  <p><strong>Dónde:</strong> tab Buscar. <strong>Código:</strong> <span class="mono">ui/search/SearchScreen.kt</span>.</p>
  <h3>Partes de la pantalla</h3>
  <ul>
    <li>Campo <strong>Buscar novelas…</strong>.</li>
    <li>Icono <strong>Proveedores</strong> (tune) → diálogo multi-select para activar/desactivar fuentes.</li>
    <li><strong>Búsquedas recientes</strong> (chips) y botón Borrar.</li>
    <li><strong>Populares</strong> — carrusel de recomendaciones del proveedor activo.</li>
    <li>Tras buscar: chips <strong>Todos</strong> y por cada proveedor con resultados; grid de portadas.</li>
  </ul>
  <h3>Caracteres especiales</h3>
  <p>Por defecto el campo <strong>no acepta</strong> símbolos tipo <span class="mono">@ # $ % !</span> (sí letras con tilde, números, espacios, guion y apóstrofe). Para permitirlos: <strong>Ajustes → Permitir caracteres especiales en búsqueda</strong>.</p>
  <h3>Multi-select de proveedores</h3>
  <ol class="steps">
    <li>En Buscar toca el icono de proveedores.</li>
    <li>Marca solo las fuentes que quieras (puedes dejar varias).</li>
    <li>Confirma con OK / Aceptar.</li>
    <li>Busca: los chips de resultados deberían limitarse a esa selección.</li>
  </ol>
  {fig("13-dialogo-proveedores-buscar.png", "Diálogo de proveedores en Buscar")}
  {fig("07-buscar-resultados.png", "Resultados con filtros")}
</section>

<section id="historial">
  <h2>6. Módulo Historial</h2>
  <p><strong>Dónde:</strong> tab Historial. <strong>Código:</strong> <span class="mono">ui/history/HistoryScreen.kt</span>.</p>
  <ul>
    <li>Pills <strong>Leyendo</strong> y <strong>Visitadas</strong>.</li>
    <li>Agrupación temporal: Hoy, Ayer, Esta semana, Antes.</li>
    <li>Búsqueda dentro del historial.</li>
    <li>Opciones para limpiar / borrar entradas.</li>
  </ul>
  <p>Estado vacío: mensaje indicando que aún no hay novelas recientes.</p>
</section>

<section id="secciones">
  <h2>7. Módulo Secciones</h2>
  <p><strong>Dónde:</strong> tab <strong>Secciones</strong> (solo en Ultimate; no existe igual en la app stock).</p>
  <ul>
    <li>Organiza novelas de biblioteca en <strong>estantes / secciones</strong> (por ejemplo género, pendientes, favoritas).</li>
    <li>Usa la misma familia visual Fire que el resto de la app.</li>
    <li>Sirve para no mezclar todo en un solo listado plano de Biblioteca.</li>
  </ul>
  {fig("06-secciones.png", "Secciones")}
</section>

<section id="ajustes">
  <h2>8. Módulo Ajustes (todas las secciones)</h2>
  <p><strong>Dónde:</strong> tab Ajustes. <strong>Código:</strong> <span class="mono">ui/settings/SettingScreen.kt</span>.</p>
  <p>Los ajustes están agrupados. Desplázate de arriba a abajo:</p>

  <h3>8.1 Ajustes generales</h3>
  <table>
    <tr><th>Opción</th><th>Qué hace</th></tr>
    <tr><td>Idioma / Locale</td><td>Idioma de la interfaz (es, en, tr, ru…).</td></tr>
    <tr><td>Idiomas del proveedor</td><td>Qué idiomas de fuentes no inglesas se muestran.</td></tr>
    <tr><td>Mostrar proveedores en inglés</td><td>Off por defecto; actívalo para listar fuentes EN.</td></tr>
    <tr><td>Permitir caracteres especiales en búsqueda</td><td>Off por defecto; permite @#$% etc. en Buscar.</td></tr>
    <tr><td>Proveedores (lista)</td><td>Multi-select de qué proveedores integrados están activos.</td></tr>
  </table>

  <h3>8.2 Tema</h3>
  <ul>
    <li><strong>Tema</strong> de colores de la app (incluye Ultimate Blue).</li>
    <li><strong>Tema de iconos Fire</strong> (Blue/Black/Lava) — afecta iconos de menú, nav, secciones y launcher.</li>
    <li><strong>Icono de la app</strong> (launcher Blue/Black/Lava Fire).</li>
    <li><strong>Estilo del menú inferior</strong> (A/B/C/D).</li>
    <li><strong>Estilo de los ajustes</strong> (lista / mosaico / etc.).</li>
    <li><strong>Color primario</strong> y <strong>formato de rating</strong>.</li>
  </ul>

  <h3>8.3 Ajustes de la app</h3>
  <ul>
    <li>Comprobar actualización (versión actual {VERSION}).</li>
    <li>Instalador de APK.</li>
    <li>Mostrar actualizaciones de app (en Ultimate conviene dejarlo off para no bajar el APK stock).</li>
    <li><strong>Copia de seguridad</strong> y ruta de backup.</li>
    <li><strong>Restaurar</strong> desde archivo.</li>
  </ul>

  <h3>8.4 Nuevos capítulos</h3>
  <ul>
    <li>Notificar capítulos nuevos de novelas seguidas.</li>
    <li>Frecuencia (cada 3 / 6 / 12 / 24 h).</li>
    <li>Comprobar ahora.</li>
  </ul>

  <h3>8.5 Traducción (Gemini)</h3>
  <ul>
    <li>Obtener clave API (abre el sitio de Google AI).</li>
    <li>Pegar API key.</li>
    <li>Elegir modelo Gemini.</li>
    <li>Probar clave.</li>
  </ul>
  <p>Sirve para flujos de traducción al descargar/leer cuando está configurado.</p>

  <h3>8.6 Tutorial</h3>
  <p>Tres entradas con diálogos de ayuda: Proveedores propios, Descargas, TTS (detalle en sección 12).</p>

  <h3>8.7 Proveedores propios</h3>
  <p>Ver sección 9 completa.</p>

  <h3>8.8 Reader Settings</h3>
  <ul>
    <li><strong>Lector externo</strong> — usar otra app para abrir el libro.</li>
    <li><strong>Ruta de descarga</strong> — carpeta donde se guardan EPUB/PDF (ej. Download/My Novels/).</li>
  </ul>

  <h3>8.9 Información</h3>
  <ul>
    <li>Ver registros (Logcat) — útil para depurar; guardar / copiar / limpiar.</li>
    <li>Ubicación del logcat.</li>
  </ul>
  {fig("09-ajustes-tutorial-y-proveedores.png", "Ajustes: Tutorial y Proveedores propios")}
</section>

<section id="proveedores">
  <h2>9. Proveedores propios (agregar / administrar / avanzado)</h2>
  <p>Este es el módulo para <strong>añadir fuentes manualmente</strong> cuando el sitio no viene de fábrica.</p>
  <p><strong>Dónde:</strong> Ajustes → grupo <strong>Proveedores propios</strong>.</p>
  <p><strong>Código:</strong> <span class="mono">CustomProviderDialogs.kt</span>, <span class="mono">CustomProviders.kt</span>, <span class="mono">providers/CustomProvider*.kt</span>.</p>

  <h3>9.1 Agregar proveedor (modo simple)</h3>
  <ol class="steps">
    <li>Ajustes → <strong>Agregar proveedor</strong>.</li>
    <li>Elige tipo:
      <ul>
        <li><strong>Blog (Blogger / Blogspot)</strong> — ej. <span class="mono">miblog.blogspot.com</span>. Si hay “Lista de Novelas”, cada novela aparece por separado en Buscar/Proveedores con sus capítulos.</li>
        <li><strong>Sitio web (Madara / otro)</strong> — sitios WordPress/Madara de novelas; la app prueba plantillas habituales.</li>
      </ul>
    </li>
    <li>Escribe <strong>Nombre</strong>, <strong>Dirección del sitio</strong> e <strong>idioma</strong> (2 letras, ej. <span class="mono">es</span>).</li>
    <li>Toca <strong>VALIDAR</strong> y espera (puede tardar unos segundos).</li>
    <li>Si es OK, toca <strong>Agregar proveedor</strong>.</li>
  </ol>
  <div class="callout">Consejo: en blogs en español elige tipo Blog. Palabras inglesas tipo “the” casi no encuentran nada; la app prueba mejor con vacío o palabras en español.</div>
  {fig("10-dialogo-agregar-proveedor.png", "Formulario simple de alta")}

  <h3>9.2 Si la detección automática falla</h3>
  <ol class="steps">
    <li>En el mismo diálogo toca <strong>Avanzado</strong>.</li>
    <li>Rellena selectores CSS a mano (o usa plantilla Madara).</li>
    <li>Usa <strong>Probar</strong> con una palabra de prueba.</li>
    <li>Guarda cuando búsqueda → novela → capítulo respondan OK.</li>
  </ol>

  <h3>9.3 Campos del modo Avanzado (qué es cada uno)</h3>
  <table>
    <tr><th>Campo</th><th>Significado</th></tr>
    <tr><td>Nombre</td><td>Nombre único del proveedor (no repetir uno existente).</td></tr>
    <tr><td>URL del sitio</td><td>Base https://…</td></tr>
    <tr><td>Idioma</td><td>Código de 2 letras.</td></tr>
    <tr><td>URL de búsqueda</td><td>Debe incluir <span class="mono">{{query}}</span> donde va el texto buscado.</td></tr>
    <tr><td>Selector de cada resultado</td><td>CSS del ítem en la lista de búsqueda.</td></tr>
    <tr><td>Enlace dentro del resultado</td><td>Normalmente <span class="mono">a</span>; vacío = a.</td></tr>
    <tr><td>Portada en resultado</td><td>Opcional.</td></tr>
    <tr><td>Título / autor / portada / sinopsis de novela</td><td>Selectores en la página de la novela.</td></tr>
    <tr><td>Enlaces de capítulos</td><td>CSS de cada capítulo.</td></tr>
    <tr><td>Texto del capítulo</td><td>Contenedor del HTML del capítulo.</td></tr>
    <tr><td>Capítulos más nuevos primero</td><td>Checkbox si el sitio lista al revés.</td></tr>
    <tr><td>Ajax Madara</td><td>Checkbox para cargar capítulos vía ajax típico Madara.</td></tr>
    <tr><td>Palabra para probar</td><td>Query de test (en blogs suele usarse algo corto).</td></tr>
    <tr><td>Rellenar Madara</td><td>Autocompleta selectores típicos Madara.</td></tr>
  </table>

  <h3>9.4 Administrar proveedores</h3>
  <ol class="steps">
    <li>Ajustes → <strong>Administrar proveedores</strong>.</li>
    <li>Si no hay ninguno: mensaje “Aún no hay proveedores propios” y botón para agregar.</li>
    <li>Si hay lista: toca uno → <strong>Editar</strong> o <strong>Eliminar</strong>.</li>
    <li>Eliminar quita el proveedor; las novelas ya descargadas se conservan.</li>
  </ol>
  {fig("11-administrar-proveedores.png", "Administrar proveedores")}

  <h3>9.5 Dónde aparece después de agregarlo</h3>
  <ul>
    <li>En el diálogo de proveedores de <strong>Buscar</strong> (multi-select).</li>
    <li>En la lista de proveedores activos de Ajustes.</li>
    <li>Puedes buscar novelas de ese sitio igual que con los integrados.</li>
  </ul>
</section>

<section id="detalle">
  <h2>10. Detalle de novela, descarga y export</h2>
  <p>Al tocar un resultado de Buscar (o una tarjeta) entras al <strong>detalle</strong>:</p>
  <ul>
    <li>Portada, título, autor, sinopsis, lista de capítulos.</li>
    <li><strong>Leer en línea</strong> — abre el lector sin bajar todo.</li>
    <li><strong>Descargar</strong> — diálogo de descarga.</li>
  </ul>
  <h3>Diálogo de descarga (conceptos)</h3>
  <ul>
    <li>Todos los capítulos o un <strong>rango</strong> (ej. 1-10, 15).</li>
    <li><strong>Capítulos por archivo</strong>: vacío = un solo EPUB/PDF; número (ej. 50) = varios archivos.</li>
    <li>Formatos: capítulos sueltos / EPUB / PDF según el flujo actual.</li>
    <li>Vista previa editable en algunos flujos de export.</li>
    <li>Al terminar: aparece en Biblioteca; Actualizar trae capítulos nuevos; Borrar limpia archivos locales.</li>
  </ul>
  <div class="callout">En fuentes lentas conviene leer en línea. La traducción Gemini, si está activa, puede alargar mucho la descarga.</div>
</section>

<section id="lector">
  <h2>11. Lector y TTS</h2>
  <h3>Lector</h3>
  <ul>
    <li>Muestra el texto del capítulo; toca el centro para mostrar/ocultar barras.</li>
    <li>Barra inferior típica: capítulos, <strong>TTS</strong>, ajustes del lector (fuente, tema, voz…).</li>
    <li>Si “Lector externo” está activo, puede ofrecer abrir con otra app (sobre todo PDF/EPUB).</li>
  </ul>
  <h3>TTS (lectura en voz alta)</h3>
  <ol class="steps">
    <li>Abre un capítulo.</li>
    <li>Toca el icono de altavoz.</li>
    <li>Barra TTS: Reproducir/Pausa, Detener, frase anterior/siguiente.</li>
    <li>Ajustes del lector → pestaña Voz: idioma, voz, velocidad, tono, temporizador de sueño, bloquear área TTS.</li>
    <li>También se controla desde la notificación con pantalla apagada.</li>
  </ol>
  <p><strong>Voces recomendadas:</strong> Piper “Dave” (es) y “Lessac” (en) vía Sherpa-ONNX. Si falla, instala datos de Google TTS / idioma del sistema.</p>
</section>

<section id="tutoriales">
  <h2>12. Tutoriales oficiales de la app (co-tutorial)</h2>
  <p>En <strong>Ajustes → Tutorial</strong> hay tres guías embebidas (los mismos textos de la app):</p>

  <h3>12.1 Proveedores propios</h3>
  <ol class="steps">
    <li>Configuración → Proveedores propios → Agregar proveedor.</li>
    <li>Elige Blog o Sitio web; pon nombre y URL.</li>
    <li>Espera la verificación; si funciona, aparece en Buscar y Proveedores.</li>
    <li>Administrar para editar/borrar; Avanzado para CSS manual.</li>
  </ol>

  <h3>12.2 Descargas</h3>
  <ol class="steps">
    <li>Abre una novela → Descargar.</li>
    <li>Todos o rango (1-10, 15…).</li>
    <li>Capítulos por archivo opcional.</li>
    <li>Ruta/formato en Ajustes → lector / ruta de descarga.</li>
    <li>Abre desde Biblioteca; Actualizar / Borrar según necesites.</li>
  </ol>

  <h3>12.3 Lectura en voz alta (TTS)</h3>
  <ol class="steps">
    <li>Capítulo → altavoz.</li>
    <li>Usa barra TTS y notificación.</li>
    <li>Ajusta voz en engranaje → Voz.</li>
    <li>Instala Sherpa/Piper para mejor calidad.</li>
  </ol>
  <p>Esta documentación amplía esos tutoriales con el mapa de pantallas, campos avanzados y el resto de módulos que no caben en un diálogo.</p>
</section>

<section id="flujos">
  <h2>13. Flujos de uso paso a paso (para principiantes)</h2>
  <h3>A) Primera búsqueda y lectura</h3>
  <ol class="steps">
    <li>Tab Buscar → (opcional) elige proveedores → escribe título → buscar.</li>
    <li>Toca una portada → Leer en línea o un capítulo.</li>
    <li>Usa TTS si quieres escuchar.</li>
  </ol>
  <h3>B) Descargar para offline</h3>
  <ol class="steps">
    <li>Detalle → Descargar → elige rango/lotes → confirma.</li>
    <li>Espera → Biblioteca → abrir.</li>
  </ol>
  <h3>C) Importar tu EPUB/PDF</h3>
  <ol class="steps">
    <li>Biblioteca → Importar un libro → elige archivo.</li>
    <li>Aparece en el listado → Leer.</li>
  </ol>
  <h3>D) Añadir un blog o sitio tuyo</h3>
  <ol class="steps">
    <li>Ajustes → Agregar proveedor → Blog o Sitio → VALIDAR → Agregar.</li>
    <li>Buscar → activa ese proveedor → busca.</li>
  </ol>
  <h3>E) Seguir novedades</h3>
  <ol class="steps">
    <li>Marca Seguir en biblioteca / novela.</li>
    <li>Ajustes → Nuevos capítulos → notificar + frecuencia.</li>
  </ol>
</section>

<section id="capturas">
  <h2>14. Galería de capturas</h2>
  <div class="gallery">{gallery}</div>
</section>

<section id="bugs">
  <h2>15. Cómo crear bugs para entregar en Word</h2>
  <p>Plantilla: <span class="mono">plantillas/Plantilla-Reporte-Bugs.docx</span>.</p>
  <h3>Checklist antes de reportar</h3>
  <ol class="steps">
    <li>Confirmar APK {VERSION} (Ultimate, no stock).</li>
    <li>Anotar dispositivo/emulador y Android.</li>
    <li>Reproducir ≥ 2 veces.</li>
    <li>Captura o video + módulo exacto (Biblioteca, Buscar, Proveedores propios, TTS…).</li>
  </ol>
  <h3>Campos obligatorios por bug</h3>
  <table>
    <tr><th>Campo</th><th>Ejemplo</th></tr>
    <tr><td>ID</td><td>BUG-012</td></tr>
    <tr><td>Título</td><td>Proveedores propios: VALIDAR no termina en blogspot X</td></tr>
    <tr><td>Severidad</td><td>Alta</td></tr>
    <tr><td>Módulo</td><td>Proveedores propios</td></tr>
    <tr><td>Precondiciones</td><td>Sin proveedores propios previos; red OK</td></tr>
    <tr><td>Pasos</td><td>1. Ajustes→Agregar proveedor 2. Tipo Blog 3. URL … 4. VALIDAR</td></tr>
    <tr><td>Esperado / Actual</td><td>Mensaje OK vs error …</td></tr>
    <tr><td>Adjuntos</td><td>imagenes/evidencia-012.png</td></tr>
  </table>
  <p>Guarda el archivo como <span class="mono">Bugs-QuickNovel-Ultimate-{VERSION}-AAAA-MM-DD.docx</span>.</p>
</section>

<section id="qa">
  <h2>16. Pruebas automatizadas</h2>
  <ul>
    <li>Unitarias: <span class="mono">./gradlew :app:testDebugUnitTest</span></li>
    <li>E2E: carpeta <span class="mono">e2e/</span> (specs 01–09: biblioteca, nav, search, historial, ajustes, import, reader/TTS con seed, providers, special chars).</li>
    <li>Allure: <span class="mono">e2e/allure-report</span></li>
  </ul>
</section>

<section id="anexos">
  <h2>17. Anexos</h2>
  <ul>
    <li>Repo: https://github.com/RaffyRod/NovelReader</li>
    <li>Upstream: https://github.com/LagradOst/QuickNovel</li>
    <li>Package Ultimate: <span class="mono">{APP_ID}</span></li>
    <li>Esta carpeta incluye APK {VERSION}, manual HTML/DOCX, plantilla de bugs e imágenes.</li>
  </ul>
</section>
</main>
<footer>Documento generado para QuickNovel Ultimate {VERSION} · {HOY} · Pensado para onboarding de usuarios nuevos</footer>
</body>
</html>"""


def set_run_font(run, size=11, bold=False, color=None):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.name = "Calibri"
    rFonts = run._element.get_or_add_rPr().get_or_add_rFonts()
    rFonts.set(qn("w:eastAsia"), "Calibri")
    if color:
        run.font.color.rgb = color


def add_para(doc, text, bold=False):
    p = doc.add_paragraph()
    r = p.add_run(text)
    set_run_font(r, 11, bold=bold)
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
                set_run_font(r, 10, bold=True)
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
    r = t.add_run(f"Manual completo QuickNovel Ultimate {VERSION}")
    set_run_font(r, 20, True, RGBColor(26, 102, 179))
    s = doc.add_paragraph()
    s.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = s.add_run(f"Onboarding + módulos + proveedores propios + bugs · {HOY}")
    set_run_font(r, 11, color=RGBColor(90, 106, 122))
    add_para(doc, f"Package: {APP_ID}")
    add_para(doc, f"APK: {APK_NAME}")

    doc.add_heading("Índice", 1)
    numbers(
        doc,
        [
            "Introducción",
            "Instalación",
            "Mapa de navegación",
            "Biblioteca",
            "Buscar",
            "Historial",
            "Secciones",
            "Ajustes (todas las secciones)",
            "Proveedores propios",
            "Detalle / descarga",
            "Lector y TTS",
            "Tutoriales oficiales",
            "Flujos paso a paso",
            "Capturas",
            "Bugs en Word",
            "QA y anexos",
        ],
    )

    doc.add_heading("1. Introducción", 1)
    add_para(
        doc,
        "QuickNovel Ultimate busca, lee, escucha (TTS) y descarga novelas; también importa EPUB/PDF. Fork con temas Fire, package com.lagradost.quicknovel.ultimate, historial Leyendo/Visitadas, Import-B, proveedores propios y búsqueda sin caracteres especiales por defecto.",
    )
    bullets(
        doc,
        [
            "No confundir con QuickNovel stock (sin Secciones Ultimate / Import-B).",
            "Si pide update a 3.x upstream: Skip update.",
        ],
    )

    doc.add_heading("2. Instalación", 1)
    numbers(
        doc,
        [
            "Desinstalar versiones viejas",
            f"Instalar {APK_NAME}",
            "Conceder notificaciones si aplica",
            "Abrir QuickNovel Ultimate",
        ],
    )

    doc.add_heading("3. Mapa de navegación", 1)
    table(
        doc,
        ["Tab", "Función"],
        [
            ["Biblioteca", "Libros locales, Import-B, ordenar"],
            ["Buscar", "Proveedores web, populares, resultados"],
            ["Historial", "Leyendo / Visitadas"],
            ["Ajustes", "Toda la configuración"],
            ["Secciones", "Estantes de biblioteca"],
        ],
    )

    doc.add_heading("4. Biblioteca", 1)
    bullets(
        doc,
        [
            "Buscar mis novelas (filtro local, barra fijada).",
            "Import-B: EPUB/PDF.",
            "Continuar leyendo, Seguir, Todas las novelas, progreso de capítulos.",
            "FAB Ordenar; vista lista/grid.",
        ],
    )

    doc.add_heading("5. Buscar", 1)
    bullets(
        doc,
        [
            "Campo Buscar novelas…; tune = multi-select proveedores.",
            "Recientes, Populares, chips de filtro en resultados.",
            "Sin especiales por defecto (@#$); toggle en Ajustes.",
        ],
    )

    doc.add_heading("6. Historial", 1)
    add_para(doc, "Pills Leyendo/Visitadas; grupos Hoy/Ayer/Esta semana/Antes; limpiar historial.")

    doc.add_heading("7. Secciones", 1)
    add_para(doc, "Tab exclusivo Ultimate para organizar la biblioteca en estantes/secciones.")

    doc.add_heading("8. Ajustes (todas las secciones)", 1)
    doc.add_heading("General", 2)
    bullets(
        doc,
        [
            "Idioma UI",
            "Idiomas de proveedor + mostrar inglés",
            "Permitir caracteres especiales en búsqueda",
            "Lista de proveedores activos",
        ],
    )
    doc.add_heading("Tema", 2)
    bullets(doc, ["Tema", "Iconos Fire", "Icono launcher", "Estilo nav", "Estilo ajustes", "Color primario", "Rating"])
    doc.add_heading("App", 2)
    bullets(doc, ["Check update", "Instalador APK", "Mostrar updates", "Backup", "Restaurar"])
    doc.add_heading("Nuevos capítulos", 2)
    bullets(doc, ["Notificar", "Frecuencia 3/6/12/24 h", "Comprobar ahora"])
    doc.add_heading("Traducción Gemini", 2)
    bullets(doc, ["Obtener clave", "API key", "Modelo", "Probar clave"])
    doc.add_heading("Tutorial", 2)
    bullets(doc, ["Proveedores propios", "Descargas", "TTS"])
    doc.add_heading("Proveedores propios", 2)
    add_para(doc, "Agregar proveedor + Administrar (detalle en §9).")
    doc.add_heading("Reader / Info", 2)
    bullets(doc, ["Lector externo", "Ruta de descarga", "Logcat ver/guardar/copiar"])

    doc.add_heading("9. Proveedores propios", 1)
    doc.add_heading("Modo simple", 2)
    numbers(
        doc,
        [
            "Ajustes → Agregar proveedor",
            "Elegir Blog (Blogger) o Sitio web (Madara)",
            "Nombre + URL + idioma",
            "VALIDAR → Agregar proveedor",
        ],
    )
    doc.add_heading("Modo avanzado (CSS)", 2)
    add_para(doc, "Botón Avanzado: URL de búsqueda con {query}, selectores de resultado, novela, capítulos, contenido, flags Madara ajax / newest first, Probar y plantilla Madara.")
    table(
        doc,
        ["Campo avanzado", "Uso"],
        [
            ["URL búsqueda + {query}", "Dónde se busca"],
            ["resultItem / resultLink / resultImage", "Lista de resultados"],
            ["novelTitle / Author / Cover / Synopsis", "Página de novela"],
            ["chapterLink / content", "Capítulos y texto"],
            ["Ajax Madara / newest first", "Comportamiento del listado"],
        ],
    )
    doc.add_heading("Administrar", 2)
    bullets(doc, ["Lista de propios", "Editar / Eliminar", "Eliminar no borra descargas locales"])
    img(doc, "10-dialogo-agregar-proveedor.png", "Agregar proveedor")
    img(doc, "11-administrar-proveedores.png", "Administrar")

    doc.add_heading("10. Detalle y descarga", 1)
    bullets(
        doc,
        [
            "Leer en línea vs Descargar",
            "Rangos y capítulos por archivo",
            "EPUB/PDF según flujo",
            "Actualizar / Borrar desde biblioteca",
        ],
    )

    doc.add_heading("11. Lector y TTS", 1)
    numbers(
        doc,
        [
            "Abrir capítulo",
            "Altavoz = TTS",
            "Pausa / stop / frases",
            "Ajustes → Voz (idioma, velocidad, sueño)",
            "Preferir Piper Dave ES / Lessac EN con Sherpa",
        ],
    )

    doc.add_heading("12. Tutoriales oficiales", 1)
    add_para(doc, "Los diálogos de Ajustes → Tutorial (Proveedores, Descargas, TTS) son la guía corta de la app; este manual los amplía con el resto de módulos.")

    doc.add_heading("13. Flujos paso a paso", 1)
    doc.add_heading("Primera lectura", 2)
    numbers(doc, ["Buscar", "Elegir proveedores", "Abrir resultado", "Leer en línea"])
    doc.add_heading("Importar EPUB", 2)
    numbers(doc, ["Biblioteca → Importar un libro", "Elegir archivo", "Abrir en listado"])
    doc.add_heading("Agregar blog/sitio", 2)
    numbers(doc, ["Ajustes → Agregar proveedor", "VALIDAR", "Buscar con ese proveedor"])

    doc.add_heading("14. Capturas", 1)
    for f, c in IMAGENES:
        img(doc, f, c)

    doc.add_heading("15. Bugs para Word", 1)
    add_para(doc, "Usar plantillas/Plantilla-Reporte-Bugs.docx. Campos: ID, título, severidad, módulo, entorno, precondiciones, pasos, esperado, actual, adjuntos.")
    numbers(
        doc,
        [
            f"Confirmar APK {VERSION}",
            "Reproducir 2+ veces",
            "Pasos numerados una acción cada uno",
            f"Guardar Bugs-QuickNovel-Ultimate-{VERSION}-AAAA-MM-DD.docx",
        ],
    )

    doc.add_heading("16. QA y anexos", 1)
    bullets(
        doc,
        [
            "Unit: gradlew :app:testDebugUnitTest",
            "E2E e2e/ specs 01-09 + Allure",
            "https://github.com/RaffyRod/NovelReader",
            APP_ID,
        ],
    )

    out = ROOT / f"Manual-QuickNovel-Ultimate-{VERSION}.docx"
    doc.save(out)
    return out


def build_bug_template():
    doc = Document()
    t = doc.add_paragraph()
    t.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = t.add_run(f"Plantilla de bugs — QuickNovel Ultimate {VERSION}")
    set_run_font(r, 18, True, RGBColor(26, 102, 179))
    add_para(doc, "Fecha: ________  Tester: ________  Dispositivo: ________  Android: ________")
    add_para(doc, f"APK: {APK_NAME}")
    doc.add_heading("Cómo completar", 1)
    numbers(
        doc,
        [
            "Un bug por bloque",
            "Indicar módulo (Biblioteca, Buscar, Proveedores propios, TTS, etc.)",
            "Pasos reproducibles",
            "Adjuntar captura",
        ],
    )
    for i in range(1, 6):
        doc.add_heading(f"Bug BUG-{i:03d}", 1)
        table(
            doc,
            ["Campo", "Contenido"],
            [
                ["Título", ""],
                ["Severidad", "Bloqueante / Alta / Media / Baja"],
                [
                    "Módulo",
                    "Biblioteca / Buscar / Historial / Secciones / Ajustes / Proveedores propios / Detalle / Descarga / Lector / TTS / Import / Traducción",
                ],
                ["Entorno", f"Ultimate {VERSION}"],
                ["Precondiciones", ""],
                ["Pasos", "1.\n2.\n3."],
                ["Esperado", ""],
                ["Actual", ""],
                ["Adjuntos", ""],
                ["Estado", "Nuevo"],
            ],
        )
    doc.add_heading("Resumen", 1)
    table(
        doc,
        ["ID", "Título", "Severidad", "Módulo", "Estado"],
        [[f"BUG-{i:03d}", "", "", "", ""] for i in range(1, 6)],
    )
    out = PLANTILLAS / "Plantilla-Reporte-Bugs.docx"
    doc.save(out)
    return out


def main():
    PLANTILLAS.mkdir(exist_ok=True)
    IMG.mkdir(exist_ok=True)
    html = ROOT / f"Manual-QuickNovel-Ultimate-{VERSION}.html"
    html.write_text(build_html(), encoding="utf-8")
    print("HTML", html)
    print("DOCX", build_manual_docx())
    print("BUGS", build_bug_template())
    for src in [
        Path(r"C:\Users\Raffy Rodriguez\Desktop") / APK_NAME,
        ROOT / APK_NAME,
    ]:
        if src.exists() and src.parent != ROOT:
            shutil.copy2(src, ROOT / APK_NAME)
            print("APK", ROOT / APK_NAME)
            break
    (ROOT / "LEEME.txt").write_text(
        f"""QuickNovel Ultimate {VERSION} — documentación completa
=====================================================
Incluye:
- {APK_NAME}
- Manual HTML y Word (índice amplio: Biblioteca, Buscar, Historial, Secciones,
  Ajustes completos, Proveedores propios simple/avanzado, descarga, TTS, tutoriales,
  flujos para principiantes, capturas, bugs)
- plantillas/Plantilla-Reporte-Bugs.docx
- imagenes/

Abre el HTML en el navegador o el DOCX en Word.
""",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
