# Changelog

All notable changes to **QuickNovel Ultimate** are listed here.
Manual **Release → Telegram** auto-bumps `versionName` / `versionCode` when there are
new commits since the last `v2.*` tag, then posts style **D** from `### Added` / `### Fixed`.

## 2.0.7

### Added
- about privacy sheet

### Fixed
- tutorial TTS dialog
- settings split e2e
- find dark mode e2e

### Notes
- versionCode 207

## 2.0.6

### Added
- Settings hub **Configuración / Administración** (mock 9 split)
- CI Unit ∥ Build → E2E gates (smoke then general, one emulator boot)
- Allure **single-file** HTML report (doble clic)

### Fixed
- Same-URL provider toggles stay independent
- E2E download-translate timeout / chapter-numbers empty TOC
- GitHub Actions Node 24 deprecations · pin `ubuntu-24.04`

### Notes
- Package: `com.ultimateSoft.quicknovel.ultimate` · versionCode 206
- New package id — install as new app (does not update `com.lagradost.quicknovel.ultimate`)

## 2.0.3

### Fixed
- CI emu cold boot
- Fuentes delete visible
- Fuentes TTS e2e

### Notes
- versionCode 203

## 2.0.2

### Added
- cut e2e to ~10m
- README 2.0.0 shots
- manual release only

### Fixed
- Appium session timeout
- android dir ownership
- CI disk for emu cache
- e2e ignore prep fail
- emulator cache prep
- CI abiFilter x86_64
- tutorial provider hint
- TTS tutorial e2e
- UI + CI e2e split
- manage proveedores
- ci catalog e2e
- catalog 2.0.1

### Notes
- versionCode 202

## 2.0.1

### Added
- Catalog **Seleccionar todos / ninguno** inside the dialog (was a hidden Neutral button)
- Name-only catalog filter + paging hint
- Scrollable **Administrar proveedores** (no ~56 setItems cap)
- Settings group **accent rail** headers · Tutorial under Información
- Plugin install **Glyph Pulse (本)** + live search in hub/catalog/proveedores
- Plugins → Buscar **como LNReader**: instalar (con URL) activa Buscar; JS usa puente Madara
- **Fuentes de Buscar** lista unificada (built-in + plugins + propios · Activas/Instaladas/Todas)
- E2E catalog suite (`15-plugin-catalog`) · reader tabs via resource-id
- CI features split (`features` / `features-plugins` / `features-flows`) + AVD cache
- Release → Telegram requires green **CI Android** (e2e) on main

### Fixed
- Select-all / full catalog paging UX
- Duplicate Search provider names in Settings
- SharedPreferences StringSet copies for custom providers
- E2E main-chrome recovery after plugin dialogs (nav tabs missing)

### Notes
- Package: `com.lagradost.quicknovel.ultimate` · versionCode 201

## 2.0.0

### Added
- Plugin language picker (Continuar)
- In-place catalog paging
- NEW pill on new Search providers
- Sections Traducciones (Translate icon)
- CI Release → Telegram

### Fixed
- Go-to-page typo jump
- Translation history icon
- Fake NEW cover glyph

### Notes
- Defaults: dark mode + Ultimate Blue · English providers ON
- Package: `com.lagradost.quicknovel.ultimate` · versionCode 200

## 1.2.9

### Added
- Plugin hub improvements (counts, catalog return after install)
- Cover placeholder (option J) in Search
- Translation history + plugin repo URL history

### Fixed
- Catalog bounce / edit flows for plugins
