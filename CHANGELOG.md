# Changelog

All notable changes to **QuickNovel Ultimate** are listed here.
Manual **Release → Telegram** auto-bumps `versionName` / `versionCode` when there are
new commits since the last `v2.*` tag, then posts style **D** from `### Added` / `### Fixed`.

## 2.0.1

### Added
- Catalog **Seleccionar todos / ninguno** inside the dialog (was a hidden Neutral button)
- Name-only catalog filter + paging hint
- Scrollable **Administrar proveedores** (no ~56 setItems cap)
- E2E catalog suite (`15-plugin-catalog`) · reader tabs via resource-id
- Release → Telegram requires green **CI Android** (e2e) on main

### Fixed
- Select-all / full catalog paging UX
- Duplicate Search provider names in Settings
- SharedPreferences StringSet copies for custom providers
- Catalog no longer auto-applies Madara/Blogger CSS (add via URL/JSON)

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
