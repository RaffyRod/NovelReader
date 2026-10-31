# 本 QuickNovel Ultimate

> Busca, lee, escucha y descarga novelas — gratis, sin anuncios, open source.

| 🌐 Idioma docs | 🔗 Enlace |
|----------------|-----------|
| 🇪🇸 Español | [Manual completo](docs/manual-1.2.3/es/) |
| 🇬🇧 English | [Full manual](docs/manual-1.2.3/en/) |
| 🇷🇺 Русский | [README_RU](README_RU.md) |

Fork de [QuickNovel](https://github.com/LagradOst/QuickNovel) con marca Ultimate, temas, Import-B, Secciones, proveedores propios, plugins y más QA.

---

## ⚡ De un vistazo

| | |
|---|---|
| 🏷️ **Versión** | **1.2.3** (`versionCode` 123) |
| 📦 **Package** | `com.lagradost.quicknovel.ultimate` |
| 🌙 **Por defecto** | Modo oscuro **ON** · tema **Ultimate Blue** |
| 📥 **Descarga** | [Releases](https://github.com/RaffyRod/NovelReader/releases) · APK universal |
| 🧑‍💻 **Repo** | https://github.com/RaffyRod/NovelReader |
| ⬆️ **Upstream** | https://github.com/LagradOst/QuickNovel |

### ✅ Primeros pasos (2 minutos)

| Paso | Qué hacer |
|------|-----------|
| 1️⃣ | Desinstala Ultimate viejo / QuickNovel stock si los tienes |
| 2️⃣ | Instala `QuickNovel-Ultimate-1.2.3-universal.apk` |
| 3️⃣ | Si pide actualizar a 3.x de LagradOst → **Cancelar / Skip** |
| 4️⃣ | Abre **QuickNovel Ultimate** (orb + 本) |
| 5️⃣ | Idioma de la app: **Español** por defecto (Ajustes) |

> 💡 Tip: si ves dos iconos “QuickNovel”, usa siempre el de **Ultimate**.

---

## ✨ Qué puedes hacer

| Área | Destacados |
|------|------------|
| 🎨 **Look** | Ultimate Blue + modo oscuro; 11 acentos; estilos de icono A–J |
| 📚 **Biblioteca** | Búsqueda local fija, seguir, Import-B (EPUB/PDF) |
| 🧭 **Secciones** | Estantes de biblioteca (solo Ultimate) |
| 🕘 **Historial** | **Leyendo** / **Visitadas** con grupos por fecha |
| 🔎 **Buscar** | Multi-proveedor, chips, sin caracteres especiales por defecto |
| 🔌 **Fuentes** | Proveedores propios (Blog/Madara) + **Plugins LNReader** |
| 📖 **Lector** | **Glyph Pulse** (本) al cargar — textos en el idioma de la app, sin URL cruda |
| ⬇️ **Descarga** | Rangos, capítulos/archivo, idioma vía Gemini / Google / Original |
| 🔊 **TTS** | Voces Piper/Sherpa (Dave ES / Lessac EN) + notificación |
| 🧪 **QA** | Unit + E2E Appium (specs 01–13) + CI en cada push |

---

## 📱 Pantallas

<p>
<img src="./.github/ultimate/library.png" height="360" alt="Biblioteca"/>
<img src="./.github/ultimate/search.png" height="360" alt="Buscar"/>
<img src="./.github/ultimate/search-results.png" height="360" alt="Resultados"/>
<img src="./.github/ultimate/history.png" height="360" alt="Historial"/>
</p>
<p>
<img src="./.github/ultimate/sections.png" height="360" alt="Secciones"/>
<img src="./.github/ultimate/settings-providers.png" height="360" alt="Ajustes"/>
<img src="./.github/ultimate/add-provider.png" height="360" alt="Agregar proveedor"/>
<img src="./.github/ultimate/provider-multiselect.png" height="360" alt="Multi-select"/>
</p>

Más capturas → [`docs/manual-1.2.3/es/imagenes/`](docs/manual-1.2.3/es/imagenes/)

---

## 📖 Manuales y tutoriales

| 🌍 | Formato | Ruta |
|----|---------|------|
| 🇪🇸 | HTML + Word + plantilla bugs | [`docs/manual-1.2.3/es/`](docs/manual-1.2.3/es/) |
| 🇬🇧 | HTML + Word + bug template | [`docs/manual-1.2.3/en/`](docs/manual-1.2.3/en/) |
| 🗂️ | Índice docs | [`docs/README.md`](docs/README.md) |
| 🎨 | Mocks HTML (temas, Glyph Pulse, plugins…) | [`docs/previews/`](docs/previews/) |

En la app: **Ajustes → Tutorial** (Proveedores · Descargas · TTS).

Pack Desktop típico: `QuickNovel-Ultimate-1.2.3-Docs/` (APK + manuals + previews).

---

## 🛠️ Build rápido

```bash
./gradlew :app:assembleRelease
```

| Comando | Para qué |
|---------|----------|
| `./gradlew :app:assembleRelease` | APK release |
| `./gradlew :app:testDebugUnitTest` | Unit tests |
| `cd e2e && npm test` | Suite E2E completa |
| `npm run test:smoke` (en `e2e/`) | Smoke 01–04 |
| `python docs/manual-1.2.3/_generar_docs_es.py` | Regenerar manual ES |

Firma el APK universal en `app/build/outputs/apk/release/` o usa el del Desktop.  
Detalle E2E → [`e2e/README.md`](e2e/README.md) · CI → [`.github/workflows/e2e-android.yml`](.github/workflows/e2e-android.yml)

---

## 🧰 Stack (resumen)

| Capa | Tecnología |
|------|------------|
| 💻 Lenguaje | Kotlin |
| 🖼️ UI | Jetpack Compose (M3) + Views legacy |
| 🌐 Red | OkHttp / NiceHttp, Jsoup |
| 🖼️ Imágenes | Coil 3 |
| 📖 Lector | EPUB + PDFBox |
| 🔊 TTS | Android TTS + Sherpa/Piper |
| 🌐 Traducción | ML Kit + Gemini opcional |
| 🧪 Tests | JUnit · WebdriverIO 9 · Appium · Allure |

---

## 🌐 Proveedores

Incluye fuentes como SkyNovels, Nova, DevilNovels, Royal Road, Scribblehub, NovelFull y más (`Apis.kt`).  
También: **proveedores propios** (Blog / Sitio) y **plugins LNReader** desde Ajustes.

---

## 👏 Créditos

Basado en QuickNovel de [LagradOst](https://github.com/LagradOst/QuickNovel).  
Discord upstream: https://discord.gg/5Hus6fM

---

## ⚖️ Aviso legal

Cualquier tema legal sobre el contenido debe tratarse con los hosts / proveedores; no estamos afiliados a ellos.

En caso de infracción de copyright, contacta directamente a las partes responsables o a los sitios.

Uso **educativo y personal**. QuickNovel no aloja contenido: agrega enlaces públicos como un buscador. El usuario es responsable de cumplir las leyes de su localidad. Úsala bajo tu propio riesgo.
